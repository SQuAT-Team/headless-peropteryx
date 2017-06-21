package io.github.squat_team.performance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.eclipse.emf.ecore.resource.Resource;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.usagemodel.UsageModel;

import io.github.squat_team.model.OptimizationType;
import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMScenario;
import io.github.squat_team.util.SQuATHelper;

public abstract class AbstractPerformancePCMScenario extends PCMScenario
		implements PerformancePCMTransformationScenario {
	private PerformanceMetric metric;

	public AbstractPerformancePCMScenario(OptimizationType type) {
		super(type);
	}

	public PerformanceMetric getMetric() {
		return metric;
	}

	public void setMetric(PerformanceMetric metric) {
		this.metric = metric;
	}
}
