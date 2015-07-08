/* sbt -- Simple Build Tool
 * Copyright 2009, 2010  Mark Harrah
 */
package sbt
package compiler

import java.io.File

object ComponentCompiler {
  val xsbtiID = "interface"
  val srcExtension = "-src"
  val binSeparator = "_"
  val compilerInterfaceID = "compiler-bridge"
  val compilerInterfaceSrcID = compilerInterfaceID + srcExtension
  val javaVersion = System.getProperty("java.class.version")
  val sbtVersion = "0.13.9-SNAPSHOT"

  def interfaceProvider(ivyConfiguration: IvyConfiguration): CompilerInterfaceProvider = new CompilerInterfaceProvider {
    def apply(scalaInstance: xsbti.compile.ScalaInstance, log: Logger): File =
      {
        // this is the instance used to compile the interface component
        val componentCompiler = new NewComponentCompiler(new RawCompiler(scalaInstance, ClasspathOptions.auto, log), ivyConfiguration, log)
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

class NewComponentCompiler(compiler: RawCompiler, ivyConfiguration: IvyConfiguration, log: Logger) {
  import ComponentCompiler._

  private val ivySbt: IvySbt = new IvySbt(ivyConfiguration)

  def apply(id: String): File =
    getPrecompiled(id) getOrElse getLocallyCompiled(id)

  private def binaryID(id: String, withJavaVersion: Boolean): String = {
    val base = id + binSeparator + compiler.scalaInstance.actualVersion
    if (withJavaVersion) base + "__" + javaVersion else base
  }

  private def getPrecompiled(id: String): Option[File] = {
    val binID = binaryID(id, false)
    val binModule = getModule(binID)
    val jarName = s"$binID-$sbtVersion.jar"
    update(binModule) find (_.getName == jarName)
  }

  private def getLocallyCompiled(id: String): File = {
    val binID = binaryID(id, true)
    val binModule = getModule(binID)
    val jarName = s"$binID-$sbtVersion.jar"
    update(binModule) find (_.getName == jarName) getOrElse compileAndInstall(id, binID, binModule)
  }

  private def compileAndInstall(id: String, binID: String, binModule: ivySbt.Module): File = {
    val srcID = id + srcExtension
    IO.withTemporaryDirectory { binaryDirectory =>
      def interfaceSources(moduleVersions: Seq[String]): File =
        moduleVersions match {
          case Seq() =>
            log.debug(s"Fetching default sources: $id")
            val jarName = s"$srcID-$sbtVersion.jar"
            update(getModule(id)) find (_.getName == jarName) getOrElse (throw new Exception("Couldn't retrieve default sources"))

          case version +: rest =>
            log.debug(s"Fetching version-specific sources: ${id}_$version")
            val moduleName = s"${id}_$version"
            val jarName = s"${srcID}_$version-$sbtVersion.jar"
            update(getModule(moduleName)) find (_.getName == jarName) getOrElse interfaceSources(rest)
        }

      val artifactName = binID
      val targetJar = new File(binaryDirectory, s"$artifactName.jar")
      val xsbtiJars = update(getModule(xsbtiID))

      val sourceModuleVersions = cascadingSourceModuleVersions(compiler.scalaInstance.actualVersion)
      AnalyzingCompiler.compileSources(Seq(interfaceSources(sourceModuleVersions)), targetJar, xsbtiJars, id, compiler, log)

      sbt.IO.withTemporaryDirectory { dir =>
        val pomFile = new File(dir, "pom.xml")
        sbt.IO.write(pomFile,
          s"""
            |<project>
            |   <groupId>org.scala-sbt</groupId>
            |   <name>$binID</name>
            |   <version>0.13.9-SNAPSHOT</version>
            |</project>
          """.stripMargin)

        val artifactsMap =
          Map(
            Artifact(artifactName) -> targetJar,
            Artifact(s"$binID-$sbtVersion", "pom", "pom") -> pomFile
          )

        val ivyFile = new java.io.File(dir, "ivy.xml")

        sbt.IO.write(ivyFile,
          s"""<?xml version="1.0" encoding="UTF-8"?>
            |<ivy-module version="2.0" xmlns:e="http://ant.apache.org/ivy/extra">
            |  <info organisation="org.scala-sbt" module="$binID" revision="$sbtVersion" status="integration" publication="${new java.text.SimpleDateFormat("yyyyMMDDHHmmss").format(new java.util.Date)}">
            |    <description>
            |    Compiler interface
            |    </description>
            |  </info>
            |  <configurations>
            |    <conf name="compile" visibility="public" description=""/>
            |    <conf name="runtime" visibility="public" description="" extends="compile"/>
            |    <conf name="test" visibility="public" description="" extends="runtime"/>
            |    <conf name="provided" visibility="public" description=""/>
            |    <conf name="optional" visibility="public" description=""/>
            |    <conf name="sources" visibility="public" description=""/>
            |    <conf name="docs" visibility="public" description=""/>
            |    <conf name="pom" visibility="public" description=""/>
            |  </configurations>
            |  <publications>
            |    <artifact name="$artifactName" type="jar" ext="jar" conf="compile"/>
            |  </publications>
            |  <dependencies>
            |  </dependencies>
            |</ivy-module>
           """.stripMargin)

        val moduleForPublication = {
          val moduleID = ModuleID(xsbti.ArtifactInfo.SbtOrganization, binID, sbtVersion, Some("component"))
          getModule(moduleID, Nil)
        }

        publishLocally(moduleForPublication, artifactsMap, Some(ivyFile))
      }

      val jarName = s"$artifactName-$sbtVersion.jar"
      update(binModule) find (_.getName == jarName) getOrElse (throw new Exception("Couldn't retrieve compiled compiler bridge."))

    }
  }

  /**
   * Returns a module that corresponds to "org.scala-sbt" % `id` % `sbtVersion`
   * Note: Actually, it returns a module that has the requested module as dependency, because sbt's
   * ivy implementation assumes that the root module is an sbt project, and thus doesn't download it.
   */
  private def getModule(id: String): ivySbt.Module = {
    val dummyID = ModuleID(xsbti.ArtifactInfo.SbtOrganization, "sbt", sbtVersion, Some("component"))
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

  private def publishLocally(module: ivySbt.Module, artifacts: Map[Artifact, File], ivyFile: Option[File]) = {
    val publishConfiguration = new PublishConfiguration(
      ivyFile = ivyFile,
      resolverName = Resolver.defaultLocal.name,
      artifacts = artifacts,
      checksums = Nil,
      logging = UpdateLogging.Default,
      overwrite = true)

    IvyActions.publish(module, publishConfiguration, log)
  }

  private def update(module: ivySbt.Module): Seq[File] = {

    val retrieveDirectory = new File(ivyConfiguration.baseDirectory, "component")
    val retrieveConfiguration = new RetrieveConfiguration(retrieveDirectory, Resolver.defaultRetrievePattern, false)
    val updateConfiguration = new UpdateConfiguration(Some(retrieveConfiguration), true, UpdateLogging.Default)

    IvyActions.updateEither(module, updateConfiguration, UnresolvedWarningConfiguration(), LogicalClock.unknown, None, log) match {
      case Left(unresolvedWarning) =>
        ???

      case Right(updateReport) =>

        for {
          conf <- updateReport.configurations
          m <- conf.modules
          (_, f) <- m.artifacts
        } yield f

    }
  }
}
