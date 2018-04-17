package com.sgametrio.wsd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import evaluation.ExtendedScorer;
import evaluation.InputExtractor;
import evaluation.InputSentence;
import evaluation.Scorer;

public class WsdLauncher {
	static CountDownLatch doneSignal = null;
	
	public static void main(String a[]){
		launchDisambiguationEvaluation();
	}

	
	/**
	 * launches disambiguation to calculate performances
	 * @param saveExamples
	 * @param saveGml
	 * @param verbose
	 * @param treeKernelType
	 */
	private static void launchDisambiguationEvaluation(){
		//MyExecutor myExecutor = new MyExecutor();
		JExecutor ex = new JExecutor();
		Instant before = Instant.now();
		System.out.println("--- Evaluation started ---");
		System.out.println("Initial time: " + before.toString());
		System.out.println("Dataset used: " + Globals.currentDataset);
		System.out.println("Configuration params: ");
		if (Globals.centrality) {
			System.out.println("Compute centrality by: " + Globals.computeCentrality);
			System.out.println("Support nodes depth: " + Globals.nodesDepth);
		}
		System.out.println("Save GML: " + Globals.saveGml);
		System.out.println("Run solver: " + Globals.runSolver);
		System.out.println("--------------------------");
		
		//deletes all files generated by previous executions
		clearOldFiles();
		// TODO: move this code to InputExtractor
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new File(Globals.currentDataFile));
			//optional, but recommended
			doc.getDocumentElement().normalize();
			//get all sentences in xml file
			NodeList allSentences = doc.getElementsByTagName("sentence");
			int sentences = allSentences.getLength();
			doneSignal = new CountDownLatch(sentences);
			// Create thread pool
			ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());			
			//iterate over all sentences and send them to inputExtractor to be processed
			for (int sentIndex = 0; sentIndex < allSentences.getLength(); sentIndex++) {
				Node sentence = allSentences.item(sentIndex);
				InputSentence iSentence = InputExtractor.myExtractInput(sentence);
				SentenceRunner runner = new SentenceRunner(ex, iSentence, doneSignal);
			    executor.execute(runner);
			}
			
			// Start evaluation only when all thread are finished
			executor.shutdown();
			// wait until all are finished
			doneSignal.await();
			while (!executor.isTerminated());
			System.out.println("Finished results");

			// Remember to close dictionary
			ex.closeDictionary();
			launchManyEvaluator(Globals.currentGoldFile);
		} catch (Exception e) {
			// Remember to close dictionary
			ex.closeDictionary();
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
		Instant after = Instant.now();
		System.out.println("Time executed: " + Duration.between(before, after));
	}
	
	private static void launchManyEvaluator(String currentgoldfile) {
		for (int depth = Globals.minDepth; depth <= Globals.maxDepth; depth++) {
			for (String currentCentrality : Globals.centralities) {
				String filename = Globals.currentDataset + "_" + currentCentrality + "_" + depth;
				 
				String evalCFilename = filename + "_by-centrality";
				String evalDFilename = filename + "_run-solver";

				launchEvaluator(currentgoldfile, evalCFilename);
				launchEvaluator(currentgoldfile, evalDFilename);
			}
		}
		printCsvReport();
	}


	private static void printCsvReport() {
		try {
			BufferedReader csv = new BufferedReader(new FileReader(Globals.csvReportFile));
			String line = "";
			while ((line = csv.readLine()) != null) {
				System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Execute Navigli's evaluation script
	 * @param goldStandardPathToFile
	 * @param resultsPathToFile
	 */
	public static void launchEvaluator(String goldStandardPathToFile, String resultsPathToFile) {
		ExtendedScorer extended_scorer = new ExtendedScorer();
		extended_scorer.doEvaluation(goldStandardPathToFile, resultsPathToFile);
	}
	
	/**
	 * clear all old files generated by tsp solver and wsd algorithm (.tour, .gtsp, log, .gml).
	 */
	private static void clearOldFiles(){
		File gml = new File(Globals.gmlPath);
		File gtsp = new File(Globals.gtspPath);
		File tour = new File(Globals.tourPath);
		File results = new File(Globals.resultsPath);
		File logs = new File(Globals.logsPath);
		File pi_files = new File(Globals.piFilesPath);
		
		if (!logs.isDirectory()) {
			logs.mkdirs();
		}
		
		if (gml.isDirectory()) {
			for(File gmlFile: gml.listFiles()){
				gmlFile.delete();
			}
		} else {
			gml.mkdirs();
		}
		if (gtsp.isDirectory()) {
			for(File gtspFile: gtsp.listFiles()){
				gtspFile.delete();
			}
		} else {
			gtsp.mkdirs();
		}
		if (pi_files.isDirectory()) {
			for(File pi: pi_files.listFiles()){
				pi.delete();
			}
		} 
		if (tour.isDirectory()) {
			for(File tourFile: tour.listFiles()){
				tourFile.delete();
			}
		} else {
			tour.mkdirs();
		}
		if (!results.exists()) {
			results.mkdirs();
		} else {
			for(File file: results.listFiles()){
				file.delete();
			}
		}
	}
}
