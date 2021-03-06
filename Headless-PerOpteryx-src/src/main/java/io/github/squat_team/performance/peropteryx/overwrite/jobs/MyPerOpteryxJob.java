package io.github.squat_team.performance.peropteryx.overwrite.jobs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.uka.ipd.sdq.dsexplore.launch.DSELaunch;
import de.uka.ipd.sdq.dsexplore.launch.DSEWorkflowConfiguration;
import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.ICompositeJob;
import de.uka.ipd.sdq.workflow.jobs.SequentialBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;


public class MyPerOpteryxJob extends
SequentialBlackboardInteractingJob<MDSDBlackboard> implements
ICompositeJob {

    //DSECandidateConfiguration candidateConfig;

    public  MyPerOpteryxJob(final DSEWorkflowConfiguration config, final DSELaunch launch) throws CoreException {
        super(false);

        // 1. Load PCM Models into memory
        this.addJob(new MyLoadPCMModelsIntoBlackboardJob(config));

        // 2. Validate PCM Models
        this.addJob(new MyValidatePCMModelsJob(config));

        //TODO remove step 3 and 5, there should be no need
        //anymore to save the models under a different name.
        //Do I ever need the initial model?
        // 3. Create model files for candidate models
        //this.candidateConfig = createCandidateSetup(config);

        // 4. Copy initial instance to separate blackboard partition
        this.add(new MyMoveInitialPCMModelPartitionJob());

        // create the PCM partition anew but empty
        this.add(new MyPreparePCMBlackboardPartitionJob());

        // 5. Load the candidate models again from updated files
        //this.addJob(new LoadPCMModelsIntoBlackboardJob(candidateConfig));

        // 6. Run Optimisation on Loaded Models
        this.add(new MyOptimisationJob(config, launch));

    }

    @Override
    public void cleanup(final IProgressMonitor monitor) throws CleanupFailedException {
        super.cleanup(monitor);
        /*try {
			this.candidateConfig.getRawConfiguration().delete();
		} catch (CoreException e) {
			throw new RollbackFailedException("Could not delete launch configuration "+this.candidateConfig.getRawConfiguration().getName(),e);
		}*/
        monitor.worked(1);
    }


}