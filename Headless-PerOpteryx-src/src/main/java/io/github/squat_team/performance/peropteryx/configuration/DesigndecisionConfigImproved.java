package io.github.squat_team.performance.peropteryx.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configure the automatic generation of the designdecision file, e.g., to
 * always use specific boundary values for the CPU clock rate.
 */
public class DesigndecisionConfigImproved extends AbstractConfigurationImproved {
	private HashMap<String, Limit> cpuLimits = new HashMap<>();

	/**
	 * A CPU must always have a lower and an upper limit for its clock rate.
	 */
	private class Limit {
		private double lowerLimit;
		private double upperLimit;

		public Limit(double lowerLimit, double upperLimit) {
			this.lowerLimit = lowerLimit;
			this.upperLimit = upperLimit;
		}
	}

	/**
	 * Sets the clock rate limits for a specific CPU.
	 * 
	 * @param serverId
	 *            the id of the server owning the CPU in the PCM model.
	 * @param lowerLimit
	 *            the lower limit for the CPU clock rate.
	 * @param upperLimit
	 *            the upper limit for the CPU clock rate.
	 */
	public void setLimits(String serverId, double lowerLimit, double upperLimit) {
		cpuLimits.put(serverId, new Limit(lowerLimit, upperLimit));
	}

	/**
	 * Checks whether changes to the default Designdecision file are necessary.
	 * 
	 * @return Changes are necessary, if true. If not, the original automatically generated file can be used.
	 */
	public boolean isChangeToDesigndecisionFileNecessary() {
		return !cpuLimits.isEmpty();
	}

	/**
	 * Checks whether a specific CPU has specified limits.
	 * 
	 * @param serverId
	 *            the id of the server owning the CPU in the PCM model.
	 * @return true if limits are set.
	 */
	public boolean hasLimit(String serverId) {
		return cpuLimits.containsKey(serverId);
	}

	/**
	 * Get the lower limit for a CPU clock rate. Should only be accessed when
	 * {@link #hasLimit(String)} is true.
	 * 
	 * @param serverId
	 *            the id of the server owning the CPU in the PCM model.
	 * @return the lower limit for the CPU clock rate.
	 */
	public Double getLowerLimit(String serverId) {
		return cpuLimits.get(serverId).lowerLimit;
	}

	/**
	 * Get the upper limit for a CPU clock rate. Should only be accessed when
	 * {@link #hasLimit(String)} is true.
	 * 
	 * @param serverId
	 *            the id of the server owning the CPU in the PCM model.
	 * @return the upper limit for the CPU clock rate.
	 */
	public Double getUpperLimit(String serverId) {
		return cpuLimits.get(serverId).upperLimit;
	}

	/**
	 * Get all the server IDs that have boundary values configured in the configuration.
	 * 
	 * @return IDs of the configured servers owning the CPUs.
	 */
	public Set<String> getConfiguredServerIds() {
		return cpuLimits.keySet();
	}

	@Override
	protected void initializeDefault() {
		// do nothing
	}

	@Override
	protected Map<String, Object> copyValuesTo(Map<String, Object> attr) {
		return attr;
	}

	@Override
	protected boolean validate() {
		for (Limit limit : cpuLimits.values()) {
			if (limit.lowerLimit < 0 || limit.upperLimit < limit.lowerLimit) {
				return false;
			}
		}
		return true;
	}
}
