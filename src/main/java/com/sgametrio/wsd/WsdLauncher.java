package com.sgametrio.wsd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import evaluation.InputExtractor;
import evaluation.Scorer;

public class WsdLauncher {
	
	public static final String frameworkFilePath = "src/main/resources/evaluation-datasets/";
	
	public static final String pathToAllGold = frameworkFilePath+"ALL/ALL.gold.key.txt";
	public static final String pathToSemeval2007Gold = frameworkFilePath+"semeval2007/semeval2007.gold.key.txt";
	public static final String pathToSemeval2013Gold = frameworkFilePath+"semeval2013/semeval2013.gold.key.txt";
	public static final String pathToSemeval2015Gold = frameworkFilePath+"semeval2015/semeval2015.gold.key.txt";
	public static final String pathToSenseval2Gold = frameworkFilePath+"senseval2/senseval2.gold.key.txt";
	public static final String pathToSenseval3Gold = frameworkFilePath+"senseval3/senseval3.gold.key.txt";

	private static String currentGoldFile = pathToSenseval3Gold;
	//settings, used to make code clearer, use params in WsdExecutor for development
	private static boolean saveExamples = false;
	private static boolean runSolver = false; //used to create only .gml file
	private static boolean saveGml = true;
	private static boolean verbose = true;
	
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
		launchDisambiguation(saveExamples, saveGml, verbose, runSolver, sentences);
//		launchDisambiguationEvaluation(saveExamples, saveGml, verbose, runSolver);
		
	}
	
	/**
	 * launches disambiguation when the input is a list of sentences
	 * @param saveExamples
	 * @param saveGml
	 * @param verbose
	 * @param sentences
	 */
	private static void launchDisambiguation(boolean saveExamples, boolean saveGml, boolean verbose, boolean runSolver, String[] sentences){
		
		WsdExecutor wsdExecutor = createWsdExecutor(saveExamples, saveGml, verbose, false, runSolver);
		clearOldFiles(wsdExecutor, false);
		for(String sentence : sentences){
			wsdExecutor.performDisambiguation(sentence);	
		}
		System.out.print("Finished");
	}

	
	/**
	 * launches disambiguation to calculate performances
	 * @param saveExamples
	 * @param saveGml
	 * @param verbose
	 * @param treeKernelType
	 */
	private static void launchDisambiguationEvaluation(boolean saveExamples, boolean saveGml, boolean verbose, boolean runSolver){
		
		WsdExecutor wsdExecutor = createWsdExecutor(saveExamples, saveGml, verbose, true, runSolver);
		//deletes all files generated by previous executions
		clearOldFiles(wsdExecutor, false);
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new File(InputExtractor.currentDataFile));
			//optional, but recommended
			doc.getDocumentElement().normalize();
			//get all sentences in xml file
			NodeList allSentences = doc.getElementsByTagName("sentence");
			//iterate over all sentences and send them to inputExtractor to be processed
			for (int sentIndex = 0; sentIndex < allSentences.getLength(); sentIndex++) {
				Node sentence = allSentences.item(sentIndex);
				//get the sentence in a format valid to be given to performDisambiguation method
				HashMap<String, ArrayList<String[]>> sentenceMap = InputExtractor.extractInput(sentence);
				wsdExecutor.performDisambiguation(sentenceMap);
			}
			
			//launch Navigli's evaluation framework script
			launchEvaluator(currentGoldFile, 
					wsdExecutor.getResultsPath()+wsdExecutor.getFileName()+wsdExecutor.getResultsFileName());
			
		} catch (Exception e) {
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
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
	 * clear all old files generated by tsp solver and wsd algorithm (.tour, .gtsp, log, .gml). If ask is set to true, a request
	 * of deletion is made for each file.
	 * @param wsd
	 * @param ask
	 */
	private static void clearOldFiles(WsdExecutor wsd, boolean ask){
		File gml = new File(wsd.getPathToGML());
		File gtsp = new File(wsd.getGtspPath());
		File tour = new File(wsd.getTourPath());
		File log = new File("log.txt");
		if(ask){
			if(getChoice(gml, "GML")){
				for(File gmlFile: gml.listFiles()){
					gmlFile.delete();
				}
				System.out.println("All GML files have been removed.");
			}
			if(getChoice(gtsp, "GTSP")){
				for(File gtspFile: gtsp.listFiles()){
					gtspFile.delete();
				}
				System.out.println("All GTSP files have been removed.");
			}
			if(getChoice(tour, "TOUR")){
				for(File tourFile: tour.listFiles()){
					tourFile.delete();
				}
				System.out.println("All TOUR files have been removed.");
			}
			if(getChoice(log, "LOG")){
				log.delete();
				System.out.println("All LOG files have been removed.");
			}
		}else{
			/*System.out.println("All old files will be removed. Confirm? (Y/N)");
			Scanner tastiera = new Scanner(System.in);
			String response = tastiera.nextLine();
			tastiera.close();
			if(response.contains("Y")||response.contains("y")){
				for(File gmlFile: gml.listFiles()){
					gmlFile.delete();
				}
				for(File gtspFile: gtsp.listFiles()){
					gtspFile.delete();
				}
				for(File tourFile: tour.listFiles()){
					tourFile.delete();
				}
				log.delete();
			}else if(response.contains("N")||response.contains("n")){
				System.out.println("All old files will be kept.");
			}else{
				System.out.println("Invalid choice. Files will be kept.");
			}*/
		}
		
	}
	
	/**
	 * manages messages to the user
	 * @param fileToDelete
	 * @param type
	 * @return
	 */
	private static boolean getChoice(File fileToDelete, String type){
		if(fileToDelete.exists()){
			System.out.println("Do you want to delete all "+type+" files? (Y/N)");
			Scanner tastiera = new Scanner(System.in);
			String choice = tastiera.nextLine();
			tastiera.close();
			if(choice.contains("Y")||choice.contains("y")){
				return true;
			}else if(choice.contains("N")||choice.contains("n")){
				if(type.equalsIgnoreCase("TOUR")){
					System.out.println("Leave .TOUR files may cause errors during computation."
							+ "Are you sure you don't want to delete them? (Y/N)" );
					String sure = tastiera.nextLine();
					if(sure.contains("Y")||sure.contains("y")){
						return true;
					}else if(sure.contains("N")||sure.contains("n")){
						return false;
					}else{
						System.err.println("Invalid choice.");
					}
				}
				
				return false;
			}else{
				System.err.println("Invalid choice. Type Y or N");
				getChoice(fileToDelete, type);
			}
		}else{
			System.err.println("File "+fileToDelete.toPath()+" not found");
		}
		return false;
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
