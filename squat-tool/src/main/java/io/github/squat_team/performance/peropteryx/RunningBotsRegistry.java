package io.github.squat_team.performance.peropteryx;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;

/**
 * Registers all the running bots in critical parts in which no logging should be active.
 * 
 * Assumes only one bot instance is running at the same time.
 */
public class RunningBotsRegistry {
	private static RunningBotsRegistry INSTANCE;
	private Set<AbstractPerOpteryxPCMBot> runningBots = new HashSet<>();
	private Level loglevel;
	
	private RunningBotsRegistry() {

	}

	public static synchronized RunningBotsRegistry getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new RunningBotsRegistry();
		}
		return INSTANCE;
	}

	public synchronized void registerBot(AbstractPerOpteryxPCMBot bot) {
		runningBots.add(bot);
	}

	public synchronized void deregisterBot(AbstractPerOpteryxPCMBot bot) {
		runningBots.remove(bot);
	}

	public synchronized boolean moreThanOneBotRunning() {
		return runningBots.size() > 1; 
	}

	public Level getLoglevel() {
		return loglevel;
	}

	public void setLoglevel(Level loglevel) {
		this.loglevel = loglevel;
	}

}
