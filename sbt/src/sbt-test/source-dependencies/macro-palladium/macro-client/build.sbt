incOptions := incOptions.value.withNameHashing(true)

// disable sbt's heauristic which recompiles everything in case
// some fraction (e.g. 50%) of files is scheduled to be recompiled
// in this test we want precise information about recompiled files
// which that heuristic would distort
incOptions := incOptions.value.copy(recompileAllFraction = 1.0)

// Check that a file has been recompiled during last compilation
InputKey[Unit]("check-recompiled") <<= inputTask { (argTask: TaskKey[Seq[String]]) =>
  (argTask, compile in Compile) map { (args: Seq[String], a: sbt.inc.Analysis) =>
    assert(args.size == 1)
    val fileCompilation = a.apis.internal.collect { case (file, src) if file.name.endsWith(args(0)) => src.compilation }.head
    val lastCompilation = a.compilations.allCompilations.last
    assert(fileCompilation.startTime == lastCompilation.startTime, "File has not been recompiled during last compilation.")
  }
}