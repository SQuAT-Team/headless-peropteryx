package io.github.squat_team.performance.peropteryx.overwrite.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
//import org.eclipse.ui.IWorkbenchPage;
//import org.eclipse.ui.PartInitException;
//import org.eclipse.ui.PlatformUI;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.solver.handler.LineServerHandler;
import org.palladiosimulator.solver.handler.LineServerHandlerFactory;
import org.palladiosimulator.solver.models.PCMInstance;
import org.palladiosimulator.solver.runconfig.MessageStrings;
import org.palladiosimulator.solver.runconfig.PCMSolverWorkflowRunConfiguration;
import org.palladiosimulator.solver.transformations.ContextWrapper;
import org.palladiosimulator.solver.transformations.SolverStrategy;
import org.palladiosimulator.solver.visitors.UsageModelVisitor;
import org.palladiosimulator.solver.visualisation.LQNHtmlResultGenerator;
import org.palladiosimulator.solver.visualisation.LQNResultEditorInput;
import org.palladiosimulator.solver.transformations.pcm2lqn.*;

/**
 * This is an excerpt of Heiko's dissertation (see below for link)
 * 
 * The Layered Queueing Network (LQN) model is a performance model in the class
 * of extended queueing networks. It is a popular model with widespread use
 * [BDIS04]. Like the PCM, it specifically targets analysing the performance of
 * distributed systems. While ordinary queueing networks model software
 * structures only implicitly via resource demands to service centers, LQNs
 * model a system as a layered hierarchy of interacting software entities, which
 * produce demands for the underlying physical resources such as CPUs or hard
 * disks. Therefore, LQNs reflect the structure of distributed systems more
 * naturally than ordinary queueing networks. In particular, they model the
 * routing of jobs in the network more realistically.
 * 
 * In the context of this work, a model transformation from PCM instances (with
 * computed context models) to LQNs has been implemented. The transformation
 * offers at least two advantages: First, it enables comparing the concepts of
 * the PCM with concepts of LQNs, which can be considered as a state-of-the-art
 * performance model. Second, the transformation makes the sophisticated
 * analytical solvers and simulation tools for LQNs available to the PCM. Other
 * than SREs, LQNs support concurrent behaviour, different kinds of workloads,
 * asynchronous interactions, and different scheduling strategies. Therefore, it
 * is possible to derive performance metrics such as resource utilizations and
 * throughput from PCM instances, which is not possible with SREs. However, LQNs
 * are restricted to exponential distributions and mean-values analysis as
 * discussed later.
 * 
 * The chapter 6.4 in Heiko's dissertation will first provide some background
 * about LQNs and their development in recent years (Chapter 6.4.2). Then, it
 * will describe the syntax and (informal) semantics of LQNs using the LQN
 * meta-model and several examples (Chapter 6.4.3). Chapter 6.4.4 briefly
 * describes two performance solvers for LQNs, before Chapter 6.4.5 presents the
 * mapping from PCM instances to LQN instances. Finally, Chapter 6.4.6 compares
 * the PCM model with the LQN model, as well as the existing PCM solvers with
 * two available LQN solvers.
 * 
 * @see Heiko's dissertation, section 6.4 at
 *      http://docserver.bis.uni-oldenburg.de
 *      /_publikationen/dissertation/2008/kozpar08/pdf/kozpar08.pdf
 * @author Heiko Koziolek
 * 
 */
public class MyPcm2LqnStrategyImrpoved implements SolverStrategy {

	private static Logger logger = Logger.getLogger(MyPcm2LqnStrategyImrpoved.class
			.getName());

	// the following filenames should be OS-independent
	private String filenameInputXML;
	private String filenameResultHumanReadable;
	private String filenameResultXML;

	// the lqn tools should be in the system path
	private static final String FILENAME_LQNS = "/usr/local/bin/lqns";
	private static final String FILENAME_LQSIM = "lqsim";
	private static final String FILENAME_LINE = "LINE";
	
	//file extension for XML should be .lqxo as that is registered in the org.palladio....lqn plugins now. Should not be one of .in, .lqn or .xlqn, as these are interpreted as the textual format by lqns. All other files extensions are interpreted as XML (see lqns manual). 
	public static final String LQN_FILE_EXTENSION = "lqxo";

	// Return values of lqns
	private static final int LQNS_RETURN_SUCCESS = 0;
	private static final int LQNS_RETURN_MODEL_FAILED_TO_CONVERGE = 1;
	private static final int LQNS_RETURN_INVALID_INPUT = 2;
	private static final int LQNS_RETURN_FATAL_ERROR = -1;

	private long overallDuration = 0;
	private PCMSolverWorkflowRunConfiguration config;

	public MyPcm2LqnStrategyImrpoved(PCMSolverWorkflowRunConfiguration configuration) {
		config = configuration;

		DateFormat dateFormat = new SimpleDateFormat("-yyyy-MM-dd-HHmmssSSSS");
		Date date = new Date();
		String timestamp = dateFormat.format(date);

		filenameInputXML = getOutputFolder()
				+ System.getProperty("file.separator") + "pcm2lqn" + timestamp
				+ ".in."+LQN_FILE_EXTENSION;
		filenameResultHumanReadable = getOutputFolder()
				+ System.getProperty("file.separator") + "pcm2lqn" + timestamp
				+ ".out";
		filenameResultXML = getOutputFolder()
				+ System.getProperty("file.separator") + "pcm2lqn"
				+ timestamp + ".out."+LQN_FILE_EXTENSION;
	}

	public String getFilenameResultXML() {
		return filenameResultXML;
	}

	private String getOutputFolder() {
		if (getSolverProgramName().equals(FILENAME_LQNS)) {
			return config.getLqnsOutputDir();
		} else if (getSolverProgramName().equals(FILENAME_LINE)) {
			return config.getLINEOutputDir();
		} else {
			return config.getLqsimOutputDir();
		}
	}

	public MyPcm2LqnStrategyImrpoved() {
	}

	public void loadTransformedModel(String fileName) {
	}

	public void solve() {
		String solverProgram = getSolverProgramName();
		String lqnsOutputType = getLqnsOutputTypeName();
		String lqnSimOutputType = getLqsimOutputTypeName();
		
		String options = "";

		String resultFile = "";
		String inputFile = "";

		long timeBeforeCalc = System.nanoTime();

		int exitVal = LQNS_RETURN_FATAL_ERROR;
		String errorMessages = "";
		
		try {
			String command = "";
			
			
			
			if (solverProgram.equals(FILENAME_LQNS) || solverProgram.equals(FILENAME_LQSIM)){
				String lqnOutputType = "";
				if (solverProgram.equals(FILENAME_LQNS)) {

					// check whether Pragmas (see LQN documentation) are used and if yes, set -P option
					if (!config.getStopOnMessageLossLQNS() 
							|| !"".equals(config.getPragmas())){
						options += " -P ";
						if (!config.getStopOnMessageLossLQNS()){
							options += "stop-on-message-loss=false "; 
						}
						if (!"".equals(config.getPragmas())){
							options += config.getPragmas();
						}
					}
					
					lqnOutputType = lqnsOutputType;
				} else if (solverProgram.equals(FILENAME_LQSIM)) {
					// LQSim config
					String blocks = config.getLQSimBlocks();
					String runtime = config.getLQSimRuntime();

					if (runtime != null && runtime != ""){
						options += " -A "+runtime;
					}
					if (blocks != null && blocks != ""){
						options += " -B "+blocks;
					}
					if (!config.getStopOnMessageLossLQSim()){
						options += " -P stop-on-message-loss=false";
					}
					
					lqnOutputType = lqnSimOutputType;

				}
				if (lqnOutputType.equals(MessageStrings.LQN_OUTPUT_HUMAN)) {
					inputFile = filenameInputXML;
					resultFile = filenameResultHumanReadable;
					command = solverProgram
							+ options
							+ " -o" + resultFile + " " + inputFile;
				} else if (lqnOutputType.equals(MessageStrings.LQN_OUTPUT_XML)
						|| lqnOutputType.equals(MessageStrings.LQN_OUTPUT_HTML)) {

					inputFile = filenameInputXML;
					resultFile = filenameResultXML;
					command = solverProgram
							+ options
							+ " -x -o" + resultFile + " " + inputFile;
				}
			} else if(solverProgram.equals(FILENAME_LINE)){			
				//Using line requires interaction with the server so execute it directly here
				inputFile = filenameInputXML;
				resultFile = filenameResultXML;
				fixXmlHeader(inputFile);
				LineServerHandlerFactory.setLINEPropertyFile(config.getLINEPropFile());
				LineServerHandler lineHandler = LineServerHandlerFactory.getHandler();
				//connect to LINE
				lineHandler.connectToLINEServer();			
				String  inputFileAbsPath = new File(inputFile).getAbsolutePath();

				//solve without random enviroment
				lineHandler.solve(inputFileAbsPath,null);		
				// wait for the model to be solved
				while (!lineHandler.isSolved(inputFileAbsPath))
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						logger.error("Error watiting for LINE solution",e);
					}
				//terminate the connection
				lineHandler.terminateLine();	

			}

			//call the solver executale only if we are not using LINE
			if(!solverProgram.equals(FILENAME_LINE)){
				//apply the patch to fix the header
				fixXmlHeader(inputFile);
				
				logger.warn("Calling LQN analysis tool with "+command);
				ProcessBuilder pb = new ProcessBuilder(splitToCommandArray(command));
				pb.redirectErrorStream(true);
				Process proc = pb.start();

				// StreamGobbler errorGobbler = new
				// StreamGobbler(proc.getErrorStream(), "ERROR");
				// StreamGobbler outputGobbler = new
				// StreamGobbler(proc.getInputStream(), "OUTPUT");
				// errorGobbler.start();
				// outputGobbler.start();

				errorMessages = readStream(proc.getInputStream());

				exitVal = proc.waitFor();
				proc.destroy();
			}

		} catch (Throwable e) {
			logger.error("Running " + solverProgram + " failed!");
			throw new RuntimeException(e);
		}

		long timeAfterCalc = System.nanoTime();
		long duration = TimeUnit.NANOSECONDS.toMillis(timeAfterCalc
				- timeBeforeCalc);
		overallDuration += duration;
		logger.warn("Finished Running " + solverProgram + ":\t\t" + duration
				+ " ms");
		logger
		.warn("Completed Analysis:\t\t" + overallDuration
				+ " ms overall");

		/* return if results are available or throw exception. */
		if(!solverProgram.equals(FILENAME_LINE)){
			
			if (exitVal == LQNS_RETURN_SUCCESS ) {
				if (errorMessages.contains("error")){
					logger.error("LQN analysis threw errors: "+errorMessages);
					//					if (errorMessages.contains("is too high")){
					//						throw new RuntimeException("The lqn model failed to converge. Detailed error: "+errorMessages);
					//					}
					logger.warn("Trying to continue and writing results to " + resultFile);
				} else {
					logger.warn("Analysis Result has been written to " + resultFile);
				}

				logger.warn("Analysis Result has been written to " + resultFile);
				if (lqnsOutputType.equals(MessageStrings.LQN_OUTPUT_HTML)){
					//showOutput(resultFile);
					LQNHtmlResultGenerator result = new LQNHtmlResultGenerator(resultFile);
					result.display();
				}

			} else if (exitVal == LQNS_RETURN_MODEL_FAILED_TO_CONVERGE){
				logger.error(solverProgram + " exited with " + exitVal
						+ ": The model failed to converge. Results are most likely inaccurate. ");
				logger.warn("Analysis Result has been written to: " + resultFile);
			} else {
				String message = "";
				if (exitVal == LQNS_RETURN_INVALID_INPUT) {
					message = solverProgram + " exited with " + exitVal
							+ ": Invalid Input.";
				} else if (exitVal == LQNS_RETURN_FATAL_ERROR) {
					message = solverProgram + " exited with " + exitVal
							+ ": Fatal error";
				} else {
					message = solverProgram
							+ " returned an unrecognised exit value "
							+ exitVal
							+ ". Key: 0 on success, 1 if the model failed to meet the convergence criteria, 2 if the input was invalid, 4 if a command line argument was incorrect, 8 for file read/write problems and -1 for fatal errors. If multiple input files are being processed, the exit code is the bit-wise OR of the above conditions.";
				}
				message += "\nFurther errors: "+errorMessages;
				logger.error(message);
				throw new RuntimeException(message);
			}
		}
		//if we are using the Performance Engine Solver
		else{
			logger.info("Using the perfromance Engine Solver");
			logger.info("Exit val: "+exitVal);
			logger.info("Results written in: "+resultFile);
		}
	}


	private String getSolverProgramName() {
		if (config.getSolver().equals(MessageStrings.LQNS_SOLVER)) {
			return FILENAME_LQNS;
		} else if (config.getSolver().equals(MessageStrings.LINE_SOLVER)) {
			return FILENAME_LINE;
		} else{
			return FILENAME_LQSIM;
		}
	}

	private String getLqnsOutputTypeName() {
		return config.getLqnsOutput();
	}

	private String getLqsimOutputTypeName() {
		return config.getLqsimOutput();
	}
	
	public String getFilenameInputXML() {
		return filenameInputXML;
	}

	/**
	 * Reads the output file and shows its content in a new text editor window.
	 * 
	 * @param filename
	 */
	/*private void showOutput(String filename) {
		FileInputStream fis = null;
		byte b[] = null;
		try {
			fis = new FileInputStream(filename);
			int x = 0;
			x = fis.available();
			b = new byte[x];
			fis.read(b);
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String content = new String(b);

		final String htmlText = getHtmlForLqnResult(content);
		
//		ResultWindow rw = new ResultWindow(content);
//		rw.open();

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				if (page != null) {
					try {
						page.openEditor(new LQNResultEditorInput(htmlText),
								"org.palladiosimulator.solver.LQNResultEditor");
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
	}*/

	private String getHtmlForLqnResult(String lqnResult) {
		String htmlText = "<html><head><title>LQN Results</title></head>" +
				"<body><pre>" +
				lqnResult +
				"</pre></body></html>";
		return htmlText;
	}

	public void storeTransformedModel(String fileName) {
	}

	public void transform(PCMInstance model) {
		long startTime = System.nanoTime();

		runDSolver(model);

		long timeAfterDSolve = System.nanoTime();
		long duration = TimeUnit.NANOSECONDS.toMillis(timeAfterDSolve
				- startTime);
		overallDuration += duration;
		logger.warn("Finished DSolver:\t\t" + duration + " ms");

		long timeBeforeTransform = System.nanoTime();

		// model.saveComputedContextToFiles(System.getProperty("user.dir")
		// + System.getProperty("file.separator")+"computedContexts");

		runPcm2Lqn(model);

		long timeAfterTransform = System.nanoTime();
		long duration2 = TimeUnit.NANOSECONDS.toMillis(timeAfterTransform
				- timeBeforeTransform);
		overallDuration += duration2;
		logger.warn("Finished PCM2LQN:\t\t" + duration2 + " ms");
	}

	private void runPcm2Lqn(PCMInstance model) {

		LqnBuilder lqnBuilder = new LqnBuilder(config.isInfiniteTaskMultiplicity());
		
		if (getSolverProgramName().equals(FILENAME_LQSIM)){
			lqnBuilder.setIsLQSimAnalysis(true);
		}

		ResourceEnvironment2Lqn reVisitor = new ResourceEnvironment2Lqn(
				lqnBuilder, config);
		reVisitor.doSwitch(model.getResourceEnvironment());

		UsageModel2Lqn umVisitor = new UsageModel2Lqn(lqnBuilder,
				new ContextWrapper(model));
		umVisitor.doSwitch(model.getUsageModel());

		lqnBuilder.finalizeLqnModel(config);

		LqnXmlHandler lqnXmlHandler = new LqnXmlHandler(lqnBuilder
				.getLqnModel());
		lqnXmlHandler.saveModelToXMI(filenameInputXML);

		Pcm2LqnHelper.clearGuidMap();
		//runLqn2Xml();
		//runLqn2XmlReformat();

	}

	private void runDSolver(PCMInstance model) {
		// TODO: fix this (only uses one usage scenario):
		UsageModelVisitor visitor = new UsageModelVisitor(model);
		List<UsageScenario> scenarios = model.getUsageModel()
				.getUsageScenario_UsageModel();
		for (UsageScenario usageScenario : scenarios) {
			visitor
					.doSwitch(usageScenario
							.getScenarioBehaviour_UsageScenario());
		}
	}

	/**
	 * 
	 * @param is
	 * @return the concatenated String of all error messages encountered during the analysis
	 */
	private String readStream(InputStream is) {
		String errorMessages = "";
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null)
				// if (type.equals("ERROR")) logger.error(line);
				if (line.contains("warning")) {
					if (isDebug()) {
						logger.debug(line);
					}
					// else do not log.
				} else {
					logger.warn(line);
					errorMessages += line + "\n";
				}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return errorMessages;
	}

	private String[] splitToCommandArray(String command) {
		return command.split("\\s");
	}

	// FIXME: This is not a good way to remove get the debugging statements. Fix
	// this when introducing a better configuration concept here.
	private boolean isDebug() {
		int level = config.getDebugLevel();
		if (level <= 1) {
			return true;
		} else
			return false;

		// case 0:
		// return Level.TRACE;
		// case 1:
		// return Level.DEBUG;
		// case 2:
		// return Level.INFO;
		// case 3:
		// return Level.WARN;
		// case 4:
		// return Level.ERROR;
		// case 5:
		// return Level.ALL;
		// default:
		// return Level.INFO;
	}
	

	/**
	 * Original patch from Gregory Franks, used to fix the XML header of the generated file
	 * @param inputFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void fixXmlHeader(String inputFile) throws FileNotFoundException,
			IOException {
		//Read the XML input file encoding the LQN instance
		FileReader fr = new FileReader(inputFile);
		BufferedReader br = new BufferedReader(fr);
		//Skip the first line (wrong encoding ASCII)
		br.readLine();
		//Read the following lines
		List<String> content = new ArrayList<String>();
		String line = br.readLine();
		while (line != null) {
			content.add(line + "\n");
			line = br.readLine();
 		}
		//Close the file
		br.close();
		fr.close();
		//Delete the file
		File f = new File(inputFile);
		f.delete();
		//Create a new file with the same name and start writing in it				
		File recordFile = new File(inputFile);			
		FileWriter recordFw = new FileWriter(recordFile);
		BufferedWriter recordBw = new BufferedWriter(recordFw);
		//Write the correct XML header (encoding us-ascii)
		recordBw.write("<?xml version=\"1.0\" encoding=\"us-ascii\"?>\n");
		recordBw.flush();
		//Write all the following lines
		for (String s : content) {
			recordBw.write(s);
			recordBw.flush();
		}
		//Close the file
		recordBw.close();
		recordFw.close();
 	}
	
}

// TODO: Anne: delete this method and the related comments above if the changes
// (to use ProcessBuilder and a single threaded reading out of the output) has
// proved useful.
@Deprecated
class StreamGobbler extends Thread {

	private static Logger logger = Logger.getLogger(StreamGobbler.class
			.getName());

	InputStream is;
	String type;

	StreamGobbler(InputStream is, String type) {
		this.is = is;
		this.type = type;
	}

	@Override
	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null)
				// if (type.equals("ERROR")) logger.error(line);
				if (line.contains("warning")) {
					logger.debug(line);
				} else {
					logger.warn(line);
				}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	 
}
