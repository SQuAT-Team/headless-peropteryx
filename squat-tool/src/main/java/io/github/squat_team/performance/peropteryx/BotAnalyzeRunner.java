package io.github.squat_team.performance.peropteryx;

import io.github.squat_team.AbstractPCMBot;
import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMScenarioResult;

/**
 * Runs {@link AbstractPCMBot#analyze(PCMArchitectureInstance)} as a runnable.
 * {@link BotSearchForAlternativesRunner#getResult()} can be called to retrieve
 * the result after the termination of the task.
 */
public class BotAnalyzeRunner implements Runnable {
	AbstractPerOpteryxPCMBot bot;
	PCMArchitectureInstance architecture;
	PCMScenarioResult result;

	public BotAnalyzeRunner(AbstractPerOpteryxPCMBot bot, PCMArchitectureInstance architecture) {
		this.bot = bot;
		this.architecture = architecture;
	}

	@Override
	public void run() {
		result = bot.analyze(architecture);
		System.out.println("Bot " + bot.getBotName() + " finished analysis");
	}

	public PCMScenarioResult getResult() {
		return result;
	}

}
