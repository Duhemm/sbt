package sbt.incrementalcompiler

import java.io.File

trait AnalysisCache {

  /**
   * Retrieve the `Analysis` for a certain classpath entry.
   * @param  cpEntry The classpath entry whose `Analysis` to retrieve.
   * @return The `Analysis` corresponding to the requested classpath entry.
   */
  def lookup(cpEntry: File): Option[Analysis]

  /** The most recent `Analysis` in this cache. */
  def previousAnalysis: Analysis

  /** Adds a new `Analysis` in the cache. */
  def save(analysis: Analysis): AnalysisCache

}
