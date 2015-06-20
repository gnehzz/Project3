package Ranking;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.util.CharArraySet;

public class EvaluateQueries {

	private static Map<Integer, String> loadQueries(String filename) {
		HashMap<Integer, String> queryIdMap = new HashMap<Integer, String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(new File(filename)));
		} catch (FileNotFoundException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}

		String line;
		try {
			while ((line = in.readLine()) != null) {
				int pos = line.indexOf(',');
				queryIdMap.put(Integer.parseInt(line.substring(0, pos)), line.substring(pos + 1));
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryIdMap;
	}

	private static Map<Integer, HashSet<String>> loadAnswers(String filename) {
		HashMap<Integer, HashSet<String>> queryAnswerMap = new HashMap<Integer, HashSet<String>>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(new File(filename)));

			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split(" ");
				HashSet<String> answers = new HashSet<String>();
				for (int i = 1; i < parts.length; i++) {
					answers.add(parts[i]);
				}
				queryAnswerMap.put(Integer.parseInt(parts[0]), answers);
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryAnswerMap;
	}

	private static double precision(HashSet<String> answers, List<String> results) {
		double matches = 0;
		for (String result : results) {
			if (answers.contains(result))
				matches++;
		}

		return matches / results.size();
	}

	/**
	 * 
	 * @param indexDir
	 * @param docsDir
	 * @param queryFile
	 * @param answerFile
	 * @param numResults
	 * @param stopwords
	 * @return
	 */
	public static double evaluate(String indexDir, String docsDir,
			String queryFile, String answerFile, int numResults, CharArraySet stopwords) {
		
		// load queries and answer
		Map<Integer, String> queries = loadQueries(queryFile);
		Map<Integer, HashSet<String>> queryAnswers = loadAnswers(answerFile);
		
		// create direct document index into term frequency values for each term in each document
		LinkedHashMap<String, HashMap<String, Integer>> collectionIndex = null;
		System.out.println("Loading index at: " + indexDir);
		try {
			collectionIndex = getIndex(indexDir);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Index Loaded");
		
		// Search and evaluate
		int numQueries = 0;
		double sum = 0;
		double apSum = 0.0;
		for (Integer i : queries.keySet()) {
			if (true) {
				List<String> results = SearchFiles.searchQuery(collectionIndex, queries.get(i), numResults, stopwords);
				sum += precision(queryAnswers.get(i), results);
				numQueries++;
				System.out.printf("\nTopic %d  ", i);
				System.out.print (results);
				System.out.println();
				apSum += averagePrecision(queryAnswers.get(i), results);
			}
		}
		System.out.println("Mean Average Precision: " + (apSum / numQueries));

		return sum / queries.size();
	}

	/**
	 * Computes the Average Precision of results on the set answers
	 * 
	 * @param answers
	 * @param results
	 * @return
	 */
	private static double averagePrecision(HashSet<String> answers, List<String> results){
		double apTotal = 0.0;
		double p = 0.0;

		for(int i = 0; i < results.size(); i++){
			if(answers.contains(results.get(i))){
				p = precision(answers, results.subList(0, i+1));
				apTotal += p;
			}
		}

		System.out.println("Average Precision: " + (apTotal / answers.size()));
		return apTotal / answers.size();
	}
	
	public static LinkedHashMap<String, HashMap<String, Integer>> getIndex(String indexDir) throws FileNotFoundException{
		LinkedHashMap<String, HashMap<String, Integer>> collectionIndex = new LinkedHashMap<String, HashMap<String, Integer>>();

		File folder = new File(indexDir);
		String[] files = folder.list();
		if (files == null || files.length == 0) {
			throw new FileNotFoundException("No Index Found");
		}
		else{
			for (int i = 0; i < files.length; i++) {
				// Iterate over files within directory
				// add each index file for collection to the collection index (representing all docs)
				collectionIndex.put(files[i].substring(0, files[i].lastIndexOf(".index")), getDocIndex(indexDir + "/" + files[i]));
			}
		}
		
		return collectionIndex;
	}
	/**
	 * Reads the map located at filepath and returns it.
	 * @param filepath
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, Integer> getDocIndex(String filepath)
	{
		HashMap<String, Integer> mapObject = null;

		// Reads hashmap from filepath
		try {
			File docIndex = new File(filepath);
			FileInputStream fStream = new FileInputStream(docIndex);
			ObjectInputStream oStream = new ObjectInputStream(fStream);

			mapObject = (HashMap<String, Integer>)oStream.readObject();
			oStream.close();
		} catch (FileNotFoundException e){
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		} catch (ClassNotFoundException e){
			e.printStackTrace();
		}

		return mapObject;
	}
}
