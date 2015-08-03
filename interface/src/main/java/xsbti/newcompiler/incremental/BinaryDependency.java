package xsbti.newcompiler.incremental;

import java.io.File;

public interface BinaryDependency {

	File sourceFile();
	String binaryFile();
	Stamp stamp();

}
