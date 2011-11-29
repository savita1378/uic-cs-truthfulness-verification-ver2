package edu.uic.cs.t_verifier.index.synonym;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.PorterStemmerExporter;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.GeneralException;

public class WordNetSynonymEngine2 implements SynonymEngine
{
	private IDictionary dict;
	private PorterStemmerExporter stemmer;

	public WordNetSynonymEngine2(String wordNetPath)
	{
		URL url = null;
		try
		{
			url = new URL("file", null, wordNetPath);
		}
		catch (MalformedURLException e)
		{
			throw new GeneralException(e);
		}

		dict = new Dictionary(url);

		// open it
		if (!dict.isOpen() && !dict.open())
		{
			throw new GeneralException("Open WordNet Dictionary failed. ");
		}

		stemmer = new PorterStemmerExporter();
	}

	/*public void closeWordnet()
	{
		dict.close(); // not necessary
	}*/

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
		for (POS pos : POS.values())
		{
			IIndexWord idxWord = dict.getIndexWord(term, pos);
			if (idxWord == null)
			{
				continue;
			}

			for (IWordID wordID : idxWord.getWordIDs())
			{
				IWord word = dict.getWord(wordID);
				int frequency = dict.getSenseEntry(word.getSenseKey())
						.getTagCount();
				ISynset synset = word.getSynset();
				if (frequency < Config.WORDNET_SYNSET_FREQUENCY_LOWER_BOUND)
				{
					// ignore those synset with too low frequency
					continue;
				}

				for (IWord wordInSynset : synset.getWords())
				{
					String wordString = wordInSynset.getLemma();
					wordString = wordString.replaceAll("_", " ").toLowerCase(
							Locale.US);
					if (!excludeTerms.contains(wordString))
					{
						synonyms.add(wordString);
					}
				}
			}
		}
	}

	public static void main(String[] args) throws Exception
	{
		new WordNetSynonymEngine2(Config.WORDNET_FOLDER).getSynonyms("good");
	}

}
