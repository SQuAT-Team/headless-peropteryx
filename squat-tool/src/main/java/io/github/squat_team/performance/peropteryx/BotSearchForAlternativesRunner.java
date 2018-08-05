package io.github.squat_team.performance.peropteryx;

import java.util.List;

import io.github.squat_team.AbstractPCMBot;
import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMScenarioResult;

/**
 * Runs {@link AbstractPCMBot#searchForAlternatives(PCMArchitectureInstance)} as
 * a runnable. {@link BotSearchForAlternativesRunner#getResult()} can be called
 * to retrieve the result after the termination of the task.
 */
public class BotSearchForAlternativesRunner implements Runnable {
	AbstractPerOpteryxPCMBot bot;
	PCMArchitectureInstance architecture;
	List<PCMScenarioResult> results;

	public BotSearchForAlternativesRunner(AbstractPerOpteryxPCMBot bot, PCMArchitectureInstance architecture) {
		this.bot = bot;
		this.architecture = architecture;
	}

	@Override
	public void run() {
		results = bot.searchForAlternatives(architecture);
		System.out.println("Bot " + bot.getBotName() + " finished search for alternatives");
	}

	public List<PCMScenarioResult> getResult() {
		return results;
	}

}
