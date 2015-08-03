package xsbti.newcompiler;

import java.io.File;
import java.util.Map;
import java.util.Set;

import sbt.Relation;

import xsbti.newcompiler.incremental.InternalDependency;
import xsbti.newcompiler.incremental.ExternalDependency;
import xsbti.newcompiler.incremental.BinaryDependency;
import xsbti.newcompiler.incremental.Product;

interface Relations {
	Set<File> allSources();

	Set<File> allProducts();

	Set<File> allBinaryDeps();

	Set<String> allExternalDeps();

	Set<String> classNames(File src);

	Set<File> definesClass(String name);

	Set<File> products(File src);

	Set<File> produced(File prod);

	Set<File> binaryDeps(File src);

	Set<File> usesBinary(File dep);

	Set<File> internalSrcDeps(File src);

	Set<File> usesInternalSrc(File dep);

	Set<String> externalDeps(File src);

	Set<String> usesExternal(String dep);

	Set<String> usedNames(File src);

	Relations addSource(File src,
		Iterable<Product> products,
		Iterable<InternalDependency> internalDeps,
		Iterable<ExternalDependency> externalDeps,
		Iterable<BinaryDependency> binaryDeps);

	Relations addProducts(File src, Iterable<Product> prods);

	Relations addInternalSrcDeps(File src, Iterable<InternalDependency> deps);

	Relations addExternalDeps(File src, Iterable<ExternalDependency> deps);

	Relations addBinaryDeps(File src, Iterable<BinaryDependency> deps);

	Relations addUsedName(File src, String name);

	Relations $plus$plus(Relations o);

	Relations $minus$minus(Relations o);

	Relation<File, File> srcProd();

	Relation<File, File> binaryDep();

	Relation<File, File> internalSrcDep();

	Relation<File, String> externalDep();

	Relation<File, String> classes();

	Relation<File, String> names();

	Map<String, Relation<File, ?>> allRelations();
}
