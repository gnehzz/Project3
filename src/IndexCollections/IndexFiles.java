package IndexCollections;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

/** Index all text files under a directory, the directory is at data/txt/
 */

public class IndexFiles {

	private IndexFiles() {}

	/** Index all text files under a directory. */
	public static void buildIndex(String indexPath, String docsPath, 
			CharArraySet stopwords) {
		// Check whether docsPath is valid
		if (docsPath == null || docsPath.isEmpty()) {
			System.err.println("Document directory cannot be null");
			System.exit(1);
		}

		// Check whether the directory is readable
		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out.println("Document directory '" + docDir.getAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		Date start = new Date();
		System.out.println("Indexing to directory '" + indexPath + "'...");

		//Creates a MyAnalyzer and indexes the Documents at docDir to indexPath
		MyAnalyzer analyzer = new MyAnalyzer(stopwords);
		indexDocs(indexPath, docDir, analyzer);

		//Prints the time taken to complete indexing
		Date end = new Date();
		System.out.println(end.getTime() - start.getTime() + " total milliseconds");
	}

	/**
	 * Takes in the file located at file and creates a Document-Term Map for 
	 * each file passed in. Placing the newly created Map within the location
	 * determined by index, with a filename derived from file with a .index
	 * extension.
	 * @param index
	 * @param file
	 * @param analyzer
	 */
	static void indexDocs(String index, File file, MyAnalyzer analyzer) {
		if (file.canRead()) {
			if (file.isDirectory()) {
				String[] files = file.list();
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						indexDocs(index, new File(file, files[i]), analyzer);
					}
				}
			} else {
				//Ignore files with no name
				if(file.toString().lastIndexOf('\\') + 1 == file.toString().lastIndexOf('.'))
					return;
				
				//Tokenize the document located at file
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
				} catch (FileNotFoundException e) {
					System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
				}
				TokenStreamComponents tsc = analyzer.createComponents("content", new BufferedReader(new InputStreamReader(fis)));
				TokenStream tokens = tsc.getTokenStream();
				CharTermAttribute cta = tokens.addAttribute(CharTermAttribute.class);

				HashMap<String,Integer> map = new HashMap<String,Integer>();

				//Places the tokens into a map object
				try {
					tokens.reset();
					while (tokens.incrementToken()) {
						if(map.containsKey(cta.toString()))
							map.put(cta.toString(), map.get(cta.toString()) + 1);
						else
							map.put(cta.toString(), 1);
					}
					tokens.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//Writes the map to a file with the 
				ObjectOutputStream vector;
				try {
					vector = new ObjectOutputStream(new FileOutputStream(new File(index 
							+ file.toString().substring(file.toString().lastIndexOf("\\"), file.toString().lastIndexOf('.')) + ".index")));
					vector.writeObject(map);
					vector.flush();
					vector.close();
				} catch (FileNotFoundException e){
					e.printStackTrace();
				} catch (IOException e){
					e.printStackTrace();
				}	
			}
		}
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
				collectionIndex.put(files[i].substring(0, files[i].lastIndexOf(".index")), IndexFiles.getDocIndex(indexDir + "/" + files[i]));
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

		//Reads hashmap from filepath
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
