package com.sgametrio.wsd;

import java.io.BufferedWriter;
import java.lang.Math;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.mit.jwi.item.IWord;

public class MyGraph {
	private static int progressiveId = 0;
	private int id;
	private String sentence = "";

	private ArrayList<MyVertex> vertexes;
	private String sentenceId = "";
	
	private String log = "";

	public MyGraph() {
		id = progressiveId++;
		vertexes = new ArrayList<MyVertex>();
	}
	
	public ArrayList<MyVertex> getNodes() {
		return vertexes;
	}
	
	/**
	 * Get position of vertex in array. Used only to create edge matrix
	 * @param id
	 * @return index of node in ArrayList 
	 */
	public int getNodeIndexById(int id) {
		for (int i = 0; i < vertexes.size(); i++) {
			if (vertexes.get(i).getId() == id)
				return i;
		}
		return -1;
	}
	
	public MyVertex getNodeById(int id) {
		for (MyVertex v : vertexes) {
			if (v.getId() == id)
				return v;
		}
		return null;
	}
	
	public void addNode(MyVertex v) {
		vertexes.add(v);
	}
	
	public void addEdge(MyVertex source, MyVertex target, float weight) {
		int indexS = vertexes.indexOf(source);
		int indexT = vertexes.indexOf(target);
		// If edge weight is ZERO or less, doesn't create the edge
		if (indexS != -1 && indexT != -1 && weight > 0.0) {
			source.addEdge(target, weight);
			// Undirected graph
			target.addEdge(source, weight);
		}
	}

	public Map<MyVertex, Float> BellmanFord(MyVertex src) {
		Map<MyVertex, Float> shortestPaths = new HashMap<MyVertex, Float>();
		ArrayList<MyVertex> nodes = this.getNodes();
		// Step 1: Initialize distances from src to all other
        // vertices as INFINITE
		for (MyVertex v : nodes) {
			shortestPaths.put(v, (float)Integer.MAX_VALUE);
		}
		shortestPaths.replace(src, (float)0);
 
        // Step 2: Relax all edges |V| - 1 times. A simple
        // shortest path from src to any other vertex can
        // have at-most |V| - 1 edges
		for (int i = 1; i < nodes.size(); i++) {
			for (MyVertex v : nodes) {
				for (MyEdge e : v.getEdges()) {
					// Avoid finding same cluster nodes
					if (e.getDest().getSentenceIndex() != -1 && e.getDest().getSentenceIndex() == src.getSentenceIndex())
						continue;
					float weight = e.getWeight();
					float distSrc = shortestPaths.get(v);
					float distDest = shortestPaths.get(e.getDest());
					
					if (distSrc != (float)Integer.MAX_VALUE && distSrc+weight < distDest) {
						shortestPaths.replace(e.getDest(), distSrc+weight);
					}
				}
			}
		}
        return shortestPaths;
    }
	
	/**
	 * compute vertexes distance
	 * @param source
	 * @param target
	 * @return edge weight, -1 if it doesn't exist
	 */
	public float distance(MyVertex source, MyVertex target) {
		MyEdge edge = source.getEdge(target);
		if (edge != null)
			return edge.getWeight();
		return -1;
	}
	
	/**
	 * Save to graph to a representable format (GML)
	 * @param path
	 */
	public void saveToGML(String path) {
		if (Globals.developmentLogs) {
			// Assuming path to file exists
			String gml = "";
			gml += "graph [\n"
					+ "\tcomment \"" + getSentence().replaceAll("\"", "").replaceAll("\n", "") + "\"\n"
					+ "\tlabel \"" + getSentenceId() + "\"\n";
			// Add nodes, and then edges
			for (MyVertex v : this.getNodes()) {
				gml += v.toGML();
			}
			for (MyVertex v : this.getNodes()) {
				for (MyEdge e : v.getEdges()) {
					if (e.getWeight() == -1)
						continue;
					gml += "\tedge [\n"
							+ "\t\tid " + e.getId() + "\n"
							+ "\t\tsource " + v.getId() + "\n"
							+ "\t\ttarget " + e.getDest().getId() + "\n"
							+ "\t\tweight " + e.getWeight() + "\n"
							+ "\t]\n";
				}
			}
			gml += "]\n";
			try {
				BufferedWriter file = new BufferedWriter(new FileWriter(path));
				file.write(gml);
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Save to GTSP format file, to be read by runGLKH
	 * @param path
	 */
	public boolean saveToGTSP(String path, String filename) {
		// Assuming path to file exists
		int size = this.getDisambiguationNodes().size();
		if (size == 0) {
			System.out.println("Graph has no vertexes, cannot save to GTSP format");
			return false;
		}
		
		float[][] matrix = this.getGraphMatrix();
		int[][] invertedMatrix = this.invertMatrixAndConvertToInt(matrix);
		ArrayList<ArrayList<Integer>> clusters = this.getNodesIndexByClusters();
		if (clusters.size() == 1) {
			System.out.println("Graph has only 1 cluster, don't save to GTSP and don't run solver");
			return false;
		}
		String content = "";
		content += "NAME : " + filename + ".gtsp\n" ;
		content += "TYPE : GTSP\n";
		content += "COMMENT : " + getSentenceId() + " " + getSentence().replaceAll("\n", " ") + "\n";
		content += "DIMENSION : "+ size +"\n";
		content += "GTSP_SETS : "+ clusters.size() +"\n";
		content += "EDGE_WEIGHT_TYPE : EXPLICIT \n";
		content += "EDGE_WEIGHT_FORMAT : FULL_MATRIX \n";
		content += "EDGE_WEIGHT_SECTION : \n";
		// Prints edge weights
		for(int row = 0; row < size; row++){
			for(int col = 0; col < size; col++){
				content += invertedMatrix[row][col]+" ";
			}
			content += "\n";
		}
		content += "GTSP_SET_SECTION : \n";
		for (int i = 0; i < clusters.size(); i++) {
			content += (i+1) + " ";
			for (int j = 0; j < clusters.get(i).size(); j++) {
				// Add 1 because ids in gtsp starts from 1 and in my graph from 0
				content += (clusters.get(i).get(j)+1) + " ";
			}
			content += "-1\n";
		}
		content += "EOF";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(path + filename+".gtsp"));
			writer.write(content);
			writer.close();
			if (Globals.verbose)
				System.out.println("Saved graph to " + path + filename + ".gtsp");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Get nodes 
	 * @return
	 */
	public ArrayList<ArrayList<Integer>> getNodesIndexByClusters() {
		ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();
		for (ArrayList<MyVertex> nodes : this.getNodesByCluster().values()) {
			ArrayList<Integer> cluster = new ArrayList<Integer>();
			for (MyVertex node : nodes) {
				cluster.add(getNodeIndexById(node.getId()));
			}
			clusters.add(cluster);
		}
		return clusters;
	}

	/**
	 * Build a map that maps sentence index to an array of vertex of that sentence index
	 * without support nodes
	 * @return map
	 */
	private Map<Integer, ArrayList<MyVertex>> getNodesByCluster() {
		// Pay attention that I choose only disambiguation nodes
		ArrayList<MyVertex> vertexes = this.getDisambiguationNodes();
		Map<Integer, ArrayList<MyVertex>> map = new HashMap<Integer, ArrayList<MyVertex>>();
		for (MyVertex v : vertexes) {
			int index = v.getSentenceIndex();
			if (!map.containsKey(index)) {
				map.put(index, new ArrayList<MyVertex>());
			}
			map.get(index).add(v);
		}
		return map;
	}

	/**
	 * Return matrix of unsigned integer, inverted and converted from float without losing too much precision
	 * @param matrix
	 * @return
	 */
	private int[][] invertMatrixAndConvertToInt(float[][] matrix) {
		int size = matrix.length;
		int[][] invertedMatrix = new int [size][size];
		
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				if (matrix[i][j] == -1)
					invertedMatrix[i][j] = -1;
				else {
					// Multiply by 100000 to lose only a .00001 of precision and not 1 using Math.round
					float value = matrix[i][j]*Globals.precision;
					invertedMatrix[i][j] = (int)Math.round(value);
				}
			}
		}
		// Now invert value by using max value in the matrix
		int max = this.getMaxValue(invertedMatrix);
		int min = this.getMinValue(invertedMatrix);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				if (invertedMatrix[i][j] == -1) {
					invertedMatrix[i][j] = max + 2;
				} else {
					invertedMatrix[i][j] = max - invertedMatrix[i][j] + min;
				}
			}
		}
		return invertedMatrix;
	}

	private int getMinValue(int[][] matrix) {
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				if (min > matrix[i][j] && matrix[i][j] != -1)
					min = matrix[i][j];
			}
		}
		return min;
	}

	/**
	 * Get max value in the matrix
	 * @param matrix
	 * @return max value
	 */
	private int getMaxValue(int[][] matrix) {
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				if (max < matrix[i][j])
					max = matrix[i][j];
			}
		}
		return max;
	}

	/**
	 * Get a matrix representing the graph
	 * @return a matrix which has in position ij the weight of the edge between node i and node j if exists,
	 * -1 otherwise, return null if graph has no nodes
	 */
	public float[][] getGraphMatrix() {
		ArrayList<MyVertex> nodes = this.getDisambiguationNodes();
		int size = nodes.size();
		if (size == 0)
			return null;
		float[][] matrix = new float[size][size];
		// Initialize matrix by setting all edges to non-existents
		for (int i = 0; i < size; i++) 
			for (int j = 0; j < size; j++)
				matrix[i][j] = -1;
		for (MyVertex v : nodes) {
			for (MyEdge e : v.getEdges()) {
				// Avoid support nodes
				if (e.getDest().getSentenceIndex() != -1) {
					matrix[getNodeIndexById(v.getId())][getNodeIndexById(e.getDest().getId())] = e.getWeight();
				}
			}
		}
		return matrix;
	}

	/**
	 * Helper method to find if graph contains a vertex representing an IWord
	 * @param word
	 * @return true if graph has a node representing the word, false otherwise
	 */
	public boolean containsWord(IWord word) {
		for (MyVertex v : vertexes) {
			if (v.getWord().equals(word)) {
				return true;
			}
		}
		return false;
	}
	
	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
	
	public float computeMeanCentrality(MyVertex v1, MyVertex v2) {
		return (v1.getCentrality()+v2.getCentrality())/2;
	}

	/**
	 * Helper method to return only certain nodes
	 * @return only disambiguation vertexes
	 */
	public ArrayList<MyVertex> getDisambiguationNodes() {
		ArrayList<MyVertex> disambiguationVertexes = new ArrayList<MyVertex>();
		for (MyVertex v : vertexes) {
			if (v.getSentenceIndex() != -1)
				disambiguationVertexes.add(v);
		}
		return disambiguationVertexes;
	}
	
	/**
	 * Helper method to return only certain nodes
	 * @return only support nodes
	 */
	public ArrayList<MyVertex> getSupportNodes() {
		ArrayList<MyVertex> supportVertexes = new ArrayList<MyVertex>();
		for (MyVertex v : vertexes) {
			if (v.getSentenceIndex() == -1)
				supportVertexes.add(v);
		}
		return supportVertexes;
	}

	public MyVertex getNodeByIndex(int i) {
		return vertexes.get(i);
	}

	public int getId() {
		return this.id;
	}

	/**
	 * Print to file useful information like: 
	 * * all nodes in a cluster have centrality 0
	 * @param filePath
	 * @return 
	 */
	public void printUsefulInformation(String filePath) {
		Map<Integer, ArrayList<MyVertex>> nodes = this.getNodesByCluster();
		for (Integer key : nodes.keySet()) {
			ArrayList<MyVertex> clusterNodes = nodes.get(key);
			boolean zeroCentrality = true;
			for (MyVertex v : clusterNodes) {
				float centrality = v.getCentrality();
				if (centrality != 0.0) {
					zeroCentrality = false;
				}
			}
			// Check clusters with no centralities
			if (zeroCentrality) {
				this.log(Globals.logInfo, "[ZERO CENTRALITY CLUSTER] cluster " + key);
			} 
		}
	}
	
	/**
	 * Compute KPP centrality (closeness centrality) on every nodes present in the graph.
	 * KPP is computed as sum of the distance divided by nodes cardinality.
	 * Edge weight here is node similarity (higher is more similar, so, the node, is more central).
	 * So I do not reverse edge weight like the original formula says (because the original formula counts on node distance)
	 * @param graph
	 */
	private void computeKppVertexCentrality() {
		// Weight vertexes by kpp centrality
		ArrayList<MyVertex> vertexes = this.getNodes();
		int size = vertexes.size();
		for (int i = 0; i < size; i++) {
			float kppCentrality = 0;
			float distance = 0;
			for (int j = 0; j < size; j++) {
				if (i != j) {
					float distPath = this.distance(vertexes.get(i), vertexes.get(j));
					// Check if path exists (or edge)
					if (distPath > 0)
						distance += distPath;
				}
			}
			if (size > 1)
				kppCentrality = distance / (size - 1);
			vertexes.get(i).setCentrality(kppCentrality);			
		}
	}
	
	/**
	 * Invert edge weight from values representing closeness to those representing distance
	 * How can I find the opposite value?
	 * newWeight = max + min - oldWeight
	 */
	public void invertEdgeWeight() {
		float min = Integer.MAX_VALUE;
		float max = Integer.MIN_VALUE;
		ArrayList<MyVertex> nodes = getNodes();
		if (nodes.size() == 0) {
			return;
		}
		// Find max and min
		for (MyVertex v : getNodes()) {
			for (MyEdge e : v.getEdges()) {
				float weight = e.getWeight();
				if (weight == -1)
					continue;
				if (min > weight) {
					min = weight;
				}
				if (max < weight) {
					max = weight;
				}
			}
		}
		// This means no edges
		if (max == Integer.MIN_VALUE ||  min == Integer.MAX_VALUE) {
			return;
		}
		// Replace value with new one
		for (MyVertex v : getNodes()) {
			for (MyEdge e : v.getEdges()) {
				float oldWeight = e.getWeight();
				float newWeight = max + min - oldWeight;
				e.setWeight(newWeight);
			}
		}	
	}
	
	public void convertEdgeWeightToInt() {
		for (MyVertex v : getNodes()) {
			for (MyEdge e : v.getEdges()) {
				float oldWeight = e.getWeight();
				long newWeight = Math.round(oldWeight * 100);
				e.setWeight(newWeight);
			}
		}
	}

	public void setSentenceId(String sentenceId) {
		this.sentenceId  = sentenceId;		
	}
	
	public String getSentenceId() {
		return this.sentenceId;
	}
	
	/**
	 * Adds log to local variable and choose different actions based on severity
	 * @param severity
	 * @param log
	 */
	public void log(int severity, String log) {
		if (Globals.developmentLogs) {
			this.log += log + "\n";
			if (severity >= Globals.logWarning) {
				System.out.println("[GRAPH " + this.getSentenceId() + "]" + log);
			} 
			if (severity >= Globals.logSevere) {
				System.out.println("Flush logs and exit...");
				this.logOnFile();
				System.exit(1);
			}
		}
	}

	/**
	 * Logs log content to file
	 */
	public void logOnFile() {
		if (this.log.length() > 0) {
			try {
				FileWriter logFile = new FileWriter(Globals.logsPath + this.getSentenceId() + ".log");
				logFile.write(this.log);
				logFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
