package xsbti.newcompiler;

import xsbti.api.Source;
import xsbti.newcompiler.incremental.BinaryDependency;
import xsbti.newcompiler.incremental.ExternalDependency;
import xsbti.newcompiler.incremental.InternalDependency;
import xsbti.newcompiler.incremental.Product;
import xsbti.newcompiler.incremental.Stamp;
import xsbti.newcompiler.incremental.Stamps;
import xsbti.newcompiler.incremental.SourceInfo;
import xsbti.newcompiler.incremental.SourceInfos;

import java.io.File;

public interface Analysis {

  Stamps stamps();
  APIs apis();
  Relations relations();
  SourceInfos infos();
  Compilations compilations();

  Analysis addSource(
    File src,
    Source api,
    Stamp stamp,
    SourceInfo info,
    Iterable<Product> products,
    Iterable<InternalDependency> internalDeps,
    Iterable<ExternalDependency> externalDeps,
    Iterable<BinaryDependency> binaryDeps);

  Analysis $plus$plus(Analysis o);
  Analysis $minus$minus(Analysis i);

  // Missing: copy
}
