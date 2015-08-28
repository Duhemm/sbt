InputKey[Unit]("count-projects") <<= inputTask { (argTask: TaskKey[Seq[String]]) =>
  (argTask, projects) map { (args: Seq[String], p: Seq[ProjectDefinition[_]]) =>
    assert(args.length == 1)
    val projectsCount = p.length
    if (projectsCount != args.head.toInt) error("Expected " + args.head + " projects, but counted " + projectsCount)
  }
}
