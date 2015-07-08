import Project.Initialize
import Util._
import Dependencies._
import Licensed._
import Scope.ThisScope
import Scripted._
import StringUtilities.normalize
import Sxr.sxr

// ThisBuild settings take lower precedence,
// but can be shared across the multi projects.
def buildLevelSettings: Seq[Setting[_]] = Seq(
  organization in ThisBuild := "org.scala-sbt",
  version in ThisBuild := "0.13.9-SNAPSHOT",
  // bintrayOrganization in ThisBuild := None,
  // bintrayRepository in ThisBuild := "test-test-test",
  bintrayOrganization in ThisBuild :=  {
    if ((publishStatus in ThisBuild).value == "releases") Some("typesafe")
    else Some("sbt")
  },
  bintrayRepository in ThisBuild := s"ivy-${(publishStatus in ThisBuild).value}",
  bintrayPackage in ThisBuild := "sbt",
  bintrayReleaseOnPublish in ThisBuild := false
)

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion := scala210,
  publishArtifact in packageDoc := false,
  publishMavenStyle := false,
  componentID := None,
  crossPaths := false,
  resolvers += Resolver.typesafeIvyRepo("releases"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  concurrentRestrictions in Global += Util.testExclusiveRestriction,
  testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-w", "1"),
  javacOptions in compile ++= Seq("-target", "6", "-source", "6", "-Xlint", "-Xlint:-serial"),
  incOptions := incOptions.value.withNameHashing(true),
  crossScalaVersions := Seq(scala210),
  bintrayPackage := (bintrayPackage in ThisBuild).value,
  bintrayRepository := (bintrayRepository in ThisBuild).value
)

def minimalSettings: Seq[Setting[_]] =
  commonSettings ++ customCommands ++
  publishPomSettings ++ Release.javaVersionCheckSettings

def baseSettings: Seq[Setting[_]] =
  minimalSettings ++ Seq(projectComponent) ++ baseScalacOptions ++ Licensed.settings ++ Formatting.settings

def testedBaseSettings: Seq[Setting[_]] =
  baseSettings ++ testDependencies

lazy val sbtRoot: Project = (project in file(".")).
  configs(Sxr.sxrConf).
  aggregate(nonRoots: _*).
  settings(
    buildLevelSettings,
    minimalSettings,
    rootSettings,
    publish := {},
    publishLocal := {}
  )

// This is used to configure an sbt-launcher for this version of sbt.
lazy val bundledLauncherProj =
  (project in file("launch")).
  settings(
    minimalSettings,
    inConfig(Compile)(Transform.configSettings),
    Release.launcherSettings(sbtLaunchJar)
  ).
  enablePlugins(SbtLauncherPlugin).
  settings(
    name := "sbt-launch",
    moduleName := "sbt-launch",
    description := "sbt application launcher",
    publishArtifact in packageSrc := false,
    autoScalaLibrary := false,
    publish := Release.deployLauncher.value,
    publishLauncher := Release.deployLauncher.value,
    packageBin in Compile := sbtLaunchJar.value
  )


/* ** subproject declarations ** */

lazy val collectionProj = (project in utilPath / "collection").
  settings(
    testedBaseSettings,
    Util.keywordsSettings,
    Util.crossBuild,
    name := "Collections",
    crossScalaVersions := Seq(scala210, scala211)
  )

lazy val applyMacroProj = (project in utilPath / "appmacro").
  dependsOn(collectionProj).
  settings(
    testedBaseSettings,
    name := "Apply Macro",
    libraryDependencies += scalaCompiler.value
  )

// Utilities related to reflection, managing Scala versions, and custom class loaders
lazy val classpathProj = (project in utilPath / "classpath").
  settings(
    testedBaseSettings,
    name := "Classpath",
    libraryDependencies ++= Seq(scalaCompiler.value, Dependencies.launcherInterface, io.value, interface.value)
  )

// Command line-related utilities.
lazy val completeProj = (project in utilPath / "complete").
  dependsOn(collectionProj).
  settings(
    testedBaseSettings,
    Util.crossBuild,
    name := "Completion",
    libraryDependencies += jline,
    crossScalaVersions := Seq(scala210, scala211),
    libraryDependencies ++= Seq(io.value, control.value)
  )

// Relation
lazy val relationProj = (project in utilPath / "relation").
  settings(
    testedBaseSettings,
    name := "Relation",
    libraryDependencies ++= Seq(process.value, interface.value)
  )

// class file reader and analyzer
lazy val classfileProj = (project in utilPath / "classfile").
  settings(
    testedBaseSettings,
    name := "Classfile",
    libraryDependencies ++= Seq(io.value, log.value, interface.value)
  )

// cross versioning
lazy val crossProj = (project in utilPath / "cross").
  settings(
    baseSettings,
    inConfig(Compile)(Transform.crossGenSettings),
    name := "Cross"
  )

// A logic with restricted negation as failure for a unique, stable model
lazy val logicProj = (project in utilPath / "logic").
  dependsOn(collectionProj, relationProj).
  settings(
    testedBaseSettings,
    name := "Logic"
  )

/* **** Intermediate-level Modules **** */

// Apache Ivy integration
lazy val ivyProj = (project in file("ivy")).
  dependsOn(crossProj, /*launchProj % "test->test",*/ collectionProj).
  settings(
    baseSettings,
    name := "Ivy",
    libraryDependencies ++= Seq(ivy, jsch, sbtSerialization, launcherInterface, io.value % "compile;test->test", log.value % "compile;test->test", interface.value),
    testExclusive)

// Runner for uniform test interface
lazy val testingProj = (project in file("testing")).
  dependsOn(classpathProj, testAgentProj).
  settings(
    baseSettings,
    name := "Testing",
    libraryDependencies ++= Seq(testInterface,launcherInterface, io.value, log.value)
  )

// Testing agent for running tests in a separate process.
lazy val testAgentProj = (project in file("testing") / "agent").
  settings(
    minimalSettings,
    name := "Test Agent",
    libraryDependencies += testInterface
  )

// Basic task engine
lazy val taskProj = (project in tasksPath).
  dependsOn(collectionProj).
  settings(
    testedBaseSettings,
    name := "Tasks",
    libraryDependencies ++= Seq(control.value)
  )

// Standard task system.  This provides map, flatMap, join, and more on top of the basic task model.
lazy val stdTaskProj = (project in tasksPath / "standard").
  dependsOn (taskProj % "compile;test->test", collectionProj).
  settings(
    testedBaseSettings,
    name := "Task System",
    testExclusive,
    libraryDependencies ++= Seq(io.value, log.value, process.value)
  )

// Persisted caching based on SBinary
lazy val cacheProj = (project in cachePath).
  dependsOn (collectionProj).
  settings(
    baseSettings,
    name := "Cache",
    libraryDependencies ++= Seq(sbinary, sbtSerialization, io.value) ++ scalaXml.value
  )

// Builds on cache to provide caching for filesystem-related operations
lazy val trackingProj = (project in cachePath / "tracking").
  dependsOn(cacheProj).
  settings(
    baseSettings,
    name := "Tracking",
    libraryDependencies ++= Seq(io.value)
  )

// Embedded Scala code runner
lazy val runProj = (project in file("run")).
  dependsOn (classpathProj).
  settings(
    testedBaseSettings,
    name := "Run",
    libraryDependencies ++= Seq(io.value, log.value % "compile;test->test", process.value % "compile;test->test")
  )

// Implements the core functionality of detecting and propagating changes incrementally.
//   Defines the data structures for representing file fingerprints and relationships and the overall source analysis
lazy val compileIncrementalProj = (project in compilePath / "inc").
  dependsOn (classpathProj, relationProj).
  settings(
    testedBaseSettings,
    name := "Incremental Compiler",
    libraryDependencies ++= Seq(io.value, log.value, api.value)
  )

// Persists the incremental data structures using SBinary
lazy val compilePersistProj = (project in compilePath / "persist").
  dependsOn(compileIncrementalProj, compileIncrementalProj % "test->test").
  settings(
    testedBaseSettings,
    name := "Persist",
    libraryDependencies ++= Seq(sbinary , api.value)
  )

// sbt-side interface to compiler.  Calls compiler-side interface reflectively
lazy val compilerProj = (project in compilePath).
  dependsOn(classpathProj, classfileProj
    /*,launchProj % "test->test" */).
  settings(
    testedBaseSettings,
    name := "Compile",
    libraryDependencies ++= Seq(io.value, scalaCompiler.value % Test, launcherInterface, compilerBridge.value % Test, log.value, log.value % "test->test", api.value, interface.value % "compile;test->test") // Why is log twice?
  )

lazy val compilerIntegrationProj = (project in (compilePath / "integration")).
  dependsOn(compileIncrementalProj, compilerProj, compilePersistProj, classfileProj).
  settings(
    baseSettings,
    name := "Compiler Integration",
    libraryDependencies ++= Seq(api.value)
  )

lazy val compilerIvyProj = (project in compilePath / "ivy").
  dependsOn (ivyProj, compilerProj).
  settings(
    testedBaseSettings,
    name := "Compiler Ivy Integration"
  )

lazy val scriptedBaseProj = (project in scriptedPath / "base").
  settings(
    testedBaseSettings,
    name := "Scripted Framework",
    libraryDependencies ++= scalaParsers.value ++ Seq(io.value, process.value)
  )

lazy val scriptedSbtProj = (project in scriptedPath / "sbt").
  dependsOn (scriptedBaseProj).
  settings(
    baseSettings,
    name := "Scripted sbt",
    libraryDependencies ++= Seq(launcherInterface % "provided", io.value, log.value, process.value, interface.value)
  )

lazy val scriptedPluginProj = (project in scriptedPath / "plugin").
  dependsOn (sbtProj, classpathProj).
  settings(
    baseSettings,
    name := "Scripted Plugin"
  )

// Implementation and support code for defining actions.
lazy val actionsProj = (project in mainPath / "actions").
  dependsOn (classpathProj, completeProj, compilerIntegrationProj, compilerIvyProj,
    ivyProj, runProj, relationProj, stdTaskProj,
    taskProj, trackingProj, testingProj).
  settings(
    testedBaseSettings,
    name := "Actions",
    libraryDependencies ++= Seq(io.value, log.value, process.value, api.value, interface.value)
  )

// General command support and core commands not specific to a build system
lazy val commandProj = (project in mainPath / "command").
  dependsOn(completeProj, classpathProj, crossProj).
  settings(
    testedBaseSettings,
    name := "Command",
    libraryDependencies ++= Seq(launcherInterface, io.value, log.value, interface.value)
  )

// Fixes scope=Scope for Setting (core defined in collectionProj) to define the settings system used in build definitions
lazy val mainSettingsProj = (project in mainPath / "settings").
  dependsOn (applyMacroProj, ivyProj, relationProj, commandProj,
    completeProj, classpathProj, stdTaskProj).
  settings(
    testedBaseSettings,
    name := "Main Settings",
    libraryDependencies ++= Seq(sbinary, io.value, log.value, process.value, interface.value)
  )

// The main integration project for sbt.  It brings all of the Projsystems together, configures them, and provides for overriding conventions.
lazy val mainProj = (project in mainPath).
  dependsOn (actionsProj, mainSettingsProj, ivyProj, logicProj, runProj, commandProj).
  settings(
    testedBaseSettings,
    name := "Main",
    libraryDependencies ++= scalaXml.value ++ Seq(launcherInterface, io.value, log.value, process.value, interface.value)
  )

// Strictly for bringing implicits and aliases from subsystems into the top-level sbt namespace through a single package object
//  technically, we need a dependency on all of mainProj's dependencies, but we don't do that since this is strictly an integration project
//  with the sole purpose of providing certain identifiers without qualification (with a package object)
lazy val sbtProj = (project in sbtPath).
  dependsOn(mainProj, scriptedSbtProj % "test->test").
  settings(
    baseSettings,
    name := "sbt",
    normalizedName := "sbt",
    libraryDependencies ++= Seq(
      compilerBridge.value
      //"org.scala-sbt" % "compiler-bridge" % version.value
    )
  )

lazy val mavenResolverPluginProj = (project in file("sbt-maven-resolver")).
  dependsOn(sbtProj, ivyProj % "test->test").
  settings(
    baseSettings,
    name := "sbt-maven-resolver",
    libraryDependencies ++= aetherLibs,
    sbtPlugin := true
  )

def scriptedTask: Initialize[InputTask[Unit]] = Def.inputTask {
  val result = scriptedSource(dir => (s: State) => scriptedParser(dir)).parsed
  publishAll.value
  doScripted((sbtLaunchJar in bundledLauncherProj).value, (fullClasspath in scriptedSbtProj in Test).value,
    (scalaInstance in scriptedSbtProj).value, scriptedSource.value, result, scriptedPrescripted.value)
}

def scriptedUnpublishedTask: Initialize[InputTask[Unit]] = Def.inputTask {
  val result = scriptedSource(dir => (s: State) => scriptedParser(dir)).parsed
  doScripted((sbtLaunchJar in bundledLauncherProj).value, (fullClasspath in scriptedSbtProj in Test).value,
    (scalaInstance in scriptedSbtProj).value, scriptedSource.value, result, scriptedPrescripted.value)
}

lazy val publishAll = TaskKey[Unit]("publish-all")
lazy val publishLauncher = TaskKey[Unit]("publish-launcher")

lazy val myProvided = config("provided") intransitive

def allProjects = Seq(
  collectionProj, applyMacroProj, classpathProj, completeProj,
  relationProj, classfileProj, crossProj, logicProj, ivyProj,
  testingProj, testAgentProj, taskProj, stdTaskProj, cacheProj, trackingProj, runProj,
  compileIncrementalProj, compilePersistProj, compilerProj,
  compilerIntegrationProj, compilerIvyProj,
  scriptedBaseProj, scriptedSbtProj, scriptedPluginProj,
  actionsProj, commandProj, mainSettingsProj, mainProj, sbtProj, bundledLauncherProj, mavenResolverPluginProj)

def projectsWithMyProvided = allProjects.map(p => p.copy(configurations = (p.configurations.filter(_ != Provided)) :+ myProvided))
lazy val nonRoots = projectsWithMyProvided.map(p => LocalProject(p.id))

def rootSettings = fullDocSettings ++
  Util.publishPomSettings ++ otherRootSettings ++ Formatting.sbtFilesSettings ++
  Transform.conscriptSettings(bundledLauncherProj)
def otherRootSettings = Seq(
  Scripted.scriptedPrescripted := { _ => },
  Scripted.scripted <<= scriptedTask,
  Scripted.scriptedUnpublished <<= scriptedUnpublishedTask,
  Scripted.scriptedSource <<= (sourceDirectory in sbtProj) / "sbt-test",
  publishAll := {
    val _ = (publishLocal).all(ScopeFilter(inAnyProject)).value
  },
  aggregate in bintrayRelease := false
) ++ inConfig(Scripted.MavenResolverPluginTest)(Seq(
  Scripted.scripted <<= scriptedTask,
  Scripted.scriptedUnpublished <<= scriptedUnpublishedTask,
  Scripted.scriptedPrescripted := { f =>
    val inj = f / "project" / "maven.sbt"
    if (!inj.exists) {
      IO.write(inj, "addMavenResolverPlugin")
      // sLog.value.info(s"""Injected project/maven.sbt to $f""")
    }
  }
))
lazy val docProjects: ScopeFilter = ScopeFilter(
  inAnyProject -- inProjects(sbtRoot, sbtProj, scriptedBaseProj, scriptedSbtProj, scriptedPluginProj, mavenResolverPluginProj),
  inConfigurations(Compile)
)
def fullDocSettings = Util.baseScalacOptions ++ Docs.settings ++ Sxr.settings ++ Seq(
  scalacOptions += "-Ymacro-no-expand", // for both sxr and doc
  sources in sxr := {
    val allSources = (sources ?? Nil).all(docProjects).value
    allSources.flatten.distinct
  }, //sxr
  sources in (Compile, doc) := (sources in sxr).value, // doc
  Sxr.sourceDirectories := {
    val allSourceDirectories = (sourceDirectories ?? Nil).all(docProjects).value
    allSourceDirectories.flatten
  },
  fullClasspath in sxr := (externalDependencyClasspath in Compile in sbtProj).value,
  dependencyClasspath in (Compile, doc) := (fullClasspath in sxr).value
)

/* Nested Projproject paths */
def sbtPath    = file("sbt")
def cachePath  = file("cache")
def tasksPath  = file("tasks")
def launchPath = file("launch")
def utilPath   = file("util")
def compilePath = file("compile")
def mainPath   = file("main")


lazy val safeUnitTests = taskKey[Unit]("Known working tests (for both 2.10 and 2.11)")
lazy val safeProjects: ScopeFilter = ScopeFilter(
  inProjects(mainSettingsProj, mainProj, ivyProj, completeProj,
    actionsProj, classpathProj, collectionProj, compileIncrementalProj,
    runProj, stdTaskProj, compilerIvyProj),
  inConfigurations(Test)
)
lazy val otherUnitTests = taskKey[Unit]("Unit test other projects")
lazy val otherProjects: ScopeFilter = ScopeFilter(
  inProjects(
    applyMacroProj,
    relationProj, classfileProj,
    crossProj, logicProj, testingProj, testAgentProj, taskProj,
    cacheProj, trackingProj,
    compileIncrementalProj,
    compilePersistProj, compilerProj,
    compilerIntegrationProj, compilerIvyProj,
    scriptedBaseProj, scriptedSbtProj, scriptedPluginProj,
    commandProj, mainSettingsProj, mainProj,
    sbtProj, mavenResolverPluginProj),
  inConfigurations(Test)
)

def customCommands: Seq[Setting[_]] = Seq(
  commands += Command.command("setupBuildScala211") { state =>
    s"""set scalaVersion in ThisBuild := "$scala211" """ ::
      state
  },
  // This is invoked by Travis
  commands += Command.command("checkBuildScala211") { state =>
    s"++ $scala211" ::
      // First compile everything before attempting to test
      "all compile test:compile" ::
      // Now run known working tests.
      safeUnitTests.key.label ::
      state
  },
  safeUnitTests := {
    test.all(safeProjects).value
  },
  otherUnitTests := {
    test.all(otherProjects)
  },
  commands += Command.command("release-sbt-local") { state =>
    "clean" ::
    "allPrecompiled/clean" ::
    "allPrecompiled/compile" ::
    "allPrecompiled/publishLocal" ::
    "so compile" ::
    "so publishLocal" ::
    "reload" ::
    state
  },
  /** There are several complications with sbt's build.
   * First is the fact that interface project is a Java-only project
   * that uses source generator from datatype subproject in Scala 2.10.4,
   * which is depended on by Scala 2.8.2, Scala 2.9.2, and Scala 2.9.3 precompiled project. 
   *
   * Second is the fact that sbt project (currently using Scala 2.10.4) depends on
   * the precompiled projects (that uses Scala 2.8.2 etc.)
   * 
   * Finally, there's the fact that all subprojects are released with crossPaths
   * turned off for the sbt's Scala version 2.10.4, but some of them are also
   * cross published against 2.11.1 with crossPaths turned on.
   *
   * Because of the way ++ (and its improved version wow) is implemented
   * precompiled compiler briges are handled outside of doge aggregation on root.
   * `so compile` handles 2.10.x/2.11.x cross building. 
   */
  commands += Command.command("release-sbt") { state =>
    // TODO - Any sort of validation
    "clean" ::
    "allPrecompiled/clean" ::
      "allPrecompiled/compile" ::
      "allPrecompiled/publishSigned" ::
      "conscript-configs" ::
      "so compile" ::
      "so publishSigned" ::
      "bundledLauncherProj/publishLauncher" ::
      state
  },
  // stamp-version doesn't work with ++ or "so".
  commands += Command.command("release-nightly") { state =>
    "stamp-version" ::
      "clean" ::
      "allPrecompiled/clean" ::
      "allPrecompiled/compile" ::
      "allPrecompiled/publish" ::
      "compile" ::
      "publish" ::
      "bintrayRelease" ::
      state
  }
)
