package edu.uic.cs.t_verifier.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.uic.cs.t_verifier.common.AbstractWordOperations;
import edu.uic.cs.t_verifier.html.CategoriesExtractor;
import edu.uic.cs.t_verifier.html.WikipediaContentExtractor;
import edu.uic.cs.t_verifier.html.data.MatchedQueryKey;
import edu.uic.cs.t_verifier.html.data.MatchedQueryKey.DisambiguationEntry;
import edu.uic.cs.t_verifier.index.data.UrlWithDescription;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.ClassFactory;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.LogHelper;

class StatementAnalyzer extends AbstractWordOperations
{
	private static final Logger LOGGER = LogHelper
			.getLogger(StatementAnalyzer.class);

	private WikipediaContentExtractor wikipediaContentExtractor = ClassFactory
			.getInstance(Config.WIKIPEDIACONTENTEXTRACTOR_CLASS_NAME);

	private static class WordsNumberComparator implements Comparator<String>
	{
		@Override
		public int compare(String s1, String s2)
		{
			int length1 = StringUtils.split(s1).length;
			int length2 = StringUtils.split(s2).length;
			if (length1 != length2)
			{
				return length2 - length1;
			}
			else
			{
				return s1.compareTo(s2);
			}
		}

	}

	private static class NoSubStringTreeSet extends TreeSet<String>
	{
		private static final long serialVersionUID = 1L;
		private static final WordsNumberComparator WORDS_NUMBER_COMPARATOR = new WordsNumberComparator();

		public NoSubStringTreeSet()
		{
			super(WORDS_NUMBER_COMPARATOR);
		}

		@Override
		public boolean add(String str)
		{
			Iterator<String> iterator = iterator();
			while (iterator.hasNext())
			{
				String containingStr = iterator.next();
				if (containingStr.contains(str))
				{
					return false;
				}
				else if (str.contains(containingStr))
				{
					iterator.remove();
				}
			}

			return super.add(str);
		}
	}

	private CategoriesExtractor categoriesExtractor = new CategoriesExtractor()
	{
	};

	public Map<String, List<UrlWithDescription>> getUrlsByTopicUnit(
			Statement statement)
	{
		Collection<String> topicUnits = statement.getTopicUnits();
		List<String> aus = statement.getAlternativeUnits();
		Set<String> allWordsInTopicUnits = statement.getAllWordsInTopicUnits();

		return processAllTopicUnitsInStatement(topicUnits, aus,
				allWordsInTopicUnits);
	}

	private Map<String, List<UrlWithDescription>> processAllTopicUnitsInStatement(
			Collection<String> topicUnits, List<String> aus,
			Set<String> allWordsInTopicUnits)
	{
		// topic units waiting for mathcing
		NoSubStringTreeSet topicUnitsRemovedStopWords = new NoSubStringTreeSet();
		for (String topicUnit : topicUnits)
		{
			topicUnit = trimStopWordsInBothSides(topicUnit);
			topicUnitsRemovedStopWords.add(topicUnit);
		}

		// //////////////////////////////////////////////////////////////////////
		Map<String, List<UrlWithDescription>> result = new HashMap<String, List<UrlWithDescription>>();

		HashSet<String> allMatchedTopicUnits = new HashSet<String>();
		while (topicUnitsRemovedStopWords.size() != 0)
		{
			HashSet<String> notMatchedTopicUnits = new HashSet<String>();

			for (String topicUnit : topicUnitsRemovedStopWords)
			{
				if (topicUnit.length() == 0)
				{
					continue;
				}

				MatchedQueryKey matchedQueryKey = wikipediaContentExtractor
						.matchQueryKey(topicUnit);
				// matched
				if (matchedQueryKey != null)
				{
					// certainly matched
					if (matchedQueryKey.isCertainly())
					{
						allMatchedTopicUnits.add(topicUnit);

						String matchedUrl = matchedTopicUnit(topicUnit,
								matchedQueryKey);
						List<String> matchedCategories = matchedQueryKey
								.getCategories();
						UrlWithDescription urlWithDescription = new UrlWithDescription(
								matchedUrl, null, matchedCategories);
						result.put(topicUnit,
								Collections.singletonList(urlWithDescription));
					}
					// disambiguations
					else
					{
						List<String> wordsInTopicUnitCausingAmbiguities = standardAnalyzeUsingDefaultStopWords(topicUnit);
						Set<String> nonstopWordsNotCausingAmbiguities = getNonstopWordsNotCausingAmbiguities(
								allWordsInTopicUnits,
								wordsInTopicUnitCausingAmbiguities);

						List<UrlWithDescription> urlWithDescriptions = findTheMostMatchedDisambiguationEntries(
								matchedQueryKey.getDisambiguationEntries(),
								nonstopWordsNotCausingAmbiguities, aus);

						for (UrlWithDescription urlWithDescription : urlWithDescriptions)
						{
							System.out.println("? "
									+ urlWithDescription.getUrl());
						}

						if (!urlWithDescriptions.isEmpty())
						{
							result.put(topicUnit, urlWithDescriptions);
						}
					}
				}
				else
				{
					// not matched
					notMatchedTopicUnits.add(topicUnit);
				}
			}

			if (notMatchedTopicUnits.isEmpty())
			{
				break;
			}

			topicUnitsRemovedStopWords = getNextLevelTopicUnits(
					allMatchedTopicUnits, notMatchedTopicUnits);
		}

		return result;
	}

	private Set<String> getNonstopWordsNotCausingAmbiguities(
			Set<String> allWordsInTopicUnits,
			List<String> wordsInTopicUnitCausingAmbiguities)
	{
		Set<String> result = new HashSet<String>();
		for (String wordInTU : allWordsInTopicUnits)
		{
			if (!isStopWord(wordInTU)
					&& !wordsInTopicUnitCausingAmbiguities.contains(wordInTU))
			{
				result.add(wordInTU);
			}
		}

		return result;
	}

	// TODO may need be changed to using Lucene
	private List<UrlWithDescription> findTheMostMatchedDisambiguationEntries(
			List<DisambiguationEntry> disambiguationEntries,
			Set<String> nonstopWordsNotCausingAmbiguities, List<String> aus)
	{
		Set<String> stemmedNonstopWordsNotCausingAmbiguities = getStemmedNonstopWordsNotCausingAmbiguities(
				nonstopWordsNotCausingAmbiguities, aus);

		int maxScore = 0;
		List<UrlWithDescription> maxScoreUrls = new ArrayList<UrlWithDescription>();
		for (DisambiguationEntry disambiguationEntry : disambiguationEntries)
		{
			int count = 0;
			String description = disambiguationEntry.getDescription();
			List<String> nonstopStemmedWordsInDesc = porterStemmingAnalyzeUsingDefaultStopWords(description);
			if (nonstopStemmedWordsInDesc.isEmpty())
			{
				continue;
			}

			// System.out.println(disambiguationEntry.getKeyWord());
			for (String wordStemmed : stemmedNonstopWordsNotCausingAmbiguities)
			{
				if (nonstopStemmedWordsInDesc.contains(wordStemmed))
				{
					count++;
				}
			}

			String url = MatchedQueryKey
					.constructPageAddress(disambiguationEntry.getKeyWord());
			List<String> categories = categoriesExtractor
					.extractCategoriesFromPage(url);
			UrlWithDescription urlWithDescription = new UrlWithDescription(url,
					description, categories);

			if (count > maxScore)
			{
				maxScore = count;
				maxScoreUrls.clear();
				maxScoreUrls.add(urlWithDescription);
			}
			else if (count != 0 && count == maxScore)
			{
				maxScoreUrls.add(urlWithDescription);
			}
		}

		//		System.out.println(maxScore);
		return maxScoreUrls;
	}

	private Set<String> getStemmedNonstopWordsNotCausingAmbiguities(
			Set<String> nonstopWordsNotCausingAmbiguities, List<String> aus)
	{
		// if there's no other words in TU can be used to disambiguate the entries,
		// use nonstop words in AU do the job
		if (nonstopWordsNotCausingAmbiguities.isEmpty())
		{
			// TODO based on current logic, the one contains most many AU words, 
			// is the one matched.  
			for (String au : aus)
			{
				nonstopWordsNotCausingAmbiguities
						.addAll(standardAnalyzeUsingDefaultStopWords(au));
			}
		}

		Set<String> stemmedNonstopWordsNotCausingAmbiguities = new HashSet<String>(
				nonstopWordsNotCausingAmbiguities.size());
		for (String word : nonstopWordsNotCausingAmbiguities)
		{
			String wordStemmed = stem(word);
			stemmedNonstopWordsNotCausingAmbiguities.add(wordStemmed);
			// System.out.println(">>>>>>>>>>>>> " + wordStemmed);
		}
		return stemmedNonstopWordsNotCausingAmbiguities;
	}

	private String matchedTopicUnit(String topicUnit, MatchedQueryKey queryKey)
	{
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug(LogHelper.LOG_LAYER_ONE + "Matched a TU[" + topicUnit
					+ "] in the URL[" + queryKey.getCertainPageUrl() + "]");
		}

		// System.out.println(keyWords.getCertainPageUrl());

		return queryKey.getCertainPageUrl();
	}

	private NoSubStringTreeSet getNextLevelTopicUnits(
			HashSet<String> allMatchedTopicUnits,
			HashSet<String> notMatchedTopicUnits)
	{
		NoSubStringTreeSet result = new NoSubStringTreeSet();

		for (String notMatched : notMatchedTopicUnits)
		{
			List<String> nextLevelTopicUnits = shrinkPharse(notMatched);
			middle: for (String nextLevelNotMatched : nextLevelTopicUnits)
			{
				for (String matched : allMatchedTopicUnits)
				{
					// if any matched already contains this TU
					if (matched.contains(nextLevelNotMatched))
					{
						// ignore this substring
						continue middle;
					}
				}

				// no already match TU contains this sub-TU
				result.add(nextLevelNotMatched);
			}
		}

		return result;
	}

	private List<String> shrinkPharse(String topicUnit)
	{
		// topicUnit = topicUnit.trim();
		int first = topicUnit.indexOf(' ');
		int last = topicUnit.lastIndexOf(' ');

		if (first < 0 || last < 0)
		{
			return Collections.emptyList();
		}

		List<String> result = new ArrayList<String>(2);
		result.add(trimStopWordsInBothSides(topicUnit.substring(0, last)));
		result.add(trimStopWordsInBothSides(topicUnit.substring(first + 1)));

		return result;
	}

	public Map<String, List<UrlWithDescription>> getUrlsByAlternativeUnit(
			Statement statement, boolean doFilter)
	{
		List<String> alternaitveUnits = statement.getAlternativeUnits();
		Map<String, List<UrlWithDescription>> result = new HashMap<String, List<UrlWithDescription>>();

		for (String auString : alternaitveUnits)
		{
			List<UrlWithDescription> matchedUrls = matchUrlsForAlternativeUnit(
					auString, statement, doFilter);

			if (matchedUrls != null)
			{
				result.put(auString, matchedUrls);
			}
		}

		return result;
	}

	private List<UrlWithDescription> matchUrlsForAlternativeUnit(
			String auString, Statement statement, boolean doFilter)
	{
		MatchedQueryKey matchedQueryKey = wikipediaContentExtractor
				.matchQueryKey(auString);
		if (matchedQueryKey == null)
		{
			return null;
		}

		if (matchedQueryKey.isCertainly())
		{
			String certainUrl = matchedQueryKey.getCertainPageUrl();
			if (LOGGER.isDebugEnabled())
			{
				LOGGER.debug(LogHelper.LOG_LAYER_ONE + "Matched a AU["
						+ auString + "] in the URL[" + certainUrl + "]");
			}

			List<String> matchedCategories = matchedQueryKey.getCategories();
			return Collections.singletonList(new UrlWithDescription(certainUrl,
					null, matchedCategories));
		}
		else
		{
			List<DisambiguationEntry> disambiguationEntryList = matchedQueryKey
					.getDisambiguationEntries();

			if (doFilter)
			{
				Set<String> allNonstopWords = statement
						.getAllNonstopWordsInTopicUnits();

				List<String> emptyList = Collections.emptyList();
				return findTheMostMatchedDisambiguationEntries(
						disambiguationEntryList, allNonstopWords, emptyList);
			}
			else
			{
				List<UrlWithDescription> disambiguationUrls = new ArrayList<UrlWithDescription>(
						disambiguationEntryList.size());
				for (DisambiguationEntry disambiguationEntry : disambiguationEntryList)
				{
					String keyWord = disambiguationEntry.getKeyWord();
					String url = MatchedQueryKey.constructPageAddress(keyWord);
					List<String> categories = categoriesExtractor
							.extractCategoriesFromPage(url);
					String description = disambiguationEntry.getDescription();
					disambiguationUrls.add(new UrlWithDescription(url,
							description, categories));
				}

				return disambiguationUrls;
			}
		}

	}

}
