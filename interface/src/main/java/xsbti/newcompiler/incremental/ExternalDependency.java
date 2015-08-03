package xsbti.newcompiler.incremental;

import xsbti.DependencyContext;
import xsbti.api.Source;

import java.io.File;

public interface ExternalDependency {

	File sourceFile();
	String targetClassName();
	Source targetSource();
	DependencyContext context();

}
