package Ranking;
import org.apache.lucene.analysis.util.CharArraySet;


public class RankingDriver {
	/**
	 * Runs Parts 3 and 4. 
	 * Will not work if there is no built index.
	 * To switch between modes, go SearchFiles.java and change which method is being used
	 * around line 65 in the algorithmBase method.
	 * @param args
	 */
	public static void main(String[] args){
		String cacmDocsDir = "data/cacm"; // directory containing CACM documents
		String medDocsDir = "data/med"; // directory containing MED documents

		String cacmIndexDir = "data/txt/cacm"; // the directory where index is written into
		String medIndexDir = "data/txt/med"; // the directory where index is written into

		String cacmQueryFile = "data/cacm_processed.query";    // CACM query file
		String cacmAnswerFile = "data/cacm_processed.rel";   // CACM relevance judgements file

		String medQueryFile = "data/med_processed.query";    // MED query file
		String medAnswerFile = "data/med_processed.rel";   // MED relevance judgements file

		String stopWordFile = "data/stopwords/stopwords_indri.txt";

		int cacmNumResults = 100;
		int medNumResults = 100;

		CharArraySet stopwords = MyAnalyzer.setStopWords(stopWordFile);

		System.out.println("Evaluating CACM: ");
		
		System.out.println(EvaluateQueries.evaluate(cacmIndexDir, cacmDocsDir, cacmQueryFile,
				cacmAnswerFile, cacmNumResults, stopwords));

		System.out.println("\nEvaluating MED: ");

		System.out.println(EvaluateQueries.evaluate(medIndexDir, medDocsDir, medQueryFile,
				medAnswerFile, medNumResults, stopwords));
	}
}
