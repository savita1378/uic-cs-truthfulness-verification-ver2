package edu.uic.cs.t_verifier.nlp.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.StopAnalyzer;

import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.Tree;
import edu.uic.cs.t_verifier.html.WikipediaContentExtractor;
import edu.uic.cs.t_verifier.html.data.MatchedQueryKey;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.index.data.UrlWithDescription;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.ClassFactory;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.LogHelper;

public class NLPAnalyzerImpl4 extends NLPAnalyzerImpl3
{
	private static final Logger LOGGER = LogHelper
			.getLogger(NLPAnalyzerImpl4.class);

	private static final String POSTAG_POSSESSIVE = "POS";
	private static final String POSTAG_NOUN = "NN";

	private WikipediaContentExtractor wikipediaContentExtractor = ClassFactory
			.getInstance(Config.WIKIPEDIACONTENTEXTRACTOR_CLASS_NAME);

	public static interface RecursiveMatcher<T>
	{
		// boolean isMatched(String term);
		boolean isMatched(
				List<Entry<String, String>> currentLevelPosTagsByTermSequence);

		T getMatchedInfo();
	}

	private RecursiveMatcher<MatchedQueryKey> wikipediaKeyWordsMatcher = new RecursiveMatcher<MatchedQueryKey>()
	{
		private MatchedQueryKey matchedQueryKey;

		@Override
		public boolean isMatched(
				List<Entry<String, String>> currentLevelPosTagsByTermSequence)
		{
			matchedQueryKey = null;

			String term = concatenateTerms(currentLevelPosTagsByTermSequence);

			matchedQueryKey = wikipediaContentExtractor.matchQueryKey(term);
			if (matchedQueryKey != null && matchedQueryKey.isCertainly())
			{
				return true;
			}

			return false;
		}

		@Override
		public MatchedQueryKey getMatchedInfo()
		{
			Assert.notNull(matchedQueryKey);
			return matchedQueryKey;
		}
	};

	private PersonNameMatcherImpl trigramPersonNameMatcher = new PersonNameMatcherImpl();

	private SentenceCache sentenceCache = new SentenceCache();

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

	public static void main(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();
		NLPAnalyzerImpl4 impl = new NLPAnalyzerImpl4();

		// String sentence = "microsoft's corporate headquarters locates in redmond";
		// String sentence = "alan shepard is the first american in space";
		//		String sentence = "the brightest star visible from earth is sirius";
		//
		//		impl.capitalizeProperNounTerms(sentence, null);

		for (Statement statement : statements)
		{
			List<String> allAlternativeUnits = statement.getAlternativeUnits();
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
				System.out.println(originalSentence);
				LOGGER.info(originalSentence);

				List<List<String>> nounSequences = new ArrayList<List<String>>();
				String capitalizedSentence = impl.capitalizeProperNounTerms(
						sentence, nounSequences);
				System.out.println("> " + capitalizedSentence);
				System.out.println();
				LOGGER.info("");
			}

		}

		impl.sentenceCache.writeCache(); // DO NOT forget this!

	}

	@Override
	protected String restoreWordCasesForSentence(String sentence,
			String alternativeUnit, List<List<String>> nounPhrases)
	{
		return capitalizeProperNounTerms(sentence, nounPhrases);
	}

	@Override
	protected String capitalizeProperNounTerms(String sentence,
			List<List<String>> possibleNounPhrases)
	{
		sentence = sentence.trim();
		// sentence = StringUtils.capitalize(sentence);

		List<Entry<String, String>> posTagsByTerm = parseIntoPosTagByTerms(sentence);
		// split by the punctuation
		List<List<Entry<String, String>>> posTagsByTermOfSubSentences = splitByPunctuations(posTagsByTerm);

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

		// Get all noun sequences
		List<List<Entry<String, String>>> nounSequences = findNounSequences(posTagsByTermOfSubSentences);
		// fill the parameter nounPhrases with nounSequences
		if (possibleNounPhrases != null)
		{
			// TODO more sophisticated method may be needed, 
			// but now is is OK, since the AU matching is using the same parser for nouns
			for (List<Entry<String, String>> sequence : nounSequences)
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

		LOGGER.info(">>>>> Noun_from_parser\t\t\t" + nounSequences);

		// Find those noun(sequence)s which have not been identified by Wikipedia
		List<List<Entry<String, String>>> nounSequenceNotIdentifiedByWiki = filterOutNounSequencesIdentifiedByWiki(
				nounSequences, capitalizationsByOriginalCaseFromWiki,
				capitalizationsBySingleNounTermFromWiki);
		LOGGER.info(">>>>> Noun_notInWiki\t\t\t"
				+ nounSequenceNotIdentifiedByWiki);

		// Find the proper noun in WordNet
		List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWordNet = findProperNounsInWordNet(nounSequenceNotIdentifiedByWiki);
		LOGGER.info(">>>>> ProperNoun_notInWiki_inWN\t\t*"
				+ capitalizationsByOriginalCaseFromWordNet);

		// match names within name-list
		List<Entry<List<Entry<String, String>>, String>> matchedNames = new ArrayList<Entry<List<Entry<String, String>>, String>>();
		for (List<Entry<String, String>> posTagByTerm : posTagsByTermOfSubSentences)
		{
			recursiveMatchTerms(trigramPersonNameMatcher,
					Collections.singletonList(posTagByTerm), matchedNames, null);
		}
		LOGGER.info(">>>>> MatchedPersonName\t\t\t" + matchedNames);

		// find those names only identified by name-list
		List<List<Entry<String, String>>> matchedNamesIdentifiedByNameListOnly = filterOutNamesByWIkiAndWordNet(
				matchedNames, capitalizationsByOriginalCaseFromWiki,
				capitalizationsBySingleNounTermFromWiki,
				capitalizationsByOriginalCaseFromWordNet);

		LOGGER.info(">>>>> PersonName_notInWiki_notInWN\t*"
				+ matchedNamesIdentifiedByNameListOnly);

		////////////////////////////////////////////////////////////////////////
		for (Entry<List<Entry<String, String>>, String> entry : capitalizationsByOriginalCaseFromWiki)
		{
			String target = concatenateTerms(entry.getKey());
			sentence = sentence.replace(target, entry.getValue());
		}

		for (Entry<Entry<String, String>, String> entry : capitalizationsBySingleNounTermFromWiki)
		{
			sentence = sentence.replace(entry.getKey().getKey(),
					entry.getValue());
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

		// return sentence;
		return StringUtils.capitalize(sentence);
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
			List<List<Entry<String, String>>> posTagsByTermOfSubSentences)
	{
		List<Entry<List<Entry<String, String>>, String>> result = new ArrayList<Entry<List<Entry<String, String>>, String>>();

		for (List<Entry<String, String>> nounSequence : posTagsByTermOfSubSentences)
		{
			String nounSequenceString = concatenateTerms(nounSequence);
			String nounSequenceInWordNet = wordNetReader
					.retrieveTermInStandardCase(nounSequenceString, POS.NOUN);
			if (nounSequenceInWordNet != null
					&& inDifferentCases(nounSequenceString,
							nounSequenceInWordNet)) // matched
			{
				result.add(new SimpleEntry<List<Entry<String, String>>, String>(
						nounSequence, nounSequenceInWordNet));
				continue;
			}

			// try each individual noun term
			for (Entry<String, String> entry : nounSequence)
			{
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

	private List<List<Entry<String, String>>> findNounSequences(
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
	}

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
				recursiveMatchTerms(wikipediaKeyWordsMatcher,
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

			Entry<Entry<String, String>, String> properSingleNounByOriginalCase = checkWhetherProperNoun(
					posTagByTerm, matchedQueryKey);
			if (properSingleNounByOriginalCase == null)
			{
				continue;
			}
			capitalizationsBySingleNounTermFromWiki
					.add(properSingleNounByOriginalCase);

			//			capitalizationsByOriginalCaseFromWiki
			//					.add(new SimpleEntry<List<Entry<String, String>>, String>(
			//							Collections
			//									.singletonList(properSingleNounByOriginalCase
			//											.getKey()),
			//							properSingleNounByOriginalCase.getValue()));
		}

	}

	private Entry<Entry<String, String>, String> checkWhetherProperNoun(
			Entry<String, String> posTagByTerm, MatchedQueryKey matchedQueryKey)
	{
		String content = extractContentFromWikiPage(matchedQueryKey);
		if (content == null)
		{
			return null;
		}

		String term = posTagByTerm.getKey();

		if (StringUtils.containsIgnoreCase(content, term + " ")
				&& !content.contains(term + " "))
		{
			String properNoun = extractProperNoun(content, term);
			return new SimpleEntry<Entry<String, String>, String>(posTagByTerm,
					properNoun);
		}

		return null;
	}

	private Entry<List<Entry<String, String>>, String> checkWhetherProperNoun(
			List<Entry<String, String>> posTagsByMatchedTerm,
			MatchedQueryKey matchedQueryKey)
	{
		String content = extractContentFromWikiPage(matchedQueryKey);
		if (content == null)
		{
			return null;
		}

		String matchedTerms = concatenateTerms(posTagsByMatchedTerm, false)
				.toLowerCase();
		String matchedTermsWithoutPossessiveCase = concatenateTerms(
				posTagsByMatchedTerm, true).toLowerCase();

		if (StringUtils.containsIgnoreCase(content, matchedTerms + " ")
				&& !content.contains(matchedTerms + " "))
		{
			String properNoun = extractProperNoun(content, matchedTerms);
			return new SimpleEntry<List<Entry<String, String>>, String>(
					posTagsByMatchedTerm, properNoun);
		}
		// be aware of the POSSESSIVE symbol
		else if (!matchedTerms.equals(matchedTermsWithoutPossessiveCase)
				&& StringUtils.containsIgnoreCase(content,
						matchedTermsWithoutPossessiveCase + " ")
				&& !content.contains(matchedTermsWithoutPossessiveCase + " "))
		{
			String properNoun = extractProperNoun(content,
					matchedTermsWithoutPossessiveCase);
			return new SimpleEntry<List<Entry<String, String>>, String>(
					posTagsByMatchedTerm, properNoun);
		}

		return null;
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
		String content = firstSegment.toString().replace(", ", " ")
				.replace(". ", " ");

		return content;
	}

	private String extractProperNoun(String content, String matchedTerms)
	{
		int index = content.toLowerCase().lastIndexOf(
				matchedTerms.toLowerCase() + " ");
		return content.substring(index, index + matchedTerms.length());
	}

	private List<List<Entry<String, String>>> splitByPunctuations(
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

	private List<Entry<String, String>> parseIntoPosTagByTerms(String sentence)
	{
		Tree parsedTree = lexicalizedParser.apply(sentence);
		List<TaggedWord> taggedWords = parsedTree.taggedYield();

		List<Entry<String, String>> result = new ArrayList<Entry<String, String>>();
		for (TaggedWord taggedWord : taggedWords)
		{
			String posTag = mapPosTagToBasicForm(taggedWord.tag());
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
								.toLowerCase()))
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

			if (isEitherSideStopWord(currentLevelPosTagsByTerm))
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

	private boolean isEitherSideStopWord(
			List<Entry<String, String>> currentLevelPosTagsByTerm)
	{
		String leftSide = currentLevelPosTagsByTerm.get(0).getKey()
				.toLowerCase();
		String rightSide = currentLevelPosTagsByTerm
				.get(currentLevelPosTagsByTerm.size() - 1).getKey()
				.toLowerCase();

		return StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(leftSide)
				|| StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(rightSide);
	}

	private String concatenateTerms(List<Entry<String, String>> posTagsByTerm)
	{
		return concatenateTerms(posTagsByTerm, false);
	}

	private String concatenateTerms(List<Entry<String, String>> posTagsByTerm,
			boolean ignorePossessiveCase)
	{
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
				stringBuilder.append(" ").append(term);
			}
		}

		return stringBuilder.toString();
	}
}
