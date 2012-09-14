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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.mit.jwi.item.POS;
import edu.uic.cs.t_verifier.html.WikipediaContentExtractor;
import edu.uic.cs.t_verifier.html.data.MatchedQueryKey;
import edu.uic.cs.t_verifier.index.data.Paragraph;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.index.data.UrlWithDescription;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.ClassFactory;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.LogHelper;
import edu.uic.cs.t_verifier.ml.PersonNameIdentifier;
import edu.uic.cs.t_verifier.nlp.TrueCaser;

abstract class AbstractTrueCaser extends AbstractNLPOperations implements
		TrueCaser
{
	private static final Logger LOGGER = LogHelper
			.getLogger(AbstractTrueCaser.class);

	private static final int SEGMENT_NUM_FOR_MATCHING = 2;

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

	protected OpenNLPChunker chunker = new OpenNLPChunker();

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

	public void commitCache()
	{
		sentenceCache.writeCache();
	}

	@Override
	public String restoreCases(String sentence)
	{
		return restoreCases(sentence, null);
	}

	protected void matchFullNames(
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

	protected String searchForAcronyms(
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

	protected String replaceMatchedWikiPhrase(String sentence,
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
	protected List<List<Entry<String, String>>> filterOutNamesByWikiAndWordNet(
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

	protected List<Entry<Entry<String, String>, String>> filterOutNonProperSingleNounByWordNet(
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

	protected List<Entry<List<Entry<String, String>>, String>> findProperNounsInWordNet(
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

	protected boolean inDifferentCases(String one, String other)
	{
		return one.toLowerCase().equals(other.toLowerCase())
				&& !one.equals(other);
	}

	@SuppressWarnings("unchecked")
	protected List<List<Entry<String, String>>> filterOutNounSequencesIdentifiedByWiki(
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

	/**
	 * @param capitalizationsBySingleNounTermFromWiki 
	 * @param capitalizationsByOriginalCaseFromWiki 
	 * @return <originalCase, capitalization>
	 */
	protected void findProperNounsInWikipedia(
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
		}

	}

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

	protected List<Entry<String, String>> mapPosTagToBasicForm(
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

	protected String concatenateTerms(List<Entry<String, String>> posTagsByTerm)
	{
		return concatenateTerms(posTagsByTerm, false, true);
	}

	protected String concatenateTerms(
			List<Entry<String, String>> posTagsByTerm,
			boolean ignorePossessiveCase, boolean lowerCase)
	{
		return concatenateTerms(posTagsByTerm, ignorePossessiveCase, lowerCase,
				" ");
	}

	private String concatenateTerms(List<Entry<String, String>> posTagsByTerm,
			boolean ignorePossessiveCase, String delimiter)
	{
		return concatenateTerms(posTagsByTerm, ignorePossessiveCase, true,
				delimiter);
	}

	private String concatenateTerms(List<Entry<String, String>> posTagsByTerm,
			boolean ignorePossessiveCase, boolean lowerCase, String delimiter)
	{
		if (posTagsByTerm.isEmpty())
		{
			return "";
		}

		String term = posTagsByTerm.get(0).getKey();
		if (lowerCase) // in case that month-term be capitalized by Stanford parser
		{
			term = term.toLowerCase(Locale.US);
		}
		StringBuilder stringBuilder = new StringBuilder(term);
		for (int index = 1; index < posTagsByTerm.size(); index++)
		{
			String posTag = posTagsByTerm.get(index).getValue();
			term = posTagsByTerm.get(index).getKey();
			if (lowerCase) // in case that month-term be capitalized by Stanford parser
			{
				term = term.toLowerCase(Locale.US);
			}

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
