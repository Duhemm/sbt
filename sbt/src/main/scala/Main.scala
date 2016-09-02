package sbt

import java.net.URLClassLoader
import java.util.Properties

/**
 * A Main class for running sbt without sbt launcher.
 */
object Main {
  def main(_args: Array[String]): Unit = {
    val (ivyHome: File, args: Array[String]) = _args match {
      case Array()                   => (null, Array.empty[String])
      case Array(ivyHome, rest @ _*) => (new File(ivyHome), rest.toArray)
    }
    val appConfiguration = new StaticAppConfiguration(ivyHome, args)
    new xMain().run(appConfiguration)
  }
}

private object StaticUtils {
  val MAIN = "sbt.Main"
  val SCALA_ORG = "org.scala-lang"
  val COMPILER = "compiler"
  val COMPILER_JAR = "scala-compiler.jar"
  val LIBRARY = "library"
  val LIBRARY_JAR = "scala-library.jar"
  val REFLECT = "reflect"
  val REFLECT_JAR = "scala-reflect.jar"
  val BRIDGE = "compiler-interface"
  val BRIDGE_JAR = s"compiler-interface-${sbtApplicationID.version}-sources.jar"
  val XSBTI = "xsbti"
  val XSBTI_JAR = s"interface-${sbtApplicationID.version}.jar"
  val MAVEN_RESOLVER_PLUGIN = "maven-resolver-plugin"
  val MAVEN_RESOLVER_PLUGIN_JAR = s"sbt-maven-resolver-assembly-${sbtApplicationID.version}.jar"
  val thisJAR: File = new File(getClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())

  def getProperty(loader: ClassLoader, filename: String, property: String): Option[String] =
    for {
      stream <- Option(loader.getResourceAsStream(filename))
      props = new Properties()
      _ = props.load(stream)
      o <- Option(props get property)
      s = o.asInstanceOf[String]
    } yield s

}

private class StaticComponentProvider(bootDirectory: File) extends xsbti.ComponentProvider {
  override def addToComponent(componentID: String, components: Array[File]): Boolean = {
    components foreach { c =>
      IO.copyFile(c, componentLocation(componentID) / c.getName)
    }
    true
  }

  override def component(componentID: String): Array[File] =
    PathFinder(componentLocation(componentID)).***.get.filter(_.isFile).toArray

  override def componentLocation(id: String): File =
    bootDirectory / s"static-sbt-${sbtApplicationID.version}" / id

  override def defineComponent(componentID: String, components: Array[File]): Unit =
    addToComponent(componentID, components)

  override def lockFile(): File = null
}

private object sbtApplicationID extends xsbti.ApplicationID {
  override val groupID: String = xsbti.ArtifactInfo.SbtOrganization
  override val name: String = "sbt"
  override def version(): String = StaticUtils.getProperty(getClass.getClassLoader, "xsbt.version.properties", "version") getOrElse "unknown"
  override val mainClass: String = StaticUtils.MAIN
  override val mainComponents: Array[String] = Array.empty
  override val crossVersioned: Boolean = false
  override val crossVersionedValue: xsbti.CrossValue = xsbti.CrossValue.Disabled
  override val classpathExtra: Array[File] = Array.empty
}

private class WeakGlobalLock extends xsbti.GlobalLock {
  override def apply[T](lockFile: File, run: java.util.concurrent.Callable[T]): T = run.call
}

private class StaticLauncher(appProvider: StaticAppProvider, scalaProvider: StaticScalaProvider, override val ivyHome: File) extends xsbti.Launcher {
  override def getScala(version: String): xsbti.ScalaProvider = getScala(version, "")
  override def getScala(version: String, reason: String): xsbti.ScalaProvider = getScala(version, reason, StaticUtils.SCALA_ORG)
  override def getScala(version: String, reason: String, scalaOrg: String): xsbti.ScalaProvider = {
    val myScalaVersion = scalaProvider.version
    if (myScalaVersion == version) scalaProvider
    else throw new InvalidComponent(s"This launcher can only provide scala $myScalaVersion, asked for scala $version")
  }
  override def app(id: xsbti.ApplicationID, version: String): xsbti.AppProvider = appProvider

  override def topLoader(): ClassLoader = new URLClassLoader(Array.empty, null)
  override def globalLock(): xsbti.GlobalLock = new WeakGlobalLock

  override def bootDirectory(): File = new File(sys props "user.home") / ".sbt" / "boot"

  override def ivyRepositories(): Array[xsbti.Repository] = {
    val fake = new FakeRepository(new FakeResolver("fakeresolver", bootDirectory / "fakeresolver-cache", modules))
    val local = new xsbti.PredefinedRepository { override def id(): xsbti.Predefined = xsbti.Predefined.Local }
    val mvn = new xsbti.PredefinedRepository { override def id(): xsbti.Predefined = xsbti.Predefined.MavenCentral }
    Array(fake, local, mvn)
  }

  override def appRepositories(): Array[xsbti.Repository] = ivyRepositories()

  override def isOverrideRepositories(): Boolean = false

  // override def ivyHome(): File = null
  override def checksums(): Array[String] = Array.empty

  private lazy val modules = Map(
    ("org.scala-sbt", "sbt", sbtApplicationID.version) ->
      Seq(FakeResolver.FakeArtifact("sbt", "jar", "jar", StaticUtils.thisJAR)),

    ("org.scala-sbt", "compiler-interface", sbtApplicationID.version) -> {
      val file = scalaProvider.getComponent(StaticUtils.BRIDGE)
      Seq(FakeResolver.FakeArtifact("compiler-interface", "src", "jar", file))
    },

    ("org.scala-sbt", "sbt-maven-resolver", sbtApplicationID.version) ->
      appProvider.components.component(StaticUtils.MAVEN_RESOLVER_PLUGIN).map { f =>
        FakeResolver.FakeArtifact(f.getName, "jar", "jar", f)
      }.toSeq
  )
}

private class StaticScalaProvider(appProvider: StaticAppProvider, ivyHome: File) extends xsbti.ScalaProvider {

  def getComponent(componentID: String): File = {
    val component = appProvider.components.component(componentID)
    assert(component.length == 1, s"""Component $componentID should have 1 file, ${component.length} files found: ${component.mkString(", ")}.""")
    component(0)
  }
  override def launcher: xsbti.Launcher = new StaticLauncher(appProvider, this, ivyHome)
  override def app(id: xsbti.ApplicationID): xsbti.AppProvider = appProvider
  override def compilerJar(): File = getComponent(StaticUtils.COMPILER)
  override def libraryJar(): File = getComponent(StaticUtils.LIBRARY)
  override def jars(): Array[File] = Array(compilerJar, libraryJar, getComponent(StaticUtils.REFLECT))
  override def loader(): ClassLoader = new URLClassLoader(jars map (_.toURI.toURL))
  override def version(): String = StaticUtils.getProperty(loader, "compiler.properties", "version.number") getOrElse "unknown"
}

private class StaticAppProvider(appConfig: StaticAppConfiguration, ivyHome: File) extends xsbti.AppProvider {

  if (components.component(StaticUtils.COMPILER).isEmpty) {
    installFromResources(StaticUtils.COMPILER_JAR, StaticUtils.COMPILER)
  }

  if (components.component(StaticUtils.LIBRARY).isEmpty) {
    installFromResources(StaticUtils.LIBRARY_JAR, StaticUtils.LIBRARY)
  }

  if (components.component(StaticUtils.REFLECT).isEmpty) {
    installFromResources(StaticUtils.REFLECT_JAR, StaticUtils.REFLECT)
  }

  if (components.component(StaticUtils.XSBTI).isEmpty) {
    installFromResources(StaticUtils.XSBTI_JAR, StaticUtils.XSBTI)
  }

  if (components.component(StaticUtils.BRIDGE).isEmpty) {
    installFromResources(StaticUtils.BRIDGE_JAR, StaticUtils.BRIDGE)
  }

  if (components.component(StaticUtils.MAVEN_RESOLVER_PLUGIN).isEmpty) {
    installFromResources(StaticUtils.MAVEN_RESOLVER_PLUGIN_JAR, StaticUtils.MAVEN_RESOLVER_PLUGIN)
  }

  override def components(): xsbti.ComponentProvider = new StaticComponentProvider(scalaProvider.launcher.bootDirectory)
  override def entryPoint(): Class[_] = loader.getClass
  override def id(): xsbti.ApplicationID = sbtApplicationID
  override def loader(): ClassLoader = getClass.getClassLoader
  override def mainClass(): Class[xsbti.AppMain] = loader.loadClass(id.mainClass).asInstanceOf[Class[xsbti.AppMain]]
  override def mainClasspath(): Array[File] = Array(StaticUtils.thisJAR)
  override def newMain(): xsbti.AppMain = new xMain
  override def scalaProvider(): xsbti.ScalaProvider = new StaticScalaProvider(this, ivyHome)

  /**
   * Retrieves `fileName` from the resources and installs it in `componentID`.
   * @param filename    Name of the file to get from the resources.
   * @param componentID ID of the component to create.
   */
  private def installFromResources(filename: String, componentID: String): Unit =
    IO.withTemporaryDirectory { tmp =>
      Option(getClass.getClassLoader.getResourceAsStream(filename)) match {
        case Some(stream) =>
          val target = tmp / filename
          val out = new java.io.FileOutputStream(target)

          var read = 0
          val content = new Array[Byte](1024)
          while ({ read = stream.read(content); read != -1 }) {
            out.write(content, 0, read)
          }

          components.defineComponent(componentID, Array(target))

        case None =>
          sys.error(s"Couldn't install component $componentID: $filename not found on resource path.")
      }
    }
}

private class StaticAppConfiguration(ivyHome: File, override val arguments: Array[String]) extends xsbti.AppConfiguration {
  override val baseDirectory: File = new File(sys props "user.dir")
  override val provider: xsbti.AppProvider = new StaticAppProvider(this, ivyHome)
}
