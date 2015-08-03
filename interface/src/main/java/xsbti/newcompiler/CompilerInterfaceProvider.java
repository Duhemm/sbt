package xsbti.newcompiler;

import xsbti.Logger;

import java.io.File;

interface CompilerInterfaceProvider {

	File apply(ScalaReference scalaReference, Logger logger);

}
