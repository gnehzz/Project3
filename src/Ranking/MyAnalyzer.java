package Ranking;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;


public class MyAnalyzer extends StopwordAnalyzerBase {

	/** Maximum allowed token length */
	public static final int DEFAULT_MAX_TOKEN_LENGTH = 10000;

	private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;


	/** Builds an analyzer with the given stop words.
	 * @param matchVersion Lucene version to match See {@link
	 * <a href="#version">above</a>}
	 * @param stopWords stop words */
	public MyAnalyzer(CharArraySet stopWords) {
		super(stopWords);
	}

	/**
	 * Set maximum allowed token length.  If a token is seen
	 * that exceeds this length then it is discarded.  This
	 * setting only takes effect the next time tokenStream or
	 * tokenStream is called.
	 */
	public void setMaxTokenLength(int length) {
		maxTokenLength = length;
	}

	/**
	 * @see #setMaxTokenLength
	 */
	public int getMaxTokenLength() {
		return maxTokenLength;
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName, final Reader reader) {
		//Creates Tokenizer and sets Maximum Token Length
		final StandardTokenizer src = new StandardTokenizer(reader);
		src.setMaxTokenLength(maxTokenLength);
		TokenStream tok = new StandardFilter(src);
		
		//Adds on the filters: LowerCase, Stop, and PorterStem
		tok = new LowerCaseFilter(tok);
		tok = new StopFilter(tok, stopwords);
		tok = new PorterStemFilter(tok);

		//Returns a new TokenStreamComponents, containing the TokenStream and StandardTokenizer
		return new TokenStreamComponents(src, tok) {
			@Override
			protected void setReader(final Reader reader) throws IOException {
				src.setMaxTokenLength(MyAnalyzer.this.maxTokenLength);
				super.setReader(reader);
			}
		};
	}
	
	/**
	 * Takes in a file and produces a CharArraySet of stopwords
	 * @param filename
	 * @param size
	 * @return
	 */
	public static CharArraySet setStopWords(String filename){
		LinkedList<String> col = new LinkedList<String>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(new File(filename)));
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}

		String line = null;
		try {
			while((line = br.readLine()) != null){
				col.add(line);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new CharArraySet(col, false);
	}
}
