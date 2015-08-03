package xsbti.newcompiler.incremental;

/**
 * Enumeration of existing dependency contexts.
 * Dependency contexts represent the various kind of dependencies that
 * can exist between symbols.
 */
enum DependencyContext {
	/**
	 * Represents a direct dependency between two symbols :
	 * object Foo
	 * object Bar { def foo = Foo }
	 */
	DependencyByMemberRef,

	/**
	 * Represents an inheritance dependency between two symbols :
	 * class A
	 * class B extends A
	 */
	DependencyByInheritance
}
