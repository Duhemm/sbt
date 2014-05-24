import sbt._
import Keys._

object build extends Build {
  val defaultSettings = Seq(
    incOptions := incOptions.value.withNameHashing(true),

    // disable sbt's heauristic which recompiles everything in case
    // some fraction (e.g. 50%) of files is scheduled to be recompiled
    // in this test we want precise information about recompiled files
    // which that heuristic would distort
    incOptions := incOptions.value.copy(recompileAllFraction = 1.0)

    scalaVersion := "2.11.0",
    // TODO Update when https://github.com/scalareflect/scalahost/pull/1 gets merged
    scalacOptions := Seq("-Xplugin:/Users/martin/Documents/Projects/xeno-by/scalahost/plugin/target/scala-2.11/scalahost_2.11.0-0.1.0-SNAPSHOT.jar")

    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  )

  val libs = Seq(
    libraryDependencies += "org.scalareflect" % "core_2.11" % "0.1.0-SNAPSHOT",
    libraryDependencies += "org.scalareflect" % "interpreter_2.11" % "0.1.0-SNAPSHOT",
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _ % "provided"),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _ % "provided")
  )

  lazy val root = Project(
     base = file("."),
     id = "macro",
     aggregate = Seq(macroProvider, macroClient),
     settings = Defaults.defaultSettings ++ defaultSettings ++ libs
  )

  lazy val macroProvider = Project(
     base = file("macro-provider"),
     id = "macro-provider",
     settings = Defaults.defaultSettings ++ defaultSettings ++ libs
  )

  lazy val macroClient = Project(
     base = file("macro-client"),
     id = "macro-client",
     dependencies = Seq(macroProvider),
     settings = Defaults.defaultSettings ++ defaultSettings ++ libs
  )

}

