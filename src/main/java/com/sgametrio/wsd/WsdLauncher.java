package com.sgametrio.wsd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import evaluation.InputExtractor;
import evaluation.InputInstance;
import evaluation.InputSentence;
import evaluation.Scorer;

public class WsdLauncher {
	
	//settings, used to make code clearer, use params in WsdExecutor for development
	public static void main(String a[]){
		//sentence to be disambiguated
		String[] sentences = {
//				"Eat what you can, and, I'll have the rest.",
//				"Dan speaks three languages, is good a DIY, and he can cook.",
//				"He made such a terrible face that the children ran away.",
////				ambiguous sentences
				"I saw him sawing wood with a saw", //saw
//				"I took out my contact lenses and put on my glasses.", //glass
//				"The water, spilled over the tops of these, \"river\" banks during the last flood." //river
		};
		//launchDisambiguationEvaluationWsd(false, Globals.saveGml, false, Globals.runSolver, false);
		launchDisambiguationEvaluation();
		//launchEvaluator(Globals.currentGoldFile, "RESULTS/centrality_wsdResults.KEY");
		//launchEvaluator(Globals.pathToSenseval3 + Globals.goldFileSuffix, "RESULTS/senseval3_centrality.KEY");
		//launchEvaluator(Globals.currentGoldFile, "RESULTS/ALL_subTrees_wsdResults.KEY");
	}

	
	/**
	 * launches disambiguation to calculate performances
	 * @param saveExamples
	 * @param saveGml
	 * @param verbose
	 * @param treeKernelType
	 */
	private static void launchDisambiguationEvaluation(){
		MyExecutor myExecutor = new MyExecutor();
		
		System.out.println("--- Evaluation started ---");
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
		Instant before = Instant.now();
		// TODO: move this code to InputExtractor
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new File(Globals.currentDataFile));
			//optional, but recommended
			doc.getDocumentElement().normalize();
			//get all sentences in xml file
			NodeList allSentences = doc.getElementsByTagName("sentence");
					
			// Create thread pool
			ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());			
			//iterate over all sentences and send them to inputExtractor to be processed
			for (int sentIndex = 0; sentIndex < allSentences.getLength(); sentIndex++) {
				Node sentence = allSentences.item(sentIndex);
				InputSentence iSentence = InputExtractor.myExtractInput(sentence);
				SentenceRunner runner = new SentenceRunner(myExecutor, iSentence);
			    executor.execute(runner);
			}
			
			// Start evaluation only when all thread are finished
			executor.shutdown();
			// wait until all are finished
			while (!executor.isTerminated());
			System.out.println("Finished results");

			//launch Navigli's evaluation framework script
			if (!Globals.runSolver) {
				launchEvaluator(Globals.currentGoldFile, Globals.resultsPath+Globals.fileNameCentrality+Globals.resultsFileName);
			} else {
				launchEvaluator(Globals.currentGoldFile, Globals.resultsPath+Globals.fileName+Globals.resultsFileName);
			}	
		} catch (Exception e) {
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
		// Remember to close dictionary
		myExecutor.closeDictionary();
		Instant after = Instant.now();
		System.out.println("Time executed: " + Duration.between(before, after));
	}
	
	/**
	 * Execute Navigli's evaluation script
	 * @param goldStandardPathToFile
	 * @param resultsPathToFile
	 */
	public static void launchEvaluator(String goldStandardPathToFile, String resultsPathToFile){
		String[] goldAndRes = {goldStandardPathToFile, resultsPathToFile};
		try {
			Scorer.main(goldAndRes);
		} catch (IOException e) {
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
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
		File log = new File("log.txt");
		
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
			// delete old results file
			File old;
			if (!Globals.runSolver) {
				old = new File(Globals.resultsPath+Globals.fileNameCentrality+Globals.resultsFileName);
			} else {
				old = new File(Globals.resultsPath+Globals.fileName+Globals.resultsFileName);
			}
			old.delete();
		}
		log.delete();
	}
	
	/**
	 * launches disambiguation to calculate performances
	 * @param saveExamples
	 * @param saveGml
	 * @param verbose
	 * @param treeKernelType
	 */
	private static void launchDisambiguationEvaluationWsd(boolean saveExamples, boolean saveGml, boolean verbose, boolean runSolver, boolean centrality){
		
		WsdExecutor wsdExecutor = createWsdExecutor(saveExamples, saveGml, verbose, true, runSolver);
		//deletes all files generated by previous executions
		clearOldFiles();
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new File(Globals.currentDataFile));
			//optional, but recommended
			doc.getDocumentElement().normalize();
			//get all sentences in xml file
			NodeList allSentences = doc.getElementsByTagName("sentence");
			// Delete old result file
			if (centrality) {
				FileWriter deleteOldResultsFile = new FileWriter(Globals.resultsPath + Globals.fileNameCentrality + Globals.resultsFileName);
				deleteOldResultsFile.close();
			} else {
				FileWriter deleteOldResultsFile = new FileWriter(Globals.resultsPath + Globals.fileName + Globals.resultsFileName);
				deleteOldResultsFile.close();
			}
			//iterate over all sentences and send them to inputExtractor to be processed
			for (int sentIndex = 0; sentIndex < allSentences.getLength(); sentIndex++) {
				Node sentence = allSentences.item(sentIndex);
				//get the sentence in a format valid to be given to performDisambiguation method
				HashMap<String, ArrayList<String[]>> sentenceMap = InputExtractor.extractInput(sentence);
				wsdExecutor.performDisambiguation(sentenceMap, centrality, false);
			}
			
			//launch Navigli's evaluation framework script
			if (centrality) {
				launchEvaluator(Globals.currentGoldFile, Globals.resultsPath+Globals.fileNameCentrality+Globals.resultsFileName);
			} else {
				launchEvaluator(Globals.currentGoldFile, Globals.resultsPath+Globals.fileName+Globals.resultsFileName);
			}			
		} catch (Exception e) {
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
	}
	

	/**
	 * initialize WsdExecutor with proper parameters
	 * @param saveExamples
	 * @param saveGml
	 * @param verbose
	 * @return
	 */
	private static WsdExecutor createWsdExecutor(boolean saveExamples, boolean saveGml, boolean verbose, boolean evaluation, boolean runSolver){
		WsdExecutor wsdExecutor = new WsdExecutor();
		if(runSolver) {
			wsdExecutor.enableSolver();
		}
		if(saveExamples){
			wsdExecutor.enableSaveExamples();
		}
		if(saveGml){
			wsdExecutor.enableSaveGml();
		}
		if(verbose){
			wsdExecutor.enableVerboseMode();
		}
		if(evaluation){
			wsdExecutor.enableEvaluationMode();
		}
		return wsdExecutor;
				
	}

}
