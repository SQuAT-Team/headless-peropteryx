package io.github.squat_team.performance;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;
import org.palladiosimulator.pcm.usagemodel.Branch;
import org.palladiosimulator.pcm.usagemodel.BranchTransition;
import org.palladiosimulator.pcm.usagemodel.Loop;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;

import io.github.squat_team.model.OptimizationType;
import io.github.squat_team.model.PCMArchitectureInstance;

/**
 * A scenario type that simulates changing usage behavior by influencing the
 * number of loop iterations in the usage model.
 */
public class PerformancePCMUsageScenario extends AbstractPerformancePCMScenario {

	private List<String> loopIDs;
	private List<Loop> consideredLoops;
	private List<String> oldLoopIterations;
	private String numberOfIterations;

	/**
	 * Constructs a new scenario based on a change of the number of loop iterations
	 * in the usage model.
	 * 
	 * @param type
	 *            describes whether the optimization is minimization or
	 *            maximization.
	 * @param loopIDs
	 *            the IDs of the loops which should be considered. Number of calls
	 *            will only be changed for these loops.
	 * @param numberOfIterations
	 *            the new number of iterations that should be set for the specified
	 *            loop. Can also be a probabilistic function.
	 */
	public PerformancePCMUsageScenario(OptimizationType type, List<String> loopIDs, String numberOfIterations) {
		super(type);
		this.loopIDs = loopIDs;
		this.numberOfIterations = numberOfIterations;
		this.consideredLoops = new ArrayList<>();
		this.oldLoopIterations = new ArrayList<>();
	}

	@Override
	public void transform(PCMArchitectureInstance architecture) {
		UsageModel usageModel = architecture.getUsageModel();
		findLoops(usageModel);
		setNewLoopIterationNumber();
		architecture.saveModel();
	}

	@Override
	public void inverseTransform(PCMArchitectureInstance architecture) {
		UsageModel usageModel = architecture.getUsageModel();
		findLoops(usageModel);
		setOldLoopIterationNumber();
		architecture.saveModel();
	}

	/**
	 * Find the specified loops in the model.
	 * 
	 * @param usageModel the model the loops are searched in.
	 */
	private void findLoops(UsageModel usageModel) {
		consideredLoops.clear();
		EList<UsageScenario> usageScenarios = usageModel.getUsageScenario_UsageModel();
		for (UsageScenario usageScenario : usageScenarios) {
			ScenarioBehaviour seff = usageScenario.getScenarioBehaviour_UsageScenario();
			handleSEFF(seff);
		}
	}
	
	/**
	 * Handles loop finding in SEFFs.
	 * 
	 * @param seff the seff to search in.
	 */
	private void handleSEFF(ScenarioBehaviour seff) {
		EList<AbstractUserAction> actions = seff.getActions_ScenarioBehaviour();
		for (AbstractUserAction action : actions) {
			if (action instanceof Loop) {
				handleLoop((Loop) action);
			}
			if (action instanceof Branch) {
				handleBranch((Branch) action);
			}
		}
	}

	/**
	 * Determines what happens when a loop is found. The loop is considered for applying changes, if its ID is specified.
	 * 
	 * @param loop the found loop.
	 */
	private void handleLoop(Loop loop) {
		if (loopIDs.contains(loop.getId())) {
			consideredLoops.add(loop);
		}
	}

	/**
	 * Determines what happens when a branch is found. Search for loops in the leaves.
	 * 
	 * @param branch the found branch.
	 */
	private void handleBranch(Branch branch) {
		for (BranchTransition currentBranchTransition : branch.getBranchTransitions_Branch()) {
			handleSEFF(currentBranchTransition.getBranchedBehaviour_BranchTransition());
		}
	}

	/**
	 * Sets the number of iterations to the specified value for all considered loops.
	 */
	private void setNewLoopIterationNumber() {
		for (Loop currentLoop : consideredLoops) {
			PCMRandomVariable loopIterationVariable = currentLoop.getLoopIteration_Loop();
			oldLoopIterations.add(loopIterationVariable.getSpecification());
			loopIterationVariable.setSpecification(numberOfIterations);
		}
	}

	/**
	 * Sets the number of iterations to the previous value, reverting the changes.
	 */
	private void setOldLoopIterationNumber() {
		for (int i = 0; i < consideredLoops.size(); i++) {
			PCMRandomVariable loopIterationVariable = consideredLoops.get(i).getLoopIteration_Loop();
			loopIterationVariable.setSpecification(oldLoopIterations.get(i));
		}
	}

}
