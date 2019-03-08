package io.github.squat_team;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.Test;

import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMScenarioResult;
import io.github.squat_team.performance.AbstractPerformancePCMScenario;
import io.github.squat_team.performance.peropteryx.AbstractPerOpteryxPCMBot;
import io.github.squat_team.performance.peropteryx.BotAnalyzeRunner;
import io.github.squat_team.performance.peropteryx.BotSearchForAlternativesRunner;
import io.github.squat_team.performance.peropteryx.ConcurrentPerOpteryxPCMBot;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;

/**
 * Provides tests related to concurrent runs of the Performance Bot.
 */
public class CocomeConcurrentTests extends AbstractCocomeTests {
	private static final String BOT_NAME1 = "PB1";
	private static final String BOT_NAME2 = "PB2";
	private static final String BOT_NAME3 = "PB3";
	private static final String BOT_NAME4 = "PB4";

	/**
	 * Can be changed to debug failing tests.
	 */
	private static final boolean DEBUG_MODE = false;

	/**
	 * A Runner that instantiates a new bot.
	 */
	private class BotRunner implements Runnable {
		ConcurrentPerOpteryxPCMBot bot;

		@Override
		public void run() {
			bot = new ConcurrentPerOpteryxPCMBot(null, null, null);
		}

		public ConcurrentPerOpteryxPCMBot getBot() {
			return bot;
		}

	}

	/**
	 * Check that the initialized bots all have an unique name.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void uniqueBotNameTest() throws InterruptedException {
		final int numberOfBots = 1000;
		List<Thread> threads = new ArrayList<>();
		List<BotRunner> runners = new ArrayList<>();

		// Instantiate many bots
		for (int i = 0; i < numberOfBots; i++) {
			BotRunner botRunner = new BotRunner();
			runners.add(botRunner);
			Thread botThread = new Thread(botRunner);
			threads.add(botThread);
			botThread.start();
		}

		// Wait for all bot instantiation threads
		for (Thread thread : threads) {
			thread.join();
		}

		// Add the names into a set
		Set<String> names = new HashSet<>();
		for (BotRunner runner : runners) {
			names.add(runner.getBot().getID());
		}

		// Elements in set should be number of bots
		assertEquals(numberOfBots, names.size());
	}

	/**
	 * Runs multiple analysis runs concurrently.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void concurrentAnalysisTest() throws InterruptedException {
		// Prepare Configurations
		ConfigurationImprovedImproved configuration1 = setupBasicConfiguration();
		ConfigurationImprovedImproved configuration2 = setupBasicConfiguration();
		ConfigurationImprovedImproved configuration3 = setupBasicConfiguration();
		ConfigurationImprovedImproved configuration4 = setupBasicConfiguration();

		// Prepare Scenarios and Architectures
		AbstractPerformancePCMScenario scenario1 = setupNullScenario();
		PCMArchitectureInstance architecture1 = loadArchitecture("test1", MODEL_NAME_1, ALTERNATIVE_REPOSITORY_NAME_1);
		AbstractPerformancePCMScenario scenario2 = setupNullScenario();
		PCMArchitectureInstance architecture2 = loadArchitecture("test2", MODEL_NAME_2, ALTERNATIVE_REPOSITORY_NAME_2);
		AbstractPerformancePCMScenario scenario3 = setupNullScenario();
		PCMArchitectureInstance architecture3 = loadArchitecture("test3", MODEL_NAME_3, ALTERNATIVE_REPOSITORY_NAME_3);
		AbstractPerformancePCMScenario scenario4 = setupNullScenario();
		PCMArchitectureInstance architecture4 = loadArchitecture("test4", MODEL_NAME_4, ALTERNATIVE_REPOSITORY_NAME_4);

		// Prepare Bots
		AbstractPerOpteryxPCMBot bot1 = new ConcurrentPerOpteryxPCMBot(BOT_NAME1, scenario1, configuration1);
		bot1.setDebugMode(DEBUG_MODE);
		AbstractPerOpteryxPCMBot bot2 = new ConcurrentPerOpteryxPCMBot(BOT_NAME2, scenario2, configuration2);
		bot2.setDebugMode(DEBUG_MODE);
		AbstractPerOpteryxPCMBot bot3 = new ConcurrentPerOpteryxPCMBot(BOT_NAME3, scenario3, configuration3);
		bot3.setDebugMode(DEBUG_MODE);
		AbstractPerOpteryxPCMBot bot4 = new ConcurrentPerOpteryxPCMBot(BOT_NAME4, scenario4, configuration4);
		bot4.setDebugMode(DEBUG_MODE);

		// Execute Search For Alternative
		ExecutorService executor = ExecutorHelper.getNewExecutorService();
		BotAnalyzeRunner task1 = new BotAnalyzeRunner(bot1, architecture1);
		executor.submit(task1);
		BotAnalyzeRunner task2 = new BotAnalyzeRunner(bot2, architecture2);
		executor.submit(task2);
		BotAnalyzeRunner task3 = new BotAnalyzeRunner(bot3, architecture3);
		executor.submit(task3);
		BotAnalyzeRunner task4 = new BotAnalyzeRunner(bot4, architecture4);
		executor.submit(task4);
		ExecutorHelper.shutdown(executor);

		// Get Results
		final PCMScenarioResult result1 = task1.getResult();
		checkResultExists(result1);
		final PCMScenarioResult result2 = task2.getResult();
		checkResultExists(result2);
		final PCMScenarioResult result3 = task3.getResult();
		checkResultExists(result3);
		final PCMScenarioResult result4 = task4.getResult();
		checkResultExists(result4);

		// Check results
		assertEquals(EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_1, result1.getResult().getResponse());
		assertEquals(EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_2, result2.getResult().getResponse());
		assertEquals(EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_3, result3.getResult().getResponse());
		assertEquals(EXPECTED_NULL_SCENARIO_RESPONSE_MODEL_4, result4.getResult().getResponse());
	}

	/**
	 * Runs multiple search for alternative runs on the same model concurrently.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void concurrentSearchForAlternativesTest() throws InterruptedException {
		// Prepare Configurations
		ConfigurationImprovedImproved configuration1 = setupBasicConfiguration();
		setSearchParameters(configuration1, 5, 2);
		setDesignDecisionLimits(configuration1);
		ConfigurationImprovedImproved configuration2 = setupBasicConfiguration();
		setSearchParameters(configuration2, 5, 2);
		setDesignDecisionLimits(configuration2);
		ConfigurationImprovedImproved configuration3 = setupBasicConfiguration();
		setSearchParameters(configuration3, 5, 2);
		setDesignDecisionLimits(configuration3);
		ConfigurationImprovedImproved configuration4 = setupBasicConfiguration();
		setSearchParameters(configuration4, 5, 2);
		setDesignDecisionLimits(configuration4);

		// Prepare Scenarios and Architectures
		AbstractPerformancePCMScenario scenario1 = setupNullScenario();
		PCMArchitectureInstance architecture = loadArchitecture("test", MODEL_NAME_1, ALTERNATIVE_REPOSITORY_NAME_1);
		AbstractPerformancePCMScenario scenario2 = setupWorkloadScenario();
		AbstractPerformancePCMScenario scenario3 = setupCPUScenario();
		AbstractPerformancePCMScenario scenario4 = setupUsageScenario();

		// Prepare Bots
		AbstractPerOpteryxPCMBot bot1 = new ConcurrentPerOpteryxPCMBot(BOT_NAME1, scenario1, configuration1);
		bot1.setDebugMode(DEBUG_MODE);
		AbstractPerOpteryxPCMBot bot2 = new ConcurrentPerOpteryxPCMBot(BOT_NAME2, scenario2, configuration2);
		bot2.setDebugMode(DEBUG_MODE);
		AbstractPerOpteryxPCMBot bot3 = new ConcurrentPerOpteryxPCMBot(BOT_NAME3, scenario3, configuration3);
		bot3.setDebugMode(DEBUG_MODE);
		AbstractPerOpteryxPCMBot bot4 = new ConcurrentPerOpteryxPCMBot(BOT_NAME4, scenario4, configuration4);
		bot4.setDebugMode(DEBUG_MODE);

		// Execute Search For Alternative
		ExecutorService executor = ExecutorHelper.getNewExecutorService();
		BotSearchForAlternativesRunner task1 = new BotSearchForAlternativesRunner(bot1, architecture);
		executor.submit(task1);
		BotSearchForAlternativesRunner task2 = new BotSearchForAlternativesRunner(bot2, architecture);
		executor.submit(task2);
		BotSearchForAlternativesRunner task3 = new BotSearchForAlternativesRunner(bot3, architecture);
		executor.submit(task3);
		BotSearchForAlternativesRunner task4 = new BotSearchForAlternativesRunner(bot4, architecture);
		executor.submit(task4);
		ExecutorHelper.shutdown(executor);

		// Get Results
		final List<PCMScenarioResult> results1;
		results1 = task1.getResult();
		checkResultsExist(results1);
		final List<PCMScenarioResult> results2;
		results2 = task2.getResult();
		checkResultsExist(results2);
		final List<PCMScenarioResult> results3;
		results3 = task3.getResult();
		checkResultsExist(results3);
		final List<PCMScenarioResult> results4;
		results4 = task4.getResult();
		checkResultsExist(results4);

		// Reanalyze last result
		PCMScenarioResult lastResult1 = results1.get(results1.size() - 1);
		reanalyze(bot1, lastResult1);
		PCMScenarioResult lastResult2 = results2.get(results2.size() - 1);
		reanalyze(bot2, lastResult2);
		PCMScenarioResult lastResult3 = results3.get(results3.size() - 1);
		reanalyze(bot3, lastResult3);
		PCMScenarioResult lastResult4 = results4.get(results4.size() - 1);
		reanalyze(bot4, lastResult4);
	}

}
