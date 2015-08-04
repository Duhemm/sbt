package sbt.incrementalcompiler.internal

/**
 * Internal API for an `Analysis`.
 * The APIs offered here are subject to change at any point.
 */
trait Analysis extends sbt.incrementalcompiler.Analysis {
  // val stamps: Stamps
  // val apis: APIs
  // /** Mappings between sources, classes, and binaries. */
  // val relations: Relations
  // val infos: SourceInfos
  // /**
  //  * Information about compiler runs accumulated since `clean` command has been run.
  //  *
  //  * The main use-case for using `compilations` field is to determine how
  //  * many iterations it took to compilen give code. The `Compilation` object
  //  * are also stored in `Source` objects so there's an indirect way to recover
  //  * information about files being recompiled in every iteration.
  //  *
  //  * The incremental compilation algorithm doesn't use information stored in
  //  * `compilations`. It's safe to prune contents of that field without breaking
  //  * internal consistency of the entire Analysis object.
  //  */
  // val compilations: Compilations

  /** Concatenates Analysis objects naively, i.e., doesn't internalize external deps on added files. See `Analysis.merge`. */
  def ++(other: Analysis): Analysis

  // /** Drops all analysis information for `sources` naively, i.e., doesn't externalize internal deps on removed files. */
  // def --(sources: Iterable[File]): Analysis

  // def copy(stamps: Stamps = stamps, apis: APIs = apis, relations: Relations = relations, infos: SourceInfos = infos,
  //   compilations: Compilations = compilations): Analysis

  // def addSource(src: File, api: Source, stamp: Stamp, info: SourceInfo,
  //   products: Iterable[(File, String, Stamp)],
  //   internalDeps: Iterable[InternalDependency],
  //   externalDeps: Iterable[ExternalDependency],
  //   binaryDeps: Iterable[(File, String, Stamp)]): Analysis

  // /** Partitions this Analysis using the discriminator function. Externalizes internal deps that cross partitions. */
  // def groupBy[K](discriminator: (File => K)): Map[K, Analysis]
}
