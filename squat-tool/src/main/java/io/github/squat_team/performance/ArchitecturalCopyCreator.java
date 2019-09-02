package io.github.squat_team.performance;

import java.io.File;

import org.eclipse.emf.common.util.URI;

import edu.squat.transformations.ArchitecturalVersion;
import io.github.squat_team.AbstractPCMBot;
import io.github.squat_team.model.PCMArchitectureInstance;
import io.github.squat_team.util.PCMWorkingCopyCreator;
import test.TestConstants;

public class ArchitecturalCopyCreator {

	private static ArchitecturalCopyCreator instance;

	public static ArchitecturalCopyCreator getInstance() {
		if (instance == null) {
			instance = new ArchitecturalCopyCreator();
		}
		return instance;
	}

	private ArchitecturalCopyCreator() {
		// SINGLETON
	}

	public ArchitecturalVersion copy(PCMArchitectureInstance archInstance, AbstractPCMBot bot) {
		URI uri = archInstance.getUsageModel().eResource().getURI();
		File modelFile = new File(uri.toFileString());
		String newModelName = modelFile.getParentFile().getParentFile().getName() + "-"
				+ modelFile.getParentFile().getName();
		PCMWorkingCopyCreator copyCreator = new PCMWorkingCopyCreator(newModelName,
				new File(TestConstants.MAIN_STORAGE_PATH), bot.getName());
		PCMArchitectureInstance archInstanceInRightLocation = copyCreator.createWorkingCopy(archInstance);
		File modelFileRightLocation = new File(
				archInstanceInRightLocation.getUsageModel().eResource().getURI().toFileString());

		ArchitecturalVersion newAlternative = new ArchitecturalVersion(
				modelFileRightLocation.getName().substring(0, modelFileRightLocation.getName().lastIndexOf('.')),
				modelFileRightLocation.getParentFile().getAbsolutePath(), AbstractPCMBot.QA_PERFORMANCE);
		newAlternative.setFullPathToAlternativeRepository(
				archInstanceInRightLocation.getRepositoryWithAlternatives().eResource().getURI().toFileString());
		return newAlternative;
	}

}
