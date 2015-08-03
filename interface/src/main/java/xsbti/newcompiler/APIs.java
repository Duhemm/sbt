package xsbti.newcompiler;

import xsbti.api.Source;

import java.util.Set;
import java.io.File;

interface APIs {
	Source internalAPIs(File src);

	Source externalAPIs(String ext);

	Set<String> allExternals();

	Set<File> allInternalSources();

	APIs $plus$plus(APIs o);

	APIs markInternalSource(File src, Source api);

	APIs markExternalAPI(String ext, Source api);

	APIs removeInternal(Iterable<File> remove);

	// APIs filterExt(Predicate<String> keep);
}
