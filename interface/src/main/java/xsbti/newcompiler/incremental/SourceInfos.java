package xsbti.newcompiler.incremental;

import java.io.File;
import java.util.Map;

public interface SourceInfos {

	SourceInfos $plus$plus(SourceInfos o);

	SourceInfos add(File file, SourceInfo info);

	SourceInfos $minus$minus(Iterable<File> files);

	SourceInfo get(File file);

	Map<File, SourceInfo> allInfos();

}
