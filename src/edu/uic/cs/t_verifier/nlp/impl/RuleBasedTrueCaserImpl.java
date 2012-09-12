package edu.uic.cs.t_verifier.nlp.impl;

import java.text.Normalizer;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.util.Span;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.StopAnalyzer;

import edu.mit.jwi.item.POS;
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
import edu.uic.cs.t_verifier.ml.PersonNameIdentifier;
import edu.uic.cs.t_verifier.nlp.TrueCaser;
import edu.uic.cs.t_verifier.nlp.impl.OpenNLPChunker.ChunkType;

public class RuleBasedTrueCaserImpl extends AbstractNLPOperations implements
		TrueCaser
{
	// private static final String WEB_ADDRESS_REGEX = "^([a-zA-Z0-9\\-]+\\.)+(com|org|net|mil|edu|COM|ORG|NET|MIL|EDU)$";

	private static final Logger LOGGER = LogHelper
			.getLogger(RuleBasedTrueCaserImpl.class);

	private static final int SEGMENT_NUM_FOR_MATCHING = 2;

	private static final String POSTAG_POSSESSIVE = "POS";
	private static final String POSTAG_DETERMINER = "DT";
	private static final String POSTAG_DOLLAR = "$";
	private static final String POSTAG_NOUN = "NN";

	private static final String PARENTHESES_LEFT = "-lrb-";
	private static final String PARENTHESES_RIGHT = "-rrb-";
	private static final List<String> PARENTHESES_VALUES = Arrays.asList(
			PARENTHESES_LEFT, PARENTHESES_RIGHT);

	private static final String REGEX_TERM_LEFT_BOUND = "\\b";
	private static final String REGEX_TERM_RIGHT_BOUND = "(\\b|\\s)";

	private static final String[] CATEGORY_KEYWORDS_SONG = { "song", "album" };

	private static final List<String> MONTHS_IN_LOWER_CASE = Arrays
			.asList(new String[] { "january", "jan.", "february", "feb.",
					"march", "mar.", "april", "apr.", "may", "june", "jun.",
					"july", "jul.", "august", "aug.", "september", "sep.",
					"october", "oct.", "november", "nov.", "december", "dec." });

	private WikipediaContentExtractor wikipediaContentExtractor = ClassFactory
			.getInstance(Config.WIKIPEDIACONTENTEXTRACTOR_CLASS_NAME);

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
			// TODO disambiguation NOT useful //////////////////////////////////
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
	// private PersonNameMatcherImpl trigramPersonNameMatcher = new PersonNameMatcherImpl();

	private SentenceCache sentenceCache = SentenceCache.getInstance();

	private OpenNLPChunker chunker = new OpenNLPChunker();

	//	private PersonNameIdentifier personNameIdentifier = new PersonNameIdentifierImpl();
	//  private PersonNameIdentifier personNameIdentifier = new PersonNameIdentifierStanfordNERImpl();
	// TODO right now we don't use any person name identifier
	private PersonNameIdentifier personNameIdentifier = new PersonNameIdentifier()
	{
		@Override
		public List<Integer> identifyNameTermsWithinNounPhrase(
				List<Entry<String, String>> tagsByTerm,
				Entry<String, String> tagsByTermBeforeNP,
				Entry<String, String> tagsByTermAfterNP)
		{
			return Collections.emptyList();
		}
	};

	public static void main(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();
		RuleBasedTrueCaserImpl impl = new RuleBasedTrueCaserImpl();

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

					String capitalizedSentence = impl.restoreCases(sentence);
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

	public void commitCache()
	{
		sentenceCache.writeCache();
	}

	@Override
	public String restoreCases(String sentence)
	{
		return restoreCases(sentence, null);
	}

	@Override
	public String restoreCases(String sentence,
			List<List<String>> possibleNounPhrases)
	{
		LOGGER.info(sentence);

		sentence = sentence.trim();
		sentence = sentence.replaceAll("\\s{2,}", " ");

		// sentence = StringUtils.capitalize(sentence);

		List<Entry<String, String>> posTagsByTerm = parseIntoPosTagByTerms(
				sentence, false); // this one is not in basic form!!
		// split by the punctuation
		List<List<Entry<String, String>>> originalPosTagsByTermOfSubSentences = splitByPunctuations(posTagsByTerm); // original form for chunking

		List<List<Entry<String, String>>> allNounPhrases = new ArrayList<List<Entry<String, String>>>();
		List<List<Entry<String, String>>> posTagsByTermOfSubSentences = new ArrayList<List<Entry<String, String>>>(
				originalPosTagsByTermOfSubSentences.size());

		List<String> matchedAcronyms = new ArrayList<String>();
		List<Entry<List<Entry<String, String>>, String>> matchedFullNames = new ArrayList<Entry<List<Entry<String, String>>, String>>();

		for (List<Entry<String, String>> tagsByTermOriginalForm : originalPosTagsByTermOfSubSentences) // for each sub-sentence
		{
			List<Entry<String, String>> tagsByTermBasicForm = mapPosTagToBasicForm(tagsByTermOriginalForm);
			posTagsByTermOfSubSentences.add(tagsByTermBasicForm); // basic form

			String acronym = searchForAcronyms(tagsByTermBasicForm);
			matchedAcronyms.add(acronym);

			List<Span> spans = chunker.getChunkSpans(tagsByTermOriginalForm,
					ChunkType.NP);

			// find names
			for (Span span : spans)
			{
				List<Entry<String, String>> phraseOriginalForm = tagsByTermOriginalForm
						.subList(span.getStart(), span.getEnd());
				List<Entry<String, String>> phraseBasicForm = mapPosTagToBasicForm(phraseOriginalForm); // basic form
				allNounPhrases.add(phraseBasicForm);

				//				Entry<String, String> tagsByTermBeforeNP = (span.getStart() == 0) ? null
				//						: tagsByTermOriginalForm.get(span.getStart() - 1);
				//				Entry<String, String> tagsByTermAfterNP = (span.getEnd() == tagsByTermOriginalForm
				//						.size()) ? null : tagsByTermOriginalForm.get(span
				//						.getEnd());
				//				List<Integer> indicesOfNameTerm = personNameIdentifier
				//						.identifyNameTermsWithinNounPhrase(phraseBasicForm,
				//								tagsByTermBeforeNP, tagsByTermAfterNP);
				//				if (indicesOfNameTerm == null || indicesOfNameTerm.isEmpty())
				//				{
				//					continue;
				//				}
				//
				//				List<Entry<String, String>> name = phraseBasicForm
				//						.subList(indicesOfNameTerm.get(0), indicesOfNameTerm
				//								.get(indicesOfNameTerm.size() - 1) + 1);
				//				matchedFullNames
				//						.add(new SimpleEntry<List<Entry<String, String>>, String>(
				//								name, concatenateTerms(name)));
				// System.out.println(name);
			}

			matchFullNames(tagsByTermOriginalForm, tagsByTermBasicForm,
					matchedFullNames);
		}

		LOGGER.info(">>>>> Acronyms\t\t\t" + matchedAcronyms);
		LOGGER.info(">>>>> NounPhrases_from_chucker\t\t\t" + allNounPhrases);

		////////////////////////////////////////////////////////////////////////
		// IF USING THIS, COMMENT matchFullNames(...), OR USE DUMMY PersonNameIdentifier
		// match names within name-list
		//		List<Entry<Entry<String, String>, String>> matchedSingleNames = new ArrayList<Map.Entry<Entry<String, String>, String>>();
		//		for (List<Entry<String, String>> posTagByTerm : posTagsByTermOfSubSentences)
		//		{
		//			recursiveMatchTerms(trigramPersonNameMatcher,
		//					Collections.singletonList(posTagByTerm), matchedFullNames,
		//					matchedSingleNames);
		//		}
		////////////////////////////////////////////////////////////////////////

		LOGGER.info(">>>>> MatchedPersonName_full\t\t\t" + matchedFullNames); // TODO no use now
		// System.out.println(allNounPhrases);

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

		/*
		// TODO this matched single name is not used now, 
		// since it may match terms like “long”, “longest”, “kings”, “big”, “from”, “games”, “late”, “states”
		// maybe introducing the frequency of each name term may do some help, but we haven't decided it yet.
		LOGGER.info(">>>>> MatchedPersonName_single\t\t\t" + matchedSingleNames);
		*/

		// find those names only identified by name-list
		List<List<Entry<String, String>>> matchedNamesIdentifiedByNameListOnly = filterOutNamesByWikiAndWordNet(
				matchedFullNames, capitalizationsByOriginalCaseFromWiki,
				capitalizationsBySingleNounTermFromWiki,
				capitalizationsByOriginalCaseFromWordNet);

		LOGGER.info(">>>>> PersonName_notInWiki_notInWN\t*"
				+ matchedNamesIdentifiedByNameListOnly);

		// REPLACE /////////////////////////////////////////////////////////////
		for (List<Entry<String, String>> entry : matchedNamesIdentifiedByNameListOnly)
		{
			String target = concatenateTerms(entry);
			String replacement = WordUtils.capitalize(target);
			sentence = sentence.replace(target, replacement);
		}

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

		for (String acronym : matchedAcronyms)
		{
			acronym = "(" + acronym + ")";
			sentence = sentence
					.replace(acronym, acronym.toUpperCase(Locale.US));
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

		LOGGER.info("========================================================");

		return sentence;
		//return StringUtils.capitalize(sentence);
	}

	private void matchFullNames(
			List<Entry<String, String>> tagsByTermOriginalForm,
			List<Entry<String, String>> tagsByTermBasicForm,
			List<Entry<List<Entry<String, String>>, String>> matchedFullNames)
	{
		List<Integer> personNameIndices = personNameIdentifier
				.identifyNameTermsWithinNounPhrase(tagsByTermOriginalForm,
						null, null);
		List<Entry<String, String>> oneName = null;
		Integer lastTermindex = null;
		for (Integer nameTermIndex : personNameIndices)
		{
			if (oneName == null) // new name
			{
				oneName = new ArrayList<Entry<String, String>>();
				oneName.add(tagsByTermBasicForm.get(nameTermIndex));
			}
			else if (lastTermindex != nameTermIndex - 1) // finish name
			{
				matchedFullNames
						.add(new SimpleEntry<List<Entry<String, String>>, String>(
								oneName, concatenateTerms(oneName)));
				oneName = null;
			}
			else
			// continue name
			{
				oneName.add(tagsByTermBasicForm.get(nameTermIndex));
			}

			lastTermindex = nameTermIndex;
		}

		if (oneName != null)
		{
			matchedFullNames
					.add(new SimpleEntry<List<Entry<String, String>>, String>(
							oneName, concatenateTerms(oneName)));
		}
	}

	private String searchForAcronyms(
			List<Entry<String, String>> tagsByTermBasicForm)
	{
		int index = 0;
		Entry<String, String> tagByTerm = null;
		for (; index < tagsByTermBasicForm.size(); index++)
		{
			tagByTerm = tagsByTermBasicForm.get(index);
			String term = tagByTerm.getKey();

			if (PARENTHESES_LEFT.equalsIgnoreCase(term)) // find "("
			{
				break;
			}
		}

		if (index > tagsByTermBasicForm.size() - 3
				|| !PARENTHESES_RIGHT.equalsIgnoreCase(tagsByTermBasicForm.get(
						index + 2).getKey())) // no ")"
		{
			return null; // no acronyms
		}

		String acronym = tagsByTermBasicForm.get(index + 1).getKey();

		int acronymsLength = acronym.length();
		if (acronymsLength > index) // no enough terms in front
		{
			return null;
		}

		// char ch = acronym.charAt(acronymsLength - 1);
		// index start with "("
		int i = 0;
		for (int j = index - acronymsLength; j < index; j++)
		{
			tagByTerm = tagsByTermBasicForm.get(j);
			if (tagByTerm.getKey().charAt(0) != acronym.charAt(i++))
			{
				return null;
			}
		}

		return acronym;
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
		// "R & D" ==> "R&D"
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
	private List<List<Entry<String, String>>> filterOutNamesByWikiAndWordNet(
			List<Entry<List<Entry<String, String>>, String>> matchedNames,
			List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWiki,
			List<Entry<Entry<String, String>, String>> capitalizationsBySingleNounTermFromWiki,
			List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWordNet)
	{
		if (matchedNames.isEmpty())
		{
			return Collections.emptyList();
		}

		// names
		List<List<Entry<String, String>>> squencesFromNameList = new ArrayList<List<Entry<String, String>>>(
				matchedNames.size());
		for (Entry<List<Entry<String, String>>, String> entry : matchedNames)
		{
			squencesFromNameList.add(entry.getKey());
		}

		// wiki phrases
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

		// wordnet phrases
		capitalizationsByOriginalCaseFromWiki
				.addAll(capitalizationsByOriginalCaseFromWordNet);

		return filterOutOverlappings(squencesFromNameList,
				capitalizationsByOriginalCaseFromWiki, /*true*/false); // I don't remember why use true now

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
				String term = entry.getKey();
				if (!POSTAG_NOUN.equals(posTag)
				/*&& !wordNetHasOnlyNounEntry(term)*/) // individual term MUST be noun!
				{
					continue;
				}

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

	// this doesn't work because there are terms like "A" which is a Noun
	/*private boolean wordNetHasOnlyNounEntry(String term)
	{
		boolean hasAdj = wordNetReader.retrieveTermInStandardCase(term,
				POS.ADJECTIVE) != null;
		boolean hasAdv = wordNetReader.retrieveTermInStandardCase(term,
				POS.ADVERB) != null;
		boolean hasVerb = wordNetReader.retrieveTermInStandardCase(term,
				POS.VERB) != null;
		boolean hasNoun = wordNetReader.retrieveTermInStandardCase(term,
				POS.NOUN) != null;

		return !hasAdj && !hasAdv && !hasVerb && hasNoun;
	}*/

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
				capitalizationsByOriginalCaseFromWiki, false);
	}

	private List<List<Entry<String, String>>> filterOutOverlappings(
			List<List<Entry<String, String>>> from,
			List<Entry<List<Entry<String, String>>, String>> filterOut,
			boolean keepIfIdentical)
	{
		if (filterOut.isEmpty()) // wikiepda has not identified any 
		{
			return from;
		}

		List<List<Entry<String, String>>> squencesFromWiki = new ArrayList<List<Entry<String, String>>>(
				filterOut.size());
		for (Entry<List<Entry<String, String>>, String> entry : filterOut)
		{
			squencesFromWiki.add(entry.getKey());
		}

		List<List<Entry<String, String>>> result = new ArrayList<List<Entry<String, String>>>();
		ns: for (List<Entry<String, String>> nounSequence : from)
		{
			// check if overlapping with any sequence from wikipedia
			for (List<Entry<String, String>> squenceFromWiki : squencesFromWiki)
			{
				if (keepIfIdentical)
				{
					@SuppressWarnings("unchecked")
					ArrayList<Entry<String, String>> nounSequenceClone = (ArrayList<Entry<String, String>>) ((ArrayList<Entry<String, String>>) nounSequence)
							.clone();
					nounSequenceClone.removeAll(squenceFromWiki);
					if (nounSequenceClone.isEmpty()) // two are identical
					{
						LOGGER.info("Keeping the identical sequences "
								+ squenceFromWiki);
						break;
					}
				}

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
		LOGGER.info(">>>>> Matched sequence in Wikipedia\t\t"
				+ matchedSequenceInfo);
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

		LOGGER.info(">>>>> Matched single term in Wikipedia\t"
				+ matchedSingleInfo);
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

	//	// term sequence
	//	private Entry<List<Entry<String, String>>, String> checkWhetherProperNoun(
	//			List<Entry<String, String>> posTagsByMatchedTerm,
	//			MatchedQueryKey matchedQueryKey)
	//	{
	//		Assert.isTrue(
	//				!posTagsByMatchedTerm.isEmpty(),
	//				"There must be terms for the matched page: "
	//						+ matchedQueryKey.getCertainPageUrl());
	//
	//		if (isSongName(posTagsByMatchedTerm, matchedQueryKey))
	//		{
	//			return null;
	//		}
	//
	//		String content = extractContentFromWikiPage(matchedQueryKey);
	//		if (content == null)
	//		{
	//			return null;
	//		}
	//		String contentInLowerCase = content.toLowerCase();
	//
	//		String title = matchedQueryKey.getKeyWord().replaceAll("_", " ").trim();
	//		String titleInLowerCase = title.toLowerCase();
	//		boolean titleContainsMultipleTerms = title.contains(" ");
	//		boolean titleCouldCapitalized = shouldCapitalized(title);
	//
	//		boolean capitalized = false;
	//		StringBuilder properNoun = new StringBuilder();
	//		String termInResult = null;
	//		Matcher matcher = null;
	//
	//		// start from second term
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
	//			else
	//			{
	//				Pattern pattern4MatchedTermInLowerCase = createMatchingPattern(matchedTermInLowerCase);
	//				if (titleContainsMultipleTerms //more than one terms
	//						&& (matcher = pattern4MatchedTermInLowerCase
	//								.matcher(titleInLowerCase)).find()) // in title 
	//				{
	//					if (titleCouldCapitalized) // title-terms are capitalized, and more than one terms
	//					{
	//						int lastStartIndex = matcher.start();
	//						int lastEndIndex = matcher.end();
	//
	//						termInResult = title.substring(lastStartIndex,
	//								lastEndIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
	//						capitalized = true;
	//					}
	//					else
	//					// but not capitalized
	//					{
	//						termInResult = matchedTermInLowerCase;
	//					}
	//				}
	//				else if (!pattern4MatchedTermInLowerCase.matcher(content)
	//						.find()
	//						&& (matcher = pattern4MatchedTermInLowerCase
	//								.matcher(contentInLowerCase)).find()) // All appearances are capitalized
	//				{
	//					int lastStartIndex = -1;
	//					int lastEndIndex = -1;
	//					do
	//					{
	//						// until the last one is found
	//						lastStartIndex = matcher.start();
	//						lastEndIndex = matcher.end();
	//					}
	//					while (matcher.find());
	//
	//					termInResult = content.substring(lastStartIndex,
	//							lastEndIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
	//					capitalized = true;
	//				}
	//				else
	//				{
	//					termInResult = matchedTermInLowerCase;
	//				}
	//			}
	//
	//			if (!POSTAG_POSSESSIVE.equals(posTag)) // POS tag is not POSSESSIVE
	//			{
	//				properNoun.append(" ");
	//			}
	//			properNoun.append(termInResult);
	//		}
	//
	//		// first term
	//		String posTag = posTagsByMatchedTerm.get(0).getValue();
	//		String matchedTermInLowerCase = posTagsByMatchedTerm.get(0).getKey()
	//				.toLowerCase();
	//
	//		if (POSTAG_DETERMINER.equals(posTag))
	//		{
	//			termInResult = matchedTermInLowerCase;
	//		}
	//		else
	//		{
	//			Pattern pattern4MatchedTermInLowerCase = createMatchingPattern(matchedTermInLowerCase);
	//			if (titleContainsMultipleTerms //more than one terms
	//					&& (matcher = pattern4MatchedTermInLowerCase
	//							.matcher(titleInLowerCase)).find()) // in title
	//			{
	//				int lastStartIndex = matcher.start();
	//				int lastEndIndex = matcher.end();
	//
	//				String firstTermInTitle = title.substring(lastStartIndex,
	//						lastEndIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
	//
	//				//				Pattern pattern4TitleWithFirstTermInLowerCase = createMatchingPattern(title
	//				//						.replaceFirst(firstTermInTitle, matchedTermInLowerCase));
	//
	//				// in *middle* of sentence ([,;-\\\\(][ \t]*|\\w[ \t]+)\\bTerm\\b
	//				// DO NOT use "\\s", since it contains "\n" which is not correct
	//				Pattern pattern4TitleWithFirstTermInLowerCaseAndNotInBeginning = Pattern
	//						.compile("([,;-\\\\(][ \t]*|\\w[ \t]+)"
	//								+ createMatchingSequqnce(title));
	//				if (pattern4TitleWithFirstTermInLowerCaseAndNotInBeginning
	//						.matcher(content).find())
	//				//				if (pattern4TitleWithFirstTermInLowerCase.matcher(content)
	//				//						.find()) // there's title phrase which has first term in lower case
	//				{
	//					termInResult = matchedTermInLowerCase;
	//				}
	//				else
	//				{
	//					termInResult = firstTermInTitle;
	//					capitalized = true;
	//				}
	//
	//			}
	//			else if (!pattern4MatchedTermInLowerCase.matcher(content).find()
	//					&& (matcher = pattern4MatchedTermInLowerCase
	//							.matcher(contentInLowerCase)).find()) // All appearances are capitalized
	//			{
	//				int lastStartIndex = -1;
	//				int lastEndIndex = -1;
	//				do
	//				{
	//					// until the last one is found
	//					lastStartIndex = matcher.start();
	//					lastEndIndex = matcher.end();
	//				}
	//				while (matcher.find());
	//
	//				if (!capitalized && lastStartIndex == 0)
	//				{
	//					// nothing capitalized so far, and the only match is the first term
	//					// do not trust it
	//					termInResult = matchedTermInLowerCase;
	//				}
	//				else
	//				{
	//					termInResult = content.substring(lastStartIndex,
	//							lastEndIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "
	//					capitalized = true;
	//				}
	//			}
	//			else
	//			{
	//				termInResult = matchedTermInLowerCase;
	//			}
	//		}
	//
	//		String result = termInResult + properNoun.toString();
	//		if (capitalized)
	//		{
	//			return new SimpleEntry<List<Entry<String, String>>, String>(
	//					posTagsByMatchedTerm, result);
	//		}
	//
	//		return null;
	//	}

	private Pattern createMatchingPattern(String matchedTerms)
	{
		return Pattern.compile(createMatchingSequqnce(matchedTerms));
	}

	// in *middle* of sentence ([,;-\\\\(][ \t]*|\\w[ \t]+)\\bTerm\\b
	// DO NOT use "\\s", since it contains "\n" which is not correct
	private Pattern createNonSentenceBeginningMatchingPattern(String term)
	{
		return Pattern.compile("([,;-\\\\&\\\\(][ \t]*|\\w[ \t]+)"
				+ createMatchingSequqnce(term));
	}

	// term sequence
	private Entry<List<Entry<String, String>>, String> checkWhetherProperNoun(
			List<Entry<String, String>> posTagsByMatchedTerm,
			MatchedQueryKey matchedQueryKey)
	{
		Assert.isTrue(!posTagsByMatchedTerm.isEmpty());

		// concatenateTerms(posTagsByTerm);

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

		// boolean titleContainsMultipleTerms = title.contains(" ");
		// if there are terms which are not the first one in the title be capitalized
		boolean titleShouldCapitalized = shouldCapitalized(title);

		StringBuilder nounPhrase = new StringBuilder();

		for (int index = 0; index < posTagsByMatchedTerm.size(); index++)
		{
			String termInLowerCase = posTagsByMatchedTerm.get(index).getKey()
					.toLowerCase(Locale.US);
			String pos = posTagsByMatchedTerm.get(index).getValue();

			if (index != 0 && !POSTAG_POSSESSIVE.equals(pos)) // No space before "'s" or first term
			{
				nounPhrase.append(" ");
			}

			if (POSTAG_DETERMINER.equals(pos) || POSTAG_POSSESSIVE.equals(pos)
					|| POSTAG_DOLLAR.equals(pos))
			{
				nounPhrase.append(termInLowerCase);
				continue;
			}

			if (MONTHS_IN_LOWER_CASE.contains(termInLowerCase))
			{
				nounPhrase.append(StringUtils.capitalize(termInLowerCase));
				continue;
			}

			Pattern termInLowerCasePattern = createMatchingPattern(termInLowerCase);
			Matcher lowerCaseTitleMatcher = termInLowerCasePattern
					.matcher(titleInLowerCase);
			if (lowerCaseTitleMatcher.find()) // exists in title
			{
				int startIndex = lowerCaseTitleMatcher.start();
				int endIndex = lowerCaseTitleMatcher.end();
				String termInTitle = title.substring(startIndex, endIndex)
						.trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "

				if (startIndex != 0) // NOT the first term in title
				{
					nounPhrase.append(termInTitle); // use title form directly
				}
				else
				// first term in title (must be capitalized, since wiki capitalizes all titles)
				{
					Pattern nonBeginningTitlePattern = createNonSentenceBeginningMatchingPattern(title);
					String[] terms = title.split(" +");

					// whole title form (Xxx ...) not at the beginning of sentence
					if (nonBeginningTitlePattern.matcher(content).find())
					{
						nounPhrase.append(termInTitle);
					}
					else if (terms.length > 1 // multi-terms in title
							&& Character.isUpperCase(terms[1].charAt(0)) // and second term is capitalized
							&& createNonSentenceBeginningMatchingPattern(
									termInTitle + " " + terms[1]).matcher(
									content).find()) // and the first two terms not at the beginning of sentence
					{
						nounPhrase.append(termInTitle);
					}
					// first term in lower case title (xxx ...) exists in content
					else if (createMatchingPattern(
							title.replaceFirst(termInTitle, termInLowerCase))
							.matcher(content).find())
					{
						nounPhrase.append(termInLowerCase);
					}
					// title should be capitalized, and there are no lower case first term exists 
					else if (titleShouldCapitalized
							&& !termInLowerCasePattern.matcher(content).find())
					{
						nounPhrase.append(termInTitle);
					}
					else
					{
						nounPhrase.append(termInLowerCase);
					}
				}
			}
			else
			// not exists in title
			{
				Matcher lowerCaseContentMatcher = termInLowerCasePattern
						.matcher(contentInLowerCase);
				if (lowerCaseContentMatcher.find()) // exists in content
				{
					int startIndex = lowerCaseContentMatcher.start();
					int endIndex = lowerCaseContentMatcher.end();
					String termInContent = content.substring(startIndex,
							endIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "

					Pattern nonBeginningCapitalizedTermPattern = createNonSentenceBeginningMatchingPattern(termInContent);
					// Pattern lowerCaseTermPattern = createMatchingPattern(termInLowerCase);

					// no lower case && has non-beginning upper case ==> upper case
					if (!termInLowerCasePattern.matcher(content).find()
							&& nonBeginningCapitalizedTermPattern.matcher(
									content).find())
					{
						nounPhrase.append(termInContent);
					}
					else
					{
						nounPhrase.append(termInLowerCase);
					}
				}
				else if (termInLowerCase.contains(".")) // abbreviation
				{
					Pattern abbreviationInLowerCasePattern = createMatchingPattern(termInLowerCase
							.replace(".", ""));
					Matcher abbreviationLowerCaseContentMatcher = abbreviationInLowerCasePattern
							.matcher(contentInLowerCase);
					if (abbreviationLowerCaseContentMatcher.find())
					{
						int startIndex = abbreviationLowerCaseContentMatcher
								.start();
						int endIndex = abbreviationLowerCaseContentMatcher
								.end();
						String termInContent = content.substring(startIndex,
								endIndex).trim(); // trim() is necessary, since the pattern (\\b|\\s) may match " "

						String termRebuild = termInLowerCase;
						for (int i = 0; i < termInContent.length(); i++)
						{
							char ch = termInContent.charAt(i);
							termRebuild = termRebuild.replace(
									Character.toLowerCase(ch), ch);
						}

						nounPhrase.append(termRebuild);
					}
					else
					{
						nounPhrase.append(termInLowerCase);
					}
				}
				else
				{
					nounPhrase.append(termInLowerCase);
				}
			}
		} // for

		String nounPhraseInLowerCase = concatenateTerms(posTagsByMatchedTerm)
				.toLowerCase(Locale.US); // in case that month-term be capitalized by Stanford parser
		String nounPhraseInProperCase = nounPhrase.toString();

		Assert.isTrue(
				nounPhraseInLowerCase.equalsIgnoreCase(nounPhraseInProperCase),
				"[" + nounPhraseInLowerCase + "] and ["
						+ nounPhraseInProperCase + "] should has same terms. ");
		if (!nounPhraseInLowerCase.equals(nounPhraseInProperCase)) // different case
		{
			return new SimpleEntry<List<Entry<String, String>>, String>(
					posTagsByMatchedTerm, nounPhraseInProperCase);
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

	private String createMatchingSequqnce(String matchedTerm)
	{
		return REGEX_TERM_LEFT_BOUND + matchedTerm.replace(".", "\\.")
				+ REGEX_TERM_RIGHT_BOUND;
	}

	private boolean shouldCapitalized(String matchedTerms)
	{
		String[] terms = matchedTerms.split(" +");
		if (terms.length == 1)
		{
			return false;
		}

		for (int index = 1; index < terms.length; index++)
		{
			if (Character.isUpperCase(terms[index].charAt(0))
					&& !isStopWord(terms[index].toLowerCase(Locale.US)))
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

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(matchedQueryKey.getKeyWord().replaceAll("_", " "))
				.append("\n");

		int num = Math.min(segments.size(), SEGMENT_NUM_FOR_MATCHING);
		for (int index = 0; index < num; index++)
		{
			Segment segment = segments.get(index);
			// replace the punctuation
			//		String content = firstSegment.toString().replace(", ", " ")
			//				.replace(". ", " ");

			// 		String content = firstSegment.toString(); 
			//		return content;
			// NO TABLE!

			if (segment.getHeading() != null)
			{
				stringBuilder.append(segment.getHeading()).append("\n");
			}

			if (segment.getParagraphs() != null)
			{
				for (Paragraph paragraph : segment.getParagraphs())
				{
					stringBuilder.append(paragraph).append("\n");
				}
			}
		}

		String result = removeDiacritics(stringBuilder.toString());
		// result = removePageURL(result);

		// fix the problem: "u.s.," can not be matched by "\\bu\\.s\\.(\\b|\\s)"
		result = result.replace(".,", ". ,");

		return result;
	}

	//	private String extractProperNoun(String content, String matchedTerms)
	//	{
	//		int index = content.toLowerCase().lastIndexOf(
	//				matchedTerms.toLowerCase() + " ");
	//		return content.substring(index, index + matchedTerms.length());
	//	}

	//	private String removePageURL(String result)
	//	{
	//		return result.replaceAll(WEB_ADDRESS_REGEX, "");
	//	}

	//	private List<List<Entry<String, String>>> mapPosTagToBasicForm(
	//			List<List<Entry<String, String>>> originalPosTagsByTermOfSubSentences)
	//	{
	//		List<List<Entry<String, String>>> result = new ArrayList<List<Entry<String, String>>>(
	//				originalPosTagsByTermOfSubSentences.size());
	//
	//		for (List<Entry<String, String>> originalSubSentence : originalPosTagsByTermOfSubSentences)
	//		{
	//			List<Entry<String, String>> resultSubSentence = mapPosTagToBasicForm2(originalSubSentence);
	//
	//			result.add(resultSubSentence);
	//		}
	//
	//		return result;
	//	}

	private List<Entry<String, String>> mapPosTagToBasicForm(
			List<Entry<String, String>> originalSubSentence)
	{
		List<Entry<String, String>> resultSubSentence = new ArrayList<Entry<String, String>>(
				originalSubSentence.size());
		for (Entry<String, String> entry : originalSubSentence)
		{
			resultSubSentence.add(new SimpleEntry<String, String>(entry
					.getKey(), mapPosTagToBasicForm(entry.getValue())));
		}
		return resultSubSentence;
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
				.getKey().toLowerCase(Locale.US)); // in case that month-term be capitalized by Stanford parser
		for (int index = 1; index < posTagsByTerm.size(); index++)
		{
			String posTag = posTagsByTerm.get(index).getValue();
			String term = posTagsByTerm.get(index).getKey()
					.toLowerCase(Locale.US); // in case that month-term be capitalized by Stanford parser

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

	private String removeDiacritics(String input)
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
