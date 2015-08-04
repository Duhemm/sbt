package sbt.incrementalcompiler

import xsbti.compile.CompileOrder

import java.io.File

/**
 * Options for an individual incremental compiler run.
 */
trait InputOptions {

  /** The compilation order for Java and Scala sources. */
  def compileOrder: CompileOrder

  /** Classpath to give to the compiler. */
  def classpath: Array[File]

  /** The set of sources that require compilation. */
  def sources: Array[File]

  /** Where to output the compiled classfiles. */
  def output: Output

  /** Options specific to the Java compiler. */
  def javacArgs: Array[String]

  /** Options specific to the Scala compiler. */
  def scalacArgs: Array[String]

}
