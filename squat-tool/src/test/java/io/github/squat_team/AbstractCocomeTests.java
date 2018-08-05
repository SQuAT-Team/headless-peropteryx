package io.github.squat_team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.usagemodel.UsageModel;

import de.uka.ipd.sdq.simucomframework.variables.cache.StoExCache;
import de.uka.ipd.sdq.workflow.blackboard.Blackboard;
import io.github.squat_team.model.OptimizationType;
import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMResult;
import io.github.squat_team.model.PCMScenarioResult;
import io.github.squat_team.model.ResponseMeasureType;
import io.github.squat_team.performance.AbstractPerformancePCMScenario;
import io.github.squat_team.performance.PerformanceMetric;
import io.github.squat_team.performance.PerformancePCMCPUScenario;
import io.github.squat_team.performance.PerformancePCMUsageScenario;
import io.github.squat_team.performance.PerformancePCMWokloadScenario;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;
import io.github.squat_team.performance.peropteryx.configuration.DesigndecisionConfigImproved;
import io.github.squat_team.performance.peropteryx.configuration.PerOpteryxConfigImproved;
import io.github.squat_team.util.SQuATHelper;

/**
 * Basic class for automated tests on the Cocome model. Generates a test
 * environment in the project.
 */
public abstract class AbstractCocomeTests {
	private static final String MAIN_PATH = "." + File.separator + "src" + File.separator + "test" + File.separator
			+ "resources" + File.separator;
	private static final String MODEL_DIRECTORY = "cocome";
	private static final String LQN_DIRECTORY = "lqnTemp";
	private static final String PCM_DIRECTORY = "pcmTemp";

	// Expected Scenario Responses
	@SuppressWarnings("rawtypes")
	protected static final Comparable EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_1 = 2.0000261;
	@SuppressWarnings("rawtypes")
	protected static final Comparable EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_2 = 3.2930841;
	@SuppressWarnings("rawtypes")
	protected static final Comparable EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_3 = 2.6805319;
	@SuppressWarnings("rawtypes")
	protected static final Comparable EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_4 = 2.2974886000000003;
	@SuppressWarnings("rawtypes")
	protected static final Comparable EXPECTED_USAGE_SCENARIO_RESPONSE_MODEL_1 = 19.409354999999998;
	@SuppressWarnings("rawtypes")
	protected static final Comparable EXPECTED_CPU_SCENARIO_RESPONSE_MODEL_1 = 2.0165495;
	@SuppressWarnings("rawtypes")
	protected static final Comparable EXPECTED_WORKLOAD_SCENARIO_RESPONSE_MODEL_1 = 2.2994905000000005;

	// Model File Names
	protected static final String PCM_MODEL_DIRECTORY = "palladioDefinitions";
	protected static final String ALTERNATIVE_REPOSITORY_NAME_1 = "alternativeRepository.repository";
	protected static final String MODEL_NAME_1 = "cocome-cloud";
	protected static final String ALTERNATIVE_REPOSITORY_NAME_2 = "alternativeRepository2.repository";
	protected static final String MODEL_NAME_2 = "cocome-cloud2";
	protected static final String ALTERNATIVE_REPOSITORY_NAME_3 = "alternativeRepository3.repository";
	protected static final String MODEL_NAME_3 = "cocome-cloud3";
	protected static final String ALTERNATIVE_REPOSITORY_NAME_4 = "alternativeRepository4.repository";
	protected static final String MODEL_NAME_4 = "cocome-cloud4";

	// Temp Directories
	protected File tempModelDirectory;
	protected File tempLqnDirectory;
	protected File tempPcmDirectory;

	// Time measurements
	protected long startTime;

	/**
	 * The assertions have to be deactivated, otherwise PerOpteryx does not behave
	 * correctly in the search for alternatives part.
	 */
	@Before
	public void dissableAsserts() {
		// dissable asserts in critical classes
		StoExCache.class.getClassLoader().setClassAssertionStatus(StoExCache.class.getName(), false);
		Blackboard.class.getClassLoader().setClassAssertionStatus(Blackboard.class.getName(), false);

		// check dissabled
		assertTrue(!StoExCache.class.desiredAssertionStatus());
		assertTrue(!Blackboard.class.desiredAssertionStatus());
	}

	/**
	 * Creates a temporary directory with the cocome model in it, so the original
	 * files are not touched by the tests.
	 * 
	 * @throws IOException
	 */
	@Before
	public void prepareModel() throws IOException {
		File modelDirectory = new File(MAIN_PATH + MODEL_DIRECTORY);
		assertTrue(modelDirectory.exists());
		tempModelDirectory = new File(modelDirectory.getPath() + "Temp");
		cleanUpModel();
		assertFalse(tempModelDirectory.exists());
		FileUtils.copyDirectory(modelDirectory, tempModelDirectory);
		assertTrue(tempModelDirectory.exists());
	}

	/**
	 * Creates temporary directories for the output files of the LQN Solver and
	 * Headless PerOpteryx.
	 * 
	 * @throws IOException
	 */
	@Before
	public void prepareTempDirectories() throws IOException {
		tempLqnDirectory = new File(MAIN_PATH + LQN_DIRECTORY);
		tempPcmDirectory = new File(MAIN_PATH + PCM_DIRECTORY);

		cleanUpTempDirectories();

		assertFalse(tempLqnDirectory.exists());
		tempLqnDirectory.mkdir();
		assertTrue(tempLqnDirectory.exists());

		assertFalse(tempPcmDirectory.exists());
		tempPcmDirectory.mkdir();
		assertTrue(tempPcmDirectory.exists());
	}

	/**
	 * Removes the temporary model directory.
	 * 
	 * @throws IOException
	 */
	public void cleanUpModel() throws IOException {
		FileUtils.deleteDirectory(tempModelDirectory);
		assertFalse(tempModelDirectory.exists());
	}

	/**
	 * Removes the temporary output files directories.
	 * 
	 * @throws IOException
	 */
	public void cleanUpTempDirectories() throws IOException {
		FileUtils.deleteDirectory(tempLqnDirectory);
		assertFalse(tempLqnDirectory.exists());

		FileUtils.deleteDirectory(tempPcmDirectory);
		assertFalse(tempPcmDirectory.exists());
	}

	/**
	 * Creates a scenario for Cocome that does not have any effect.
	 * 
	 * @return the null scenario.
	 */
	protected AbstractPerformancePCMScenario setupNullScenario() {
		ArrayList<String> workloadIDs = new ArrayList<String>();
		workloadIDs.add("_VgwxwHr3Eeek77WF10mCCg");

		return setFakeResponseValues(
				new PerformancePCMWokloadScenario(OptimizationType.MINIMIZATION, workloadIDs, 1.0));
	}

	/**
	 * Creates a {@link PerformancePCMWokloadScenario} for Cocome.
	 * 
	 * @return the scenario.
	 */
	protected AbstractPerformancePCMScenario setupWorkloadScenario() {
		ArrayList<String> workloadIDs = new ArrayList<String>();
		workloadIDs.add("_VgwxwHr3Eeek77WF10mCCg");

		return setFakeResponseValues(
				new PerformancePCMWokloadScenario(OptimizationType.MINIMIZATION, workloadIDs, 1.1));
	}

	/**
	 * Creates a {@link PerformancePCMCPUScenario} for Cocome.
	 * 
	 * @return the scenario.
	 */
	protected AbstractPerformancePCMScenario setupCPUScenario() {
		ArrayList<String> cpuIDs = new ArrayList<String>();
		cpuIDs.add("_WV4YUK2VEeaxN4gXuIkS2A");

		return setFakeResponseValues(new PerformancePCMCPUScenario(OptimizationType.MINIMIZATION, cpuIDs, 0.5));
	}

	/**
	 * Creates a {@link PerformancePCMUsageScenario} for Cocome.
	 * 
	 * @return the scenario.
	 */
	protected AbstractPerformancePCMScenario setupUsageScenario() {
		ArrayList<String> loopIDs = new ArrayList<String>();
		loopIDs.add("_fsG44tqFEee4ToXBRRujSw");

		return setFakeResponseValues(new PerformancePCMUsageScenario(OptimizationType.MINIMIZATION, loopIDs,
				"IntPMF[(1; 0.01)(2; 0.01)(3; 0.02)(4; 0.02)(5; 0.03)(6; 0.03)(7; 0.04)(8; 0.04)(9; 0.05)(10; 0.06)"
						+ "(11; 0.06)(12; 0.06)(13; 0.08)(14; 0.09)(15; 0.10)(16; 0.09)(17; 0.07)(18; 0.06)(19; 0.05)(20; 0.03)]"));
	}

	/**
	 * Sets some response values to the scenario, that are not used but required.
	 * 
	 * @param scenario
	 * @return
	 */
	private AbstractPerformancePCMScenario setFakeResponseValues(AbstractPerformancePCMScenario scenario) {
		PCMResult expectedResponse = new PCMResult(ResponseMeasureType.DECIMAL);
		expectedResponse.setResponse(6.0);
		scenario.setExpectedResponse(expectedResponse);
		scenario.setMetric(PerformanceMetric.RESPONSE_TIME);
		return scenario;
	}

	/**
	 * Creates a basic configuration for Cocome in the generated testing
	 * environment.
	 * 
	 * @return the configuration.
	 */
	protected ConfigurationImprovedImproved setupBasicConfiguration() {
		ConfigurationImprovedImproved configuration = new ConfigurationImprovedImproved();

		configuration.getLqnsConfig().setLqnsOutputDir(tempLqnDirectory.getAbsolutePath());
		configuration.getExporterConfig().setPcmOutputFolder(tempPcmDirectory.getAbsolutePath());
		configuration.getPcmModelsConfig().setPathmapFolder(
				"file:/" + (new File(MAIN_PATH + PCM_MODEL_DIRECTORY)).getAbsolutePath() + File.separator);
		return configuration;
	}

	/**
	 * Sets the search parameters in the configuration.
	 * 
	 * @param configuration
	 *            the parameters are set here.
	 * @param generationSize
	 *            {@link PerOpteryxConfigImproved#setGenerationSize(int)
	 * @param maxIterations
	 *            {@link PerOpteryxConfigImproved#setMaxIterations(int)
	 * @return
	 */
	protected ConfigurationImprovedImproved setSearchParameters(ConfigurationImprovedImproved configuration,
			int generationSize, int maxIterations) {
		configuration.getPerOpteryxConfig().setGenerationSize(generationSize);
		configuration.getPerOpteryxConfig().setMaxIterations(maxIterations);
		return configuration;
	}

	/**
	 * Sets the limits for the CPU clockrates in the designdecision file for Cocome.
	 * 
	 * @param configuration
	 */
	protected void setDesignDecisionLimits(ConfigurationImprovedImproved configuration) {
		DesigndecisionConfigImproved designdecisionConfig = configuration.getDesignDecisionConfig();
		designdecisionConfig.setLimits("_78qo4K2UEeaxN4gXuIkS2A", 100, 6000);
		designdecisionConfig.setLimits("_-5Q84K2UEeaxN4gXuIkS2A", 110, 6100);
		designdecisionConfig.setLimits("_BgmykK2VEeaxN4gXuIkS2A", 120, 6200);
		designdecisionConfig.setLimits("_FM6FMK2VEeaxN4gXuIkS2A", 130, 6300);
	}

	/**
	 * Reanalyzes the given result with the analysis method and compares the
	 * results.
	 * 
	 * @param bot
	 *            the bot that generated the result.
	 * @param result
	 *            the result to reanalyze.
	 */
	@SuppressWarnings("rawtypes")
	protected void reanalyze(AbstractPCMBot bot, PCMScenarioResult result) {
		Comparable responseForLastCandidate = result.getResult().getResponse();
		PCMArchitectureInstance lastCandidateArchitecture = result.getResultingArchitecture();

		PCMScenarioResult reanalyzedLastCandidateResult = bot.analyze(lastCandidateArchitecture);
		assertNotNull(reanalyzedLastCandidateResult);

		Comparable reanalyzedResponseForLastCandidate = reanalyzedLastCandidateResult.getResult().getResponse();
		assertNotNull(reanalyzedResponseForLastCandidate);

		// Compare two results
		assertEquals(responseForLastCandidate, reanalyzedResponseForLastCandidate);
	}

	/**
	 * Loads the given architecture. See attributes of this class for parameters.
	 * 
	 * @param modelName
	 *            The name the model should have.
	 *            {@link PCMArchitectureInstance#getName()}
	 * @param modelFileName
	 *            The name of the model to load, e.g., {@link #MODEL_NAME_1}
	 * @param alternativeRepositoryFileName
	 *            The name of the corresponding alternative reposiotry to load,
	 *            e.g., {@link #ALTERNATIVE_REPOSITORY_NAME_1}
	 * @return
	 */
	protected PCMArchitectureInstance loadArchitecture(String modelName, String modelFileName,
			String alternativeRepositoryFileName) {
		String allocationModelPath = "file:/" + tempModelDirectory.getAbsolutePath().replace("./", "") + File.separator
				+ modelFileName + ".allocation";
		Allocation allocation = SQuATHelper.loadAllocationModel(allocationModelPath);

		String systemModelPath = "file:/" + tempModelDirectory.getAbsolutePath().replace("./", "") + File.separator
				+ modelFileName + ".system";
		org.palladiosimulator.pcm.system.System system = SQuATHelper.loadSystemModel(systemModelPath);

		String resourceenvironmentModelPath = "file:/" + tempModelDirectory.getAbsolutePath().replace("./", "")
				+ File.separator + modelFileName + ".resourceenvironment";
		ResourceEnvironment resourceenvironment = SQuATHelper
				.loadResourceEnvironmentModel(resourceenvironmentModelPath);

		String repositoryModelPath = "file:/" + tempModelDirectory.getAbsolutePath().replace("./", "") + File.separator
				+ modelFileName + ".repository";
		Repository repository = SQuATHelper.loadRepositoryModel(repositoryModelPath);

		String usageModelPath = "file:/" + tempModelDirectory.getAbsolutePath().replace("./", "") + File.separator
				+ modelFileName + ".usagemodel";
		UsageModel usageModel = SQuATHelper.loadUsageModel(usageModelPath);

		PCMArchitectureInstance architecture = new PCMArchitectureInstance(modelName, repository, system, allocation,
				resourceenvironment, usageModel);

		String alternativeRepositoryModelPath = "file:" + tempModelDirectory.getAbsolutePath().replace("./", "")
				+ File.separator + alternativeRepositoryFileName;
		architecture.setRepositoryWithAlternatives(SQuATHelper.loadRepositoryModel(alternativeRepositoryModelPath));

		return architecture;
	}

	/**
	 * Checks the existence of search for alternatives results. Assures that new
	 * architectures have been generated.
	 * 
	 * @param results
	 *            the results of a search for alternatives run.
	 */
	protected void checkResultsExist(List<PCMScenarioResult> results) {
		assertTrue(results.size() > 1);
	}

	/**
	 * Checks the existence of analysis results.
	 * 
	 * @param result
	 *            the result of an analysis run.
	 */
	protected void checkResultExists(PCMScenarioResult result) {
		assertNotNull(result);
	}

}
