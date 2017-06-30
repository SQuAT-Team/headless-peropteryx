package io.github.squat_team.performance.peropteryx.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigurationImprovedImproved extends AbstractConfigurationImproved {
	private List<AbstractConfigurationImproved> configs;

	private PCMInstanceConfigImproved pcmInstanceConfig;
	private LQNSConfigImproved lqnsConfig;
	private TacticsConfigImrpoved tacticsConfig;
	private PCMModelsConfigImproved pcmModelsConfig;
	private PerOpteryxConfigImproved perOpteryxConfig;
	private TerminationCriteriaConfigImrpoved terminationCriteriaConfig;
	
	private ExporterConfigImproved exporterConfig;

	@Override
	public void initializeDefault() {
		configs  = new ArrayList<AbstractConfigurationImproved>();
		
		pcmInstanceConfig = new PCMInstanceConfigImproved();
		lqnsConfig = new LQNSConfigImproved();
		tacticsConfig = new TacticsConfigImrpoved();
		pcmModelsConfig = new PCMModelsConfigImproved();
		perOpteryxConfig = new PerOpteryxConfigImproved();
		exporterConfig = new ExporterConfigImproved();
		terminationCriteriaConfig = new TerminationCriteriaConfigImrpoved();

		configs.add(pcmInstanceConfig);
		configs.add(lqnsConfig);
		configs.add(tacticsConfig);
		configs.add(pcmModelsConfig);
		configs.add(perOpteryxConfig);
		configs.add(exporterConfig);
		configs.add(terminationCriteriaConfig);
	}

	@Override
	public Map<String, Object> copyValuesTo(Map<String, Object> attr) {
		for (AbstractConfigurationImproved config : configs) {
			config.copyValuesTo(attr);
		}
		return attr;
	}

	@Override
	public boolean validate() {
		for (AbstractConfigurationImproved config : configs) {
			if (!config.validate()) {
				return false;
			}
		}
		return true;
	}

	
	public PCMInstanceConfigImproved getPcmInstanceConfig() {
		return pcmInstanceConfig;
	}

	public LQNSConfigImproved getLqnsConfig() {
		return lqnsConfig;
	}

	public TacticsConfigImrpoved getTacticsConfig() {
		return tacticsConfig;
	}

	public PCMModelsConfigImproved getPcmModelsConfig() {
		return pcmModelsConfig;
	}

	public PerOpteryxConfigImproved getPerOpteryxConfig() {
		return perOpteryxConfig;
	}

	public ExporterConfigImproved getExporterConfig() {
		return exporterConfig;
	}

	public TerminationCriteriaConfigImrpoved getTerminationCriteriaConfig() {
		return terminationCriteriaConfig;
	}

	public void setTerminationCriteriaConfig(TerminationCriteriaConfigImrpoved terminationCriteriaConfig) {
		this.terminationCriteriaConfig = terminationCriteriaConfig;
	}
	
}
