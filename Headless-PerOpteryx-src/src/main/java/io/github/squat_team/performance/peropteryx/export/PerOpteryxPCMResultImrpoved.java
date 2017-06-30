package io.github.squat_team.performance.peropteryx.export;

/**
 * This class carries the paths to a PCM model and the result of its
 * evaluation.
 */
public class PerOpteryxPCMResultImrpoved implements Comparable<PerOpteryxPCMResultImrpoved> {

	private Double value;
	private String folderPath;
	private boolean filesExist;

	public PerOpteryxPCMResultImrpoved(Double value, String folderPath) {
		this.value = value;
		this.folderPath = folderPath;
		this.filesExist = !(folderPath == null);
	}

	public Double getValue() {
		return this.value;
	}

	public boolean filesExist() {
		return filesExist;
	}

	public String getAllocationPath() {
		return folderPath + PCMFileExporterImrpoved.FILE_PREFIX + ".allocation";
	}

	public String getRepositoryPath() {
		return folderPath + PCMFileExporterImrpoved.FILE_PREFIX + ".repository";
	}

	public String getResourceTypePath() {
		return folderPath + PCMFileExporterImrpoved.FILE_PREFIX + ".resourcetype";
	}

	public String getResourceEnvironmentPath() {
		return folderPath + PCMFileExporterImrpoved.FILE_PREFIX + ".resourceenvironment";
	}

	public String getSystemPath() {
		return folderPath + PCMFileExporterImrpoved.FILE_PREFIX + ".system";
	}

	public String getUsagemodelPath() {
		return folderPath + PCMFileExporterImrpoved.FILE_PREFIX + ".usagemodel";
	}

	@Override
	public int compareTo(PerOpteryxPCMResultImrpoved o) {
		if(o.getValue() > this.getValue()){
			return -1;
		}else if(o.getValue() == this.getValue()){
			return 0;
		}else{
			return 1;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof PerOpteryxPCMResultImrpoved) {
			PerOpteryxPCMResultImrpoved result = (PerOpteryxPCMResultImrpoved) o;
			return result.getValue().doubleValue() == this.getValue().doubleValue();
		}
		return false;
	}
}
