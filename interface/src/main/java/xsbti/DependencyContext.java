package xsbti;

/**
 * Enumeration of existing dependency contexts.
 * Dependency contexts represent the various kind of dependencies that
 * can exist between symbols.
 * @see sbt.inc.DependencyContext
 */
public enum DependencyContext {
	/**
	 * Represents a direct dependency between two symbols :
	 * object Foo
	 * object Bar { def foo = Foo }
	 * @see sbt.inc.DependencyByMemberRef
	 */
	DependencyByMemberRef,

	/**
	 * Represents an inheritance dependency between two symbols :
	 * class A
	 * class B extends A
	 * @see sbt.inc.DependencyByInheritance
	 */
	DependencyByInheritance
}