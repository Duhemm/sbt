package xsbti.newcompiler.incremental;

import xsbti.Problem;

public interface SourceInfo {
	Iterable<Problem> reportedProblems();

	Iterable<Problem> unreportedProblems();
}
