package sbt.incrementalcompiler

import java.io.File

/**
 * Represents the location where the compiled classfiles should be output.
 */
sealed trait Output extends xsbti.compile.Output

/**
 * Configuration in which all the classfiles are put inside `outputDirectory`.
 */
trait SingleOutput extends Output {

  /** The directory to where the classfiles should be output. */
  def outputDirectory: File
}

/**
 * Configuration in which each output directory depends on the input directories.
 */
trait MultipleOutput extends Output {

  /**
   * A mapping from `sourceDirectory` to `outputDirectory`.
   * A mapping from A to B means that the classfiles issued from a source file in source directory A
   * should be put in output directory B.
   */
  trait OutputGroup {
    def sourceDirectory: File
    def outputDirectory: File
  }

  /** All the mappings from source directory to output directory. */
  def outputGroups: Array[OutputGroup]

}
