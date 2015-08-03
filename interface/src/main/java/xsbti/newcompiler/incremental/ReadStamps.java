package xsbti.newcompiler.incremental;

import java.io.File;

interface ReadStamps {
	/** The Stamp for the given product at the time represented by this Stamps instance.*/
	<T> Stamp<T> product(File prod);

	/** The Stamp for the given source file at the time represented by this Stamps instance.*/
	<T> Stamp<T> internalSource(File src);

	/** The Stamp for the given binary dependency at the time represented by this Stamps instance.*/
	<T> Stamp<T> binary(File bin);
}
