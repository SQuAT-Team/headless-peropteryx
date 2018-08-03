package io.github.squat_team.performance.peropteryx.start;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.LogManager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import de.uka.ipd.sdq.dsexplore.launch.DSELaunch;
import de.uka.ipd.sdq.dsexplore.launch.DSEWorkflowConfiguration;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import io.github.squat_team.performance.peropteryx.configuration.ConfigurationImprovedImproved;
import io.github.squat_team.performance.peropteryx.configuration.DSEWorkflowConfigurationBuilderImproved;
import io.github.squat_team.performance.peropteryx.environment.PalladioEclipseEnvironmentImrpoved;
import io.github.squat_team.performance.peropteryx.environment.PerOpteryxEclipseEnvironmentImrpoved;
import io.github.squat_team.performance.peropteryx.export.PCMFileExporterImrpoved;
import io.github.squat_team.performance.peropteryx.export.PCMResultsProviderImrpoved;
import io.github.squat_team.performance.peropteryx.export.PerOpteryxPCMResultImrpoved;
import io.github.squat_team.performance.peropteryx.overwrite.MyDSELaunchImrpoved;
import io.github.squat_team.performance.peropteryx.overwrite.jobs.MyPerOpteryxJobImproved;

public class MyHeadlessPerOpteryxRunnerImrpoved implements Callable<List<PerOpteryxPCMResultImrpoved>> {
	private static Logger logger = Logger.getLogger(MyHeadlessPerOpteryxRunnerImrpoved.class.getName());
	private ConfigurationImprovedImproved configuration;
	private boolean debugMode = false;
	private Level loglevel;

	public boolean isDebugMode() {
		return debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public void init(ConfigurationImprovedImproved configuration) {
		this.configuration = configuration;
	}

	@Override
	public List<PerOpteryxPCMResultImrpoved> call() throws Exception {
		List<PerOpteryxPCMResultImrpoved> results;
		validate();
		deactivateLog();
		initialize();
		execute();
		results = export();
		activateLog();
		cleanUp();
		return results;
	}

	private void activateLog() {
		if (!debugMode) {
			org.apache.log4j.LogManager.getRootLogger().setLevel(loglevel);
		}
	}

	private void deactivateLog() {
		if (!debugMode) {
			LogManager.getLogManager().reset();
			loglevel = org.apache.log4j.LogManager.getRootLogger().getLevel();
			org.apache.log4j.LogManager.getRootLogger().setLevel(Level.OFF);
		}
	}

	private void cleanUp() {
		System.gc(); // Run Garbage Collection
	}

	private List<PerOpteryxPCMResultImrpoved> export() {
		List<PerOpteryxPCMResultImrpoved> results = PCMResultsProviderImrpoved.getInstance().provide();
		return results;
	}

	private void execute() {
		try {
			DSELaunch launch = new MyDSELaunchImrpoved(); // just uses reset debugger
			IProgressMonitor monitor = new NullProgressMonitor();

			DSEWorkflowConfigurationBuilderImproved builder = new DSEWorkflowConfigurationBuilderImproved();
			builder.init(configuration);
			DSEWorkflowConfiguration dseConfiguration = builder.build(launch);

			MyPerOpteryxJobImproved job = new MyPerOpteryxJobImproved(dseConfiguration, launch);
			job.setBlackboard(new MDSDBlackboard());

			job.execute(monitor);
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void validate() {
		if (configuration == null) {
			throw new RuntimeException("init must be called first!");
		}
	}

	private void initialize() {
		initializePalladio();
		initializePerOpteryx();
		initializeExporter();
	}

	private void initializePalladio() {
		PalladioEclipseEnvironmentImrpoved.INSTANCE.setup(configuration.getPcmModelsConfig().getPathmapFolder());
	}

	private void initializePerOpteryx() {
		PerOpteryxEclipseEnvironmentImrpoved.INSTANCE.setup();
	}

	private void initializeExporter() {
		PCMFileExporterImrpoved.getInstance().init(configuration.getExporterConfig().getPcmOutputFolder(), configuration.getExporterConfig().isMinimalExport());
		PCMResultsProviderImrpoved.getInstance().setBoundaryValue(configuration.getExporterConfig().getBoundaryValue());
		PCMResultsProviderImrpoved.getInstance().setDirection(configuration.getExporterConfig().getOptimizationDirection());
		PCMResultsProviderImrpoved.getInstance().setExportMode(configuration.getExporterConfig().getExportMode());
		PCMResultsProviderImrpoved.getInstance().setAmount(configuration.getExporterConfig().getAmount());
	}

}
