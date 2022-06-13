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
import org.palladiosimulator.solver.models.PCMInstance;

import de.fakeller.palladio.analysis.pcm2lqn.runner.PcmLqnsAnalyzer;
import de.fakeller.palladio.analysis.pcm2lqn.runner.PcmLqnsAnalyzerConfig;
import de.fakeller.palladio.analysis.pcm2lqn.runner.PcmLqnsAnalyzerContext;
import de.fakeller.palladio.analysis.provider.FileSystemProvider;
import de.fakeller.palladio.config.PcmModelConfig;
import edu.squat.transformations.ArchitecturalVersion;
import io.github.squat_team.AbstractPCMBot;
import io.github.squat_team.model.OptimizationType;
import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMScenario;
import io.github.squat_team.model.PCMScenarioResult;
import io.github.squat_team.performance.AbstractPerformancePCMScenario;
import io.github.squat_team.performance.ArchitecturalCopyCreator;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;
import io.github.squat_team.performance.peropteryx.configuration.PerOpteryxConfigImproved;
import io.github.squat_team.performance.peropteryx.configuration.PerOpteryxConfigImproved.Mode;
import io.github.squat_team.performance.peropteryx.environment.PalladioEclipseEnvironmentImrpoved;
import io.github.squat_team.performance.peropteryx.export.ExportModeImrpoved;
import io.github.squat_team.performance.peropteryx.export.OptimizationDirectionImrpoved;
import io.github.squat_team.performance.peropteryx.export.PerOpteryxPCMResultImrpoved;
import io.github.squat_team.performance.peropteryx.start.MyHeadlessPerOpteryxRunnerImrpoved;
import io.github.squat_team.util.DesigndecisionFileModifier;
import io.github.squat_team.util.PCMFileFinder;
import io.github.squat_team.util.PCMRepositoryModifier;

/**
 * This class contains the functionality for a bot using PerOpteryx.
 */
public abstract class AbstractPerOpteryxPCMBot extends AbstractPCMBot {
	private static Logger logger = Logger.getLogger(AbstractPerOpteryxPCMBot.class.getName());
	private ArchitecturalCopyCreator copyCreator = ArchitecturalCopyCreator.getInstance();

	// Bot State
	protected String id;
	protected String currentModelName;
	protected ConfigurationImprovedImproved configuration;
	protected AbstractPerformancePCMScenario performanceScenario;

	// Bot debugging options
	protected Boolean debugMode = false;
	protected Boolean detailedAnalysis = false;

	// Stored values to reset it later
	private String configurationDesigndecisionInitial;
	private String configurationQMLDefinitionFileInitial;
	private PerOpteryxConfigImproved.Mode configurationModeInitial;
	private Level loglevel;

	/**
	 * 
	 * @param name
	 * @param scenario
	 *            An instance of {@link AbstractPerformancePCMScenario} is expected
	 *            as input.
	 * @param qualityAttribute
	 * @param configuration
	 * @param id
	 */
	public AbstractPerOpteryxPCMBot(String name, PCMScenario scenario, String qualityAttribute,
			ConfigurationImprovedImproved configuration, String id) {
		this(name, scenario, qualityAttribute, configuration);
		this.id = id;
	}

	/**
	 * 
	 * @param name
	 * @param scenario
	 *            An instance of {@link AbstractPerformancePCMScenario} is expected
	 *            as input.
	 * @param qualityAttribute
	 * @param configuration
	 */
	public AbstractPerOpteryxPCMBot(String name, PCMScenario scenario, String qualityAttribute,
			ConfigurationImprovedImproved configuration) {
		super(name, scenario, qualityAttribute, "latency" ,true);
		this.configuration = configuration;
		if (scenario instanceof AbstractPerformancePCMScenario) {
			this.performanceScenario = (AbstractPerformancePCMScenario) scenario;
		} else {
			throw new IllegalArgumentException(
					"A scenario of type " + AbstractPerformancePCMScenario.class.getName() + " is expected as inputF");
		}
	}
	
	@Override
	public ArchitecturalVersion transformCandidate(PCMArchitectureInstance candidate) {
		return copyCreator.copy(candidate, this);
	}

	public String getID() {
		return id;
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

	protected void loadTemporarilyChangedConfiurationValues() {
		configurationDesigndecisionInitial = configuration.getPerOpteryxConfig().getDesignDecisionFile();
		configurationQMLDefinitionFileInitial = configuration.getPerOpteryxConfig().getQmlDefinitionFile();
		configurationModeInitial = configuration.getPerOpteryxConfig().getMode();
	}

	protected void resetTemporarilyChangedConfiurationValues() {
		configuration.getPerOpteryxConfig().setDesignDecisionFile(configurationDesigndecisionInitial);
		configuration.getPerOpteryxConfig().setQmlDefinitionFile(configurationQMLDefinitionFileInitial);
		configuration.getPerOpteryxConfig().setMode(configurationModeInitial);
	}

	protected void separateAll(List<PCMScenarioResult> results, PCMRepositoryModifier repositoryModifier) {
		for (PCMScenarioResult result : results) {
			repositoryModifier.separateRepository(result.getResultingArchitecture());
		}
	}

	protected void inverseTransformAll(List<PCMScenarioResult> results) {
		for (PCMScenarioResult result : results) {
			performanceScenario.inverseTransform(result.getResultingArchitecture());
		}
	}

	protected void setupEnvironmentforAnalysis() {
		PalladioEclipseEnvironmentImrpoved.INSTANCE.setup(configuration.getPcmModelsConfig().getPathmapFolder());
		de.fakeller.palladio.environment.PalladioEclipseEnvironment.INSTANCE.setup();
	}

	protected PCMInstance buildPcmInstanceForAnalysis() {
		PcmModelConfig pcmConfig = new PcmModelConfig();
		pcmConfig.setAllocationModel(configuration.getPcmInstanceConfig().getAllocationModel());
		pcmConfig.setUsageModel(configuration.getPcmInstanceConfig().getUsageModel());
		FileSystemProvider provider = new FileSystemProvider(pcmConfig);
		return provider.provide();
	}

	protected String executeHeadlessLqns(PCMInstance pcmInstance) {
		final PcmLqnsAnalyzerConfig config = PcmLqnsAnalyzerConfig.defaultConfig();
		final PcmLqnsAnalyzer analyzer = new PcmLqnsAnalyzer(config);
		final PcmLqnsAnalyzerContext ctx = analyzer.setupAnalysis(pcmInstance);
		ctx.executePalladio();
		return config.getOutputPath();
	}

	protected void deactivateLog() {
		if (!debugMode) {
			LogManager.getLogManager().reset();
			loglevel = org.apache.log4j.LogManager.getRootLogger().getLevel();
			org.apache.log4j.LogManager.getRootLogger().setLevel(Level.OFF);
		}
	}

	protected void activateLog() {
		if (!debugMode) {
			org.apache.log4j.LogManager.getRootLogger().setLevel(loglevel);
		}
	}

	protected List<PerOpteryxPCMResultImrpoved> getPerOpteryxResults(Future<List<PerOpteryxPCMResultImrpoved>> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
			return new ArrayList<PerOpteryxPCMResultImrpoved>();
		} catch (ExecutionException e) {
			logger.error(e.getMessage(), e);
			return new ArrayList<PerOpteryxPCMResultImrpoved>();
		}
	}

	protected Future<List<PerOpteryxPCMResultImrpoved>> runPerOpteryx(PCMArchitectureInstance architecture,
			boolean headlessPerOpteryxMode) {
		modifyDesigndecisionFile(architecture);
		MyHeadlessPerOpteryxRunnerImrpoved runner = new MyHeadlessPerOpteryxRunnerImrpoved();
		runner.init(configuration);
		runner.setDebugMode(headlessPerOpteryxMode);
		return ThreadPoolProvider.POOL.submit(runner);
	}

	protected void configurePerOpteryx() {
		String designDecisionFile = configuration.getPerOpteryxConfig().getDesignDecisionFile();
		if (designDecisionFile == null || designDecisionFile.isEmpty()) {
			configuration.getPerOpteryxConfig().setMode(Mode.DESIGN_DECISIONS_AND_OPTIMIZE);
		} else {
			configuration.getPerOpteryxConfig().setMode(Mode.OPTIMIZE);
		}
	}

	protected void configurePerOpteryxForOptimization() {
		// TODO: choose good values
		configuration.getTacticsConfig().useTactics(true);
		if (configuration.getPerOpteryxConfig().getGenerationSize() <= 1
				&& configuration.getPerOpteryxConfig().getMaxIterations() <= 1) {
			configuration.getPerOpteryxConfig().setGenerationSize(25);
			configuration.getPerOpteryxConfig().setMaxIterations(7);
		}

		// TODO: use a stop criteria? Will improve speed in many cases
		configuration.getTerminationCriteriaConfig().setActivateTerminationCriteria(true);
		configuration.getTerminationCriteriaConfig().setActivateInsignificantFrontChange(true);
		configuration.getTerminationCriteriaConfig().setInsignificantFrontChangeGenerationNumber(4);
		configuration.getTerminationCriteriaConfig().setInsignificantFrontChangeImprovementPercentage(3);
	}

	protected void configureExportForOptimization() {
		// TODO: which results should be exported? all/better than expected/x
		// best/only the best?
		configuration.getExporterConfig().setAmount(10);
		configuration.getExporterConfig().setExportMode(ExportModeImrpoved.AMOUNT);
	}

	protected void configureWith(PCMArchitectureInstance currentArchitecture) {
		String allocationPath = currentArchitecture.getAllocation().eResource().getURI().toString();
		String usagemodelPath = currentArchitecture.getUsageModel().eResource().getURI().toString();
		allocationPath = allocationPath.replaceAll("file:", "");
		usagemodelPath = usagemodelPath.replaceAll("file:", "");
		configuration.getPcmInstanceConfig().setAllocationModel(allocationPath);
		configuration.getPcmInstanceConfig().setUsageModel(usagemodelPath);
	}

	protected void configureWith(AbstractPerformancePCMScenario scenario) throws IOException {
		configureOptimizationDirection(scenario.getType());
		configureBoundaryValue(scenario.getExpectedResult().getResponse());
		String qmlPath = configuration.getPerOpteryxConfig().getQmlDefinitionFile();
		if (qmlPath == null || qmlPath.isEmpty()) {
			String generatedQmlFilePath = PerOpteryxQMLConverter.convert(currentModelName,
					configuration.getPcmInstanceConfig().getUsageModel(), getID(), scenario);
			configuration.getPerOpteryxConfig().setQmlDefinitionFile(generatedQmlFilePath);
		}
	}

	protected void validateConfiguration() {
		if (configuration == null) {
			throw new RuntimeException("Headless PerOpteryx needs a configuration");
		}
		if (!configuration.validate()) {
			throw new RuntimeException("Configuration for Headless PerOpteryx incomplete");
		}
	}

	private void modifyDesigndecisionFile(PCMArchitectureInstance architecture) {
		if (configuration.getDesignDecisionConfig().isChangeToDesigndecisionFileNecessary()) {
			if (configuration.getPerOpteryxConfig().getMode().equals(Mode.OPTIMIZE)) {
				modifyDesignDecisionFile(new File(configuration.getPerOpteryxConfig().getDesignDecisionFile()));
			} else if (configuration.getPerOpteryxConfig().getMode().equals(Mode.DESIGN_DECISIONS_AND_OPTIMIZE)) {
				PCMFileFinder fileFinder = new PCMFileFinder(architecture);
				String designDecisionPath = fileFinder.getPath() + File.separator + currentModelName + "-"
						+ this.getID() + ".designdecision";
				configuration.getPerOpteryxConfig().setDesignDecisionFile("file:" + designDecisionPath);
				runDesignDecisionCreation();
				modifyDesignDecisionFile(new File(designDecisionPath));
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

	private void modifyDesignDecisionFile(File designdecisionFile) {
		DesigndecisionFileModifier designdecisionFileModifier = new DesigndecisionFileModifier(designdecisionFile,
				configuration.getDesignDecisionConfig());
		designdecisionFileModifier.modify();
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

}