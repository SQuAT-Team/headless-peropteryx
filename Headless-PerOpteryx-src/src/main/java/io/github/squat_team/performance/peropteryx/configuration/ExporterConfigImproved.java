package io.github.squat_team.performance.peropteryx.configuration;

import java.util.Map;

import io.github.squat_team.performance.peropteryx.export.ExportModeImrpoved;
import io.github.squat_team.performance.peropteryx.export.OptimizationDirectionImrpoved;

public class ExporterConfigImproved extends AbstractConfigurationImproved {

	private String pcmOutputFolder = "";
	private boolean minimalExport;
	private double boundaryValue;
	private int amount;
	private ExportModeImrpoved extractionMode;
	private OptimizationDirectionImrpoved optimizationDirection;

	public String getPcmOutputFolder() {
		return pcmOutputFolder;
	}

	public void setPcmOutputFolder(String pcmOutputFolder) {
		this.pcmOutputFolder = pcmOutputFolder;
	}

	public double getBoundaryValue() {
		return boundaryValue;
	}

	public void setBoundaryValue(double boundaryValue) {
		this.boundaryValue = boundaryValue;
	}

	public ExportModeImrpoved getExportMode() {
		return extractionMode;
	}

	public void setExportMode(ExportModeImrpoved extractionMode) {
		this.extractionMode = extractionMode;
	}

	public OptimizationDirectionImrpoved getOptimizationDirection() {
		return optimizationDirection;
	}

	public void setOptimizationDirection(OptimizationDirectionImrpoved optimizationDirection) {
		this.optimizationDirection = optimizationDirection;
	}

	@Override
	protected void initializeDefault() {
		boundaryValue = 0.0;
		extractionMode = ExportModeImrpoved.PARETO;
		optimizationDirection = OptimizationDirectionImrpoved.MINIMIZE;
		minimalExport = true;
	}

	@Override
	protected Map<String, Object> copyValuesTo(Map<String, Object> attr) {
		// do nothing
		return attr;
	}

	@Override
	protected boolean validate() {
		return validatePath(pcmOutputFolder);
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public boolean isMinimalExport() {
		return minimalExport;
	}

	/**
	 * This mode will reduce the number of exported files to a minimum, but they
	 * will not be available for debugging. The benefit is a slight performance
	 * improvement.
	 * 
	 * @param minimalExport
	 */
	public void setMinimalExport(boolean minimalExport) {
		this.minimalExport = minimalExport;
	}

}
