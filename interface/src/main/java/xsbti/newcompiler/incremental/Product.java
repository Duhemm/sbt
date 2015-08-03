package xsbti.newcompiler.incremental;

import java.io.File;

public interface Product {

	File src();
	File prod();
	String name();

}
