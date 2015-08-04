package sbt.incrementalcompiler

import xsbti.AnalysisCallback

import java.io.File

/** A compiler which produces an `Analysis` during compilation. */
trait AnalyzingCompiler {

  /**
   * Starts a compilation run.
   *
   * @param sources The sources to compile.
   * @param classpath The classpath to give to the compiler.
   * @param output Where to put the generated classfiles.
   * @param options The options specific to this compiler.
   * @param reporters The reporters that will handle events during compilation.
   */
  def compile(sources: Array[File],
    classpath: Array[File],
    output: Output,
    options: Array[String],
    reporters: Reporters): Analysis
}
