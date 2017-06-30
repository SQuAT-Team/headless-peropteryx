package io.github.squat_team.performance.peropteryx.registry;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.spi.IRegistryProvider;

public class RegistryProviderImproved implements IRegistryProvider{

	@Override
	public IExtensionRegistry getRegistry() {
		return new ExtensionRegistryImproved();
	}

}
