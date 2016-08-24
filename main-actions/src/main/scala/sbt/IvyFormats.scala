// package sbt

// import Predef.{ StringFormat => _, _ }
// import java.io.File
// import java.net.URL

// import sbt.internal.librarymanagement._
// import sbt.librarymanagement._
// import sbt.librarymanagement.RepositoryHelpers._
// import org.apache.ivy.plugins.resolver.DependencyResolver

// import sbt.internal.util.CacheImplicits._

// import sjsonnew.{ IsoLList, JsonFormat, LCons, LList, LNil }
// import sjsonnew.BasicJsonProtocol.{ unionFormat1, unionFormat2, unionFormat3, unionFormat8, project }
// import sjsonnew.LList.:*:

// import scala.xml.NodeSeq

// trait IvyFormats {

//   object :-: {
//     def unapply[A1: JsonFormat: ClassManifest, A2 <: LList](cons: LCons[A1, A2]): Option[(A1, A2)] = Some((cons.head, cons.tail))
//   }

//   implicit lazy val DateFormat = project((_: java.util.Date).getTime(), new java.util.Date(_: Long))

//   // implicit lazy val CallerFormat = {
//   //   type Out = ModuleID :*: Seq[String] :*: Map[String, String] :*: Boolean :*: Boolean :*: Boolean :*: Boolean :*: LNil
//   //   LList.iso[Caller, Out](
//   //     { c =>
//   //       ("caller", c.caller) :*: ("callerConfigurations", c.callerConfigurations) :*: ("callerExtraAttributes", c.callerExtraAttributes) :*:
//   //       ("isForceDependency", c.isForceDependency) :*: ("isChangingDependency", c.isChangingDependency) :*:
//   //       ("isTransitiveDependency", c.isTransitiveDependency) :*: ("isDirectlyForceDependency", c.isDirectlyForceDependency) :*: LNil
//   //     },
//   //     {
//   //       case caller :-: callerConfigurations :-: callerExtraAttributes :-: isForceDependency :-: isChangingDependency :-: isTransitiveDependency :-: isDirectlyForceDependency :-: LNil =>
//   //         new Caller(caller, callerConfigurations, callerExtraAttributes, isForceDependency, isChangingDependency, isTransitiveDependency, isDirectlyForceDependency)
//   //       })
//   // }

//   // implicit lazy val ModuleReportFormat = {
//   //   type Out = ModuleID :*: Seq[(Artifact, File)] :*: Seq[Artifact] :*: Option[String] :*: Option[java.util.Date] :*: Option[String] :*: Option[String] :*: Boolean :*: Option[String] :*: Option[String] :*: Option[String] :*: Option[String] :*: Map[String, String] :*: Option[Boolean] :*: Option[String] :*: Seq[String] :*: Seq[(String, Option[String])] :*: Seq[Caller] :*: LNil
//   //   LList.iso[ModuleReport, Out](
//   //     { m =>
//   //       ("module", m.module) :*: ("artifacts", m.artifacts) :*: ("missingArtifacts", m.missingArtifacts) :*:
//   //       ("status", m.status) :*: ("publicationDate", m.publicationDate) :*: ("resolver", m.resolver) :*:
//   //       ("artifactResolver", m.artifactResolver) :*: ("evicted", m.evicted) :*: ("evictedData", m.evictedData) :*:
//   //       ("evictedReason", m.evictedReason) :*: ("problem", m.problem) :*: ("homepage", m.homepage) :*:
//   //       ("extraAttributes", m.extraAttributes) :*: ("isDefault", m.isDefault) :*: ("branch", m.branch) :*:
//   //       ("configurations", m.configurations) :*: ("licenses", m.licenses) :*: ("callers", m.callers) :*: LNil
//   //     },
//   //     {
//   //       case module :-: artifacts :-: missingArtifacts :-: status :-: publicationDate :-: resolver :-: artifactResolver :-: evicted :-: evictedData :-: evictedReason :-: problem :-: homepage :-: extraAttributes :-: isDefault :-: branch :-: configurations :-: licenses :-: callers :-: LNil =>
//   //         new ModuleReport(module, artifacts, missingArtifacts, status, publicationDate, resolver, artifactResolver,evicted, evictedData, evictedReason, problem, homepage, extraAttributes, isDefault, branch, configurations, licenses, callers)
//   //     })
//   // }

//   // implicit lazy val OrganizationArtifactReportFormat = {
//   //   type Out = String :*: String :*: Seq[ModuleReport] :*: LNil
//   //   LList.iso[OrganizationArtifactReport, Out](
//   //     { o => ("organization", o.organization) :*: ("name", o.name) :*: ("modules", o.modules) :*: LNil },
//   //     { case org :-: name :-: modules :-: LNil => new OrganizationArtifactReport(org, name, modules) }
//   //   )
//   // }

//   implicit lazy val ConfigurationReportFormat = {
//     type Out = String :*: Seq[ModuleReport] :*: Seq[OrganizationArtifactReport] :*: LNil
//     LList.iso[ConfigurationReport, Out](
//       { c => ("configuration", c.configuration) :*: ("modules", c.modules) :*: ("details", c.details) :*: LNil },
//       { case configuration :-: modules :-: defaults :-: LNil => new ConfigurationReport(configuration, modules, defaults) }
//     )
//   }

//   // implicit lazy val UpdateStatsFormat = {
//   //   type Out = Long :*: Long :*: Long :*: Boolean :*: LNil
//   //   LList.iso[UpdateStats, Out](
//   //     { u => ("resolveTime", u.resolveTime) :*: ("downloadTime", u.downloadTime) :*: ("downloadSize", u.downloadSize) :*: ("cached", u.cached) :*: LNil },
//   //     { case resolveTime :-: downloadTime :-: downloadSize :-: cached :-: LNil => new UpdateStats(resolveTime, downloadTime, downloadSize, cached) }
//   //   )
//   // }

//   implicit lazy val UpdateReportFormat = {
//     type Out = File :*: Seq[ConfigurationReport] :*: UpdateStats :*: Map[File, Long] :*: LNil
//     LList.iso[UpdateReport, Out](
//       { u => ("cachedDescriptor", u.cachedDescriptor) :*: ("configurations", u.configurations) :*: ("stats", u.stats) :*: ("stamps", u.stamps) :*: LNil },
//       { case cachedDescriptor :-: configurations :-: stats :-: stamps :-: LNil => new UpdateReport(cachedDescriptor, configurations, stats, stamps) }
//     )
//   }

//   implicit lazy val ArtifactTypeFilterFormat = LList.iso(
//     { a: ArtifactTypeFilter => ("types", a.types.toSeq) :*: ("inverted", a.inverted) :*: LNil},
//     { in: Seq[String] :*: Boolean :*: LNil => new ArtifactTypeFilter(in.head.toSet, in.tail.head) }
//   )

//   // implicit lazy val UpdateLoggingFormat = LList.iso(
//   //   { u: UpdateLogging.Value => ("logging", u.id) :*: LNil},
//   //   { in: Int :*: LNil => UpdateLogging(in.head) }
//   // )

//   // implicit lazy val RetrieveConfigurationFormat = {
//   //   type Out = File :*: String :*: Boolean :*: Option[Seq[Configuration]] :*: LNil
//   //   LList.iso[RetrieveConfiguration, Out](
//   //     { r => ("retrieveDirectory", r.retrieveDirectory) :*: ("outputPattern", r.outputPattern) :*: ("sync", r.sync) :*: ("configurationsToRetrieve", r.configurationsToRetrieve.map(_.toSeq)) :*: LNil },
//   //     { case retrieveDirectory :-: outputPattern :-: sync :-: configurationsToRetrieve :-: LNil => new RetrieveConfiguration(retrieveDirectory, outputPattern, sync, configurationsToRetrieve.map(_.toSet)) }
//   //   )
//   // }

//   // implicit lazy val UpdateConfigurationFormat = {
//   //   type Out = Option[RetrieveConfiguration] :*: Boolean :*: UpdateLogging.Value :*: ArtifactTypeFilter :*: LNil
//   //   LList.iso[UpdateConfiguration, Out](
//   //     { u => ("retrieve", u.retrieve) :*: ("missingOk", u.missingOk) :*: ("logging", u.logging) :*: ("artifactFilter", u.artifactFilter) :*: LNil },
//   //     { case retrieve :-: missingOk :-: logging :-: artifactFilter :-: LNil => new UpdateConfiguration(retrieve, missingOk, logging, artifactFilter) }
//   //   )
//   // }

//   // implicit lazy val ScmInfoFormat = {
//   //   type Out = URL :*: String :*: Option[String] :*: LNil
//   //   LList.iso[ScmInfo, Out](
//   //     { s => ("browseUrl", s.browseUrl) :*: ("connection", s.connection) :*: ("devConnection", s.devConnection) :*: LNil },
//   //     { case browseUrl :-: connection :-: devConnection :-: LNil => new ScmInfo(browseUrl, connection, devConnection) }
//   //   )
//   // }

//   // implicit lazy val DeveloperFormat = {
//   //   type Out = String :*: String :*: String :*: URL :*: LNil
//   //   LList.iso[Developer, Out](
//   //     { d => ("id", d.id) :*: ("name", d.name) :*: ("email", d.email) :*: ("url", d.url) :*: LNil },
//   //     { case id :-: name :-: email :-: url :-: LNil => new Developer(id, name, email, url) })
//   // }

//   // implicit lazy val SbtExclusionRuleFormat = {
//   //   type Out = String :*: String :*: String :*: Seq[String] :*: CrossVersion :*: LNil
//   //   LList.iso[SbtExclusionRule, Out](
//   //     { r => ("organization", r.organization) :*: ("name", r.name) :*: ("artifact", r.artifact) :*: ("configurations", r.configurations) :*: ("crossVersion", r.crossVersion) :*: LNil },
//   //     { case org :-: name :-: art :-: confs :-: cross :-: LNil => new SbtExclusionRule(org, name, art, confs, cross) }
//   //   )
//   // }

//   // implicit lazy val ModuleInfoFormat = {
//   //   type Out = String :*: String :*: Option[URL] :*: Option[Int] :*: Seq[(String, URL)] :*: String :*: Option[URL] :*: Option[ScmInfo] :*: Seq[Developer] :*: LNil
//   //   LList.iso[ModuleInfo, Out](
//   //     { m =>
//   //       ("nameFormal", m.nameFormal) :*: ("description", m.description) :*: ("homepage", m.homepage) :*: ("startYear", m.startYear) :*:
//   //       ("licenses", m.licenses) :*: ("organizationName", m.organizationName) :*: ("organizationHomepage", m.organizationHomepage) :*:
//   //       ("scmInfo", m.scmInfo) :*: ("developers", m.developers) :*: LNil
//   //     },
//   //     {
//   //       case nameFormal :-: description :-: homepage :-: startYear :-: licenses :-: organizationName :-: organizationHomepage :-: scmInfo :-: developers :-: LNil =>
//   //         new ModuleInfo(nameFormal, description, homepage, startYear, licenses, organizationName, organizationHomepage, scmInfo, developers)
//   //     })
//   // }

//   // implicit lazy val ConflictManagerFormat = {
//   //   type Out = String :*: String :*: String :*: LNil
//   //   LList.iso[ConflictManager, Out](
//   //     { c => ("name", c.name) :*: ("organization", c.organization) :*: ("module", c.module) :*: LNil },
//   //     { case name :-: org :-: module :-: LNil => new ConflictManager(name, org, module) }
//   //   )
//   // }

//   // implicit lazy val IvyScalaFormat = {
//   //   type Out = String :*: String :*: Seq[Configuration] :*: Boolean :*: Boolean :*: Boolean :*: String :*: LNil
//   //   LList.iso[IvyScala, Out](
//   //     { i =>
//   //       ("scalaFullVersion", i.scalaFullVersion) :*: ("scalaBinaryVersion", i.scalaBinaryVersion) :*: ("configurations", i.configurations.toSeq) :*:
//   //       ("checkExplicit", i.checkExplicit) :*: ("filterImplicit", i.filterImplicit) :*: ("overrideScalaVersion", i.overrideScalaVersion) :*:
//   //       ("scalaOrganization", i.scalaOrganization) :*: LNil
//   //     },
//   //     {
//   //       case scalaFullVersion :-: scalaBinaryVersion :-: configurations :-: checkExplicit :-: filterImplicit :-: overrideScalaVersion :-: scalaOrganization :-: LNil =>
//   //         new IvyScala(scalaFullVersion, scalaBinaryVersion, configurations.toIterable, checkExplicit, filterImplicit, overrideScalaVersion, scalaOrganization)
//   //     }
//   //   )
//   // }

//   // implicit lazy val InlineConfigurationFormat = {
//   //   type Out = ModuleID :*: ModuleInfo :*: Seq[ModuleID] :*: Seq[ModuleID] :*: Seq[SbtExclusionRule] /*:*: NodeSeq*/ :*: Seq[Configuration] :*:  Option[Configuration] :*: Option[IvyScala] :*: Boolean :*: ConflictManager :*: LNil
//   //   LList.iso[InlineConfiguration, Out](
//   //     { i =>
//   //       ("module", i.module) :*: ("moduleInfo", i.moduleInfo) :*: ("dependencies", i.dependencies) :*: ("overrides", i.overrides.toSeq) :*:
//   //         ("excludes", i.excludes) /*:*: ("ivyXml", i.ivyXml)*/ :*: ("configurations", i.configurations) :*:
//   //         ("defaultConfiguration", i.defaultConfiguration) :*: ("ivyScala", i.ivyScala) :*: ("validate", i.validate) :*:
//   //         ("conflictManager", i.conflictManager) :*: LNil
//   //     },
//   //     {
//   //       case module :-: moduleInfo :-: dependencies :-: overrides :-: excludes /*:-: ivyXml*/ :-: configurations :-: defaultConfiguration :-: ivyScala :-: validate :-: conflictManager :-: LNil =>
//   //         InlineConfiguration(module, moduleInfo, dependencies, overrides.toSet, excludes, NodeSeq.Empty, configurations, defaultConfiguration, ivyScala, validate, conflictManager)
//   //     }
//   //   )
//   // }

//   // implicit lazy val PomConfigurationFormat = {
//   //   type Out = File :*: Option[IvyScala] :*: Boolean :*: Boolean :*: LNil
//   //   LList.iso[PomConfiguration, Out](
//   //     { i => ("file", i.file) :*: ("ivyScala", i.ivyScala) :*: ("validate", i.validate) :*: ("autoScalaTools", i.autoScalaTools) :*: LNil },
//   //     { case file :-: ivyScala :-: validate :-: autoScalaTools :-: LNil => new PomConfiguration(file, ivyScala, validate, autoScalaTools) }
//   //   )
//   // }

//   // implicit lazy val IvyFileConfigurationFormat = {
//   //   type Out = File :*: Option[IvyScala] :*: Boolean :*: Boolean :*: LNil
//   //   LList.iso[IvyFileConfiguration, Out](
//   //     { i => ("file", i.file) :*: ("ivyScala", i.ivyScala) :*: ("validate", i.validate) :*: ("autoScalaTools", i.autoScalaTools) :*: LNil },
//   //     { case file :-: ivyScala :-: validate :-: autoScalaTools :-: LNil => new IvyFileConfiguration(file, ivyScala, validate, autoScalaTools) }
//   //   )
//   // }

//   // implicit lazy val ModuleSettingsFormat = unionFormat3[ModuleSettings, IvyFileConfiguration, PomConfiguration, InlineConfiguration]

//   // implicit lazy val IvyPathsFormat = LList.iso(
//   //   { i: IvyPaths => ("baseDirectory", i.baseDirectory.getAbsoluteFile) :*: ("ivyHome", i.ivyHome) :*: LNil },
//   //   { in: File :*: Option[File] :*: LNil => new IvyPaths(in.head, in.tail.head) }) // Absolute file?

//   // implicit lazy val InclExclRuleFormat = {
//   //   type Out = String :*: String :*: String :*: Seq[String] :*: LNil
//   //   LList.iso[InclExclRule, Out](
//   //     { r => ("organization", r.organization) :*: ("name", r.name) :*: ("artifact", r.artifact) :*: ("configurations", r.configurations) :*: LNil },
//   //     { case org :-: name :-: artifact :-: configurations :-: LNil => new InclExclRule(org, name, artifact, configurations) })
//   // }
//   // implicit lazy val InclusionRuleFormat: JsonFormat[InclusionRule] = InclusionRuleFormat.jsonFormat

//   // implicit lazy val CrossVersionFormat: JsonFormat[CrossVersion] = ???

//   // implicit lazy val ConfigurationFormat: IsoLList[Configuration] = {
//   //   type Out = String :*: String :*: Boolean :*: scala.collection.immutable.Seq[Configuration] :*: Boolean :*: LNil
//   //   LList.iso[Configuration, Out](
//   //     { c => ("name", c.name) :*: ("description", c.description) :*: ("isPublic", c.isPublic) :*: ("extendsConfigs", c.extendsConfigs.toSeq) :*: ("transitive", c.transitive) :*: LNil },
//   //     { case n :-: d :-: ip :-: ec :-: t :-: LNil => new Configuration(n, d, ip, ec.toList, t) }
//   //   )
//   // }

//   // implicit lazy val ArtifactFormat: IsoLList[Artifact] = {
//   //   type Out = String :*: String :*: String :*: Option[String] :*: Seq[Configuration] :*: Option[URL] :*: Map[String, String] :*: LNil
//   //   LList.iso[Artifact, Out](
//   //     { a => ("name", a.name) :*: ("type", a.`type`) :*: ("extension", a.extension) :*: ("classifier", a.classifier) :*: ("configurations", a.configurations.toSeq) :*: ("url", a.url) :*: ("extraAttributes", a.extraAttributes) :*: LNil},
//   //     { case n :-: t :-: e :-: c :-: confs :-: url :-: ea :-: LNil => new Artifact(n, t, e, c, confs.toIterable, url, ea) }
//   //   )
//   // }

//   // implicit lazy val PasswordAuthenticationFormat = LList.iso(
//   //   { p: PasswordAuthentication => ("user", p.user) :*: ("password", p.password) :*: LNil },
//   //   { in: String :*: Option[String] :*: LNil => new PasswordAuthentication(in.head, in.tail.head) }
//   // )

//   // implicit lazy val KeyFileAuthenticationFormat = {
//   //   type Out = String :*: File :*: Option[String] :*: LNil
//   //   LList.iso[KeyFileAuthentication, Out](
//   //     { k => ("user", k.user) :*: ("keyfile", k.keyfile) :*: ("password", k.password) :*: LNil},
//   //     { case u :-: k :-: p :-: LNil => new KeyFileAuthentication(u, k, p) }
//   //   )
//   // }

//   // implicit lazy val SshAuthenticationFormat = unionFormat2[SshAuthentication, PasswordAuthentication, KeyFileAuthentication]

//   // implicit lazy val SshConnectionFormat = {
//   //   type Out = Option[SshAuthentication] :*: Option[String] :*: Option[Int] :*: LNil
//   //   LList.iso[SshConnection, Out](
//   //     { s => ("authentication", s.authentication) :*: ("hostname", s.hostname) :*: ("port", s.port) :*: LNil },
//   //     { case a :-: h :-: p :-: LNil => new SshConnection(a, h, p) }
//   //   )
//   // }

//   // implicit lazy val FileConfigurationFormat = LList.iso(
//   //   { f: FileConfiguration => ("isLocal", f.isLocal) :*: ("isTransactional", f.isTransactional) :*: LNil },
//   //   { in: Boolean :*: Option[Boolean] :*: LNil => FileConfiguration(in.head, in.tail.head) }
//   // )

//   // implicit lazy val PatternsFormat = {
//   //   type Out = Seq[String] :*: Seq[String] :*: Boolean :*: Boolean :*: Boolean :*: LNil
//   //   LList.iso[Patterns, Out](
//   //     { p => ("ivyPatterns", p.ivyPatterns) :*: ("artifactPatterns", p.artifactPatterns) :*: ("isMavenCompatible", p.isMavenCompatible) :*: ("descriptorOptional", p.descriptorOptional) :*: ("skipConsistencyCheck", p.skipConsistencyCheck) :*: LNil },
//   //     { case ip :-: ap :-: imc :-: dopt :-: scc :-: LNil => new Patterns(ip, ap, imc, dopt, scc) }
//   //   )
//   // }

//   // implicit lazy val MavenCacheFormat = LList.iso(
//   //   { m: MavenCache => ("name", m.name) :*: ("root", m.rootFile.getAbsolutePath) :*: LNil },
//   //   { in: String :*: String :*: LNil => new MavenCache(in.head, new File(in.tail.head)) }
//   // )

//   // implicit lazy val MavenRepositoryFormat = LList.iso(
//   //   { m: MavenRepository => ("name", m.name) :*: ("root", m.root) :*: LNil },
//   //   { in: String :*: String :*: LNil => new MavenRepository(in.head, in.tail.head) }
//   // )

//   // implicit lazy val FileRepositoryFormat = {
//   //   type Out = String :*: FileConfiguration :*: Patterns :*: LNil
//   //   LList.iso[FileRepository, Out](
//   //     { f => ("name", f.name) :*: ("configuration", f.configuration) :*: ("patterns", f.patterns) :*: LNil },
//   //     { case n :-: c :-: p :-: LNil => new FileRepository(n, c, p) }
//   //   )
//   // }

//   // implicit lazy val URLRepositoryFormat = LList.iso(
//   //   { u: URLRepository => ("name", u.name) :*: ("patterns", u.patterns) :*: LNil },
//   //   { in: String :*: Patterns :*: LNil => new URLRepository(in.head, in.tail.head) }
//   // )

//   // implicit lazy val SshRepositoryFormat = {
//   //   type Out = String :*: SshConnection :*: Patterns :*: Option[String] :*: LNil
//   //   LList.iso[SshRepository, Out](
//   //     { s => ("name", s.name) :*: ("connection", s.connection) :*: ("patterns", s.patterns) :*: ("publishPermissions", s.publishPermissions) :*: LNil },
//   //     { case n :-: c :-: p :-: pp :-: LNil => new SshRepository(n, c, p, pp) }
//   //   )
//   // }

//   // implicit lazy val SftpRepository = {
//   //   type Out = String :*: SshConnection :*: Patterns :*: LNil
//   //   LList.iso[SftpRepository, Out](
//   //     { s => ("name", s.name) :*: ("connection", s.connection) :*: ("patterns", s.patterns) :*: LNil },
//   //     { case n :-: c :-: p :-: LNil => new SftpRepository(n, c, p) }
//   //   )
//   // }

//   // implicit lazy val RawRepositoryFormat = LList.iso(
//   //   { r: RawRepository => ("name", r.name) :*: ("class", r.getClass.getName) :*: LNil },
//   //   { in: String :*: String :*: LNil => new RawRepository(???) }
//   // )

//   // implicit lazy val ChainedResolverFormat = LList.iso(
//   //   { c: ChainedResolver => ("name", c.name) :*: ("resolvers", c.resolvers) :*: LNil },
//   //   { in: String :*: Seq[Resolver] :*: LNil => new ChainedResolver(in.head, in.tail.head) }
//   // )

//   // implicit lazy val ModuleIDFormat: IsoLList[ModuleID] = {
//   //   type Out = String :*: String :*: String :*: Option[String] :*: Boolean :*: Boolean :*: Boolean :*: Seq[Artifact] :*: Seq[InclusionRule] :*: Seq[ExclusionRule] :*: Map[String, String] :*: CrossVersion :*: Option[String] :*: LNil
//   //   LList.iso[ModuleID, Out](
//   //     { m =>
//   //       ("organization", m.organization) :*: ("name", m.name) :*: ("revision", m.revision) :*: ("configurations", m.configurations) :*:
//   //         ("isChanging", m.isChanging) :*: ("isTransitive", m.isTransitive) :*: ("isForce", m.isForce) :*: ("explicitArtifacts", m.explicitArtifacts) :*:
//   //         ("inclusions", m.inclusions) :*: ("exclusions", m.exclusions) :*: ("extraAttributes", m.extraAttributes) :*:
//   //         ("crossVersion", m.crossVersion) :*: ("branchName", m.branchName) :*: LNil
//   //     },
//   //     {
//   //       case org :-: name :-: rev :-: confs :-: changing :-: transitive :-: force :-: expArts :-: inc :-: exc :-: extAttrs :-: cross :-: branch :-: LNil =>
//   //         ModuleID(org, name, rev, confs, changing, transitive, force, expArts, inc, exc, extAttrs, cross, branch)
//   //     }
//   //   )
//   // }

//   // implicit lazy val ResolverFormat: JsonFormat[Resolver] = unionFormat8[Resolver, MavenCache, MavenRepository, FileRepository, URLRepository, SshRepository, SftpRepository, RawRepository, ChainedResolver]

//   // implicit lazy val ModuleConfigurationFormat = {
//   //   type Out = String :*: String :*: String :*: Resolver :*: LNil
//   //   LList.iso[ModuleConfiguration, Out](
//   //     { m => ("organization", m.organization) :*: ("name", m.name) :*: ("revision", m.revision) :*: ("resolver", m.resolver) :*: LNil },
//   //     { case o :-: n :-: rev :-: res :-: LNil => new ModuleConfiguration(o, n, rev, res) }
//   //   )
//   // }

//   // implicit lazy val InlineIvyConfigurationFormat = {
//   //   type Out = IvyPaths :*: Seq[Resolver] :*: Seq[Resolver] :*: Seq[ModuleConfiguration] :*: Boolean :*: Seq[String] :*: LNil
//   //   LList.iso[InlineIvyConfiguration, Out](
//   //     { i =>
//   //       ("paths", i.paths) :*: ("resolvers", i.resolvers) :*: ("otherResolvers", i.otherResolvers) :*:
//   //         ("moduleConfigurations", i.moduleConfigurations) :*: ("localOnly", i.localOnly) :*: ("checksums", i.checksums) :*: LNil
//   //     },
//   //     {
//   //       case p :-: r :-: or :-: mc :-: lo :-: cs :-: LNil =>
//   //         new InlineIvyConfiguration(p, r, or, mc, lo, None, cs, None, UpdateOptions(), ???)
//   //     }
//   //   )
//   // }

//   // implicit lazy val IvyConfigurationFormat = unionFormat1[IvyConfiguration, InlineIvyConfiguration]
// }