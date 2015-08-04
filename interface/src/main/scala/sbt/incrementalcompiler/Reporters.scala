package sbt.incrementalcompiler

import xsbti.{ Logger, Reporter }

/**
 * Set of objects to which events should be reported.
 */
trait Reporters {

  /** The logger to which log events will be reported. */
  def logger: Logger

  /** The compilation reporter to which compiler messages will be reported. */
  def compileReporter: CompileReporter

  /** The progress reporter which will be notified of the different steps of Scala compilation. */
  def progress: CompileProgress

}
