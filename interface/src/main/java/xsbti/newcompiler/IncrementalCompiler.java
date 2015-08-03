package xsbti.newcompiler;

import xsbti.Logger;
import xsbti.Reporter;

interface IncrementalCompiler {

	CompilationResult compile(Inputs in, Logger logger, Reporter reporter);

}
