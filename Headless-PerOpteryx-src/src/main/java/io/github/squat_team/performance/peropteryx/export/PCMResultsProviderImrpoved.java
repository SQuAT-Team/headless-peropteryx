package io.github.squat_team.performance.peropteryx.export;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opt4j.core.Individual;
import org.opt4j.core.Objective;

import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import de.uka.ipd.sdq.dsexplore.opt4j.representation.DSEIndividual;
import de.uka.ipd.sdq.dsexplore.opt4j.representation.DSEObjectives;
import de.uka.ipd.sdq.dsexplore.opt4j.start.Opt4JStarter;

/**
 * Provides the results of the Headless PerOpteryx run. It must be specified, if
 * only the Pareto Front, all candidates or candidates better than a boundary
 * value should be exported. The remaining PCMs will be deleted. This method
 * works only for single objectives!
 */
public class PCMResultsProviderImrpoved {
	private static Map<Long, PCMResultsProviderImrpoved> threadInstances = new HashMap<Long, PCMResultsProviderImrpoved>();
	private Map<String, Double> values = new HashMap<String, Double>();
	private Map<String, String> pathsToPCMFiles;
	private Double boundaryValue = 0.0;
	private Integer amount = 1;
	private ExportModeImrpoved mode = ExportModeImrpoved.PARETO;
	private OptimizationDirectionImrpoved direction = OptimizationDirectionImrpoved.MINIMIZE;
	private Set<String> keep = new HashSet<String>();

	private PCMResultsProviderImrpoved() {
	}

	public static PCMResultsProviderImrpoved getInstance() {
		PCMResultsProviderImrpoved provider;
		if (threadInstances.containsKey(Thread.currentThread().getId())) {
			provider = threadInstances.get(Thread.currentThread().getId());
		} else {
			provider = new PCMResultsProviderImrpoved();
			threadInstances.put(Thread.currentThread().getId(), provider);
		}
		return provider;
	}

	public void extractValues() {
		switch (mode) {
		case ALL:
			extractAll();
			break;
		case BETTER:
			extractAll();
			break;
		case AMOUNT:
			extractAll();
			break;
		case PARETO:
			extractParetoFront();
			break;
		default:
			extractParetoFront();
			break;
		}
	}

	private void extractParetoFront() {
		for (Individual individual : Opt4JStarter.getArchiveIndividuals()) {
			extractValue(individual);
		}
	}

	private void extractAll() {
		for (Individual individual : Opt4JStarter.getAllIndividuals().getIndividuals()) {
			extractValue(individual);
		}
	}

	private void extractValue(Individual individual) {
		DSEIndividual dseindividual = (DSEIndividual) individual;
		DSEObjectives objectives = dseindividual.getObjectives();
		for (Objective objective : objectives.getKeys()) {
			if (objective.getName().contains("de.uka.ipd.sdq.dsexplore.performance")) {
				Double value = objectives.get(objective).getDouble();
				addIfValid(dseindividual, value);
			}
		}
	}

	private void addIfValid(DSEIndividual individual, Double value) {
		if (!(value.isInfinite() || value.isNaN())) {
			values.put(individual.getGenotypeString(), value);
		}
	}

	private List<PerOpteryxPCMResultImrpoved> provideAllAvailable() {
		PCMFileExporterImrpoved fileExporter = getFileExporter();
		extractPaths(fileExporter);

		List<PerOpteryxPCMResultImrpoved> results = buildPCMResults();

		close(fileExporter);
		return results;
	}

	private List<PerOpteryxPCMResultImrpoved> provideBetter() {
		PCMFileExporterImrpoved fileExporter = getFileExporter();
		extractPaths(fileExporter);

		List<PerOpteryxPCMResultImrpoved> results = buildBetterPCMResults();

		close(fileExporter);
		return results;
	}

	private List<PerOpteryxPCMResultImrpoved> provideAmount() {
		PCMFileExporterImrpoved fileExporter = getFileExporter();
		extractPaths(fileExporter);

		List<PerOpteryxPCMResultImrpoved> results;
		if (!isAmountTooBig()) {
			results = buildAmountPCMResults();
		} else {
			results = buildPCMResults();
		}

		close(fileExporter);
		return results;
	}

	public List<PerOpteryxPCMResultImrpoved> provide() {
		switch (mode) {
		case ALL:
			return provideAllAvailable();
		case BETTER:
			return provideBetter();
		case AMOUNT:
			return provideAmount();
		case PARETO:
			return provideAllAvailable();
		default:
			return provideAllAvailable();
		}
	}

	private PCMFileExporterImrpoved getFileExporter() {
		return PCMFileExporterImrpoved.getInstance();
	}

	public void close(PCMFileExporterImrpoved fileExporter) {
		deletePCMsFromDisc();
		values.clear();
		pathsToPCMFiles.clear();
		keep.clear();
		fileExporter.close();
		threadInstances.remove(Thread.currentThread().getId());
	}

	private void extractPaths(PCMFileExporterImrpoved fileExporter) {
		pathsToPCMFiles = fileExporter.getPCMStoragePaths();
	}

	private List<PerOpteryxPCMResultImrpoved> buildPCMResults() {
		List<PerOpteryxPCMResultImrpoved> results = new ArrayList<PerOpteryxPCMResultImrpoved>();

		for (String id : values.keySet()) {
			results.add(buildPCMResult(id));
		}

		return results;
	}

	private List<PerOpteryxPCMResultImrpoved> buildBetterPCMResults() {
		List<PerOpteryxPCMResultImrpoved> results = new ArrayList<PerOpteryxPCMResultImrpoved>();

		for (String id : values.keySet()) {
			if ((direction == OptimizationDirectionImrpoved.MAXIMIZE) && (values.get(id) > boundaryValue)) {
				results.add(buildPCMResult(id));
				keep.add(id);
			} else if ((direction == OptimizationDirectionImrpoved.MINIMIZE) && (values.get(id) < boundaryValue)) {
				results.add(buildPCMResult(id));
				keep.add(id);
			}
		}

		return results;
	}

	private List<PerOpteryxPCMResultImrpoved> buildAmountPCMResults() {
		List<PerOpteryxPCMResultImrpoved> results = new ArrayList<PerOpteryxPCMResultImrpoved>();

		Multimap<Double, String> multiReverseValues;
		if (direction == OptimizationDirectionImrpoved.MAXIMIZE) {
			multiReverseValues = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural().reverse());
		} else {
			multiReverseValues = TreeMultimap.create();
		}

		for (String id : values.keySet()) {
			multiReverseValues.put(values.get(id), id);
		}

		Iterator<String> iterator = multiReverseValues.values().iterator();
		for (int i = 0; i < amount; i++) {
			String id = iterator.next();
			results.add(buildPCMResult(id));
			keep.add(id);
		}

		return results;
	}

	private PerOpteryxPCMResultImrpoved buildPCMResult(String id) {
		return new PerOpteryxPCMResultImrpoved(values.get(id), pathsToPCMFiles.get(id));
	}

	private void deletePCMsFromDisc() {
		if (mode == ExportModeImrpoved.BETTER || (mode == ExportModeImrpoved.AMOUNT && !isAmountTooBig())) {
			for (String genotypeID : pathsToPCMFiles.keySet()) {
				if (!keep.contains(genotypeID)) {
					deletePCMfromDisc(pathsToPCMFiles.get(genotypeID));
				}
			}
		} else {
			for (String genotypeID : pathsToPCMFiles.keySet()) {
				if (!values.keySet().contains(genotypeID)) {
					deletePCMfromDisc(pathsToPCMFiles.get(genotypeID));
				}
			}
		}
	}

	private void deletePCMfromDisc(String filePath) {
		File candidateDirectory = new File(filePath);
		deleteDir(candidateDirectory);
	}

	private void deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) {
				if (f.isDirectory()) {
					// Stop if there are other folders!
					return;
				}
				deleteDir(f);
			}
		}
		file.delete();
	}

	public ExportModeImrpoved getExportMode() {
		return mode;
	}

	public void setExportMode(ExportModeImrpoved mode) {
		this.mode = mode;
	}

	public OptimizationDirectionImrpoved getDirection() {
		return direction;
	}

	public void setDirection(OptimizationDirectionImrpoved direction) {
		this.direction = direction;
	}

	public Double getBoundaryValue() {
		return boundaryValue;
	}

	public void setBoundaryValue(Double boundaryValue) {
		this.boundaryValue = boundaryValue;
	}

	public Integer getAmount() {
		return amount;
	}

	public void setAmount(Integer amount) {
		this.amount = amount;
	}

	private boolean isAmountTooBig() {
		return this.amount >= this.values.size();
	}

}
