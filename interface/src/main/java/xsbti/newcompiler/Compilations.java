package xsbti.newcompiler;

import xsbti.api.Compilation;

interface Compilations {

	Compilation[] allCompilations();

	Compilations $plus$plus(Compilations o);

	Compilations add(Compilation c);
}
