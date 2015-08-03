package xsbti.newcompiler;

import java.io.File;
import java.util.Map;

import xsbti.Maybe;
import xsbti.Reporter;

interface Setup {
	/** Provides the Analysis for the given classpath entry.*/
	Maybe<Analysis> analysisMap(File file);

	/** The resulting Analysis from the previous compiler run */
	Maybe<Analysis> previousAnalysis();

	/** The file used to cache information across compilations.
	 * This file can be removed to force a full recompilation.
	 * The file should be unique and not shared between compilations. */
	File cacheFile();

	/** If returned, the progress that should be used to report scala compilation to. */
	Maybe<CompileProgress> progress();

	/** The reporter that should be used to report scala and java compilation to. */
	Reporter reporter();

	/**
	 * Returns incremental compiler options.
	 * @see sbt.inc.IncOptions for details
	 * You can get default options by calling <code>sbt.inc.IncOptions.toStringMap(sbt.inc.IncOptions.Default)</code>.
	 * In the future, we'll extend API in <code>xsbti</code> to provide factory methods that would allow to obtain
	 * defaults values so one can depend on <code>xsbti</code> package only.
	**/
	Map<String, String> incrementalCompilerOptions();

}
