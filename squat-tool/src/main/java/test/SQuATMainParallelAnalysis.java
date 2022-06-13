package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationPackage;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentPackage;
import org.palladiosimulator.pcm.system.SystemPackage;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsagemodelPackage;

import io.github.squat_team.model.OptimizationType;
import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.model.PCMResult;
import io.github.squat_team.model.PCMScenarioResult;
import io.github.squat_team.model.ResponseMeasureType;
import io.github.squat_team.performance.PerformanceMetric;
import io.github.squat_team.performance.PerformancePCMCPUScenario;
import io.github.squat_team.performance.PerformancePCMUsageScenario;
import io.github.squat_team.performance.PerformancePCMWokloadScenario;
import io.github.squat_team.performance.AbstractPerformancePCMScenario;
import io.github.squat_team.performance.peropteryx.ConcurrentPerOpteryxPCMBot;
import io.github.squat_team.performance.peropteryx.PerOpteryxPCMBot;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;
import io.github.squat_team.performance.peropteryx.configuration.DesigndecisionConfigImproved;
import io.github.squat_team.performance.peropteryx.start.OptimizationInfoImrpoved;
import io.github.squat_team.util.SQuATHelper;
import test.TestConstants;

/**
 * Main class to run the SQuAT Performance Bot
 */
public class SQuATMainParallelAnalysis {
	private static final String BOT_NAME1 = "PB1";
	private static final String BOT_NAME2 = "PB2";
	private static final String BOT_NAME3 = "PB3";
	private static final String BOT_NAME4 = "PB4";
	private static Boolean multiOptimisation = false;
	private static final Boolean DEBUG_MODE = false;
	
	private static void register() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		EPackage.Registry.INSTANCE.put(RepositoryPackage.eNS_URI, RepositoryPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(ResourceenvironmentPackage.eNS_URI, ResourceenvironmentPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(SystemPackage.eNS_URI, SystemPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(AllocationPackage.eNS_URI, AllocationPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(UsagemodelPackage.eNS_URI, UsagemodelPackage.eINSTANCE);
	}

	public static void main(String[] args) throws IOException {
		register();

		// create scenario
		ArrayList<String> workloadIDs = new ArrayList<String>();
		workloadIDs.add(TestConstants.WORKLOAD_ID);

		ArrayList<String> cpuIDs = new ArrayList<String>();
		cpuIDs.add(TestConstants.CPU_ID);

		ArrayList<String> loopIDs = new ArrayList<String>();
		loopIDs.add(TestConstants.LOOP_ID);

		// Null-Scenario
		// AbstractPerformancePCMScenario scenario = new
		// PerformancePCMWokloadScenario(OptimizationType.MINIMIZATION, workloadIDs,
		// 1.0);

		// Scenario 1: Workload +10%
		// AbstractPerformancePCMScenario scenario = new
		// PerformancePCMWokloadScenario(OptimizationType.MINIMIZATION, workloadIDs,
		// 1.1);
		// Scenario 2: Workload +50%
		AbstractPerformancePCMScenario scenario = new PerformancePCMWokloadScenario(OptimizationType.MINIMIZATION,
				workloadIDs, 1.5);
		// Scenario 3: Fail of a Server in a 2-server-cluster
		// AbstractPerformancePCMScenario scenario = new
		// PerformancePCMCPUScenario(OptimizationType.MINIMIZATION, cpuIDs, 0.5);
		// Scenario 4: Users buy more products (other distribution for barcode scanning)
		// AbstractPerformancePCMScenario scenario = new
		// PerformancePCMUsageScenario(OptimizationType.MINIMIZATION, loopIDs,
		// TestConstants.LOOP_DISTRIBUTION);

		PCMResult expectedResponse = new PCMResult(ResponseMeasureType.DECIMAL);
		expectedResponse.setResponse(6.0);
		scenario.setExpectedResponse(expectedResponse);
		scenario.setMetric(PerformanceMetric.RESPONSE_TIME);

		// create configuration
		ConfigurationImprovedImproved configuration = new ConfigurationImprovedImproved();
		// configuration.getPerOpteryxConfig().setDesignDecisionFile(TestConstants.DESIGNDECISION_FILE_PATH);
		// configuration.getPerOpteryxConfig().setQmlDefinitionFile(TestConstants.QML_FILE_PATH);
		configuration.getPerOpteryxConfig().setGenerationSize(10);
		configuration.getPerOpteryxConfig().setMaxIterations(2);

		configuration.getLqnsConfig().setLqnsOutputDir(TestConstants.LQN_OUTPUT);
		configuration.getExporterConfig().setPcmOutputFolder(TestConstants.PCM_STORAGE_PATH);
		configuration.getPcmModelsConfig().setPathmapFolder(TestConstants.PCM_MODEL_FILES);

		// Design Decision Adjustment
		DesigndecisionConfigImproved designdecisionConfig = configuration.getDesignDecisionConfig();
		designdecisionConfig.setLimits("_78qo4K2UEeaxN4gXuIkS2A", 100, 6000);
		designdecisionConfig.setLimits("_-5Q84K2UEeaxN4gXuIkS2A", 110, 6100);
		designdecisionConfig.setLimits("_BgmykK2VEeaxN4gXuIkS2A", 120, 6200);
		designdecisionConfig.setLimits("_FM6FMK2VEeaxN4gXuIkS2A", 130, 6300);

		// init bot
		ConcurrentPerOpteryxPCMBot bot = new ConcurrentPerOpteryxPCMBot(BOT_NAME1, scenario, configuration);
		bot.setDebugMode(DEBUG_MODE);
		bot.setDetailedAnalysis(false);

		AbstractPerformancePCMScenario scenario2 = new PerformancePCMWokloadScenario(OptimizationType.MINIMIZATION,
				workloadIDs, 1.5);
		// Scenario 3: Fail of a Server in a 2-server-cluster
		// AbstractPerformancePCMScenario scenario = new
		// PerformancePCMCPUScenario(OptimizationType.MINIMIZATION, cpuIDs, 0.5);
		// Scenario 4: Users buy more products (other distribution for barcode scanning)
		// AbstractPerformancePCMScenario scenario = new
		// PerformancePCMUsageScenario(OptimizationType.MINIMIZATION, loopIDs,
		// TestConstants.LOOP_DISTRIBUTION);

		PCMResult expectedResponse2 = new PCMResult(ResponseMeasureType.DECIMAL);
		expectedResponse2.setResponse(6.0);
		scenario2.setExpectedResponse(expectedResponse2);
		scenario2.setMetric(PerformanceMetric.RESPONSE_TIME);

		// create configuration
		ConfigurationImprovedImproved configuration2 = new ConfigurationImprovedImproved();
		// configuration.getPerOpteryxConfig().setDesignDecisionFile(TestConstants.DESIGNDECISION_FILE_PATH);
		// configuration.getPerOpteryxConfig().setQmlDefinitionFile(TestConstants.QML_FILE_PATH);
		configuration2.getPerOpteryxConfig().setGenerationSize(10);
		configuration2.getPerOpteryxConfig().setMaxIterations(2);

		configuration2.getLqnsConfig().setLqnsOutputDir(TestConstants.LQN_OUTPUT);
		configuration2.getExporterConfig().setPcmOutputFolder(TestConstants.PCM_STORAGE_PATH);
		configuration2.getPcmModelsConfig().setPathmapFolder(TestConstants.PCM_MODEL_FILES);

		// Design Decision Adjustment
		DesigndecisionConfigImproved designdecisionConfig2 = configuration2.getDesignDecisionConfig();
		designdecisionConfig2.setLimits("_78qo4K2UEeaxN4gXuIkS2A", 100, 6000);
		designdecisionConfig2.setLimits("_-5Q84K2UEeaxN4gXuIkS2A", 110, 6100);
		designdecisionConfig2.setLimits("_BgmykK2VEeaxN4gXuIkS2A", 120, 6200);
		designdecisionConfig2.setLimits("_FM6FMK2VEeaxN4gXuIkS2A", 130, 6300);

		// init bot
		ConcurrentPerOpteryxPCMBot bot2 = new ConcurrentPerOpteryxPCMBot(BOT_NAME2, scenario2, configuration2);
		bot2.setDebugMode(DEBUG_MODE);
		bot2.setDetailedAnalysis(false);

		AbstractPerformancePCMScenario scenario3 = new PerformancePCMWokloadScenario(OptimizationType.MINIMIZATION,
				workloadIDs, 1.5);
		// Scenario 3: Fail of a Server in a 2-server-cluster
		// AbstractPerformancePCMScenario scenario = new
		// PerformancePCMCPUScenario(OptimizationType.MINIMIZATION, cpuIDs, 0.5);
		// Scenario 4: Users buy more products (other distribution for barcode scanning)
		// AbstractPerformancePCMScenario scenario = new
		// PerformancePCMUsageScenario(OptimizationType.MINIMIZATION, loopIDs,
		// TestConstants.LOOP_DISTRIBUTION);

		PCMResult expectedResponse3 = new PCMResult(ResponseMeasureType.DECIMAL);
		expectedResponse3.setResponse(6.0);
		scenario3.setExpectedResponse(expectedResponse3);
		scenario3.setMetric(PerformanceMetric.RESPONSE_TIME);

		// create configuration
		ConfigurationImprovedImproved configuration3 = new ConfigurationImprovedImproved();
		// configuration.getPerOpteryxConfig().setDesignDecisionFile(TestConstants.DESIGNDECISION_FILE_PATH);
		// configuration.getPerOpteryxConfig().setQmlDefinitionFile(TestConstants.QML_FILE_PATH);
		configuration3.getPerOpteryxConfig().setGenerationSize(10);
		configuration3.getPerOpteryxConfig().setMaxIterations(2);

		configuration3.getLqnsConfig().setLqnsOutputDir(TestConstants.LQN_OUTPUT);
		configuration3.getExporterConfig().setPcmOutputFolder(TestConstants.PCM_STORAGE_PATH);
		configuration3.getPcmModelsConfig().setPathmapFolder(TestConstants.PCM_MODEL_FILES);

		// Design Decision Adjustment
		DesigndecisionConfigImproved designdecisionConfig3 = configuration3.getDesignDecisionConfig();
		designdecisionConfig3.setLimits("_78qo4K2UEeaxN4gXuIkS2A", 100, 6000);
		designdecisionConfig3.setLimits("_-5Q84K2UEeaxN4gXuIkS2A", 110, 6100);
		designdecisionConfig3.setLimits("_BgmykK2VEeaxN4gXuIkS2A", 120, 6200);
		designdecisionConfig3.setLimits("_FM6FMK2VEeaxN4gXuIkS2A", 130, 6300);

		// init bot
		ConcurrentPerOpteryxPCMBot bot3 = new ConcurrentPerOpteryxPCMBot(BOT_NAME3, scenario3, configuration3);
		bot3.setDebugMode(DEBUG_MODE);
		bot3.setDetailedAnalysis(false);

		AbstractPerformancePCMScenario scenario4 = new PerformancePCMWokloadScenario(OptimizationType.MINIMIZATION,
				workloadIDs, 1.5);
		// Scenario 3: Fail of a Server in a 2-server-cluster
		// AbstractPerformancePCMScenario scenario = new
		// PerformancePCMCPUScenario(OptimizationType.MINIMIZATION, cpuIDs, 0.5);
		// Scenario 4: Users buy more products (other distribution for barcode scanning)
		// AbstractPerformancePCMScenario scenario = new
		// PerformancePCMUsageScenario(OptimizationType.MINIMIZATION, loopIDs,
		// TestConstants.LOOP_DISTRIBUTION);

		PCMResult expectedResponse4 = new PCMResult(ResponseMeasureType.DECIMAL);
		expectedResponse4.setResponse(6.0);
		scenario4.setExpectedResponse(expectedResponse4);
		scenario4.setMetric(PerformanceMetric.RESPONSE_TIME);

		// create configuration
		ConfigurationImprovedImproved configuration4 = new ConfigurationImprovedImproved();
		// configuration.getPerOpteryxConfig().setDesignDecisionFile(TestConstants.DESIGNDECISION_FILE_PATH);
		// configuration.getPerOpteryxConfig().setQmlDefinitionFile(TestConstants.QML_FILE_PATH);
		configuration4.getPerOpteryxConfig().setGenerationSize(10);
		configuration4.getPerOpteryxConfig().setMaxIterations(2);

		configuration4.getLqnsConfig().setLqnsOutputDir(TestConstants.LQN_OUTPUT);
		configuration4.getExporterConfig().setPcmOutputFolder(TestConstants.PCM_STORAGE_PATH);
		configuration4.getPcmModelsConfig().setPathmapFolder(TestConstants.PCM_MODEL_FILES);

		// Design Decision Adjustment
		DesigndecisionConfigImproved designdecisionConfig4 = configuration4.getDesignDecisionConfig();
		designdecisionConfig4.setLimits("_78qo4K2UEeaxN4gXuIkS2A", 100, 6000);
		designdecisionConfig4.setLimits("_-5Q84K2UEeaxN4gXuIkS2A", 110, 6100);
		designdecisionConfig4.setLimits("_BgmykK2VEeaxN4gXuIkS2A", 120, 6200);
		designdecisionConfig4.setLimits("_FM6FMK2VEeaxN4gXuIkS2A", 130, 6300);

		// init bot
		ConcurrentPerOpteryxPCMBot bot4 = new ConcurrentPerOpteryxPCMBot(BOT_NAME4, scenario4, configuration4);
		bot4.setDebugMode(DEBUG_MODE);
		bot4.setDetailedAnalysis(false);

		List<String> basicPaths = new ArrayList<String>();

		/*
		 * Searchs for subfolders with allocation files in it.
		 */
		File file = new File(TestConstants.BASIC_FILE_PATH);
		multiOptimisation = file.isDirectory();
		System.out.println("Switched to multi optimization/analysis mode");
		if (multiOptimisation) {
			System.out.println("Directory Mode");
			File[] subFolders = file.listFiles();
			for (File subFolder : subFolders) {
				if (subFolder.isDirectory()) {
					File[] subFiles = subFolder.listFiles();
					for (File subFile : subFiles) {
						String subFilePath = subFile.getPath();
						if (subFilePath.endsWith(".allocation")) {
							String newBasicPath = subFilePath.substring(0, subFilePath.length() - 11);
							newBasicPath = newBasicPath.replace("\\", "\\\\");
							basicPaths.add(newBasicPath);
						}
					}
				}
			}
		} else {
			System.out.println("Single File Mode");
			basicPaths.add(TestConstants.BASIC_FILE_PATH);
		}

		System.out.println("OPTIMIZE PCM INSTANCES:");
		System.out.println(basicPaths);
		System.out.println("no of instances: " + basicPaths.size());

		for (String basicPath : basicPaths) {
			if (multiOptimisation) {
				configuration.getExporterConfig().setPcmOutputFolder(basicPath.replace("\\\\", "/"));
			}

			String model1 = basicPath.replace("cocome-cloud", "pall/cocome-cloud");
			// create Instance
			Allocation allocation1 = SQuATHelper.loadAllocationModel("file:/" + model1 + ".allocation");
			org.palladiosimulator.pcm.system.System system1 = SQuATHelper
					.loadSystemModel("file:/" + model1 + ".system");
			ResourceEnvironment resourceenvironment1 = SQuATHelper
					.loadResourceEnvironmentModel("file:/" + model1 + ".resourceenvironment");
			Repository repository1 = SQuATHelper.loadRepositoryModel("file:/" + model1 + ".repository");
			UsageModel usageModel1 = SQuATHelper.loadUsageModel("file:/" + model1 + ".usagemodel");
			PCMArchitectureInstance architecture1 = new PCMArchitectureInstance("", repository1, system1, allocation1,
					resourceenvironment1, usageModel1);
			architecture1.setRepositoryWithAlternatives(SQuATHelper
					.loadRepositoryModel("file:" + "/home/rss/SQuAT/Cocome/test/pall/alternativeRepository.repository"));
			architecture1.setName("1");

			String model2 = basicPath.replace("cocome-cloud", "pall/cocome-cloud2");
			// create Instance
			Allocation allocation2 = SQuATHelper.loadAllocationModel("file:/" + model2 + ".allocation");
			org.palladiosimulator.pcm.system.System system2 = SQuATHelper
					.loadSystemModel("file:/" + model2 + ".system");
			ResourceEnvironment resourceenvironment2 = SQuATHelper
					.loadResourceEnvironmentModel("file:/" + model2 + ".resourceenvironment");
			Repository repository2 = SQuATHelper.loadRepositoryModel("file:/" + model2 + ".repository");
			UsageModel usageModel2 = SQuATHelper.loadUsageModel("file:/" + model2 + ".usagemodel");
			PCMArchitectureInstance architecture2 = new PCMArchitectureInstance("", repository2, system2, allocation2,
					resourceenvironment2, usageModel2);
			architecture2.setRepositoryWithAlternatives(SQuATHelper
					.loadRepositoryModel("file:" + "/home/rss/SQuAT/Cocome/test/pall/alternativeRepository2.repository"));
			architecture2.setName("2");

			String model3 = basicPath.replace("cocome-cloud", "pall/cocome-cloud3");
			// create Instance
			Allocation allocation3 = SQuATHelper.loadAllocationModel("file:/" + model3 + ".allocation");
			org.palladiosimulator.pcm.system.System system3 = SQuATHelper
					.loadSystemModel("file:/" + model3 + ".system");
			ResourceEnvironment resourceenvironment3 = SQuATHelper
					.loadResourceEnvironmentModel("file:/" + model3 + ".resourceenvironment");
			Repository repository3 = SQuATHelper.loadRepositoryModel("file:/" + model3 + ".repository");
			UsageModel usageModel3 = SQuATHelper.loadUsageModel("file:/" + model3 + ".usagemodel");
			PCMArchitectureInstance architecture3 = new PCMArchitectureInstance("", repository3, system3, allocation3,
					resourceenvironment3, usageModel3);
			architecture3.setRepositoryWithAlternatives(SQuATHelper
					.loadRepositoryModel("file:" + "/home/rss/SQuAT/Cocome/test/pall/alternativeRepository3.repository"));
			architecture3.setName("3");

			String model4 = basicPath.replace("cocome-cloud", "pall/cocome-cloud4");
			// create Instance
			Allocation allocation4 = SQuATHelper.loadAllocationModel("file:/" + model4 + ".allocation");
			org.palladiosimulator.pcm.system.System system4 = SQuATHelper
					.loadSystemModel("file:/" + model4 + ".system");
			ResourceEnvironment resourceenvironment4 = SQuATHelper
					.loadResourceEnvironmentModel("file:/" + model4 + ".resourceenvironment");
			Repository repository4 = SQuATHelper.loadRepositoryModel("file:/" + model4 + ".repository");
			UsageModel usageModel4 = SQuATHelper.loadUsageModel("file:/" + model4 + ".usagemodel");
			PCMArchitectureInstance architecture4 = new PCMArchitectureInstance("", repository4, system4, allocation4,
					resourceenvironment4, usageModel4);
			architecture4.setRepositoryWithAlternatives(SQuATHelper
					.loadRepositoryModel("file:" + "/home/rss/SQuAT/Cocome/test/pall/alternativeRepository4.repository"));
			architecture4.setName("4");

			// TODO: should not be used in multiOptimization
			// architecture.setRepositoryWithAlternatives(SQuATHelper.loadRepositoryModel("file:/"
			// + TestConstants.ALTERNATIVE_REPOSITORY_PATH));

			// configuration.getPerOpteryxConfig().setMaxIterations(1);
			// configuration.getPerOpteryxConfig().setGenerationSize(1);

			// TODO:
			// optimize(bot, architecture, basicPath, configuration);
			
			optimize(bot, architecture1, basicPath, configuration);
			
			 analyze(bot, architecture1, model1); 
			 //analyze(bot2, architecture2, model2);
			 //analyze(bot3, architecture3, model3);
			 //analyze(bot4, architecture4, model4);
 
			//bot.searchForAlternatives(architecture1);
	
			/*
			new Thread(() -> {
				try {
					optimize(bot, architecture1, basicPath, configuration);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();		
			new Thread(() -> {
				try {
					optimize(bot2, architecture2, basicPath, configuration2);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();		
			new Thread(() -> {
				try {
					optimize(bot, architecture1, basicPath, configuration);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();		
			
			new Thread(() -> {
				try {
					analyze(bot, architecture1, model1);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();
			new Thread(() -> {
				try {
					analyze(bot2, architecture2, model2);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();
			new Thread(() -> {
				try {
					optimize(bot, architecture1, basicPath, configuration);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();		
			new Thread(() -> {
				try {
					analyze(bot3, architecture3, model3);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();
			new Thread(() -> {
				try {
					analyze(bot4, architecture4, model4);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();
			new Thread(() -> {
				try {
					analyze(bot3, architecture3, model3);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();
			new Thread(() -> {
				try {
					analyze(bot4, architecture4, model4);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();
									*/
		}
		// AUTOMATIC EVALUATION - cant be used
		/*
		 * Map<Integer, Comparable> values = new HashMap<Integer, Comparable>();
		 * Map<Integer, Long> times = new HashMap<Integer, Long>(); for(int i = 0; i <
		 * 1; i++){ System.out.println("Starting iteration " + i);
		 * 
		 * //execute long start = System.currentTimeMillis(); List<PCMScenarioResult>
		 * results = bot.searchForAlternatives(architecture); long end =
		 * System.currentTimeMillis();
		 * 
		 * times.put(i, (end-start)); Comparable value = 100000.0; for(PCMScenarioResult
		 * result : results){ if(result.getResult().getResponse().compareTo(value) < 0){
		 * value = result.getResult().getResponse(); } } values.put(i, value);
		 * 
		 * System.gc(); }
		 * 
		 * System.out.println("RESULTS:"); for(int i : values.keySet()){
		 * System.out.println("===================="); System.out.println( "iteration: "
		 * + i); System.out.println("result: " + values.get(i));
		 * System.out.println("time: " + times.get(i)); }
		 * System.out.println("====================");
		 */
	}

	public static Comparable analyze(ConcurrentPerOpteryxPCMBot bot, PCMArchitectureInstance architecture, String basicPath)
			throws IOException {
		// run bot analyse
		long start = System.currentTimeMillis();
		PCMScenarioResult result = bot.analyze(architecture);
		long end = System.currentTimeMillis();

		File basicFile = new File(TestConstants.BASIC_FILE_PATH);
		File metricFile;
		if (basicFile.isDirectory()) {
			metricFile = new File(basicFile, "analysisResults.txt");
		} else {
			metricFile = new File(basicFile.getParentFile(), "analysisResults.txt");
		}

		metricFile.createNewFile();
		FileOutputStream is = new FileOutputStream(metricFile, true);
		OutputStreamWriter osw = new OutputStreamWriter(is);
		BufferedWriter w = new BufferedWriter(osw);

		w.newLine();
		w.write("-------------------");
		w.newLine();
		w.write("Candidate: " + (new File(basicPath)).getParentFile().getName());
		w.newLine();
		try {
			w.write("result: " + result.getResult().getResponse());
		} catch (Exception e) {
			w.write("result: error (unsolvable)");
		}
		w.newLine();
		w.write("-------------------");
		w.newLine();

		w.close();

		/*
		 * System.out.println(metricFile.getPath());
		 * 
		 * System.out.println(""); System.out.println(TestConstants.BASIC_FILE_PATH);
		 * System.out.println("BOT FINISHED: ");
		 * System.out.println(result.getOriginatingBot());
		 * System.out.println(result.getResult().getResponse());
		 * System.out.println(result.getResultingArchitecture().getName());
		 * System.out.println(result.getResultingArchitecture().getAllocation()) ;
		 * System.out.println(result.getResultingArchitecture().getRepository()) ;
		 * System.out.println(result.getResultingArchitecture().
		 * getResourceEnvironment());
		 * System.out.println(result.getResultingArchitecture().getSystem());
		 * System.out.println(result.getResultingArchitecture().getUsageModel()) ;
		 * System.out.println(end - start);
		 */

		System.out.println("RESULT " + architecture.getName() + ": " + result.getResult().getResponse());

		return result.getResult().getResponse();
	}

	public static void optimize(ConcurrentPerOpteryxPCMBot bot, PCMArchitectureInstance architecture, String basicPath,
			ConfigurationImprovedImproved configuration) throws IOException {
		// run bot optimization
		long start = System.currentTimeMillis();
		List<PCMScenarioResult> results = bot.searchForAlternatives(architecture);
		long end = System.currentTimeMillis();

		File metricFile = new File(basicPath.replace("\\\\", "/") + "_Metrics.txt");

		if (metricFile.exists()) {
			metricFile.delete();
		}
		metricFile.createNewFile();

		FileOutputStream is = new FileOutputStream(metricFile);
		OutputStreamWriter osw = new OutputStreamWriter(is);
		BufferedWriter w = new BufferedWriter(osw);

		System.out.println("BOT FINISHED: ");
		w.write("BOT FINISHED: ");
		w.newLine();
		System.out.println("Population Size: " + configuration.getPerOpteryxConfig().getGenerationSize());
		w.write("Population Size: " + configuration.getPerOpteryxConfig().getGenerationSize());
		w.newLine();
		System.out.println("Max Iterations: " + configuration.getPerOpteryxConfig().getMaxIterations());
		w.write("Max Iterations: " + configuration.getPerOpteryxConfig().getMaxIterations());
		w.newLine();
		System.out.println("Runtime " + (end - start) + " ms");
		w.write("Runtime " + (end - start) + " ms");
		w.newLine();
		System.out.println("Real Iterations: " + OptimizationInfoImrpoved.getIterations());
		w.write("Real Iterations: " + OptimizationInfoImrpoved.getIterations());
		w.newLine();
		System.out.println("");
		w.write("");
		w.newLine();
		System.out.println("Best 10 Candidates:");
		w.write("Best 10 Candidates:");
		w.newLine();
		for (PCMScenarioResult result : results) {
			// bot.analyze(result.getResultingArchitecture());

			System.out.println("----");
			w.write("----");
			w.newLine();
			String uri = result.getResultingArchitecture().getAllocation().eResource().getURI()
					.segment(result.getResultingArchitecture().getAllocation().eResource().getURI().segmentCount() - 2)
					.toString();
			System.out.println("Name: " + uri);
			w.write("Name: " + uri);
			w.newLine();

			// System.out.println(result.getOriginatingBot());
			System.out.println("Response Time: " + result.getResult().getResponse());
			w.write("Response Time: " + result.getResult().getResponse());
			w.newLine();
			/*
			 * System.out.println(result.getResultingArchitecture().getName());
			 * System.out.println(result.getResultingArchitecture(). getAllocation());
			 * System.out.println(result.getResultingArchitecture(). getRepository());
			 * System.out.println(result.getResultingArchitecture().
			 * getResourceEnvironment());
			 * System.out.println(result.getResultingArchitecture().getSystem()) ;
			 * System.out.println(result.getResultingArchitecture(). getUsageModel());
			 */
		}
		w.close();
	}
}
