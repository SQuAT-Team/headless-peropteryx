package io.github.squat_team.performance.peropteryx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.LogManager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.palladiosimulator.pcm.core.entity.NamedElement;
import org.palladiosimulator.solver.models.PCMInstance;

import de.fakeller.palladio.analysis.pcm2lqn.runner.PcmLqnsAnalyzer;
import de.fakeller.palladio.analysis.pcm2lqn.runner.PcmLqnsAnalyzerConfig;
import de.fakeller.palladio.analysis.pcm2lqn.runner.PcmLqnsAnalyzerContext;
import de.fakeller.palladio.analysis.provider.FileSystemProvider;
import de.fakeller.palladio.config.PcmModelConfig;
import de.fakeller.performance.analysis.result.PerformanceResult;
import io.github.squat_team.AbstractPCMBot;
import io.github.squat_team.model.OptimizationType;
import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMScenarioResult;
import io.github.squat_team.performance.AbstractPerformancePCMScenario;
import io.github.squat_team.performance.lqns.LQNSResultConverter;
import io.github.squat_team.performance.lqns.LQNSResultExtractor;
import io.github.squat_team.performance.lqns.LQNSDetailedResultWriter;
import io.github.squat_team.performance.lqns.LQNSResult;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;
import io.github.squat_team.performance.peropteryx.configuration.PerOpteryxConfigImproved.Mode;
import io.github.squat_team.performance.peropteryx.environment.PalladioEclipseEnvironmentImrpoved;
import io.github.squat_team.performance.peropteryx.export.ExportModeImrpoved;
import io.github.squat_team.performance.peropteryx.export.OptimizationDirectionImrpoved;
import io.github.squat_team.performance.peropteryx.export.PerOpteryxPCMResultImrpoved;
import io.github.squat_team.performance.peropteryx.start.MyHeadlessPerOpteryxRunnerImrpoved;
import io.github.squat_team.util.DesigndecisionFileModifier;
import io.github.squat_team.util.PCMFileFinder;
import io.github.squat_team.util.PCMRepositoryModifier;
import io.github.squat_team.util.PCMWorkingCopyCreator;
import io.github.squat_team.util.SQuATHelper;

public class PerOpteryxPCMBot extends AbstractPCMBot {
	private static Logger logger = Logger.getLogger(PerOpteryxPCMBot.class.getName());
	private Level loglevel;
	private ConfigurationImprovedImproved configuration;
	private AbstractPerformancePCMScenario performanceScenario;
	private Boolean debugMode = false;
	private Boolean detailedAnalysis = false;

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
	public PerOpteryxPCMBot(AbstractPerformancePCMScenario scenario, ConfigurationImprovedImproved configuration) {
		super(scenario);
		this.configuration = configuration;
		this.performanceScenario = scenario;
	}

	@Override
	public PCMScenarioResult analyze(PCMArchitectureInstance currentArchitecture) {
		try {
			PCMWorkingCopyCreator workingCopyCreator = new PCMWorkingCopyCreator();
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
			return LQNSResultConverter.convert(currentArchitecture, lqnsResult, performanceScenario.getMetric(), this);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
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
			List<PCMScenarioResult> results = exportOptimizationResults(future);
			if (detailedAnalysis) {
				analyzeDetailed(results);
			}
			separateAll(results, repositoryModifier);
			inverseTransformAll(results);
			SQuATHelper.delete(copiedArchitecture);
			return results;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new ArrayList<PCMScenarioResult>();
		}
	}

	private void separateAll(List<PCMScenarioResult> results, PCMRepositoryModifier repositoryModifier) {
		for (PCMScenarioResult result : results) {
			repositoryModifier.separateRepository(result.getResultingArchitecture());
		}
	}

	private void inverseTransformAll(List<PCMScenarioResult> results) {
		for (PCMScenarioResult result : results) {
			performanceScenario.inverseTransform(result.getResultingArchitecture());
		}
	}

	private void setupEnvironmentforAnalysis() {
		PalladioEclipseEnvironmentImrpoved.INSTANCE.setup(configuration.getPcmModelsConfig().getPathmapFolder());
		de.fakeller.palladio.environment.PalladioEclipseEnvironment.INSTANCE.setup();
	}

	private PCMInstance buildPcmInstanceForAnalysis() {
		PcmModelConfig pcmConfig = new PcmModelConfig();
		pcmConfig.setAllocationModel(configuration.getPcmInstanceConfig().getAllocationModel());
		pcmConfig.setUsageModel(configuration.getPcmInstanceConfig().getUsageModel());
		FileSystemProvider provider = new FileSystemProvider(pcmConfig);
		return provider.provide();
	}

	private String executeHeadlessLqns(PCMInstance pcmInstance) {
		final PcmLqnsAnalyzerConfig config = PcmLqnsAnalyzerConfig.defaultConfig();
		final PcmLqnsAnalyzer analyzer = new PcmLqnsAnalyzer(config);
		final PcmLqnsAnalyzerContext ctx = analyzer.setupAnalysis(pcmInstance);
		ctx.executePalladio();
		return config.getOutputPath();
	}

	private void deactivateLog() {
		if (!debugMode) {
			LogManager.getLogManager().reset();
			loglevel = org.apache.log4j.LogManager.getRootLogger().getLevel();
			org.apache.log4j.LogManager.getRootLogger().setLevel(Level.OFF);
		}
	}

	private void activateLog() {
		if (!debugMode) {
			org.apache.log4j.LogManager.getRootLogger().setLevel(loglevel);
		}
	}

	private List<PCMScenarioResult> exportOptimizationResults(Future<List<PerOpteryxPCMResultImrpoved>> future) {
		try {
			List<PerOpteryxPCMResultImrpoved> peropteryxResult = future.get();
			return PerOpteryxResultConverter.convert(peropteryxResult, this);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
			return new ArrayList<PCMScenarioResult>();
		} catch (ExecutionException e) {
			logger.error(e.getMessage(), e);
			return new ArrayList<PCMScenarioResult>();
		}
	}

	private Future<List<PerOpteryxPCMResultImrpoved>> runPerOpteryx(PCMArchitectureInstance architecture) {
		modifyDesigndecisionFile(architecture);
		MyHeadlessPerOpteryxRunnerImrpoved runner = new MyHeadlessPerOpteryxRunnerImrpoved();
		runner.init(configuration);
		runner.setDebugMode(this.debugMode);
		return ThreadPoolProvider.POOL.submit(runner);
	}

	private void modifyDesigndecisionFile(PCMArchitectureInstance architecture) {
		if (configuration.getDesignDecisionConfig().isChangeToDesigndecisionFileNecessary()) {
			if (configuration.getPerOpteryxConfig().getMode().equals(Mode.OPTIMIZE)) {
				modifyDesignDecisionFile(new File(configuration.getPerOpteryxConfig().getDesignDecisionFile()));
			} else if (configuration.getPerOpteryxConfig().getMode().equals(Mode.DESIGN_DECISIONS_AND_OPTIMIZE)) {
				runDesignDecisionCreation();
				updateDesignDecisionFileLocation(architecture);
			}
		}
	}

	private void runDesignDecisionCreation() {
		configuration.getPerOpteryxConfig().setMode(Mode.DESIGN_DECISIONS);
		MyHeadlessPerOpteryxRunnerImrpoved runner = new MyHeadlessPerOpteryxRunnerImrpoved();
		runner.init(configuration);
		runner.setDebugMode(this.debugMode);
		Future<List<PerOpteryxPCMResultImrpoved>> tempResult = ThreadPoolProvider.POOL.submit(runner);
		try {
			// IMPORTANT: Call get to make sure the run terminated!
			tempResult.get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		configuration.getPerOpteryxConfig().setMode(Mode.OPTIMIZE);
	}

	/**
	 * Sets the location of the created designdecision file in the configuration.
	 * 
	 * @param architecture
	 *            The model that should be modified.
	 */
	private void updateDesignDecisionFileLocation(PCMArchitectureInstance architecture) {
		PCMFileFinder fileFinder = new PCMFileFinder(architecture);
		String designDecisionPath = fileFinder.getPath() + File.separator + "default" + ".designdecision";
		modifyDesignDecisionFile(new File(designDecisionPath));

		configuration.getPerOpteryxConfig().setDesignDecisionFile("file:" + designDecisionPath);
	}

	private void modifyDesignDecisionFile(File designdecisionFile) {
		DesigndecisionFileModifier designdecisionFileModifier = new DesigndecisionFileModifier(designdecisionFile,
				configuration.getDesignDecisionConfig());
		designdecisionFileModifier.modify();
	}

	private void configurePerOpteryx() {
		String designDecisionFile = configuration.getPerOpteryxConfig().getDesignDecisionFile();
		if (designDecisionFile == null || designDecisionFile.isEmpty()) {
			configuration.getPerOpteryxConfig().setMode(Mode.DESIGN_DECISIONS_AND_OPTIMIZE);
		} else {
			configuration.getPerOpteryxConfig().setMode(Mode.OPTIMIZE);
		}
	}

	private void configurePerOpteryxForOptimization() {
		// TODO: choose good values
		configuration.getTacticsConfig().useTactics(true);
		if (configuration.getPerOpteryxConfig().getGenerationSize() <= 1
				&& configuration.getPerOpteryxConfig().getMaxIterations() <= 1) {
			configuration.getPerOpteryxConfig().setGenerationSize(100);
			configuration.getPerOpteryxConfig().setMaxIterations(20);
		}

		// TODO: use a stop criteria? Will improve speed in many cases
		configuration.getTerminationCriteriaConfig().setActivateTerminationCriteria(true);
		configuration.getTerminationCriteriaConfig().setActivateInsignificantFrontChange(true);
		configuration.getTerminationCriteriaConfig().setInsignificantFrontChangeGenerationNumber(4);
		configuration.getTerminationCriteriaConfig().setInsignificantFrontChangeImprovementPercentage(3);
	}

	private void configureExportForOptimization() {
		// TODO: which results should be exported? all/better than expected/x
		// best/only the best?
		configuration.getExporterConfig().setAmount(10);
		configuration.getExporterConfig().setExportMode(ExportModeImrpoved.AMOUNT);
	}

	private void configureWith(PCMArchitectureInstance currentArchitecture) {
		String allocationPath = currentArchitecture.getAllocation().eResource().getURI().toString();
		String usagemodelPath = currentArchitecture.getUsageModel().eResource().getURI().toString();
		allocationPath = allocationPath.replaceAll("file:", "");
		usagemodelPath = usagemodelPath.replaceAll("file:", "");
		configuration.getPcmInstanceConfig().setAllocationModel(allocationPath);
		configuration.getPcmInstanceConfig().setUsageModel(usagemodelPath);
	}

	private void configureWith(AbstractPerformancePCMScenario scenario) throws IOException {
		configureOptimizationDirection(scenario.getType());
		configureBoundaryValue(scenario.getExpectedResult().getResponse());
		String qmlPath = configuration.getPerOpteryxConfig().getQmlDefinitionFile();
		if (qmlPath == null || qmlPath.isEmpty()) {
			String generatedQmlFilePath = PerOpteryxQMLConverter
					.convert(configuration.getPcmInstanceConfig().getUsageModel(), scenario);
			configuration.getPerOpteryxConfig().setQmlDefinitionFile(generatedQmlFilePath);
		}
	}

	private void configureOptimizationDirection(OptimizationType type) {
		if (type == OptimizationType.MINIMIZATION) {
			configuration.getExporterConfig().setOptimizationDirection(OptimizationDirectionImrpoved.MINIMIZE);
		} else if (type == OptimizationType.MAXIMIZATION) {
			configuration.getExporterConfig().setOptimizationDirection(OptimizationDirectionImrpoved.MAXIMIZE);
		}
	}

	@SuppressWarnings("rawtypes")
	private void configureBoundaryValue(Comparable comparable) {
		// TODO: maybe there is a better way to do this: either change interface
		// type or type in peropteryx, but just comparable is maybe to general -
		// not needed if we don't want the ExportMode to be 'Better'
		if (comparable instanceof Double) {
			configuration.getExporterConfig().setBoundaryValue((Double) comparable);
		} else if (comparable instanceof Float) {
			Float value = (Float) comparable;
			configuration.getExporterConfig().setBoundaryValue(value.doubleValue());
		} else if (comparable instanceof Integer) {
			Integer value = (Integer) comparable;
			configuration.getExporterConfig().setBoundaryValue(value.doubleValue());
		}
	}

	private void validateConfiguration() {
		if (configuration == null) {
			throw new RuntimeException("Headless PerOpteryx needs a configuration");
		}
		if (!configuration.validate()) {
			throw new RuntimeException("Configuration for Headless PerOpteryx incomplete");
		}
	}

	public Boolean getDebugMode() {
		return debugMode;
	}

	/**
	 * Prints all information from the loggers to the console. This option requires
	 * a higher computational effort and is therefore deactivated by default.
	 * 
	 * @param debugMode
	 *            true activates the unfiltered console output
	 */
	public void setDebugMode(Boolean debugMode) {
		this.debugMode = debugMode;
	}

	public Boolean getDetailedAnalysis() {
		return detailedAnalysis;
	}

	/**
	 * The detailed analysis writes additional information to the destination of the
	 * pcm instances. This includes the utilization of the components and servers
	 * used in the model. This option requires a higher computational effort and is
	 * therefore deactivated by default.
	 * 
	 * @param detailedAnalysis
	 *            true activates the detailed analysis
	 */
	public void setDetailedAnalysis(Boolean detailedAnalysis) {
		this.detailedAnalysis = detailedAnalysis;
	}

}
