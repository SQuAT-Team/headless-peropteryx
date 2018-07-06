package io.github.squat_team;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import io.github.squat_team.performance.peropteryx.ConcurrentPerOpteryxPCMBot;

public class ConcurrentPerformanceBotTest {

	private class BotRunner implements Runnable {
		ConcurrentPerOpteryxPCMBot bot;

		@Override
		public void run() {
			bot = new ConcurrentPerOpteryxPCMBot(null, null);
		}

		public ConcurrentPerOpteryxPCMBot getBot() {
			return bot;
		}

	}

	@Test
	public void uniqueBotNameTest() throws InterruptedException {
		List<Thread> threads = new ArrayList<>();
		List<BotRunner> runners = new ArrayList<>();

		Set<String> names = new HashSet<>();

		for (int i = 0; i < 1000; i++) {
			BotRunner botRunner = new BotRunner();
			runners.add(botRunner);
			Thread botThread = new Thread(botRunner);
			threads.add(botThread);
			botThread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		for (BotRunner runner : runners) {
			names.add(runner.getBot().getBotName());
		}

		assertEquals(1000, names.size());
	}

}
