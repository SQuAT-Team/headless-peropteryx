package io.github.squat_team.util;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
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

public class SQuATHelper {
	
	//Just in case the registry is empty
	static {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		EPackage.Registry.INSTANCE.put(RepositoryPackage.eNS_URI, RepositoryPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(SystemPackage.eNS_URI, SystemPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(AllocationPackage.eNS_URI, AllocationPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(ResourceenvironmentPackage.eNS_URI, ResourceenvironmentPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(UsagemodelPackage.eNS_URI, UsagemodelPackage.eINSTANCE);
	}
	
	private static EObject load(String inputString) {
		URI resourceURI = URI.createURI(inputString);
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource = resourceSet.getResource(resourceURI, true);
		EObject content = resource.getContents().get(0);
		return content;
	}
	
	public static Repository loadRepositoryModel(String repositoryFile) {
		Repository repository = (Repository) SQuATHelper.load(repositoryFile);
		return repository;
	}
	
	public static org.palladiosimulator.pcm.system.System loadSystemModel(String systemFile) {
		org.palladiosimulator.pcm.system.System baseSystem = (org.palladiosimulator.pcm.system.System) SQuATHelper.load(systemFile);
		return baseSystem;
	}
	
	public static Allocation loadAllocationModel(String allocationFile) {
		Allocation allocation = (Allocation) SQuATHelper.load(allocationFile);
		return allocation;
	}

	public static ResourceEnvironment loadResourceEnvironmentModel(String resourceEnvironmentFile) {
		ResourceEnvironment resourceEnvironment = (ResourceEnvironment) SQuATHelper.load(resourceEnvironmentFile);
		return resourceEnvironment;
	}

	public static UsageModel loadUsageModel(String usageModelFile) {
		UsageModel usageModel = (UsageModel) SQuATHelper.load(usageModelFile);
		return usageModel;
	}
}
