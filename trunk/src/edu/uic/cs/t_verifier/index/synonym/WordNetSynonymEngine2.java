package edu.uic.cs.t_verifier.index.synonym;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.PorterStemmerExporter;

import edu.uic.cs.t_verifier.nlp.WordNetReader;
import edu.uic.cs.t_verifier.nlp.impl.WordNetReaderImpl;

public class WordNetSynonymEngine2 implements SynonymEngine
{
	private PorterStemmerExporter stemmer;
	private WordNetReader wordNetReader;

	public WordNetSynonymEngine2()
	{
		stemmer = new PorterStemmerExporter();
		wordNetReader = new WordNetReaderImpl();
	}

	@Override
	public String[] getSynonyms(String term)
	{
		Set<String> synonyms = new TreeSet<String>();
		retrieveSynonyms(term, synonyms);

		String stemmedTerm = stemmer.stem(term);
		Set<String> excludeTerms = new HashSet<String>();
		excludeTerms.add(term);
		excludeTerms.add(stemmedTerm);
		retrieveSynonyms(stemmedTerm, synonyms, excludeTerms);

		return synonyms.toArray(new String[synonyms.size()]);
	}

	protected void retrieveSynonyms(String term, Set<String> synonyms)
	{
		retrieveSynonyms(term, synonyms, Collections.singleton(term));
	}

	private void retrieveSynonyms(String term, Set<String> synonyms,
			Set<String> excludeTerms)
	{
		synonyms.addAll(wordNetReader.retrieveSynonyms(term, excludeTerms));
	}

	public static void main(String[] args) throws Exception
	{
		String[] result = new WordNetSynonymEngine2().getSynonyms("nice");
		System.out.println(Arrays.asList(result));
	}

}
