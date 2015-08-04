package sbt.incrementalcompiler

/**
 * An object holding two `AnalyzingCompiler`s:
 *  - One for Java compilation
 *  - One for Scala compilation
 */
trait Compilers {

  /** The `AnalyzingCompiler` to use to compile Java code. */
  def javac: AnalyzingCompiler

  /** The `AnalyzingCompiler` to use to compile Scala code. */
  def scalac: AnalyzingCompiler

}
