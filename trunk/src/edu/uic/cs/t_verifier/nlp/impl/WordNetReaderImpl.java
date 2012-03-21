package edu.uic.cs.t_verifier.nlp.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.uic.cs.t_verifier.common.AbstractWordOperations;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.nlp.WordNetReader;

public class WordNetReaderImpl extends AbstractWordOperations implements
		WordNetReader
{
	// consider making it a singleton

	private IDictionary dict;

	public WordNetReaderImpl()
	{
		URL url = null;
		try
		{
			url = new URL("file", null, Config.WORDNET_FOLDER);
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
	}

	@Override
	public Set<String> retrieveSynonyms(String term, Set<String> excludeTerms)
	{
		Set<String> synonymsResult = new TreeSet<String>();

		for (POS pos : POS.values())
		{
			IIndexWord idxWord = dict.getIndexWord(term, pos);
			if (idxWord == null)
			{
				continue;
			}

			int minAcceptFrequency = minAcceptFrequency(idxWord);
			for (IWordID wordID : idxWord.getWordIDs())
			{
				IWord word = dict.getWord(wordID);
				int frequency = dict.getSenseEntry(word.getSenseKey())
						.getTagCount();
				ISynset synset = word.getSynset();
				if (frequency < minAcceptFrequency)
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
						synonymsResult.add(wordString);
					}
				}
			}
		}

		return synonymsResult;
	}

	@Override
	public String retrieveTermInStandardCase(String term, POS pos)
	{
		term = term.toLowerCase(Locale.US);
		IIndexWord idxWord = dict.getIndexWord(term, pos);
		if (idxWord == null)
		{
			return term;
		}

		int minAcceptFrequency = minAcceptFrequency(idxWord);
		for (IWordID wordID : idxWord.getWordIDs())
		{
			IWord word = dict.getWord(wordID);
			int frequency = dict.getSenseEntry(word.getSenseKey())
					.getTagCount();
			if (frequency < minAcceptFrequency)
			{
				// ignore those synset with too low frequency
				continue;
			}

			String wordString = word.getLemma();
			wordString = wordString.replaceAll("_", " ");

			if (sameTermButDifferentCase(term, wordString))
			{
				return wordString/*WordUtils.capitalize(term)*/;
			}
		}

		return term;
	}

	private int minAcceptFrequency(IIndexWord idxWord)
	{
		int maxFrequency = findMaxFrequency(idxWord);
		return (int) (Config.WORDNET_SYNSET_FREQUENCY_RATIO_TO_MAX_LOWER_BOUND * maxFrequency);
	}

	private int findMaxFrequency(IIndexWord idxWord)
	{
		int maxFrequency = 0;
		for (IWordID wordID : idxWord.getWordIDs())
		{
			IWord word = dict.getWord(wordID);
			int frequency = dict.getSenseEntry(word.getSenseKey())
					.getTagCount();
			if (frequency > maxFrequency)
			{
				maxFrequency = frequency;
			}
		}

		return maxFrequency;
	}

	private boolean sameTermButDifferentCase(String originalTerm,
			String termInWN)
	{
		if (!Character.isUpperCase(termInWN.charAt(0)))
		{
			return false;
		}

		if (originalTerm.equalsIgnoreCase(termInWN))
		{
			return true;
		}

		String stemmedTermInWN = stem(termInWN);
		String stemmedOriginalTerm = stem(originalTerm);
		return stemmedOriginalTerm.equalsIgnoreCase(stemmedTermInWN);

	}

	public static void main(String[] args)
	{
		WordNetReaderImpl wordNetReaderImpl = new WordNetReaderImpl();
		String termInStandardCase = wordNetReaderImpl
				.retrieveTermInStandardCase("new york", POS.NOUN);
		System.out.println(termInStandardCase);
	}

}
