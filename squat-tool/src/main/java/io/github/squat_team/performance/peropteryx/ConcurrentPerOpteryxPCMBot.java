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
import io.github.squat_team.performance.lqns.LQNSDetailedResultWriter;
import io.github.squat_team.performance.lqns.LQNSResult;
import io.github.squat_team.performance.lqns.LQNSResultConverter;
import io.github.squat_team.performance.lqns.LQNSResultExtractor;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;
import io.github.squat_team.performance.peropteryx.export.PerOpteryxPCMResultImrpoved;
import io.github.squat_team.util.PCMRepositoryModifier;
import io.github.squat_team.util.PCMWorkingCopyCreator;
import io.github.squat_team.util.SQuATHelper;

/**
 * A thread-safe and optimized implementation of the
 * {@link AbstractPerOpteryxPCMBot}. Each bot assures that its name is unique.
 *
 */
public class ConcurrentPerOpteryxPCMBot extends AbstractPerOpteryxPCMBot {
	private static final String ID_PREFIX = "pb";
	private static volatile int idCounter = 0;
	private static final Logger LOGGER = Logger.getLogger(ConcurrentPerOpteryxPCMBot.class.getName());

	public ConcurrentPerOpteryxPCMBot(AbstractPerformancePCMScenario scenario,
			ConfigurationImprovedImproved configuration) {
		super(scenario, configuration);
		botName = generateUniqueName();
	}

	/**
	 * Generates a unique name for the bot.
	 * 
	 * @return a unique name.
	 */
	private static synchronized String generateUniqueName() {
		String uniqueName = ID_PREFIX + idCounter;
		idCounter++;
		return uniqueName;
	}

	@Override
	public synchronized PCMScenarioResult analyze(PCMArchitectureInstance currentArchitecture) {
		PCMScenarioResult result;
		loadTemporarilyChangedConfiurationValues();
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
			LOGGER.error(e.getMessage(), e);
			result = null;
		}
		resetTemporarilyChangedConfiurationValues();
		return result;
	}

	@Override
	public synchronized PCMScenarioResult analyze(PCMArchitectureInstance currentArchitecture, String botName) {
		return analyze(currentArchitecture);
	}

	@Override
	public synchronized List<PCMScenarioResult> searchForAlternatives(PCMArchitectureInstance currentArchitecture) {
		List<PCMScenarioResult> results;
		loadTemporarilyChangedConfiurationValues();
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
			Future<List<PerOpteryxPCMResultImrpoved>> future = runPerOpteryx(copiedArchitecture);
			results = exportOptimizationResults(future);
			if (detailedAnalysis) {
				analyzeDetailed(results);
			}
			separateAll(results, repositoryModifier);
			inverseTransformAll(results);
			SQuATHelper.delete(copiedArchitecture);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			results = new ArrayList<PCMScenarioResult>();
		}
		resetTemporarilyChangedConfiurationValues();
		return results;
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
			LOGGER.error(e.getMessage(), e);
		}
	}

	private void analyzeDetailed(List<PCMScenarioResult> results) {
		for (PCMScenarioResult result : results) {
			analyzeDetailed(result.getResultingArchitecture(), result.getResultingArchitecture());
		}
	}
	
	protected void activateLog() {
		
	}
	
	protected void deactivateLog() {
		
	}

}
