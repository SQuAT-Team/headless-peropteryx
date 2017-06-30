package test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;
import io.github.squat_team.performance.peropteryx.configuration.PerOpteryxConfigImproved;
import io.github.squat_team.performance.peropteryx.export.ExportModeImrpoved;
import io.github.squat_team.performance.peropteryx.export.OptimizationDirectionImrpoved;
import io.github.squat_team.performance.peropteryx.export.PerOpteryxPCMResultImrpoved;
import io.github.squat_team.performance.peropteryx.start.MyHeadlessPerOpteryxRunnerImrpoved;

/**
 * Main class to run Headless PerOpteryx
 */
public class HPOMain {
	
	private static ConfigurationImprovedImproved config;
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		configurate();
		start();
	}

	private static void configurate(){
		config = new ConfigurationImprovedImproved();
		config.getPerOpteryxConfig().setMaxIterations(1);
		config.getPerOpteryxConfig().setGenerationSize(1);
		
		config.getPcmInstanceConfig().setAllocationModel(TestConstants.ALLOCATION_FILE_PATH);
		config.getPcmInstanceConfig().setUsageModel(TestConstants.USAGE_FILE_PATH);
		config.getPerOpteryxConfig().setDesignDecisionFile(TestConstants.DESIGNDECISION_FILE_PATH);
		config.getPerOpteryxConfig().setQmlDefinitionFile(TestConstants.QML_FILE_PATH);
		config.getPerOpteryxConfig().setMode(PerOpteryxConfigImproved.Mode.OPTIMIZE);
		
		config.getPcmModelsConfig().setPathmapFolder(TestConstants.PCM_MODEL_FILES);
		
		config.getLqnsConfig().setLqnsOutputDir(TestConstants.LQN_OUTPUT);
		config.getExporterConfig().setPcmOutputFolder(TestConstants.PCM_STORAGE_PATH);
		config.getExporterConfig().setExportMode(ExportModeImrpoved.AMOUNT);
		config.getExporterConfig().setAmount(2);
		config.getExporterConfig().setOptimizationDirection(OptimizationDirectionImrpoved.MINIMIZE);
		config.getExporterConfig().setBoundaryValue(6.0);
	}
	
	private static void start() throws InterruptedException, ExecutionException{
	    ExecutorService pool = Executors.newFixedThreadPool(4);
		
	    long start = System.currentTimeMillis();
		MyHeadlessPerOpteryxRunnerImrpoved runner = new MyHeadlessPerOpteryxRunnerImrpoved();
		runner.init(config);
		runner.setDebugMode(false);
	    Future<List<PerOpteryxPCMResultImrpoved>> future = pool.submit(runner);
	    future.get();
	    long end = System.currentTimeMillis();
	    System.out.println("TIME: " + (end-start));
	    System.out.println(future.get().get(0).getValue());
	}
	
}
