/* sbt -- Simple Build Tool
 * Copyright 2009, 2010  Mark Harrah
 */
package sbt
package compiler

import java.io.File
import scala.util.Try

object ComponentCompiler {
  val xsbtiID = "xsbti"
  val srcExtension = "-src"
  val binSeparator = "-bin_"
  val compilerInterfaceID = "compiler-bridge"
  val compilerInterfaceSrcID = compilerInterfaceID + srcExtension
  val javaVersion = System.getProperty("java.class.version")
  val sbtVersion = "0.13.9-SNAPSHOT" // TODO: Shouldn't be hardcoded...

  def interfaceProvider(manager: ComponentManager): CompilerInterfaceProvider = new CompilerInterfaceProvider {
    def apply(scalaInstance: xsbti.compile.ScalaInstance, log: Logger): File =
      {
        // this is the instance used to compile the interface component
        val componentCompiler = new ComponentCompiler(new RawCompiler(scalaInstance, ClasspathOptions.auto, log), manager)
        log.debug("Getting " + compilerInterfaceID + " from component compiler for Scala " + scalaInstance.version)
        componentCompiler(compilerInterfaceID)
      }
  }

  def interfaceProvider(manager: ComponentManager, ivyConfiguration: IvyConfiguration): CompilerInterfaceProvider = new CompilerInterfaceProvider {
    def apply(scalaInstance: xsbti.compile.ScalaInstance, log: Logger): File =
      {
        // this is the instance used to compile the interface component
        val componentCompiler = new NewComponentCompiler(new RawCompiler(scalaInstance, ClasspathOptions.auto, log), manager, ivyConfiguration, log)
        log.debug("Getting " + compilerInterfaceID + " from component compiler for Scala " + scalaInstance.version)
        componentCompiler(compilerInterfaceID)
      }
  }

  /**
   * Generates a sequence of version of numbers, from more to less specific from `fullVersion`.
   */
  private[sbt] def cascadingSourceModuleVersions(fullVersion: String): Seq[String] = {

    scala.util.Try {
      val VersionNumber(numbers @ maj +: min +: _, tags, _) = VersionNumber(fullVersion)
      Seq(fullVersion,
        numbers.mkString(".") + (if (tags.nonEmpty) tags.mkString("-", "-", "") else ""),
        numbers.mkString("."),
        s"$maj.$min"
      ).distinct
    } getOrElse {
      Seq(fullVersion)
    }
  }
}

/**
 * This class provides source components compiled with the provided RawCompiler.
 * The compiled classes are cached using the provided component manager according
 * to the actualVersion field of the RawCompiler.
 */
class ComponentCompiler(compiler: RawCompiler, manager: ComponentManager) {
  import ComponentCompiler._
  def apply(id: String): File =
    try { getPrecompiled(id) }
    catch { case _: InvalidComponent => getLocallyCompiled(id) }

  /**
   * Gets the precompiled (distributed with sbt) component with the given 'id'
   * If the component has not been precompiled, this throws InvalidComponent.
   */
  def getPrecompiled(id: String): File = manager.file(binaryID(id, false))(IfMissing.Fail)
  /**
   * Get the locally compiled component with the given 'id' or compiles it if it has not been compiled yet.
   * If the component does not exist, this throws InvalidComponent.
   */
  def getLocallyCompiled(id: String): File =
    {
      val binID = binaryID(id, true)
      manager.file(binID)(new IfMissing.Define(true, compileAndInstall(id, binID)))
    }
  def clearCache(id: String): Unit = manager.clearCache(binaryID(id, true))
  protected def binaryID(id: String, withJavaVersion: Boolean) =
    {
      val base = id + binSeparator + compiler.scalaInstance.actualVersion
      if (withJavaVersion) base + "__" + javaVersion else base
    }
  protected def compileAndInstall(id: String, binID: String) {
    val srcID = id + srcExtension
    IO.withTemporaryDirectory { binaryDirectory =>
      val targetJar = new File(binaryDirectory, id + ".jar")
      val xsbtiJars = manager.files(xsbtiID)(IfMissing.Fail)
      AnalyzingCompiler.compileSources(manager.files(srcID)(IfMissing.Fail), targetJar, xsbtiJars, id, compiler, manager.log)
      manager.define(binID, Seq(targetJar))
    }
  }
}

private[compiler] class NewComponentCompiler(compiler: RawCompiler, manager: ComponentManager, ivyConfiguration: IvyConfiguration, log: Logger) {
  import ComponentCompiler._

  private val ivySbt: IvySbt = new IvySbt(ivyConfiguration)

  def apply(id: String): File = {
    val binID = binaryID(id)
    manager.file(binID)(new IfMissing.Define(true, compileAndInstall(id, binID)))
  }

  private def binaryID(id: String): String = {
    val base = id + binSeparator + compiler.scalaInstance.actualVersion
    base + "__" + javaVersion
  }

  private def compileAndInstall(id: String, binID: String): Unit = {
    val srcID = id + srcExtension
    def interfaceSources(moduleVersions: Seq[String]): File =
      moduleVersions match {
        case Seq() =>
          log.debug(s"Fetching default sources: $id")
          val jarName = s"$srcID-$sbtVersion.jar"
          update(getModule(id))(_.getName == jarName) getOrElse (throw new InvalidComponent(s"Couldn't retrieve default sources: file '$jarName' in module '$id'"))

        case version +: rest =>
          log.debug(s"Fetching version-specific sources: ${id}_$version")
          val moduleName = s"${id}_$version"
          val jarName = s"${srcID}_$version-$sbtVersion.jar"
          update(getModule(moduleName))(_.getName == jarName) getOrElse interfaceSources(rest)
      }
    IO.withTemporaryDirectory { binaryDirectory =>

      val targetJar = new File(binaryDirectory, s"$binID.jar")
      val xsbtiJars = manager.files(xsbtiID)(IfMissing.Fail)

      val sourceModuleVersions = cascadingSourceModuleVersions(compiler.scalaInstance.actualVersion)
      AnalyzingCompiler.compileSources(Seq(interfaceSources(sourceModuleVersions)), targetJar, xsbtiJars, id, compiler, log)

      manager.define(binID, Seq(targetJar))

    }
  }

  /**
   * Returns a module that corresponds to "org.scala-sbt" % `id` % `sbtVersion`
   * Note: Actually, it returns a module that has the requested module as dependency, because sbt's
   * ivy implementation assumes that the root module is an sbt project, and thus doesn't download it.
   */
  private def getModule(id: String): ivySbt.Module = {
    val dummyID = ModuleID(xsbti.ArtifactInfo.SbtOrganization, scala.util.Random.alphanumeric take 20 mkString "", sbtVersion, Some("component"))
    val moduleID = ModuleID(xsbti.ArtifactInfo.SbtOrganization, id, sbtVersion, Some("component"))
    getModule(dummyID, Seq(moduleID))
  }

  private def getModule(moduleID: ModuleID, deps: Seq[ModuleID], uo: UpdateOptions = UpdateOptions()): ivySbt.Module = {
    val moduleSetting = InlineConfiguration(
      module = moduleID,
      moduleInfo = ModuleInfo(moduleID.name),
      dependencies = deps,
      configurations = Seq(Configurations.Component),
      ivyScala = None)

    new ivySbt.Module(moduleSetting)
  }

  private def dependenciesNames(module: ivySbt.Module): String = module.moduleSettings match {
    // `module` is a dummy module, we will only fetch its dependencies.
    case ic: InlineConfiguration =>
      ic.dependencies map {
        case mID: ModuleID =>
          import mID._
          s"$organization % $name % $revision"
      } mkString ", "
    case _ =>
      s"unknown"
  }

  private def update(module: ivySbt.Module)(predicate: File => Boolean): Option[File] = {

    val retrieveDirectory = new File(ivyConfiguration.baseDirectory, "component")
    val retrieveConfiguration = new RetrieveConfiguration(retrieveDirectory, Resolver.defaultRetrievePattern, false)
    val updateConfiguration = new UpdateConfiguration(Some(retrieveConfiguration), true, UpdateLogging.DownloadOnly)

    log.info(s"Attempting to fetch ${dependenciesNames(module)}. This operation may fail.")
    IvyActions.updateEither(module, updateConfiguration, UnresolvedWarningConfiguration(), LogicalClock.unknown, None, log) match {
      case Left(unresolvedWarning) =>
        None

      case Right(updateReport) =>
        val files =
          for {
            conf <- updateReport.configurations
            m <- conf.modules
            (_, f) <- m.artifacts
            if predicate(f)
          } yield f
        files.headOption

    }
  }
}
