package io.github.squat_team.performance.peropteryx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.LogManager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.palladiosimulator.pcm.core.entity.NamedElement;
import org.palladiosimulator.solver.models.PCMInstance;

import de.fakeller.performance.analysis.result.PerformanceResult;
import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMScenario;
import io.github.squat_team.model.PCMScenarioResult;
import io.github.squat_team.performance.AbstractPerformancePCMScenario;
import io.github.squat_team.performance.lqns.LQNSDetailedResultWriter;
import io.github.squat_team.performance.lqns.LQNSResult;
import io.github.squat_team.performance.lqns.LQNSResultConverter;
import io.github.squat_team.performance.lqns.LQNSResultExtractor;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;
import io.github.squat_team.performance.peropteryx.export.PerOpteryxPCMResultImrpoved;
import io.github.squat_team.util.PCMFileFinder;
import io.github.squat_team.util.PCMRepositoryModifier;
import io.github.squat_team.util.PCMWorkingCopyCreator;
import io.github.squat_team.util.SQuATHelper;

/**
 * A thread-safe and optimized implementation of the
 * {@link AbstractPerOpteryxPCMBot}. Each bot assures that its name is unique.
 * 
 * Thread-safe in this case means that the bots will behave correctly in nearly
 * all parallel execution scenarios. However, it should be avoided to run
 * analysis and search for alternatives in parallel, as this setting could still
 * have some undetected error cases.
 * 
 * Note: You should either set all bots to debugMode or none. Please note that
 * while a bot is running, other debug messages in org.apache.log4j might be
 * suppressed!
 *
 */
public class ConcurrentPerOpteryxPCMBot extends AbstractPerOpteryxPCMBot {
	private static final String ID_PREFIX = "pb";
	private static volatile int idCounter = 0;
	private static final Logger LOGGER = Logger.getLogger(ConcurrentPerOpteryxPCMBot.class.getName());

	/**
	 * 
	 * @param name
	 * @param scenario
	 *            An instance of {@link AbstractPerformancePCMScenario} is expected
	 *            as input.
	 * @param configuration
	 */
	public ConcurrentPerOpteryxPCMBot(String name, PCMScenario scenario, ConfigurationImprovedImproved configuration) {
		super(name, scenario, ConcurrentPerOpteryxPCMBot.QA_PERFORMANCE, configuration);
		id = generateUniqueName();
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
		currentModelName = (new PCMFileFinder(currentArchitecture)).getName();
		try {
			PCMWorkingCopyCreator workingCopyCreator = new PCMWorkingCopyCreator(currentModelName, id);
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
			java.lang.System.out.println(currentArchitecture.getName());
			java.lang.System.err.println(e);
			LOGGER.error(e.getMessage(), e);
			result = null;
		}
		resetTemporarilyChangedConfiurationValues();
		return result;
	}

	@Override
	public synchronized List<PCMScenarioResult> searchForAlternatives(PCMArchitectureInstance currentArchitecture) {
		List<PCMScenarioResult> results;
		loadTemporarilyChangedConfiurationValues();
		currentModelName = (new PCMFileFinder(currentArchitecture)).getName();
		try {
			PCMWorkingCopyCreator workingCopyCreator = new PCMWorkingCopyCreator(currentModelName, id);
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
			List<PerOpteryxPCMResultImrpoved> peropteryxResult;
			synchronized (ConcurrentPerOpteryxPCMBot.class) {
				deactivateLog();
				Future<List<PerOpteryxPCMResultImrpoved>> future = runPerOpteryx(copiedArchitecture, true);
				peropteryxResult = getPerOpteryxResults(future);
				activateLog();
			}
			results = PerOpteryxResultConverter.convert(peropteryxResult, this);
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

	/**
	 * If this is the last bot leaving the critical part, the logging will be
	 * activated.
	 */
	protected synchronized void activateLog() {
		if (!debugMode) {
			RunningBotsRegistry registry = RunningBotsRegistry.getInstance();
			if (!registry.moreThanOneBotRunning()) {
				org.apache.log4j.LogManager.getRootLogger().setLevel(registry.getLoglevel());
			}
			registry.deregisterBot(this);
		}
	}

	/**
	 * If this is the first bot leaving the ciritcal part, the logging will be
	 * deactivated
	 */
	protected synchronized void deactivateLog() {
		if (!debugMode) {
			RunningBotsRegistry registry = RunningBotsRegistry.getInstance();
			registry.registerBot(this);
			if (!registry.moreThanOneBotRunning()) {
				LogManager.getLogManager().reset();
				registry.setLoglevel(org.apache.log4j.LogManager.getRootLogger().getLevel());
				org.apache.log4j.LogManager.getRootLogger().setLevel(Level.OFF);
			}
		}
	}

}
