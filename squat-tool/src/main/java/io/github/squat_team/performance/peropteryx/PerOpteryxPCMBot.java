package io.github.squat_team.performance.peropteryx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.palladiosimulator.pcm.core.entity.NamedElement;
import org.palladiosimulator.solver.models.PCMInstance;

import de.fakeller.performance.analysis.result.PerformanceResult;
import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMScenarioResult;
import io.github.squat_team.performance.AbstractPerformancePCMScenario;
import io.github.squat_team.performance.lqns.LQNSResultConverter;
import io.github.squat_team.performance.lqns.LQNSResultExtractor;
import io.github.squat_team.performance.lqns.LQNSDetailedResultWriter;
import io.github.squat_team.performance.lqns.LQNSResult;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;
import io.github.squat_team.performance.peropteryx.export.PerOpteryxPCMResultImrpoved;
import io.github.squat_team.util.PCMFileFinder;
import io.github.squat_team.util.PCMRepositoryModifier;
import io.github.squat_team.util.PCMWorkingCopyCreator;
import io.github.squat_team.util.SQuATHelper;

public class PerOpteryxPCMBot extends AbstractPerOpteryxPCMBot {
	private static Logger logger = Logger.getLogger(PerOpteryxPCMBot.class.getName());

	/**
	 * This bot uses a LQN solver to analyze, and PerOpteryx (based on the LQN
	 * solver) to optimize architectures. All PCM files have to be in the same
	 * directory! If the bot fails, null/a empty list will be returned.
	 * 
	 * @param scenario
	 * @param configuration
	 *            the configuration should at least contain the paths to the general
	 *            pcm files (not the instance!) and the paths for the export of the
	 *            pcm models (should not contain other data/folders!). A QML file
	 *            and a designdecision file will be generated automatically, if no
	 *            path is given. Some values will be added or overwritten later.
	 */
	public PerOpteryxPCMBot(AbstractPerformancePCMScenario scenario, ConfigurationImprovedImproved configuration,
			String botName) {
		super(scenario, PerOpteryxPCMBot.QA_PERFORMANCE, configuration, botName);
	}

	@Override
	public PCMScenarioResult analyze(PCMArchitectureInstance currentArchitecture) {
		return analyze(currentArchitecture, this.botName);
	}

	@Override
	public PCMScenarioResult analyze(PCMArchitectureInstance currentArchitecture, String botName) {
		PCMScenarioResult result;
		loadTemporarilyChangedConfiurationValues();
		currentModelName = (new PCMFileFinder(currentArchitecture)).getName();
		try {
			PCMWorkingCopyCreator workingCopyCreator = new PCMWorkingCopyCreator(botName);
			PCMArchitectureInstance copiedArchitecture = workingCopyCreator.createWorkingCopy(currentArchitecture);
			performanceScenario.transform(copiedArchitecture);
			configureWith(copiedArchitecture);
			configureWith(this.performanceScenario);
			deactivateLog();
			setupEnvironmentforAnalysis();
			PCMInstance pcmInstance = buildPcmInstanceForAnalysis();
			String outputPath = executeHeadlessLqns(pcmInstance);
			LQNSResult lqnsResult = LQNSResultExtractor.extract(pcmInstance, configuration, outputPath);
			activateLog();
			if (detailedAnalysis) {
				analyzeDetailed(copiedArchitecture, currentArchitecture);
			}
			SQuATHelper.delete(copiedArchitecture);
			result = LQNSResultConverter.convert(currentArchitecture, lqnsResult, performanceScenario.getMetric(),
					this);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			result = null;
		}
		resetTemporarilyChangedConfiurationValues();
		return result;
	}

	/**
	 * Perform a detailed analysis and create output file in human readable form.
	 * 
	 * @param architectureToAnalyze
	 *            this architecture will be analyzed.
	 * @param originalArchitecture
	 *            the output file will be created on the properties of this
	 *            architecture.
	 */
	private void analyzeDetailed(PCMArchitectureInstance architectureToAnalyze,
			PCMArchitectureInstance originalArchitecture) {
		try {
			configureWith(architectureToAnalyze);
			configureWith(this.performanceScenario);
			deactivateLog();
			setupEnvironmentforAnalysis();
			PCMInstance pcmInstance = buildPcmInstanceForAnalysis();

			PerOpteryxPCMDetailedAnalyser detailedAnalyser = new PerOpteryxPCMDetailedAnalyser(pcmInstance);
			PerformanceResult<NamedElement> analysisResult = detailedAnalyser.analyze();
			LQNSDetailedResultWriter detailedWriter = new LQNSDetailedResultWriter(analysisResult);
			File exportDestination = LQNSDetailedResultWriter.determineFileDestination(originalArchitecture);
			detailedWriter.writeTo(exportDestination);

			activateLog();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void analyzeDetailed(List<PCMScenarioResult> results) {
		for (PCMScenarioResult result : results) {
			analyzeDetailed(result.getResultingArchitecture(), result.getResultingArchitecture());
		}
	}

	@Override
	public List<PCMScenarioResult> searchForAlternatives(PCMArchitectureInstance currentArchitecture) {
		List<PCMScenarioResult> results;
		loadTemporarilyChangedConfiurationValues();
		currentModelName = (new PCMFileFinder(currentArchitecture)).getName();
		try {
			PCMWorkingCopyCreator workingCopyCreator = new PCMWorkingCopyCreator();
			PCMArchitectureInstance copiedArchitecture = workingCopyCreator.createWorkingCopy(currentArchitecture);
			PCMRepositoryModifier repositoryModifier = new PCMRepositoryModifier(copiedArchitecture);
			repositoryModifier.mergeRepositories();
			performanceScenario.transform(copiedArchitecture);
			configureWith(copiedArchitecture);
			configureWith(this.performanceScenario);
			configurePerOpteryx();
			configureExportForOptimization();
			configurePerOpteryxForOptimization();
			validateConfiguration();
			Future<List<PerOpteryxPCMResultImrpoved>> future = runPerOpteryx(copiedArchitecture, this.debugMode);
			List<PerOpteryxPCMResultImrpoved> peropteryxResult = getPerOpteryxResults(future);
			results = PerOpteryxResultConverter.convert(peropteryxResult, this);
			if (detailedAnalysis) {
				analyzeDetailed(results);
			}
			separateAll(results, repositoryModifier);
			inverseTransformAll(results);
			SQuATHelper.delete(copiedArchitecture);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			results = new ArrayList<PCMScenarioResult>();
		}
		resetTemporarilyChangedConfiurationValues();
		return results;
	}

}
