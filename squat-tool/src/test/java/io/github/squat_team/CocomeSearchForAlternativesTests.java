package io.github.squat_team;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.junit.Test;

import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMScenarioResult;
import io.github.squat_team.performance.AbstractPerformancePCMScenario;
import io.github.squat_team.performance.peropteryx.AbstractPerOpteryxPCMBot;
import io.github.squat_team.performance.peropteryx.BotSearchForAlternativesRunner;
import io.github.squat_team.performance.peropteryx.ConcurrentPerOpteryxPCMBot;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;

/**
 * Provides tests for the search for alternatives on Cocome.
 */
public class CocomeSearchForAlternativesTests extends AbstractCocomeTests {
	private static final String BOT_NAME = "PB1";
	
	/**
	 * Can be changed to debug failing tests.
	 */
	private static final boolean DEBUG_MODE = false;

	/**
	 * Executes a search for alternatives twice and checks the results.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void secondSearchTest() throws InterruptedException {
		// Prepare Configuration
		ConfigurationImprovedImproved configuration = setupBasicConfiguration();
		setSearchParameters(configuration, 5, 2);
		setDesignDecisionLimits(configuration);

		// Prepare Scenario and Architecture
		AbstractPerformancePCMScenario scenario = setupWorkloadScenario();
		PCMArchitectureInstance architecture = loadArchitecture("test", MODEL_NAME_1, ALTERNATIVE_REPOSITORY_NAME_1);

		// Prepare Bot
		AbstractPerOpteryxPCMBot bot = new ConcurrentPerOpteryxPCMBot(BOT_NAME, scenario, configuration);
		bot.setDebugMode(DEBUG_MODE);

		// Execute Search For Alternative
		ExecutorService executor = ExecutorHelper.getNewExecutorService();
		BotSearchForAlternativesRunner task = new BotSearchForAlternativesRunner(bot, architecture);
		executor.submit(task);
		ExecutorHelper.shutdown(executor);

		// Get Results
		final List<PCMScenarioResult> results;
		results = task.getResult();
		checkResultsExist(results);

		// Reanalyze last result
		PCMScenarioResult lastResult = results.get(results.size() - 1);

		// Start Second Run
		ExecutorService executor2 = ExecutorHelper.getNewExecutorService();
		BotSearchForAlternativesRunner task2 = new BotSearchForAlternativesRunner(bot,
				lastResult.getResultingArchitecture());
		executor2.submit(task2);
		ExecutorHelper.shutdown(executor2);

		// Get Results
		final List<PCMScenarioResult> results2;
		results2 = task2.getResult();
		checkResultsExist(results2);

		// Reanalyze last result
		PCMScenarioResult lastResult2 = results2.get(results2.size() - 1);

		reanalyze(bot, lastResult2);
	}

	/**
	 * A simple test that runs a search for alternatives and checks the results.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void executeSearchForAlternativesTest() throws InterruptedException {
		// Prepare Configuration
		ConfigurationImprovedImproved configuration = setupBasicConfiguration();
		setSearchParameters(configuration, 5, 2);
		setDesignDecisionLimits(configuration);

		// Prepare Scenario and Architecture
		AbstractPerformancePCMScenario scenario = setupWorkloadScenario();
		PCMArchitectureInstance architecture = loadArchitecture("test", MODEL_NAME_1, ALTERNATIVE_REPOSITORY_NAME_1);

		// Prepare Bot
		AbstractPerOpteryxPCMBot bot = new ConcurrentPerOpteryxPCMBot(BOT_NAME, scenario, configuration);
		bot.setDebugMode(DEBUG_MODE);

		// Execute Search For Alternative
		ExecutorService executor = ExecutorHelper.getNewExecutorService();
		BotSearchForAlternativesRunner task = new BotSearchForAlternativesRunner(bot, architecture);
		executor.submit(task);
		ExecutorHelper.shutdown(executor);

		// Get Results
		final List<PCMScenarioResult> results;
		results = task.getResult();
		checkResultsExist(results);

		// Reanalyze last result
		PCMScenarioResult lastResult = results.get(results.size() - 1);
		reanalyze(bot, lastResult);

		checkDesignDecisionLimits();
	}

	/**
	 * Checks that the limits in the designdecision file are set.
	 */
	private void checkDesignDecisionLimits() {
		File designDecisionFile = findDesignDecisionFile();
		checkLimits(designDecisionFile);
	}

	/**
	 * Find the designdecision file and assert that it exists.
	 * 
	 * @return the designdecision file.
	 */
	private File findDesignDecisionFile() {
		File designDecisionFile = null;
		for (File childFile : tempModelDirectory.listFiles()) {
			if (childFile.getName().contains("designdecision")) {
				designDecisionFile = childFile;
				break;
			}
		}
		assertNotNull(designDecisionFile);
		return designDecisionFile;
	}

	/**
	 * Checks that the limits are set as specified in the designdecision file.
	 * 
	 * @param designdecisionFile
	 */
	private void checkLimits(File designdecisionFile) {
		BufferedReader br;

		boolean foundLimit1 = false;
		boolean foundLimit2 = false;
		boolean foundLimit3 = false;
		boolean foundLimit4 = false;

		try {
			br = new BufferedReader(new FileReader(designdecisionFile));

			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains(
						"degreesOfFreedom from=\"100.0\" lowerBoundIncluded=\"false\" to=\"6000.0\" xsi:type=\"specific:ContinuousProcessingRateDegree\"")) {
					foundLimit1 = true;
				} else if (line.contains(
						"degreesOfFreedom from=\"110.0\" lowerBoundIncluded=\"false\" to=\"6100.0\" xsi:type=\"specific:ContinuousProcessingRateDegree\"")) {
					foundLimit2 = true;
				} else if (line.contains(
						"degreesOfFreedom from=\"120.0\" lowerBoundIncluded=\"false\" to=\"6200.0\" xsi:type=\"specific:ContinuousProcessingRateDegree\"")) {
					foundLimit3 = true;
				} else if (line.contains(
						"degreesOfFreedom from=\"130.0\" lowerBoundIncluded=\"false\" to=\"6300.0\" xsi:type=\"specific:ContinuousProcessingRateDegree\"")) {
					foundLimit4 = true;
				}
			}
			assertTrue(foundLimit1);
			assertTrue(foundLimit2);
			assertTrue(foundLimit3);
			assertTrue(foundLimit4);
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
}
