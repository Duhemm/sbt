package xsbti.newcompiler.incremental;

import java.io.File;

interface ExternalDependencyChanges {
	boolean isEmpty();

	File[] modifiedBinaries();

	String[] modifiedClasses();
}
