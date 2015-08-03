package xsbti.newcompiler.incremental;

import xsbti.DependencyContext;

import java.io.File;

public interface InternalDependency {

	File sourceFile();
	File targetFile();
	DependencyContext context();
}
