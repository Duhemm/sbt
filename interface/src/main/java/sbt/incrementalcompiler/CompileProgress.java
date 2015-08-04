package sbt.incrementalcompiler;

/**
 * An API for reporting when files are being compiled.
 *
 * Note; This is tied VERY SPECIFICALLY to scala.
 */
public interface CompileProgress extends xsbti.compile.CompileProgress { }
