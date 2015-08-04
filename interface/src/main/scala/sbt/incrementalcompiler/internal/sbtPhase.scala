package sbt.incrementalcompiler.internal

/**
 * Represents a phase during which sbt inspects the trees given by the compiler
 * and registers information about them.
 */
trait sbtPhase {

  /** The compiler */
  def global: CallbackGlobal

  /** Runs the phase */
  def run(tree: global.Tree): Unit

}
