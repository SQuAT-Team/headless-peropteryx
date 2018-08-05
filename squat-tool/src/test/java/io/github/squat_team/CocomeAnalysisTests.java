package io.github.squat_team;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMScenarioResult;
import io.github.squat_team.performance.AbstractPerformancePCMScenario;
import io.github.squat_team.performance.lqns.LQNSDetailedResultWriter;
import io.github.squat_team.performance.peropteryx.ConcurrentPerOpteryxPCMBot;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;

/**
 * Provides tests for the analysis of Cocome.
 */
public class CocomeAnalysisTests extends AbstractCocomeTests {

	/**
	 * Can be changed to debug failing tests.
	 */
	private static final boolean DEBUG_MODE = false;

	/**
	 * Runs the analysis with the null scenario.
	 * 
	 * @throws IOException
	 */
	@Test
	public void executeAnalysisWithNullScenarioTest() throws IOException {
		ConfigurationImprovedImproved configuration = setupBasicConfiguration();
		AbstractPerformancePCMScenario scenario = setupNullScenario();

		ConcurrentPerOpteryxPCMBot bot = new ConcurrentPerOpteryxPCMBot(scenario, configuration);
		bot.setDebugMode(DEBUG_MODE);
		PCMArchitectureInstance architecture = loadArchitecture("test", MODEL_NAME_1, ALTERNATIVE_REPOSITORY_NAME_1);

		PCMScenarioResult result = bot.analyze(architecture);

		assertNotNull(result.getResult().getResponse());
		// Compare to result from original PerOpteryx with LQN Solver
		assertEquals(EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_1, result.getResult().getResponse());
	}

	/**
	 * Runs the analysis two times with the null scenario.
	 * 
	 * @throws IOException
	 */
	@Test
	public void consecutiveAnalysisTest() throws IOException {
		ConfigurationImprovedImproved configuration = setupBasicConfiguration();
		AbstractPerformancePCMScenario scenario = setupNullScenario();

		ConcurrentPerOpteryxPCMBot bot = new ConcurrentPerOpteryxPCMBot(scenario, configuration);
		bot.setDebugMode(DEBUG_MODE);
		PCMArchitectureInstance architecture = loadArchitecture("test", MODEL_NAME_1, ALTERNATIVE_REPOSITORY_NAME_1);

		PCMScenarioResult result = bot.analyze(architecture);

		assertNotNull(result.getResult().getResponse());
		// Compare to result from original PerOpteryx with LQN Solver
		assertEquals(EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_1, result.getResult().getResponse());

		PCMScenarioResult result2 = bot.analyze(architecture);

		assertNotNull(result2.getResult().getResponse());
		// Compare to result from original PerOpteryx with LQN Solver
		assertEquals(EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_1, result2.getResult().getResponse());
	}

	/**
	 * Runs the analysis with the null scenario and the detailed analysis.
	 * 
	 * @throws IOException
	 */
	@Test
	public void executeAnalysisWithDetailedAnalysisTest() throws IOException {
		ConfigurationImprovedImproved configuration = setupBasicConfiguration();
		AbstractPerformancePCMScenario scenario = setupNullScenario();

		ConcurrentPerOpteryxPCMBot bot = new ConcurrentPerOpteryxPCMBot(scenario, configuration);
		bot.setDebugMode(DEBUG_MODE);
		bot.setDetailedAnalysis(true);
		PCMArchitectureInstance architecture = loadArchitecture("test", MODEL_NAME_1, ALTERNATIVE_REPOSITORY_NAME_1);

		PCMScenarioResult result = bot.analyze(architecture);

		assertNotNull(result.getResult().getResponse());
		// Compare to result from original PerOpteryx with LQN Solver
		assertEquals(2.0000261, result.getResult().getResponse());

		// check that there is a detailed analysis file
		boolean detailedAnalysisFileExists = false;
		for (File childFile : tempModelDirectory.listFiles()) {
			if (childFile.getName().contains(LQNSDetailedResultWriter.FILE_EXTENSION)) {
				detailedAnalysisFileExists = true;
				break;
			}
		}
		assertTrue(detailedAnalysisFileExists);
	}

	/**
	 * Runs the analysis with the workload scenario.
	 * 
	 * @throws IOException
	 */
	@Test
	public void executeAnalysisWithWorkloadScenarioTest() throws IOException {
		ConfigurationImprovedImproved configuration = setupBasicConfiguration();
		AbstractPerformancePCMScenario scenario = setupWorkloadScenario();

		ConcurrentPerOpteryxPCMBot bot = new ConcurrentPerOpteryxPCMBot(scenario, configuration);
		bot.setDebugMode(DEBUG_MODE);
		PCMArchitectureInstance architecture = loadArchitecture("test", MODEL_NAME_1, ALTERNATIVE_REPOSITORY_NAME_1);

		PCMScenarioResult result = bot.analyze(architecture);

		assertNotNull(result.getResult().getResponse());
		// Compare to result from original PerOpteryx with LQN Solver
		assertEquals(EXPECTED_WORKLOAD_SCENARIO_RESPONSE_MODEL_1, result.getResult().getResponse());
	}

	/**
	 * Runs the analysis with the CPU scenario.
	 * 
	 * @throws IOException
	 */
	@Test
	public void executeAnalysisWithCPUScenarioTest() throws IOException {
		ConfigurationImprovedImproved configuration = setupBasicConfiguration();
		AbstractPerformancePCMScenario scenario = setupCPUScenario();

		ConcurrentPerOpteryxPCMBot bot = new ConcurrentPerOpteryxPCMBot(scenario, configuration);
		bot.setDebugMode(DEBUG_MODE);
		PCMArchitectureInstance architecture = loadArchitecture("test", MODEL_NAME_1, ALTERNATIVE_REPOSITORY_NAME_1);

		PCMScenarioResult result = bot.analyze(architecture);

		assertNotNull(result.getResult().getResponse());
		// Compare to result from original PerOpteryx with LQN Solver
		assertEquals(EXPECTED_CPU_SCENARIO_RESPONSE_MODEL_1, result.getResult().getResponse());
	}

	/**
	 * Runs the analysis with the usage scenario.
	 * 
	 * @throws IOException
	 */
	@Test
	public void executeAnalysisWithUsageScenarioTest() throws IOException {
		ConfigurationImprovedImproved configuration = setupBasicConfiguration();
		AbstractPerformancePCMScenario scenario = setupUsageScenario();

		ConcurrentPerOpteryxPCMBot bot = new ConcurrentPerOpteryxPCMBot(scenario, configuration);
		bot.setDebugMode(DEBUG_MODE);
		PCMArchitectureInstance architecture = loadArchitecture("test", MODEL_NAME_1, ALTERNATIVE_REPOSITORY_NAME_1);

		PCMScenarioResult result = bot.analyze(architecture);

		assertNotNull(result.getResult().getResponse());
		// Compare to result from original PerOpteryx with LQN Solver
		assertEquals(EXPECTED_USAGE_SCENARIO_RESPONSE_MODEL_1, result.getResult().getResponse());
	}

}
