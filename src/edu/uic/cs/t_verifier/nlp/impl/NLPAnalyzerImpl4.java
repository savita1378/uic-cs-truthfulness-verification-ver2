package edu.uic.cs.t_verifier.nlp.impl;

import java.text.Normalizer;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.StopAnalyzer;

import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.Tree;
import edu.uic.cs.t_verifier.html.WikipediaContentExtractor;
import edu.uic.cs.t_verifier.html.data.MatchedQueryKey;
import edu.uic.cs.t_verifier.index.data.Paragraph;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.index.data.UrlWithDescription;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.ClassFactory;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.LogHelper;
import edu.uic.cs.t_verifier.nlp.impl.OpenNLPChunker.ChunkType;

public class NLPAnalyzerImpl4 extends NLPAnalyzerImpl3
{
	private static final Logger LOGGER = LogHelper
			.getLogger(NLPAnalyzerImpl4.class);

	private static final String POSTAG_POSSESSIVE = "POS";
	private static final String POSTAG_DETERMINER = "DT";
	private static final String POSTAG_NOUN = "NN";

	private static final List<String> PARENTHESES_VALUES = Arrays.asList(
			"-lrb-", "-rrb-");

	private static final String REGEX_TERM_LEFT_BOUND = "\\b";
	private static final String REGEX_TERM_RIGHT_BOUND = "(\\b|\\s)";

	private static final String[] CATEGORY_KEYWORDS_SONG = { "song", "album" };

	private WikipediaContentExtractor wikipediaContentExtractor = ClassFactory
			.getInstance(Config.WIKIPEDIACONTENTEXTRACTOR_CLASS_NAME);

	public static interface RecursiveMatcher<T>
	{
		boolean isMatched(
				List<Entry<String, String>> currentLevelPosTagsByTermSequence);

		T getMatchedInfo();
	}

	private class WikipediaKeyWordsMatcher implements
			RecursiveMatcher<MatchedQueryKey>
	{
		private MatchedQueryKey matchedQueryKey;

		// private String wholeSentence;

		@Override
		public boolean isMatched(
				List<Entry<String, String>> currentLevelPosTagsByTermSequence)
		{
			matchedQueryKey = null;

			String term = concatenateTerms(currentLevelPosTagsByTermSequence);

			matchedQueryKey = wikipediaContentExtractor.matchQueryKey(term);
			if (matchedQueryKey == null)
			{
				return false;
			}

			if (matchedQueryKey.isCertainly())
			{
				return true;
			}
			else
			// TODO disambiguation /////////////////////////////////////////////
			{
				return false;
				/*List<DisambiguationEntry> disambiguationEntryList = matchedQueryKey
						.getDisambiguationEntries();
				matchedQueryKey = findTheMostLikelyDisambiguationEntery(
						disambiguationEntryList, term);
				if (matchedQueryKey == null)
				{
					return false;
				}
				else
				{
					LOGGER.info("Disambiguation for '" + term + "' "
							+ matchedQueryKey);
					return true;
				}*/
			}
			////////////////////////////////////////////////////////////////////
		}

		//		private MatchedQueryKey findTheMostLikelyDisambiguationEntery(
		//				List<DisambiguationEntry> disambiguationEntryList,
		//				String currentTerm)
		//		{
		//			String remainingTerms = wholeSentence.replaceAll(currentTerm, "");
		//			List<String> remainingNonstopStemmedTerms = stem(standardAnalyzeUsingDefaultStopWords(remainingTerms));
		//
		//			MatchedQueryKey result = null;
		//			int maxCount = 0;
		//			for (DisambiguationEntry disambiguationEntry : disambiguationEntryList)
		//			{
		//				String description = disambiguationEntry.getDescription();
		//				List<String> nonstopStemmedWordsInDesc = porterStemmingAnalyzeUsingDefaultStopWords(description);
		//				if (nonstopStemmedWordsInDesc.isEmpty())
		//				{
		//					continue;
		//				}
		//
		//				int count = 0;
		//				for (String wordStemmed : remainingNonstopStemmedTerms)
		//				{
		//					if (nonstopStemmedWordsInDesc.contains(wordStemmed))
		//					{
		//						count++;
		//					}
		//				}
		//
		//				if (count > maxCount
		//						&& disambiguationEntry.getKeyWord() != null) // ">" prefer the front ones if counts are equal
		//				{
		//					maxCount = count;
		//					result = new MatchedQueryKey(
		//							disambiguationEntry.getKeyWord(), null,
		//							Collections.<String> emptyList()); // no categories stored since we don't use it now
		//				}
		//			}
		//
		//			return result;
		//		}

		@Override
		public MatchedQueryKey getMatchedInfo()
		{
			Assert.notNull(matchedQueryKey);
			return matchedQueryKey;
		}

		private RecursiveMatcher<MatchedQueryKey> setWholeSentence(
				String wholeSentence)
		{
			// this.wholeSentence = wholeSentence;
			return this;
		}
	}

	private WikipediaKeyWordsMatcher wikipediaKeyWordsMatcher = new WikipediaKeyWordsMatcher();

	private PersonNameMatcherImpl trigramPersonNameMatcher = new PersonNameMatcherImpl();

	private SentenceCache sentenceCache = SentenceCache.getInstance();

	private OpenNLPChunker openNLPChunker = new OpenNLPChunker();

	//	private WikipediaContentExtractor wikipediaContentExtractor = new WikipediaContentExtractor() // for test
	//	{
	//		@Override
	//		public MatchedQueryKey matchQueryKey(final String queryWords)
	//		{
	//			return new MatchedQueryKey(null, null, null)
	//			{
	//				@Override
	//				public boolean isCertainly()
	//				{
	//					/*return "C D E".equals(queryWords)
	//							|| "F G".equals(queryWords);*/
	//					/*return "A B C D".equals(queryWords)
	//							|| "E F G".equals(queryWords);*/
	//					/*return "A B C D".equals(queryWords)
	//							|| "F G".equals(queryWords);*/
	//					/*return "A B C".equals(queryWords)
	//							|| "E F G".equals(queryWords);*/
	//					return "B C".equals(queryWords) || "H".equals(queryWords);
	//				}
	//			};
	//		}
	//
	//		@Override
	//		public List<Segment> extractSegmentsFromWikipedia(
	//				UrlWithDescription urlWithDescription, boolean isBulletinPage)
	//		{
	//			return null;
	//		}
	//	};
	//
	//	public static void main(String[] args)
	//	{
	//		NLPAnalyzerImpl4 impl = new NLPAnalyzerImpl4();
	//
	//		String sentence = "A B C D E F G H";
	//		impl.capitalizeProperNounTerms(sentence, null);
	//	}

	public static void main2(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();
		NLPAnalyzerImpl4 nlpAnalyzer = new NLPAnalyzerImpl4();

		for (Statement statement : statements)
		{
			List<String> allAlternativeUnits = statement.getAlternativeUnits();
			List<String> allAlternativeStatements = statement
					.getAllAlternativeStatements();

			for (int index = 0; index < allAlternativeStatements.size(); index++)
			{
				String alternativeUnit = allAlternativeUnits.get(index);
				String sentence = allAlternativeStatements.get(index);
				String counterPartOfAU = nlpAnalyzer
						.retrieveTopicTermIfSameTypeAsAU(sentence,
								alternativeUnit); // this method will restore cases

				if (counterPartOfAU != null)
				{
					System.out.println(sentence);
					System.out.println(counterPartOfAU + "\t-\t"
							+ alternativeUnit);
				}
			}
		}
	}

	public static void main1(String[] args)
	{
		NLPAnalyzerImpl4 impl = new NLPAnalyzerImpl4();

		String sentence = "microsoft's corporate headquarters locates in united states";

		System.out.println(impl.capitalizeProperNounTerms(sentence, null));

	}

	public static void main(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();
		NLPAnalyzerImpl4 impl = new NLPAnalyzerImpl4();

		try
		{
			for (Statement statement : statements)
			{
				//				if (statement.getId().intValue() != 4)
				//				{
				//					continue;
				//				}
				List<String> allAlternativeUnits = statement
						.getAlternativeUnits();
				List<String> allAlternativeStatements = statement
						.getAllAlternativeStatements();

				//			String alternativeUnit = allAlternativeUnits.get(0);
				//			String sentence = allAlternativeStatements.get(0);
				//
				//			System.out.println("["
				//					+ statement.getId()
				//					+ "] "
				//					+ sentence.replace(alternativeUnit, "[" + alternativeUnit
				//							+ "]"));
				//
				//			String capitalizedSentence = impl.capitalizeProperNounTerms(
				//					sentence, null);

				for (int index = 0; index < allAlternativeStatements.size(); index++)
				{
					String alternativeUnit = allAlternativeUnits.get(index);
					String sentence = allAlternativeStatements.get(index);

					String originalSentence = "["
							+ statement.getId()
							+ "] "
							+ sentence.replace(alternativeUnit, "["
									+ alternativeUnit + "]");
					// System.out.println(originalSentence);
					LOGGER.info(originalSentence);

					List<List<String>> nounSequences = new ArrayList<List<String>>();
					String capitalizedSentence = impl
							.capitalizeProperNounTerms(sentence, nounSequences);
					System.out.println(/*"> " + */capitalizedSentence);

					LOGGER.info("");
				}
				System.out.println();

			}
		}
		finally
		{
			impl.commitCache(); // DO NOT forget this!
		}

	}

	protected void commitCache()
	{
		sentenceCache.writeCache();
	}

	private static Map<String, Entry<String, List<List<String>>>> RESTORED_SENTENCE_CACHE = new ConcurrentHashMap<String, Entry<String, List<List<String>>>>();

	//	@Override
	//	protected String restoreWordCasesForSentence(String sentence,
	//			String alternativeUnit, List<List<String>> nounPhrases)
	//	{
	//		return capitalizeProperNounTerms(sentence, nounPhrases);
	//	}

	protected String restoreWordCasesForSentence(String sentence,
			String alternativeUnit, List<List<String>> nounPhrases)
	{
		Entry<String, List<List<String>>> restoredSentenceAndNPs = RESTORED_SENTENCE_CACHE
				.get(sentence);
		if (restoredSentenceAndNPs == null)
		{
			synchronized (RESTORED_SENTENCE_CACHE)
			{
				restoredSentenceAndNPs = RESTORED_SENTENCE_CACHE.get(sentence);
				if (restoredSentenceAndNPs == null)
				{
					List<List<String>> nounPhrasesToStore = new ArrayList<List<String>>();
					String resultSentence = capitalizeProperNounTerms(sentence,
							nounPhrasesToStore);

					restoredSentenceAndNPs = new SimpleEntry<String, List<List<String>>>(
							resultSentence, nounPhrasesToStore);
					RESTORED_SENTENCE_CACHE.put(sentence,
							restoredSentenceAndNPs);
				}
			}
		}

		nounPhrases.addAll(restoredSentenceAndNPs.getValue());
		return restoredSentenceAndNPs.getKey();
	}

	@Override
	protected String capitalizeProperNounTerms(String sentence,
			List<List<String>> possibleNounPhrases)
	{
		sentence = sentence.trim();
		sentence = sentence.replaceAll("\\s{2,}", " ");

		// sentence = StringUtils.capitalize(sentence);

		List<Entry<String, String>> posTagsByTerm = parseIntoPosTagByTerms(
				sentence, false); // this one is not in basic form!!
		// split by the punctuation
		List<List<Entry<String, String>>> originalPosTagsByTermOfSubSentences = splitByPunctuations(posTagsByTerm); // for chunking
		List<List<Entry<String, String>>> allNounPhrases = new ArrayList<List<Entry<String, String>>>();

		for (List<Entry<String, String>> tagsByTerm : originalPosTagsByTermOfSubSentences)
		{
			List<List<Entry<String, String>>> nounPhrases = openNLPChunker
					.getChunks(tagsByTerm, ChunkType.NP);
			allNounPhrases.addAll(nounPhrases);
		}
		allNounPhrases = mapPosTagToBasicForm(allNounPhrases);
		LOGGER.info(">>>>> NounPhrases_from_chucker\t\t\t" + allNounPhrases);
		// System.out.println(allNounPhrases);

		List<List<Entry<String, String>>> posTagsByTermOfSubSentences = mapPosTagToBasicForm(originalPosTagsByTermOfSubSentences);

		// Find the proper noun in Wikiepdia
		// sequence
		List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWiki = new ArrayList<Entry<List<Entry<String, String>>, String>>();
		// single term
		List<Entry<Entry<String, String>, String>> possibleCapitalizationsBySingleNounTermFromWiki = new ArrayList<Entry<Entry<String, String>, String>>();
		findProperNounsInWikipedia(sentence, posTagsByTermOfSubSentences,
				capitalizationsByOriginalCaseFromWiki,
				possibleCapitalizationsBySingleNounTermFromWiki);
		LOGGER.info(">>>>> ProperNoun_wiki_sequence\t\t*"
				+ capitalizationsByOriginalCaseFromWiki);
		LOGGER.info(">>>>> Candidate_ProperNoun_wiki_single\t"
				+ possibleCapitalizationsBySingleNounTermFromWiki);

		// single noun from wikipedia is not reliable, 
		// for example, "Fastest" is considered as a movie name which is capitalized; also "Become", "MCG", "Descendents"
		List<Entry<Entry<String, String>, String>> capitalizationsBySingleNounTermFromWiki = filterOutNonProperSingleNounByWordNet(possibleCapitalizationsBySingleNounTermFromWiki);
		LOGGER.info(">>>>> ProperNoun_wiki_single\t\t*"
				+ capitalizationsBySingleNounTermFromWiki);

		//		// Get all noun sequences
		//		List<List<Entry<String, String>>> nounSequences = findNounSequences(posTagsByTermOfSubSentences);
		//		LOGGER.info(">>>>> Noun_from_parser\t\t\t" + nounSequences);

		// Find those noun(sequence)s which have not been identified by Wikipedia
		List<List<Entry<String, String>>> nounPhrasesNotIdentifiedByWiki = filterOutNounSequencesIdentifiedByWiki(
		/*nounSequences*/allNounPhrases,
				capitalizationsByOriginalCaseFromWiki,
				capitalizationsBySingleNounTermFromWiki);
		LOGGER.info(">>>>> Noun_notInWiki\t\t\t"
				+ nounPhrasesNotIdentifiedByWiki);

		// Find the proper noun in WordNet
		List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWordNet = findProperNounsInWordNet(nounPhrasesNotIdentifiedByWiki);
		LOGGER.info(">>>>> ProperNoun_notInWiki_inWN\t\t*"
				+ capitalizationsByOriginalCaseFromWordNet);

		// match names within name-list
		List<Entry<List<Entry<String, String>>, String>> matchedFullNames = new ArrayList<Entry<List<Entry<String, String>>, String>>();
		List<Entry<Entry<String, String>, String>> matchedSingleNames = new ArrayList<Map.Entry<Entry<String, String>, String>>();
		for (List<Entry<String, String>> posTagByTerm : posTagsByTermOfSubSentences)
		{
			recursiveMatchTerms(trigramPersonNameMatcher,
					Collections.singletonList(posTagByTerm), matchedFullNames,
					matchedSingleNames);
		}
		LOGGER.info(">>>>> MatchedPersonName_full\t\t\t" + matchedFullNames);
		// TODO this matched single name is not used now, 
		// since it may match terms like “long”, “longest”, “kings”, “big”, “from”, “games”, “late”, “states”
		// maybe introducing the frequency of each name term may do some help, but we haven't decided it yet.
		LOGGER.info(">>>>> MatchedPersonName_single\t\t\t" + matchedSingleNames);

		// find those names only identified by name-list
		List<List<Entry<String, String>>> matchedNamesIdentifiedByNameListOnly = filterOutNamesByWIkiAndWordNet(
				matchedFullNames, capitalizationsByOriginalCaseFromWiki,
				capitalizationsBySingleNounTermFromWiki,
				capitalizationsByOriginalCaseFromWordNet);

		LOGGER.info(">>>>> PersonName_notInWiki_notInWN\t*"
				+ matchedNamesIdentifiedByNameListOnly);

		// REPLACE /////////////////////////////////////////////////////////////
		for (Entry<List<Entry<String, String>>, String> entry : capitalizationsByOriginalCaseFromWiki)
		{
			sentence = replaceMatchedWikiPhrase(sentence, entry.getKey(),
					entry.getValue());
		}

		for (Entry<Entry<String, String>, String> entry : capitalizationsBySingleNounTermFromWiki)
		{
			String target = entry.getKey().getKey();
			sentence = sentence.replace(target, entry.getValue());
		}

		for (Entry<List<Entry<String, String>>, String> entry : capitalizationsByOriginalCaseFromWordNet)
		{
			String target = concatenateTerms(entry.getKey());
			sentence = sentence.replace(target, entry.getValue());
		}

		for (List<Entry<String, String>> entry : matchedNamesIdentifiedByNameListOnly)
		{
			String target = concatenateTerms(entry);
			String replacement = WordUtils.capitalize(target);
			sentence = sentence.replace(target, replacement);
		}
		////////////////////////////////////////////////////////////////////////

		//		// re-parse it /////////////////////////////////////////////////////////
		//		posTagsByTerm = parseIntoPosTagByTerms(sentence);
		//		// split by the punctuation
		//		posTagsByTermOfSubSentences = splitByPunctuations(posTagsByTerm);
		//		nounSequences = findNounSequences(posTagsByTermOfSubSentences);
		//		LOGGER.info(">>>>> Re-parsed_Noun_from_parser\t\t" + nounSequences);

		// fill the parameter nounPhrases with nounSequences
		if (possibleNounPhrases != null)
		{
			// TODO more sophisticated method may be needed, 
			// but now it is OK, since the AU matching is using the same parser for nouns
			for (List<Entry<String, String>> sequence : /*nounSequences*/allNounPhrases)
			{
				List<String> nounSequence = new ArrayList<String>(
						sequence.size());
				possibleNounPhrases.add(nounSequence);
				for (Entry<String, String> entry : sequence)
				{
					nounSequence.add(entry.getKey());
				}
			}
		}

		return sentence;
		//return StringUtils.capitalize(sentence);
	}

	private String replaceMatchedWikiPhrase(String sentence,
			List<Entry<String, String>> posTagsByTerm, String replacement)
	{
		String target = concatenateTerms(posTagsByTerm);

		if (sentence.contains(target))
		{
			sentence = sentence.replace(target, replacement);
		}
		else
		{
			target = concatenateTerms(posTagsByTerm, false, "");
			replacement = replacement.replace(" ", "");

			// Assert.isTrue(sentence.contains(target));
			if (!sentence.contains(target))
			{
				LOGGER.warn("[" + sentence + "] should contains [" + target
						+ "], but not. ");
			}
			sentence = sentence.replace(target, replacement);
		}

		return sentence;
	}

	@SuppressWarnings("unchecked")
	private List<List<Entry<String, String>>> filterOutNamesByWIkiAndWordNet(
			List<Entry<List<Entry<String, String>>, String>> matchedNames,
			List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWiki,
			List<Entry<Entry<String, String>, String>> capitalizationsBySingleNounTermFromWiki,
			List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWordNet)
	{
		capitalizationsByOriginalCaseFromWiki = (List<Entry<List<Entry<String, String>>, String>>) ((ArrayList<Entry<List<Entry<String, String>>, String>>) capitalizationsByOriginalCaseFromWiki)
				.clone();
		// combine noun-sequence and single-noun
		for (Entry<Entry<String, String>, String> entry : capitalizationsBySingleNounTermFromWiki)
		{
			capitalizationsByOriginalCaseFromWiki
					.add(new SimpleEntry<List<Entry<String, String>>, String>(
							Collections.singletonList(entry.getKey()), entry
									.getValue()));
		}

		capitalizationsByOriginalCaseFromWiki
				.addAll(capitalizationsByOriginalCaseFromWordNet);

		List<List<Entry<String, String>>> squencesFromNameList = new ArrayList<List<Entry<String, String>>>(
				matchedNames.size());
		for (Entry<List<Entry<String, String>>, String> entry : matchedNames)
		{
			squencesFromNameList.add(entry.getKey());
		}

		return filterOutOverlappings(squencesFromNameList,
				capitalizationsByOriginalCaseFromWiki);

	}

	private List<Entry<Entry<String, String>, String>> filterOutNonProperSingleNounByWordNet(
			List<Entry<Entry<String, String>, String>> possibleCapitalizationsBySingleNounTermFromWiki)
	{
		List<Entry<Entry<String, String>, String>> result = new ArrayList<Entry<Entry<String, String>, String>>();
		for (Entry<Entry<String, String>, String> entry : possibleCapitalizationsBySingleNounTermFromWiki)
		{
			String term = entry.getValue();
			String termInWordNet = wordNetReader.retrieveTermInStandardCase(
					term.toLowerCase(), POS.NOUN); // use lowercase to search if capitalized return
			// if the term doesn't exist in wordnet, we can NOT trust wiki (Become)
			if (termInWordNet != null && term.equals(termInWordNet)) // same case, 
			{
				result.add(entry);
			}
		}

		return result;
	}

	private List<Entry<List<Entry<String, String>>, String>> findProperNounsInWordNet(
			List<List<Entry<String, String>>> posTagsByTermOfNounPhrases)
	{
		List<Entry<List<Entry<String, String>>, String>> result = new ArrayList<Entry<List<Entry<String, String>>, String>>();

		for (List<Entry<String, String>> nounSequence : posTagsByTermOfNounPhrases)
		{
			if (nounSequence.size() > 1) // more than one term (i.e. phrase)
			{
				String nounSequenceString = concatenateTerms(nounSequence);
				String nounSequenceInWordNet = wordNetReader
						.retrieveTermInStandardCase(nounSequenceString,
								POS.NOUN);
				if (nounSequenceInWordNet != null
						&& inDifferentCases(nounSequenceString,
								nounSequenceInWordNet)) // matched
				{
					result.add(new SimpleEntry<List<Entry<String, String>>, String>(
							nounSequence, nounSequenceInWordNet));
					continue;
				}
			}

			// try each individual noun term
			for (Entry<String, String> entry : nounSequence)
			{
				String posTag = entry.getValue();
				if (!POSTAG_NOUN.equals(posTag)) // individual term MUST be noun!
				{
					continue;
				}

				String term = entry.getKey();
				String termInWordNet = wordNetReader
						.retrieveTermInStandardCase(term, POS.NOUN);
				if (termInWordNet != null
						&& inDifferentCases(term, termInWordNet))
				{
					result.add(new SimpleEntry<List<Entry<String, String>>, String>(
							Collections.singletonList(entry), termInWordNet));
				}
			}

		}

		return result;
	}

	private boolean inDifferentCases(String one, String other)
	{
		return one.toLowerCase().equals(other.toLowerCase())
				&& !one.equals(other);
	}

	@SuppressWarnings("unchecked")
	private List<List<Entry<String, String>>> filterOutNounSequencesIdentifiedByWiki(
			List<List<Entry<String, String>>> nounSequences,
			List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWiki,
			List<Entry<Entry<String, String>, String>> capitalizationsBySingleNounTermFromWiki)
	{
		capitalizationsByOriginalCaseFromWiki = (List<Entry<List<Entry<String, String>>, String>>) ((ArrayList<Entry<List<Entry<String, String>>, String>>) capitalizationsByOriginalCaseFromWiki)
				.clone();
		// combine noun-sequence and single-noun
		for (Entry<Entry<String, String>, String> entry : capitalizationsBySingleNounTermFromWiki)
		{
			capitalizationsByOriginalCaseFromWiki
					.add(new SimpleEntry<List<Entry<String, String>>, String>(
							Collections.singletonList(entry.getKey()), entry
									.getValue()));
		}

		////////////////////////////////////////////////////////////////////////
		return filterOutOverlappings(nounSequences,
				capitalizationsByOriginalCaseFromWiki);
	}

	private List<List<Entry<String, String>>> filterOutOverlappings(
			List<List<Entry<String, String>>> nounSequences,
			List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseBase)
	{
		if (capitalizationsByOriginalCaseBase.isEmpty()) // wikiepda has not identified any 
		{
			return nounSequences;
		}

		List<List<Entry<String, String>>> squencesFromWiki = new ArrayList<List<Entry<String, String>>>(
				capitalizationsByOriginalCaseBase.size());
		for (Entry<List<Entry<String, String>>, String> entry : capitalizationsByOriginalCaseBase)
		{
			squencesFromWiki.add(entry.getKey());
		}

		List<List<Entry<String, String>>> result = new ArrayList<List<Entry<String, String>>>();
		ns: for (List<Entry<String, String>> nounSequence : nounSequences)
		{
			// check if overlapping with any sequence from wikipedia
			for (List<Entry<String, String>> squenceFromWiki : squencesFromWiki)
			{
				if (!Collections.disjoint(nounSequence, squenceFromWiki))
				{
					// overlap
					continue ns;
				}
			}

			// no overlapping
			result.add(nounSequence);
		}

		return result;
	}

	/*private List<List<Entry<String, String>>> findNounSequences(
			List<List<Entry<String, String>>> posTagsByTermOfSubSentences)
	{
		List<List<Entry<String, String>>> result = new ArrayList<List<Entry<String, String>>>();

		for (List<Entry<String, String>> posTagsByTermOfSubSentence : posTagsByTermOfSubSentences)
		{
			List<Entry<String, String>> nounSequence = null;
			for (Entry<String, String> posTagByTerm : posTagsByTermOfSubSentence)
			{
				// String term = posTagByTerm.getKey();
				String posTag = posTagByTerm.getValue();

				if (!POSTAG_NOUN.equals(posTag)
						&& !POSTAG_POSSESSIVE.equals(posTag))
				{
					if (nounSequence == null || nounSequence.isEmpty())
					{
						continue;
					}

					nounSequence = new ArrayList<Entry<String, String>>();
					result.add(nounSequence);
				}
				else
				{
					if (nounSequence == null) // start with noun
					{
						nounSequence = new ArrayList<Entry<String, String>>();
						result.add(nounSequence);
					}

					nounSequence.add(posTagByTerm);
				}
			}
		}

		// in case the last one is empty
		if (!result.isEmpty() && result.get(result.size() - 1).isEmpty())
		{
			result.remove(result.size() - 1);
		}

		return result;
	}*/

	/**
	 * @param capitalizationsBySingleNounTermFromWiki 
	 * @param capitalizationsByOriginalCaseFromWiki 
	 * @return <originalCase, capitalization>
	 */
	private void findProperNounsInWikipedia(
			String sentence,
			List<List<Entry<String, String>>> posTagsByTermOfSubSentences,
			List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWiki,
			List<Entry<Entry<String, String>, String>> capitalizationsBySingleNounTermFromWiki)
	{
		List<Entry<List<Entry<String, String>>, MatchedQueryKey>> matchedSequenceInfo = null;
		List<Entry<Entry<String, String>, MatchedQueryKey>> matchedSingleInfo = null;

		Entry<List<Entry<List<Entry<String, String>>, MatchedQueryKey>>, List<Entry<Entry<String, String>, MatchedQueryKey>>> pair = sentenceCache
				.retrieveWikipeidaSentenceCache(sentence);
		if (pair != null)
		{
			matchedSequenceInfo = pair.getKey();
			matchedSingleInfo = pair.getValue();
		}
		else
		{
			matchedSequenceInfo = new ArrayList<Entry<List<Entry<String, String>>, MatchedQueryKey>>();
			matchedSingleInfo = new ArrayList<Entry<Entry<String, String>, MatchedQueryKey>>();
			for (List<Entry<String, String>> posTagsByTermOfSubSentence : posTagsByTermOfSubSentences)
			{
				recursiveMatchTerms(
						wikipediaKeyWordsMatcher.setWholeSentence(sentence),
						Collections.singletonList(posTagsByTermOfSubSentence),
						matchedSequenceInfo, matchedSingleInfo);
			}

			sentenceCache.addToCache(sentence, matchedSequenceInfo,
					matchedSingleInfo);
		}

		//		System.out.println();
		// noun sequences
		for (Entry<List<Entry<String, String>>, MatchedQueryKey> entry : matchedSequenceInfo)
		{
			List<Entry<String, String>> posTagsByMatchedTerm = entry.getKey();
			MatchedQueryKey matchedQueryKey = entry.getValue();

			Entry<List<Entry<String, String>>, String> properNounByOriginalCase = checkWhetherProperNoun(
					posTagsByMatchedTerm, matchedQueryKey);

			if (properNounByOriginalCase == null)
			{
				continue;
			}
			capitalizationsByOriginalCaseFromWiki.add(properNounByOriginalCase);
		}

		// single noun
		for (Entry<Entry<String, String>, MatchedQueryKey> entry : matchedSingleInfo)
		{
			Entry<String, String> posTagByTerm = entry.getKey();
			MatchedQueryKey matchedQueryKey = entry.getValue();

			Entry<List<Entry<String, String>>, String> properSingleNounByOriginalCase = checkWhetherProperNoun(
					Collections.singletonList(posTagByTerm), matchedQueryKey);
			if (properSingleNounByOriginalCase == null)
			{
				continue;
			}

			Assert.isTrue(properSingleNounByOriginalCase.getKey().size() == 1);
			capitalizationsBySingleNounTermFromWiki
					.add(new SimpleEntry<Entry<String, String>, String>(
							posTagByTerm, properSingleNounByOriginalCase
									.getValue()));

			//			capitalizationsByOriginalCaseFromWiki
			//					.add(new SimpleEntry<List<Entry<String, String>>, String>(
			//							Collections
			//									.singletonList(properSingleNounByOriginalCase
			//											.getKey()),
			//							properSingleNounByOriginalCase.getValue()));
		}

	}

	//	// single term
	//	private Entry<Entry<String, String>, String> checkWhetherProperNoun(
	//			Entry<String, String> posTagByTerm, MatchedQueryKey matchedQueryKey)
	//	{
	//		String content = extractContentFromWikiPage(matchedQueryKey);
	//		if (content == null)
	//		{
	//			return null;
	//		}
	//
	//		String term = posTagByTerm.getKey();
	//
	//		if (StringUtils.containsIgnoreCase(content, term + " ")
	//				&& !content.contains(term + " "))
	//		{
	//			String properNoun = extractProperNoun(content, term);
	//			return new SimpleEntry<Entry<String, String>, String>(posTagByTerm,
	//					properNoun);
	//		}
	//
	//		return null;
	//	}
	//
	//	// term sequence
	//	private Entry<List<Entry<String, String>>, String> checkWhetherProperNoun(
	//			List<Entry<String, String>> posTagsByMatchedTerm,
	//			MatchedQueryKey matchedQueryKey)
	//	{
	//		String content = extractContentFromWikiPage(matchedQueryKey);
	//		if (content == null)
	//		{
	//			return null;
	//		}
	//
	//		String matchedTerms = concatenateTerms(posTagsByMatchedTerm, false)
	//				.toLowerCase();
	//		String matchedTermsWithoutPossessiveCase = concatenateTerms(
	//				posTagsByMatchedTerm, true).toLowerCase();
	//
	//		if (StringUtils.containsIgnoreCase(content, matchedTerms + " ")
	//				&& !content.contains(matchedTerms + " "))
	//		{
	//			String properNoun = extractProperNoun(content, matchedTerms);
	//			return new SimpleEntry<List<Entry<String, String>>, String>(
	//					posTagsByMatchedTerm, properNoun);
	//		}
	//		// be aware of the POSSESSIVE symbol
	//		else if (!matchedTerms.equals(matchedTermsWithoutPossessiveCase)
	//				&& StringUtils.containsIgnoreCase(content,
	//						matchedTermsWithoutPossessiveCase + " ")
	//				&& !content.contains(matchedTermsWithoutPossessiveCase + " "))
	//		{
	//			String properNoun = extractProperNoun(content,
	//					matchedTermsWithoutPossessiveCase);
	//			return new SimpleEntry<List<Entry<String, String>>, String>(
	//					posTagsByMatchedTerm, properNoun);
	//		}
	//
	//		return null;
	//	}

	//	private Entry<List<Entry<String, String>>, String> checkWhetherProperNoun_backup(
	//			List<Entry<String, String>> posTagsByMatchedTerm,
	//			MatchedQueryKey matchedQueryKey)
	//	{
	//		String content = extractContentFromWikiPage(matchedQueryKey);
	//		if (content == null)
	//		{
	//			return null;
	//		}
	//
	//		String matchedTerms = matchedQueryKey.getKeyWord().replaceAll("_", " ")
	//				.trim();
	//		boolean isSingleTerm = (matchedTerms.indexOf(" ") == -1);
	//
	//		// actually it should already been capitalized since it is the title of wiki page
	//		boolean shouldCapitalized = shouldCapitalized(matchedTerms);
	//		if (!isSingleTerm && !shouldCapitalized)
	//		{
	//			return null;
	//		}
	//
	//		int lastIndexOf = content.lastIndexOf(matchedTerms/* + " "*/);
	//		// if there's one capitalized
	//		//		if ((isSingleTerm && lastIndexOf > 0) // single term cannot be the first one
	//		//				|| (!isSingleTerm  && lastIndexOf > 0) // not not capitalized can not be the first 
	//		//				|| (!isSingleTerm && allCapitalized && lastIndexOf > -1)) // more than one all capitalized term can be the first
	//
	//		String lowerCasedMatchedTerms = matchedTerms.toLowerCase();
	//		if (!createMatchingPattern(lowerCasedMatchedTerms).matcher(content)
	//				.find() // doesn't contain all lower case form
	//				&& ((isSingleTerm && lastIndexOf > 0) // single term cannot be the first one
	//				|| (!isSingleTerm && lastIndexOf > -1))) // more than one all capitalized term can be the first
	//		{
	//			StringBuilder properNoun = new StringBuilder();
	//			for (int index = 0; index < posTagsByMatchedTerm.size(); index++)
	//			{
	//				String originalTerm = posTagsByMatchedTerm.get(index).getKey()
	//						.toLowerCase();
	//				String posTag = posTagsByMatchedTerm.get(index).getValue();
	//
	//				int beginIndex = lowerCasedMatchedTerms.indexOf(originalTerm);
	//
	//				String captializedMatchedTerm = null;
	//				if (POSTAG_DETERMINER.equals(posTag))
	//				{
	//					captializedMatchedTerm = originalTerm;
	//				}
	//				else if (beginIndex != -1) // if they are in the same literal form
	//				{
	//					// accept the form in Wikipedia page since it could be acronym like IBM
	//					// which is not just capitalize the first character
	//					captializedMatchedTerm = matchedTerms.substring(beginIndex,
	//							beginIndex + originalTerm.length());
	//				}
	//				else
	//				{ // else if the original term is different with the term in Wikipedia
	//					int lastIndexOfUnMatchedTerm = content.toLowerCase()
	//							.lastIndexOf(originalTerm);
	//					if (lastIndexOfUnMatchedTerm != -1)
	//					{
	//						captializedMatchedTerm = content.substring(
	//								lastIndexOfUnMatchedTerm,
	//								lastIndexOfUnMatchedTerm
	//										+ originalTerm.length());
	//					}
	//					else
	//					{
	//						captializedMatchedTerm = originalTerm;
	//					}
	//				}
	//
	//				if (index > 0 && !POSTAG_POSSESSIVE.equals(posTag)) // POS tag is not POSSESSIVE
	//				{
	//					properNoun.append(" ");
	//				}
	//				properNoun.append(captializedMatchedTerm);
	//			}
	//
	//			return new SimpleEntry<List<Entry<String, String>>, String>(
	//					posTagsByMatchedTerm, properNoun.toString());
	//
	//		}
	//
	//		return null;
	//	}

	private Pattern createMatchingPattern(String lowerCasedMatchedTerms)
	{
		return Pattern.compile(createMatchingSequqnce(lowerCasedMatchedTerms));
	}

	// term sequence
	private Entry<List<Entry<String, String>>, String> checkWhetherProperNoun(
			List<Entry<String, String>> posTagsByMatchedTerm,
			MatchedQueryKey matchedQueryKey)
	{
		Assert.isTrue(
				!posTagsByMatchedTerm.isEmpty(),
				"There must be terms for the matched page: "
						+ matchedQueryKey.getCertainPageUrl());

		if (isSongName(posTagsByMatchedTerm, matchedQueryKey))
		{
			return null;
		}

		String content = extractContentFromWikiPage(matchedQueryKey);
		if (content == null)
		{
			return null;
		}
		String contentInLowerCase = content.toLowerCase();

		String title = matchedQueryKey.getKeyWord().replaceAll("_", " ").trim();
		String titleInLowerCase = title.toLowerCase();
		boolean titleContainsMultipleTerms = title.contains(" ");
		boolean titleShouldCapitalized = shouldCapitalized(title);

		boolean capitalized = false;
		StringBuilder properNoun = new StringBuilder();
		String termInResult = null;
		Matcher matcher = null;

		// start from second term
		for (int index = 1; index < posTagsByMatchedTerm.size(); index++)
		{
			String posTag = posTagsByMatchedTerm.get(index).getValue();
			String matchedTermInLowerCase = posTagsByMatchedTerm.get(index)
					.getKey().toLowerCase();

			if (POSTAG_DETERMINER.equals(posTag))
			{
				termInResult = matchedTermInLowerCase;
			}
			else
			{
				Pattern pattern4MatchedTermInLowerCase = createMatchingPattern(matchedTermInLowerCase);
				if (titleContainsMultipleTerms //more than one terms
						&& (matcher = pattern4MatchedTermInLowerCase
								.matcher(titleInLowerCase)).find()) // in title 
				{
					if (titleShouldCapitalized) // title-terms are capitalized, and more than one terms
					{
						int lastStartIndex = matcher.start();
						int lastEndIndex = matcher.end();

						termInResult = title.substring(lastStartIndex,
								lastEndIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
						capitalized = true;
					}
					else
					// but not capitalized
					{
						termInResult = matchedTermInLowerCase;
					}
				}
				else if (!pattern4MatchedTermInLowerCase.matcher(content)
						.find()
						&& (matcher = pattern4MatchedTermInLowerCase
								.matcher(contentInLowerCase)).find()) // All appearances are capitalized
				{
					int lastStartIndex = -1;
					int lastEndIndex = -1;
					do
					{
						// until the last one is found
						lastStartIndex = matcher.start();
						lastEndIndex = matcher.end();
					}
					while (matcher.find());

					termInResult = content.substring(lastStartIndex,
							lastEndIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
					capitalized = true;
				}
				else
				{
					termInResult = matchedTermInLowerCase;
				}
			}

			if (!POSTAG_POSSESSIVE.equals(posTag)) // POS tag is not POSSESSIVE
			{
				properNoun.append(" ");
			}
			properNoun.append(termInResult);
		}

		// first term
		String posTag = posTagsByMatchedTerm.get(0).getValue();
		String matchedTermInLowerCase = posTagsByMatchedTerm.get(0).getKey()
				.toLowerCase();

		if (POSTAG_DETERMINER.equals(posTag))
		{
			termInResult = matchedTermInLowerCase;
		}
		else
		{
			Pattern pattern4MatchedTermInLowerCase = createMatchingPattern(matchedTermInLowerCase);
			if (titleContainsMultipleTerms //more than one terms
					&& (matcher = pattern4MatchedTermInLowerCase
							.matcher(titleInLowerCase)).find()
					&& titleShouldCapitalized) // in title
			{
				if (titleShouldCapitalized) // title-terms are capitalized, and more than one terms
				{
					int lastStartIndex = matcher.start();
					int lastEndIndex = matcher.end();

					termInResult = title
							.substring(lastStartIndex, lastEndIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
					capitalized = true;
				}
				else
				// but not capitalized
				{
					termInResult = matchedTermInLowerCase;
				}
			}
			else if (!pattern4MatchedTermInLowerCase.matcher(content).find()
					&& (matcher = pattern4MatchedTermInLowerCase
							.matcher(contentInLowerCase)).find()) // All appearances are capitalized
			{
				int lastStartIndex = -1;
				int lastEndIndex = -1;
				do
				{
					// until the last one is found
					lastStartIndex = matcher.start();
					lastEndIndex = matcher.end();
				}
				while (matcher.find());

				if (!capitalized && lastStartIndex == 0)
				{
					// nothing capitalized so far, and the only match is the first term
					// do not trust it
					termInResult = matchedTermInLowerCase;
				}
				else
				{
					termInResult = content.substring(lastStartIndex,
							lastEndIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
					capitalized = true;
				}
			}
			else
			{
				termInResult = matchedTermInLowerCase;
			}
		}

		String result = termInResult + properNoun.toString();
		if (capitalized)
		{
			return new SimpleEntry<List<Entry<String, String>>, String>(
					posTagsByMatchedTerm, result);
		}

		return null;
	}

	private boolean isSongName(
			List<Entry<String, String>> posTagsByMatchedTerm,
			MatchedQueryKey matchedQueryKey)
	{
		for (Entry<String, String> posTagByTerm : posTagsByMatchedTerm)
		{
			if (POSTAG_NOUN.equals(posTagByTerm.getValue()))
			{
				return false; // if contains Noun, we can not make sure if the terms really mean a song name
			}
		}

		for (String category : matchedQueryKey.getCategories())
		{
			category = category.toLowerCase(Locale.US);

			for (String categoryKeyWord : CATEGORY_KEYWORDS_SONG)
			{
				if (category.contains(categoryKeyWord))
				{
					return true;
				}
			}

		}

		return false;
	}

	//	private Entry<List<Entry<String, String>>, String> checkWhetherProperNoun_backup_2(
	//			List<Entry<String, String>> posTagsByMatchedTerm,
	//			MatchedQueryKey matchedQueryKey)
	//	{
	//		Assert.isTrue(
	//				!posTagsByMatchedTerm.isEmpty(),
	//				"There must be terms for the matched page: "
	//						+ matchedQueryKey.getCertainPageUrl());
	//		String content = extractContentFromWikiPage(matchedQueryKey);
	//		if (content == null)
	//		{
	//			return null;
	//		}
	//
	//		String contentInLowerCase = content.toLowerCase();
	//		String matchedSequenceInLowerCase = concatenateTerms(
	//				posTagsByMatchedTerm).toLowerCase();
	//
	//		boolean differentCaseAtAllAppearances = true;
	//		String matchedSequenceInOriginalCase = null;
	//
	//		Matcher matcher = createMatchingPattern(matchedSequenceInLowerCase)
	//				.matcher(contentInLowerCase);
	//		while (matcher.find())
	//		{
	//			int startIndex = matcher.start();
	//			int endIndex = matcher.end();
	//			matchedSequenceInOriginalCase = content.substring(startIndex,
	//					endIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
	//			if (matchedSequenceInOriginalCase
	//					.equals(matchedSequenceInLowerCase))
	//			{
	//				differentCaseAtAllAppearances = false;
	//				break;
	//			}
	//		}
	//
	//		if (differentCaseAtAllAppearances
	//				&& matchedSequenceInOriginalCase != null)
	//		{
	//			return new SimpleEntry<List<Entry<String, String>>, String>(
	//					posTagsByMatchedTerm, matchedSequenceInOriginalCase);
	//		}
	//
	//		if (matchedSequenceInLowerCase.indexOf(" ") == -1) // single term
	//		{
	//			// nothing matched for single term or different cases for this single term
	//			return null;
	//		}
	//
	//		// here more than one term, try each one
	//		StringBuilder properNoun = new StringBuilder();
	//		String termInResult = null;
	//		boolean hasCapitalied = false;
	//
	//		// from the second one
	//		for (int index = 1; index < posTagsByMatchedTerm.size(); index++)
	//		{
	//			String posTag = posTagsByMatchedTerm.get(index).getValue();
	//			String matchedTermInLowerCase = posTagsByMatchedTerm.get(index)
	//					.getKey().toLowerCase();
	//
	//			if (POSTAG_DETERMINER.equals(posTag))
	//			{
	//				termInResult = matchedTermInLowerCase;
	//			}
	//			//			else if (!content.contains(matchedTermInLowerCase)
	//			//					&& lastIndexOfTermInLowerCaseContent > 0)
	//			else if (!createMatchingPattern(matchedTermInLowerCase).matcher(
	//					content).find()
	//					&& (matcher = createMatchingPattern(matchedTermInLowerCase)
	//							.matcher(contentInLowerCase)).find())
	//			{
	//				// all the term are NOT in lower case
	//				int lastStartIndex = -1;
	//				int lastEndIndex = -1;
	//
	//				do
	//				{
	//					// until the last one is found
	//					lastStartIndex = matcher.start();
	//					lastEndIndex = matcher.end();
	//				}
	//				while (matcher.find());
	//
	//				termInResult = content.substring(lastStartIndex, lastEndIndex)
	//						.trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
	//				hasCapitalied = true;
	//			}
	//			else
	//			{
	//				termInResult = matchedTermInLowerCase;
	//			}
	//
	//			if (/*index > 0 && */!POSTAG_POSSESSIVE.equals(posTag)) // POS tag is not POSSESSIVE
	//			{
	//				properNoun.append(" ");
	//			}
	//			properNoun.append(termInResult);
	//		}
	//
	//		// first one
	//		String posTag = posTagsByMatchedTerm.get(0).getValue();
	//		String matchedTermInLowerCase = posTagsByMatchedTerm.get(0).getKey()
	//				.toLowerCase();
	//
	//		if (POSTAG_DETERMINER.equals(posTag))
	//		{
	//			termInResult = matchedTermInLowerCase;
	//		}
	//		//		else if (!content.contains(matchedTermInLowerCase)
	//		//				&& lastIndexOfTermInLowerCaseContent > (hasCapitalied ? -1 : 0))
	//		else if (!createMatchingPattern(matchedTermInLowerCase)
	//				.matcher(content).find()
	//				&& (matcher = createMatchingPattern(matchedTermInLowerCase)
	//						.matcher(contentInLowerCase)).find())
	//		{
	//			int lastStartIndex = -1;
	//			int lastEndIndex = -1;
	//			do
	//			{
	//				// until the last one is found
	//				lastStartIndex = matcher.start();
	//				lastEndIndex = matcher.end();
	//			}
	//			while (matcher.find());
	//
	//			if (!hasCapitalied && lastStartIndex == 0)
	//			{
	//				// nothing capitalized so far, and the only match is the first term
	//				// do not trust it
	//				termInResult = matchedTermInLowerCase;
	//			}
	//			else
	//			{
	//				termInResult = content.substring(lastStartIndex, lastEndIndex)
	//						.trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
	//				hasCapitalied = true;
	//			}
	//		}
	//		else
	//		{
	//			termInResult = matchedTermInLowerCase;
	//		}
	//
	//		String result = termInResult + properNoun.toString();
	//		if (hasCapitalied)
	//		{
	//			return new SimpleEntry<List<Entry<String, String>>, String>(
	//					posTagsByMatchedTerm, result);
	//		}
	//
	//		return null;
	//	}

	private String createMatchingSequqnce(String matchedTermInLowerCase)
	{
		return REGEX_TERM_LEFT_BOUND
				+ matchedTermInLowerCase.replace(".", "\\.")
				+ REGEX_TERM_RIGHT_BOUND;
	}

	private boolean shouldCapitalized(String matchedTerms)
	{
		String[] terms = matchedTerms.split(" ");
		for (int index = 1; index < terms.length; index++)
		{
			if (Character.isUpperCase(terms[index].charAt(0)))
			{
				return true;
			}
		}

		return false;
	}

	private String extractContentFromWikiPage(MatchedQueryKey matchedQueryKey)
	{
		UrlWithDescription urlWithDescription = new UrlWithDescription(
				matchedQueryKey.getCertainPageUrl(), null,
				matchedQueryKey.getCategories());

		List<Segment> segments = wikipediaContentExtractor
				.extractSegmentsFromWikipedia(urlWithDescription, true);
		if (segments == null || segments.isEmpty())
		{
			return null;
		}

		Segment firstSegment = segments.get(0);
		// replace the punctuation
		//		String content = firstSegment.toString().replace(", ", " ")
		//				.replace(". ", " ");

		// 		String content = firstSegment.toString(); 
		//		return content;
		// NO TABLE!
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(matchedQueryKey.getKeyWord().replaceAll("_", " "))
				.append("\n");

		if (firstSegment.getHeading() != null)
		{
			stringBuilder.append(firstSegment.getHeading()).append("\n");
		}

		if (firstSegment.getParagraphs() != null)
		{
			for (Paragraph paragraph : firstSegment.getParagraphs())
			{
				stringBuilder.append(paragraph).append("\n");
			}
		}

		return removeDiacritics(stringBuilder.toString());
	}

	//	private String extractProperNoun(String content, String matchedTerms)
	//	{
	//		int index = content.toLowerCase().lastIndexOf(
	//				matchedTerms.toLowerCase() + " ");
	//		return content.substring(index, index + matchedTerms.length());
	//	}

	public List<List<Entry<String, String>>> splitByPunctuations(
			List<Entry<String, String>> posTagsByTerm)
	{
		List<List<Entry<String, String>>> result = new ArrayList<List<Entry<String, String>>>();

		List<Entry<String, String>> subSentence = null;
		boolean newSubSentence = true;
		for (Entry<String, String> pair : posTagsByTerm)
		{
			if (!isPunctuation(pair.getValue()))
			{
				if (newSubSentence)
				{
					subSentence = new ArrayList<Entry<String, String>>();
					result.add(subSentence);
					newSubSentence = false;
				}

				subSentence.add(pair);
			}
			else
			{
				newSubSentence = true;
			}
		}

		return result;
	}

	private List<List<Entry<String, String>>> mapPosTagToBasicForm(
			List<List<Entry<String, String>>> originalPosTagsByTermOfSubSentences)
	{
		List<List<Entry<String, String>>> result = new ArrayList<List<Entry<String, String>>>(
				originalPosTagsByTermOfSubSentences.size());

		for (List<Entry<String, String>> originalSubSentence : originalPosTagsByTermOfSubSentences)
		{
			List<Entry<String, String>> resultSubSentence = new ArrayList<Entry<String, String>>(
					originalSubSentence.size());
			for (Entry<String, String> entry : originalSubSentence)
			{
				resultSubSentence.add(new SimpleEntry<String, String>(entry
						.getKey(), mapPosTagToBasicForm(entry.getValue())));
			}

			result.add(resultSubSentence);
		}

		return result;
	}

	//	private List<Entry<String, String>> parseIntoPosTagByTerms(String sentence)
	//	{
	//		return parseIntoPosTagByTerms(sentence, true);
	//	}

	public List<Entry<String, String>> parseIntoPosTagByTerms(String sentence,
			boolean mapTagToBasicForm)
	{
		Tree parsedTree = lexicalizedParser.apply(sentence);
		List<TaggedWord> taggedWords = parsedTree.taggedYield();

		List<Entry<String, String>> result = new ArrayList<Entry<String, String>>();
		for (TaggedWord taggedWord : taggedWords)
		{
			String posTag = mapTagToBasicForm ? mapPosTagToBasicForm(taggedWord
					.tag()) : taggedWord.tag();
			result.add(new SimpleEntry<String, String>(taggedWord.word(),
					posTag));
		}

		return result;
	}

	private <T> void recursiveMatchTerms(RecursiveMatcher<T> matcher,
			List<List<Entry<String, String>>> currentLevelPosTagByTermList,
			List<Entry<List<Entry<String, String>>, T>> matchedSequenceInfo,
			List<Entry<Entry<String, String>, T>> matchedSingleInfo)
	{
		int currentLevelSize = currentLevelPosTagByTermList.size();

		for (int index = 0; index < currentLevelPosTagByTermList.size(); index++)
		{
			List<Entry<String, String>> currentLevelPosTagsByTerm = currentLevelPosTagByTermList
					.get(index);

			int currentLevelSequenceLength = currentLevelPosTagsByTerm.size();
			if (currentLevelSequenceLength < 1)
			{
				return;
			}

			//			System.out.println(currentLevelSequenceLength + " "
			//					+ concatenateTerms(currentLevelPosTagsByTerm));
			if (currentLevelSequenceLength == 1)
			{
				Entry<String, String> posTagByTerm = currentLevelPosTagsByTerm
						.get(0);
				String term = posTagByTerm.getKey();

				String posTag = posTagByTerm.getValue();
				if (!isPunctuation(posTag)
						&& !POSTAG_POSSESSIVE.equals(posTag)
						&& !StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(term
								.toLowerCase())
						&& !PARENTHESES_VALUES.contains(term.toLowerCase()))
				{
					if (matcher.isMatched(currentLevelPosTagsByTerm))
					{
						matchedSingleInfo
								.add(new SimpleEntry<Entry<String, String>, T>(
										posTagByTerm, matcher.getMatchedInfo()));
					}

				}

				continue; // there's only one term in current level
			}

			if (isEitherSideStopWordOrParentheses(currentLevelPosTagsByTerm))
			{
				continue;
			}

			if (matcher.isMatched(currentLevelPosTagsByTerm))
			{
				// matched!
				matchedSequenceInfo
						.add(new SimpleEntry<List<Entry<String, String>>, T>(
								currentLevelPosTagsByTerm, matcher
										.getMatchedInfo()));

				// ahead remains
				int currentLevelAheadSize = index - currentLevelSequenceLength
						+ 1;
				if (currentLevelAheadSize > 0)
				{
					List<List<Entry<String, String>>> nextLevelAheadPosTagByTermList = new ArrayList<List<Entry<String, String>>>(
							currentLevelAheadSize + 1);
					List<Entry<String, String>> currentLevelAheadSequence = null;
					List<Entry<String, String>> nextLevelAheadSequence;
					for (int i = 0; i < currentLevelAheadSize; i++)
					{
						currentLevelAheadSequence = currentLevelPosTagByTermList
								.get(i);
						nextLevelAheadSequence = new ArrayList<Entry<String, String>>(
								currentLevelAheadSequence.subList(0,
										currentLevelAheadSequence.size() - 1));

						nextLevelAheadPosTagByTermList
								.add(nextLevelAheadSequence);
					}

					nextLevelAheadSequence = new ArrayList<Entry<String, String>>(
							currentLevelAheadSequence.subList(1,
									currentLevelAheadSequence.size()));
					nextLevelAheadPosTagByTermList.add(nextLevelAheadSequence);

					// recursively invoke next level
					recursiveMatchTerms(matcher,
							nextLevelAheadPosTagByTermList,
							matchedSequenceInfo, matchedSingleInfo);
				}
				else
				{
					List<Entry<String, String>> firstInCurrentLevel = currentLevelPosTagByTermList
							.get(0);
					List<Entry<String, String>> nextLevelAheadPosTagsByTerm = new ArrayList<Entry<String, String>>(
							firstInCurrentLevel.subList(0, index));
					recursiveMatchTerms(
							matcher,
							Collections
									.singletonList(nextLevelAheadPosTagsByTerm),
							matchedSequenceInfo, matchedSingleInfo);
				}

				// after remains
				int currentLevelRemainSize = currentLevelSize
						- (index + currentLevelSequenceLength);
				if (currentLevelRemainSize > 0) // current level remains
				{
					List<List<Entry<String, String>>> currentLevelRemainingPosTagByTermList = new ArrayList<List<Entry<String, String>>>(
							currentLevelRemainSize);
					for (int i = index + currentLevelSequenceLength; i < currentLevelSize; i++)
					{
						currentLevelRemainingPosTagByTermList
								.add(currentLevelPosTagByTermList.get(i));
					}

					recursiveMatchTerms(matcher,
							currentLevelRemainingPosTagByTermList,
							matchedSequenceInfo, matchedSingleInfo);
				}
				else
				// next (or next next...) level
				{
					int lastIndexInCurrentLevel = currentLevelSize - 1;
					// only one
					int startIndex = currentLevelSequenceLength
							- (lastIndexInCurrentLevel - index);
					List<Entry<String, String>> lastInCurrentLevel = currentLevelPosTagByTermList
							.get(lastIndexInCurrentLevel);
					List<Entry<String, String>> nextLevelRemainingPosTagsByTerm = new ArrayList<Entry<String, String>>(
							lastInCurrentLevel.subList(startIndex,
									lastInCurrentLevel.size()));

					recursiveMatchTerms(
							matcher,
							Collections
									.singletonList(nextLevelRemainingPosTagsByTerm),
							matchedSequenceInfo, matchedSingleInfo);
				}

				return;
			}
		}

		// nothing matched in this level, then prepare and proceed next level
		int nextLevelSize = currentLevelSize + 1;
		List<List<Entry<String, String>>> nextLevelPosTagByTermList = new ArrayList<List<Entry<String, String>>>(
				nextLevelSize);

		List<Entry<String, String>> currentLevelSequence = null;
		List<Entry<String, String>> nextLevelSequence;
		for (int index = 0; index < currentLevelSize; index++)
		{
			currentLevelSequence = currentLevelPosTagByTermList.get(index);
			nextLevelSequence = new ArrayList<Entry<String, String>>(
					currentLevelSequence.subList(0,
							currentLevelSequence.size() - 1));

			nextLevelPosTagByTermList.add(nextLevelSequence);
		}

		nextLevelSequence = new ArrayList<Entry<String, String>>(
				currentLevelSequence.subList(1, currentLevelSequence.size()));
		nextLevelPosTagByTermList.add(nextLevelSequence);

		// recursively invoke next level
		recursiveMatchTerms(matcher, nextLevelPosTagByTermList,
				matchedSequenceInfo, matchedSingleInfo);
	}

	private boolean isEitherSideStopWordOrParentheses(
			List<Entry<String, String>> currentLevelPosTagsByTerm)
	{
		String leftSide = currentLevelPosTagsByTerm.get(0).getKey()
				.toLowerCase();
		String rightSide = currentLevelPosTagsByTerm
				.get(currentLevelPosTagsByTerm.size() - 1).getKey()
				.toLowerCase();

		return StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(leftSide)
				|| StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(rightSide)
				|| PARENTHESES_VALUES.contains(leftSide)
				|| PARENTHESES_VALUES.contains(rightSide);
	}

	private String concatenateTerms(List<Entry<String, String>> posTagsByTerm)
	{
		return concatenateTerms(posTagsByTerm, false);
	}

	private String concatenateTerms(List<Entry<String, String>> posTagsByTerm,
			boolean ignorePossessiveCase)
	{
		return concatenateTerms(posTagsByTerm, ignorePossessiveCase, " ");
	}

	private String concatenateTerms(List<Entry<String, String>> posTagsByTerm,
			boolean ignorePossessiveCase, String delimiter)
	{
		if (posTagsByTerm.isEmpty())
		{
			return "";
		}

		StringBuilder stringBuilder = new StringBuilder(posTagsByTerm.get(0)
				.getKey());
		for (int index = 1; index < posTagsByTerm.size(); index++)
		{
			String posTag = posTagsByTerm.get(index).getValue();
			String term = posTagsByTerm.get(index).getKey();

			if (POSTAG_POSSESSIVE.equals(posTag))
			{
				if (!ignorePossessiveCase)
				{
					stringBuilder.append(term);
				}
			}
			else
			{
				stringBuilder.append(delimiter).append(term);
			}
		}

		return stringBuilder.toString();
	}

	public String removeDiacritics(String input)
	{
		String nrml = Normalizer.normalize(input, Normalizer.Form.NFD);
		StringBuilder stripped = new StringBuilder();
		for (int i = 0; i < nrml.length(); ++i)
		{
			if (Character.getType(nrml.charAt(i)) != Character.NON_SPACING_MARK)
			{
				stripped.append(nrml.charAt(i));
			}
		}
		return stripped.toString();
	}
}