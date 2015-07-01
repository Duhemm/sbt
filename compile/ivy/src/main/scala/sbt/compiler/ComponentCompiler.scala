/* sbt -- Simple Build Tool
 * Copyright 2009, 2010  Mark Harrah
 */
package sbt
package compiler

import java.io.File

object ComponentCompiler {
  val xsbtiID = "xsbti"
  val srcExtension = "-src"
  val binSeparator = "-bin_"
  val compilerInterfaceID = "compiler-interface"
  val compilerInterfaceSrcID = compilerInterfaceID + srcExtension
  val javaVersion = System.getProperty("java.class.version")

  def interfaceProvider(manager: ComponentManager): CompilerInterfaceProvider = new CompilerInterfaceProvider {
    def apply(scalaInstance: xsbti.compile.ScalaInstance, log: Logger): File =
      {
        // this is the instance used to compile the interface component
        val componentCompiler = new ComponentCompiler(new RawCompiler(scalaInstance, ClasspathOptions.auto, log), manager)
        log.debug("Getting " + compilerInterfaceID + " from component compiler for Scala " + scalaInstance.version)
        componentCompiler(compilerInterfaceID)
      }
  }

  /**
   * Generates a sequence of version of numbers, from more to less specific from `fullVersion`.
   * `fullVersion` is expected to match Scala's versioning scheme (maj.min.rev[-XXX]).
   */
  private[sbt] def cascadingSourceModuleVersions(fullVersion: String): Seq[String] = {
    val VersionParts = """(\d+)\.(\d+)\.(\d+)(?:-(.+?))?""".r

    fullVersion match {
      case VersionParts(major, minor, revision, null) =>
        Seq(s"$major.$minor.$revision", s"$major.$minor")
      case VersionParts(major, minor, revision, rest) =>
        Seq(s"$major.$minor.$revision-$rest", s"$major.$minor.$revision", s"$major.$minor")
      case unrecognized =>
        // If we cannot recognize the version of the scala instance, then we will try to find
        // a perfect match for the compiler interface, or fallback to the default sources.
        Seq(unrecognized)
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
      def interfaceSources(moduleVersions: Seq[String]): Iterable[File] =
        moduleVersions match {
          case Seq() =>
            manager.log.debug(s"Fetching default sources: $srcID")
            manager.files(srcID)(IfMissing.Fail)
          case version +: rest =>
            manager.log.debug(s"Fetching version-specific sources: ${srcID}_$version")
            manager.files(srcID + "_" + version)(new IfMissing.Fallback(interfaceSources(rest)))
        }
      val targetJar = new File(binaryDirectory, id + ".jar")
      val xsbtiJars = manager.files(xsbtiID)(IfMissing.Fail)

      val sourceModuleVersions = cascadingSourceModuleVersions(compiler.scalaInstance.actualVersion)
      AnalyzingCompiler.compileSources(interfaceSources(sourceModuleVersions), targetJar, xsbtiJars, id, compiler, manager.log)
      manager.define(binID, Seq(targetJar))
    }
  }
}
