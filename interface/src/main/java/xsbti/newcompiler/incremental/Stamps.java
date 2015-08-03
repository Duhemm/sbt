package xsbti.newcompiler.incremental;

import xsbti.Maybe;

import java.io.File;
import java.util.Map;
import java.util.Set;

public interface Stamps extends ReadStamps {
	Set<File> allBinaries();

	Set<File> allProducts();

	Map<File, Stamp<?>> sources();
	Map<File, Stamp<?>> binaries();
	Map<File, Stamp<?>> products();
	Map<File, String> classNames();

	Maybe<String> className(File bin);

	<T> Stamps markInternalSource(File src, Stamp<T> s);

	<T> Stamps markBinary(File bin, String className, Stamp<T> s);

	<T> Stamps markProduct(File prod, Stamp<T> s);

	// Missing: filter, groupBy

	Stamps $plus$plus(Stamps o);

}
