package io.github.squat_team;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Provides methods for the usage of executors in the tests.
 */
public class ExecutorHelper {

	/**
	 * Provides a default executor.
	 * 
	 * @return
	 */
	public static ExecutorService getNewExecutorService() {
		return Executors.newFixedThreadPool(4);
	}

	/**
	 * Wait until the executor terminated.
	 * 
	 * @param executor
	 * @throws InterruptedException
	 */
	public static void shutdown(ExecutorService executor) throws InterruptedException {
		System.out.println("Wait for Termination");
		executor.shutdown();
		while (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
			java.lang.System.err.println("Threads didn't finish in 60000 seconds!");
		}
		System.out.println("Terminated");
	}

}
