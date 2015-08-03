package xsbti.newcompiler;

import xsbti.Logger;
import xsbti.Reporter;

import java.io.File;

interface Compiler {
	void compile(File[] sources, File[] classpath, Output output, String[] options, Reporter reporter, Logger log);
}
