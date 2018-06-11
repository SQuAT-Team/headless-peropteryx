package test;

import java.io.File;
import java.io.IOException;

import io.github.squat_team.performance.peropteryx.configuration.DesigndecisionConfigImproved;
import io.github.squat_team.util.DesigndecisionFileModifier;

/**
 * Runs the {@link DesigndecisionFileModifier}.
 */
public class DesigndecisionFileModifierTest {

	public static void main(String[] args) throws IOException {
		DesigndecisionConfigImproved designdecisionConfig = new DesigndecisionConfigImproved();
		
		// Set the boundary values for the IDs of the servers
		designdecisionConfig.setLimits("_78qo4K2UEeaxN4gXuIkS2A", 1.0, 10.0);
		designdecisionConfig.setLimits("_-5Q84K2UEeaxN4gXuIkS2A", 1.1, 10.1);
		designdecisionConfig.setLimits("_BgmykK2VEeaxN4gXuIkS2A", 1.2, 10.2);
		designdecisionConfig.setLimits("_FM6FMK2VEeaxN4gXuIkS2A", 1.3, 10.3);
		
		File designdecisionFile = new File("/home/rss/SQuAT/tests/default.designdecision");
		
		DesigndecisionFileModifier modifier = new DesigndecisionFileModifier(designdecisionFile, designdecisionConfig);
		modifier.modify();
	}	
}
