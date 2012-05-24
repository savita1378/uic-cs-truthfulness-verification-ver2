package edu.uic.cs.t_verifier.nlp.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
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
		// term = term.toLowerCase(Locale.US);
		IIndexWord idxWord = dict.getIndexWord(term, pos);
		if (idxWord == null)
		{
			return null;
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

	public class HypernymSet
	{
		private ISynsetID setID;

		private HashSet<String> terms;
		private LinkedHashSet<HypernymSet> hyperHypernyms;

		public HypernymSet(ISynsetID setID)
		{
			this.setID = setID;
		}

		public Set<String> getTerms()
		{
			if (terms == null)
			{
				synchronized (this)
				{
					if (terms == null)
					{
						terms = new HashSet<String>();
						List<IWord> words = WordNetReaderImpl.this.dict
								.getSynset(setID).getWords();
						for (IWord word : words)
						{
							String wordString = word.getLemma();
							wordString = wordString.replaceAll("_", " ");

							terms.add(wordString);
						}
					}
				}
			}

			return terms;
		}

		public LinkedHashSet<HypernymSet> getHyperHypernyms()
		{
			if (hyperHypernyms == null)
			{
				synchronized (this)
				{
					if (hyperHypernyms == null)
					{
						hyperHypernyms = new LinkedHashSet<HypernymSet>();

						ISynset synset = WordNetReaderImpl.this.dict
								.getSynset(setID);
						List<ISynsetID> hypernyms = synset
								.getRelatedSynsets(Pointer.HYPERNYM);
						for (ISynsetID sid : hypernyms)
						{
							hyperHypernyms.add(new HypernymSet(sid));
						}
					}
				}
			}

			return hyperHypernyms;
		}

		@Override
		public int hashCode()
		{
			return setID.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			return setID.equals(obj);
		}
	}

	@Override
	public LinkedHashSet<HypernymSet> retrieveHypernyms(String term, POS pos)
	{
		LinkedHashSet<HypernymSet> result = new LinkedHashSet<HypernymSet>();

		IIndexWord idxWord = dict.getIndexWord(term, pos);
		if (idxWord == null)
		{
			return result;
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

			ISynset synset = word.getSynset();
			List<ISynsetID> hypernyms = synset
					.getRelatedSynsets(Pointer.HYPERNYM);
			for (ISynsetID sid : hypernyms)
			{
				result.add(new HypernymSet(sid));
			}
		}

		return result;
	}

	public static void main2(String[] args)
	{
		WordNetReaderImpl wordNetReaderImpl = new WordNetReaderImpl();
		String termInStandardCase = wordNetReaderImpl
				.retrieveTermInStandardCase("new york", POS.NOUN);
		System.out.println(termInStandardCase);
	}

	public static void main(String[] args)
	{
		WordNetReaderImpl wordNetReaderImpl = new WordNetReaderImpl();
		LinkedHashSet<HypernymSet> hypernyms = wordNetReaderImpl
				.retrieveHypernyms("designer", POS.NOUN);

		recursivePrintHypernyms(hypernyms, 0);
		System.out.println("==========================");
		System.out.println(recursiveFindHypernym(hypernyms, "person"));
	}

	private static boolean recursiveFindHypernym(
			LinkedHashSet<HypernymSet> hypernyms, String hypernymToFind)
	{
		for (HypernymSet hypernym : hypernyms)
		{
			System.out.println(hypernym.getTerms());
			if (hypernym.getTerms().contains(hypernymToFind))
			{
				return true;
			}

			if (recursiveFindHypernym(hypernym.getHyperHypernyms(),
					hypernymToFind))
			{
				return true;
			}
		}

		return false;
	}

	private static void recursivePrintHypernyms(
			LinkedHashSet<HypernymSet> hypernyms, int indentCount)
	{
		for (HypernymSet hypernym : hypernyms)
		{
			for (int i = 0; i < indentCount; i++)
			{
				System.out.print(" ");
			}
			System.out.println(hypernym.getTerms());

			recursivePrintHypernyms(hypernym.getHyperHypernyms(),
					indentCount + 1);
		}
	}

}
