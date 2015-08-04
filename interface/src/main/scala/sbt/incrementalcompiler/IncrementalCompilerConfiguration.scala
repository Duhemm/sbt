package sbt.incrementalcompiler

/**
 * The configuration of the incremental compiler
 */
trait IncrementalCompilerConfiguration {

  /** The cache to use to store `Analysis` objects. */
  def cache: AnalysisCache

  /** Options specific to the incremental compiler. */
  def incrementalOptions: Map[String, String]

  /** The pair of compilers to use to compile code. */
  def compilers: Compilers

  /** An incremental compiler ready to run with this configuration. */
  def incrementalCompiler: IncrementalCompiler
}
