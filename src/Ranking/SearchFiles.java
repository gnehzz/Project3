package Ranking;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

public class SearchFiles {

	private SearchFiles() {
	}

	/** This function is only for test search. */
	public static List<String> searchQuery(
			LinkedHashMap<String, HashMap<String, Integer>> index,
			String queryString, int numResults, CharArraySet stopwords) {

		MyAnalyzer analyzer = new MyAnalyzer(stopwords);

		TokenStreamComponents tsc = analyzer.createComponents("query",
				new StringReader(queryString));
		TokenStream tokens = tsc.getTokenStream();
		CharTermAttribute cta = tokens.addAttribute(CharTermAttribute.class);

		HashMap<String, Integer> queryMap = new HashMap<String, Integer>();

		// Places the tokens into a map object
		try {
			tokens.reset();
			while (tokens.incrementToken()) {
				if (queryMap.containsKey(cta.toString()))
					queryMap.put(cta.toString(),
							queryMap.get(cta.toString()) + 1);
				else
					queryMap.put(cta.toString(), 1);
			}
			tokens.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/** Edit This **/
		LinkedList<String> results = algorithmBase(index, queryMap, numResults);

		analyzer.close();
		return results;
	}

	/**
	 * Use comments to change which weighting scheme you want to use Gets a map
	 * of documents and their scores, then returns a list of the top numResults
	 * 
	 * @param index
	 * @param query
	 * @param numResults
	 * @return
	 */
	public static LinkedList<String> algorithmBase(
			LinkedHashMap<String, HashMap<String, Integer>> index,
			HashMap<String, Integer> query, int numResults) {

		LinkedList<String> topDocs = new LinkedList<String>();
		HashMap<String, Double> docScores = new HashMap<String, Double>();

		/**
		 * Change version to switch between weighting schemes Part 3a : 0 Part
		 * 3b : 1
		 * **/
		int version = 0;

		/** If version<0 or version>4, returns an empty list of Strings **/
		switch (version) {
		case 0:
			docScores = atcatc(index, query); // part 3a
			break;
		case 1:
			docScores = atnatn(index, query); // part 3b
			break;
		}

		// Compute top numResults
		double max = 0.0;
		String highest = null;

		// System.out.println("Score: " + docScores.get("CACM-0115"));

		for (int i = 0; i < 100 && !docScores.isEmpty(); i++) {
			for (String val : docScores.keySet()) {
				if (max < docScores.get(val)) {
					max = docScores.get(val);
					highest = val;
				}
			}
			// System.out.println(highest + ": " + max);
			topDocs.addLast(highest);
			docScores.remove(highest);
			max = 0.0;
			highest = null;
		}

		return topDocs;
	}

	/**
	 * Part 3a Implementation: atc.atc Computes the augmented TF and IDF for the
	 * query and documents Returns a HashMap<String, Double> of the document
	 * name and its score based on the scheme.
	 * 
	 * @param index
	 * @param query
	 * @return
	 */
	public static HashMap<String, Double> atcatc(
			LinkedHashMap<String, HashMap<String, Integer>> index,
			HashMap<String, Integer> query) {

		HashMap<String, Integer> termFreq = termFrequency(index);

		// compute tfidf values for query
		HashMap<String, Double> qryTfidf = queryCosTFIDF(index, query, termFreq);
		// System.out.println(qryTfidf);

		// compute tfidf values for each document
		HashMap<String, HashMap<String, Double>> docTfidfIndex = docCosTFIDF(
				index, termFreq);
		// System.out.println(docTfidfIndex.get("CACM-0115"));

		// compute dot product of query map with each document map
		HashMap<String, Double> docScores = new HashMap<String, Double>();
		for (String docName : docTfidfIndex.keySet()) {

			double docScore = 0.0;
			for (String term : qryTfidf.keySet()) {
				if (docTfidfIndex.get(docName).containsKey(term)) {
					docScore = docScore
							+ (docTfidfIndex.get(docName).get(term) * qryTfidf
									.get(term));
				}
			}
			docScores.put(docName, docScore);
		}

		// top30Docs stores top 30 in rank order
		LinkedList<String> top30Docs = new LinkedList<String>();
		// top30DocsIndex stores the doc name as key, term vector as values
		HashMap<String, HashMap<String, Double>> top30DocsIndex = new HashMap<String, HashMap<String, Double>>();

		// Compute top numResults
		double max = 0.0;
		String highest = null;

		for (int i = 0; i < 30 && !docScores.isEmpty(); i++) {
			for (String val : docScores.keySet()) {
				if (max < docScores.get(val)
						&& !top30DocsIndex.containsKey(val)) {
					max = docScores.get(val);
					highest = val;
				}
			}
			
			// contains titles of top-30 documents
			top30Docs.addLast(highest); 
			// title/term weight pairs for top 30 docs
			top30DocsIndex.put(highest, docTfidfIndex.get(highest)); 

			max = 0.0;
			highest = null;
		}
		System.out.println("\n-----\nTop 30 Docs: " + top30Docs);

		/** 
		 * Part 2 starts
		 * 
		 * **/
		// Initialize clustering
		List<LinkedList<String>> clusters = new ArrayList<LinkedList<String>>();
		for (int i = 0; i < 30; i++) {
			LinkedList<String> c = new LinkedList<String>();
			c.add(top30Docs.get(i));
			clusters.add(c);
		}
		//System.out.println("\n-----\nInitialized clusters: " + clusters + "\n");
		
		int K = 20;
		switch (K) {
		case 10:
			clustering(clusters, K, top30DocsIndex);
			rerankHigh(clusters, docScores);
			break;
		case 20:
			clustering(clusters, K, top30DocsIndex);
			rerankHigh(clusters, docScores);
			break;
		}
		
		return docScores;
	}
	
	public static void clustering (List<LinkedList<String>> clusters, int K, HashMap<String, HashMap<String, Double>> top30DocsIndex){
		// Main loop of clustering
		while (clusters.size() > K) {
			// Compute the distance
			double distance = -1;
			double minDistance = 1000;

			// Pairs with minimal distance
			LinkedList<String> pair1 = new LinkedList<String>();
			LinkedList<String> pair2 = new LinkedList<String>();

			for (LinkedList<String> currC : clusters) {
				for (LinkedList<String> c : clusters) {
					if(!currC.equals(c)){
						distance = clusterDist(currC, c, top30DocsIndex);
						if (distance < minDistance) {
							minDistance = distance;
							pair1 = currC;
							pair2 = c;
						}
					//System.out.println("[" + currC + "," + c + " distance= " + distance + "]");
					}
				}
			}
			// System.out.println("\n-----\ngroup: " + pair1 + ", " + pair2);

			// Remove and add
			clusters.remove(pair1);
			clusters.remove(pair2);
			pair1.addAll(pair2);
			clusters.add(pair1);
			//System.out.println("\n-----\nCurrent clusters [" + clusters.size() + "] : " + clusters + "\n");
		}
		System.out.println("Final clustering: " + clusters);		
	}
	
	public static void rerankAvg(List<LinkedList<String>> clusters, HashMap<String, Double> docScores){
		
		// Find the highest score within a cluster
		for (LinkedList<String> c: clusters){
			double avg;
			double totalScore = 0;
			//System.out.print("clusters: [" );
			
			for (String doc : c){
				//System.out.print(doc + ": " + docScores.get(doc) + "| ");
				totalScore = totalScore + docScores.get(doc);
			}
			avg = totalScore/c.size();			
			
			// Reassign the cluster
			for (String doc : c){
				docScores.put(doc, avg);
				//System.out.print(doc + ": " + docScores.get(doc) + "| ");
			}	
			//System.out.println();
		}		
	}
	
	public static void rerankHigh(List<LinkedList<String>> clusters, HashMap<String, Double> docScores){
		
		// Find the highest score within a cluster
		for (LinkedList<String> c: clusters){
			double highestScore = -1;
			double currScore;
			//System.out.print("clusters: [" );
			
			for (String doc : c){
				//System.out.print(doc + ": " + docScores.get(doc) + "| ");
				currScore = docScores.get(doc);
				if (currScore > highestScore){
					highestScore = currScore;
				}
			}
			
			// Reassign the cluster
			for (String doc : c){
				docScores.put(doc, highestScore);
				//System.out.print(doc + ": " + docScores.get(doc) + "| ");
			}	
			//System.out.println();
		}		
	}

	/**
	 * Part 2 clustering Computes the distance between two clusters c1 and c2 using complete linking
	 * 
	 * @param clusters
	 * @param docindex
	 * @return distance
	 */
	public static double clusterDist(LinkedList<String> c1,
			LinkedList<String> c2,
			HashMap<String, HashMap<String, Double>> top30DocsIndex) {
		
		double dist = 0;
		double maxDist = -1;
		double score = 0;

		for (String doc1 : c1) {
			HashMap<String, Double> currDoc1 = new HashMap<String, Double>();
			currDoc1 = top30DocsIndex.get(doc1);

			for (String doc2 : c2) {
				HashMap<String, Double> currDoc2 = new HashMap<String, Double>();
				currDoc2 = top30DocsIndex.get(doc2);

				// Compute the distance between doc1 and doc2
				for (String term : currDoc1.keySet()) {
					if (currDoc2.containsKey(term)) {
						score += (currDoc1.get(term) * currDoc2.get(term));
					}
				}
				
				dist = 1 / score;

				if (dist > maxDist) {
					maxDist = dist;
				}
			}
		}

		return maxDist;
	}

	/**
	 * Part 3b Implementation: atn.atn Computes the augmented TF and IDF for the
	 * query and documents, weighted by cosine normalization Returns a
	 * HashMap<String, Double> of the document name and its score based on the
	 * scheme.
	 * 
	 * @param index
	 * @param query
	 * @return
	 */
	public static HashMap<String, Double> atnatn(
			LinkedHashMap<String, HashMap<String, Integer>> index,
			HashMap<String, Integer> query) {

		HashMap<String, Integer> termFreq = termFrequency(index);

		// compute tfidf values for query
		HashMap<String, Double> qryTfidf = queryTFIDF(index, query, termFreq);
		// System.out.println(qryTfidf);

		// compute tfidf values for each document
		HashMap<String, HashMap<String, Double>> docTfidfIndex = docTFIDF(
				index, termFreq);
		// System.out.println(docTfidfIndex.get("CACM-0115"));

		HashMap<String, Double> docScores = new HashMap<String, Double>();
		for (String docName : docTfidfIndex.keySet()) {

			double docScore = 0.0;
			for (String term : qryTfidf.keySet()) {
				if (docTfidfIndex.get(docName).containsKey(term)) {
					docScore = docScore
							+ (docTfidfIndex.get(docName).get(term) * qryTfidf
									.get(term));
				}
			}
			docScores.put(docName, docScore);
		}

		return docScores;
	}

	// Query ATC
	private static HashMap<String, Double> queryCosTFIDF(
			LinkedHashMap<String, HashMap<String, Integer>> index,
			HashMap<String, Integer> query, HashMap<String, Integer> termFreq) {

		HashMap<String, Double> tfidf = queryTFIDF(index, query, termFreq);

		double normalWeight = 0.0;

		for (String term : tfidf.keySet()) {
			normalWeight += Math.pow(tfidf.get(term), 2);
		}

		normalWeight = Math.sqrt(normalWeight);

		for (String term : tfidf.keySet()) {
			tfidf.put(term, tfidf.get(term) / normalWeight);
		}

		return tfidf;
	}

	// Query ATN
	private static HashMap<String, Double> queryTFIDF(
			LinkedHashMap<String, HashMap<String, Integer>> index,
			HashMap<String, Integer> query, HashMap<String, Integer> termFreq) {

		HashMap<String, Double> tfidf = new HashMap<String, Double>();

		double maxFreq = 0.0;
		for (String term : query.keySet()) {
			if (query.get(term) > maxFreq) {
				maxFreq = query.get(term);
			}
		}

		for (String term : query.keySet()) {
			if (termFreq.containsKey(term)) {
				double tf = termFreq.get(term);

				tfidf.put(
						term,
						(0.5 + (0.5 * query.get(term)) / maxFreq)
								* Math.log10(((double) index.size())
										/ termFreq.get(term)));
			} else {
				tfidf.put(term, 0.0);
			}
		}

		return tfidf;
	}

	// Doc ATC
	private static HashMap<String, HashMap<String, Double>> docCosTFIDF(
			LinkedHashMap<String, HashMap<String, Integer>> index,
			HashMap<String, Integer> termFreq) {
		HashMap<String, HashMap<String, Double>> tfidf = docTFIDF(index,
				termFreq);

		// Weights with cosine normalization
		for (String doc : tfidf.keySet()) {
			double normalWeight = 0.0;

			for (String term : tfidf.get(doc).keySet()) {
				normalWeight += Math.pow(tfidf.get(doc).get(term), 2);
			}

			normalWeight = Math.sqrt(normalWeight);

			for (String term : tfidf.get(doc).keySet()) {
				tfidf.get(doc).put(term,
						tfidf.get(doc).get(term) / normalWeight);
			}
		}

		return tfidf;
	}

	// Doc ATN
	private static HashMap<String, HashMap<String, Double>> docTFIDF(
			LinkedHashMap<String, HashMap<String, Integer>> index,
			HashMap<String, Integer> termFreq) {

		HashMap<String, HashMap<String, Double>> tfidf = new HashMap<String, HashMap<String, Double>>();

		for (String doc : index.keySet()) {
			tfidf.put(doc, new HashMap<String, Double>());
			double maxtf = 0.0;

			for (String term : index.get(doc).keySet()) {
				if (index.get(doc).get(term) > maxtf)
					maxtf = index.get(doc).get(term);
			}

			for (String term : index.get(doc).keySet()) {
				tfidf.get(doc).put(
						term,
						(0.5 + (0.5 * index.get(doc).get(term)) / maxtf)
								* Math.log10(((double) index.size())
										/ (termFreq.get(term))));
				// if(doc.equals("CACM-1410"))
				// System.out.println(term + ": \tTF(" + (0.5 + (0.5 *
				// index.get(doc).get(term)) / maxtf) + "), \tIDF(" +
				// Math.log10(((double)index.size())/(termFreq.get(term))) +
				// "), TFIDF(" + ((0.5 + index.get(doc).get(term) / maxtf) *
				// Math.log10(((double)index.size())/(termFreq.get(term)))) +
				// ")");
			}
			// if(doc.equals("CACM-1410"))
			// System.out.println("CACM-1410: " + tfidf.get(doc) + "\tMax TF: "
			// + maxtf);
		}

		return tfidf;
	}

	private static HashMap<String, Integer> termFrequency(
			LinkedHashMap<String, HashMap<String, Integer>> index) {
		HashMap<String, Integer> termFreq = new HashMap<String, Integer>();

		for (String doc : index.keySet()) {
			for (String term : index.get(doc).keySet()) {
				if (termFreq.get(term) != null)
					termFreq.put(term, termFreq.get(term) + 1);
				else
					termFreq.put(term, 1);
			}
		}

		return termFreq;
	}

}
