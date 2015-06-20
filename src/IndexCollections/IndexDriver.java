package IndexCollections;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.lucene.analysis.util.CharArraySet;


public class IndexDriver {

	/**
	 * Main file for just creating the index for CACM and MED
	 * Code for Part 2 is implemented in IndexFiles and MyAnalyzer
	 * Minor changes to EvaluateQuereies were made to use IndesFiles and MyAnalyzer
	 * @param args
	 */
	public static void main(String[] args){
		String cacmDocsDir = "data/cacm"; // directory containing CACM documents
		String medDocsDir = "data/med"; // directory containing MED documents

		String cacmIndexDir = "data/txt/cacm"; // the directory where index is written into
		String medIndexDir = "data/txt/med"; // the directory where index is written into

		String stopWordFile = "data/stopwords/stopwords_indri.txt";

		System.out.println("Creating Stopword Set\n......");
		CharArraySet stopwords = MyAnalyzer.setStopWords(stopWordFile);
		System.out.println("Created Stopword Set : ");
		System.out.println(stopwords + "\n");

		//Cacm Index Creation
		IndexFiles.buildIndex(cacmIndexDir, cacmDocsDir, stopwords);
		System.out.println("Created CACM Index\n");

		//Med Index Creation
		IndexFiles.buildIndex(medIndexDir, medDocsDir, stopwords);
		System.out.println("Created MED Index\n");

		System.out.println("CACM Index:");
		LinkedHashMap<String, HashMap<String, Integer>> cIndex = null;
		try {
			cIndex = IndexFiles.getIndex(cacmIndexDir);
			for(String doc : cIndex.keySet()){
				System.out.println(doc + " Term Frequencies: " + cIndex.get(doc));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("\nMED Index:");

		LinkedHashMap<String, HashMap<String, Integer>> mIndex = null;
		try {
			mIndex = IndexFiles.getIndex(medIndexDir);
			for(String doc : mIndex.keySet()){
				System.out.println(doc + " Term Frequencies: " + mIndex.get(doc));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("\nDone");
	}
}
