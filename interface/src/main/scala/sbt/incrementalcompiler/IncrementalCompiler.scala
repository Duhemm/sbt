package sbt.incrementalcompiler

/**
 * Interface to the incremental compiler.
 */
trait IncrementalCompiler {

  /** Start a new incremental compiler run. */
  def compile(options: InputOptions, reporters: Reporters): IncrementalCompilationResult

}
