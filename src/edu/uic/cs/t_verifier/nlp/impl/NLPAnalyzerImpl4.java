package edu.uic.cs.t_verifier.nlp.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
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
import edu.uic.cs.t_verifier.misc.ClassFactory;
import edu.uic.cs.t_verifier.misc.Config;

public class NLPAnalyzerImpl4 extends NLPAnalyzerImpl3
{
	private static final String POSTAG_POSSESSIVE = "POS";
	private static final String POSTAG_NOUN = "NN";

	private WikipediaContentExtractor wikipediaContentExtractor = ClassFactory
			.getInstance(Config.WIKIPEDIACONTENTEXTRACTOR_CLASS_NAME);

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
	//					return "F G H".equals(queryWords)
	//							|| "G H".equals(queryWords);
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
	//		String sentence = "A B C D E F G H I";
	//		impl.capitalizeProperNounTerms(sentence, null);
	//	}

	public static void main(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();
		NLPAnalyzerImpl4 impl = new NLPAnalyzerImpl4();

		// String sentence = "microsoft's corporate headquarters locates in redmond";
		// String sentence = "alan shepard is the first american in space";

		// impl.capitalizeProperNounTerms(sentence, null);

		for (Statement statement : statements)
		{
			List<String> allAlternativeUnits = statement.getAlternativeUnits();
			List<String> allAlternativeStatements = statement
					.getAllAlternativeStatements();

			String alternativeUnit = allAlternativeUnits.get(0);
			String sentence = allAlternativeStatements.get(0);

			System.out.println("["
					+ statement.getId()
					+ "] "
					+ sentence.replace(alternativeUnit, "[" + alternativeUnit
							+ "]"));

			impl.capitalizeProperNounTerms(sentence, null);

			//			for (int index = 0; index < allAlternativeStatements.size(); index++)
			//			{
			//				String alternativeUnit = allAlternativeUnits.get(index);
			//				String sentence = allAlternativeStatements.get(index);
			//
			//				System.out.println("["
			//						+ statement.getId()
			//						+ "] "
			//						+ sentence.replace(alternativeUnit, "["
			//								+ alternativeUnit + "]"));
			//
			//				impl.capitalizeProperNounTerms(sentence, null);
			//
			//			}

			System.out.println();
		}

	}

	@Override
	protected String capitalizeProperNounTerms(String sentence,
			List<List<String>> nounPhrases)
	{
		sentence = sentence.trim();
		// sentence = StringUtils.capitalize(sentence);

		List<Entry<String, String>> posTagsByTerm = parseIntoPosTagByTerms(sentence);
		// split by the punctuation
		List<List<Entry<String, String>>> posTagsByTermOfSubSentences = splitByPunctuations(posTagsByTerm);

		// Find the proper noun in Wikiepdia
		List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWiki = findProperNounsInWikipedia(
				sentence, posTagsByTermOfSubSentences);
		System.out.println(">>>>>\t" + capitalizationsByOriginalCaseFromWiki);

		// Get all noun sequences
		List<List<Entry<String, String>>> nounSequences = findNounSequences(posTagsByTermOfSubSentences);
		System.out.println(">>>>>\t" + nounSequences);

		// Find those noun(sequence)s which have not been identified by Wikipedia
		List<List<Entry<String, String>>> nounSequenceNotIdentifiedByWiki = filterOutNounSequencesIdentifiedByWiki(
				nounSequences, capitalizationsByOriginalCaseFromWiki);
		System.out.println(">>>>>\t" + nounSequenceNotIdentifiedByWiki);

		// Find the proper noun in WordNet
		List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWordNet = findProperNounsInWordNet(nounSequenceNotIdentifiedByWiki);
		System.out
				.println(">>>>>\t" + capitalizationsByOriginalCaseFromWordNet);

		// FIXME nounPhrases needs to be filled
		// TODO Auto-generated method stub
		return null;
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

	private List<List<Entry<String, String>>> filterOutNounSequencesIdentifiedByWiki(
			List<List<Entry<String, String>>> nounSequences,
			List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWiki)
	{
		if (capitalizationsByOriginalCaseFromWiki.isEmpty()) // wikiepda has not identified any 
		{
			return nounSequences;
		}

		List<List<Entry<String, String>>> squencesFromWiki = new ArrayList<List<Entry<String, String>>>(
				capitalizationsByOriginalCaseFromWiki.size());
		for (Entry<List<Entry<String, String>>, String> entry : capitalizationsByOriginalCaseFromWiki)
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
	 * @return <originalCase, capitalization>
	 */
	private List<Entry<List<Entry<String, String>>, String>> findProperNounsInWikipedia(
			String sentence,
			List<List<Entry<String, String>>> posTagsByTermOfSubSentences)
	{
		List<Entry<List<Entry<String, String>>, MatchedQueryKey>> matchedSequenceInfo = new ArrayList<Entry<List<Entry<String, String>>, MatchedQueryKey>>();
		for (List<Entry<String, String>> posTagsByTermOfSubSentence : posTagsByTermOfSubSentences)
		{
			recursiveMatchWikipediaArticleTitle(
					Collections.singletonList(posTagsByTermOfSubSentence),
					matchedSequenceInfo);
		}

		if (matchedSequenceInfo.isEmpty())
		{
			return Collections.emptyList();
		}

		//		System.out.println();
		List<Entry<List<Entry<String, String>>, String>> result = new ArrayList<Entry<List<Entry<String, String>>, String>>();
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
			result.add(properNounByOriginalCase);
		}

		return result;
	}

	private Entry<List<Entry<String, String>>, String> checkWhetherProperNoun(
			List<Entry<String, String>> posTagsByMatchedTerm,
			MatchedQueryKey matchedQueryKey)
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

	private void recursiveMatchWikipediaArticleTitle(
			List<List<Entry<String, String>>> currentLevelPosTagByTermList,
			List<Entry<List<Entry<String, String>>, MatchedQueryKey>> matchedSequenceInfo)
	{
		int currentLevelSize = currentLevelPosTagByTermList.size();

		for (int index = 0; index < currentLevelPosTagByTermList.size(); index++)
		{
			List<Entry<String, String>> currentLevelPosTagsByTerm = currentLevelPosTagByTermList
					.get(index);

			int currentLevelSequenceLength = currentLevelPosTagsByTerm.size();
			if (currentLevelSequenceLength <= 1)
			{
				return; // there's only one term in current level, ignore it, since we want phrase
			}

			if (isEitherSideStopWord(currentLevelPosTagsByTerm))
			{
				continue;
			}

			String concatenatedTerms = concatenateTerms(currentLevelPosTagsByTerm);
			//			System.out.println(currentLevelSequenceLength + " "
			//					+ concatenatedTerms);

			MatchedQueryKey matchedQueryKey = wikipediaContentExtractor
					.matchQueryKey(concatenatedTerms);
			if (matchedQueryKey != null && matchedQueryKey.isCertainly()) // TODO here, we ignore the overlapping situation
			{
				// matched!
				matchedSequenceInfo
						.add(new SimpleEntry<List<Entry<String, String>>, MatchedQueryKey>(
								currentLevelPosTagsByTerm, matchedQueryKey));

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
						nextLevelAheadSequence = currentLevelAheadSequence
								.subList(0,
										currentLevelAheadSequence.size() - 1);

						nextLevelAheadPosTagByTermList
								.add(nextLevelAheadSequence);
					}

					nextLevelAheadSequence = currentLevelAheadSequence.subList(
							1, currentLevelAheadSequence.size());
					nextLevelAheadPosTagByTermList.add(nextLevelAheadSequence);

					// recursively invoke next level
					recursiveMatchWikipediaArticleTitle(
							nextLevelAheadPosTagByTermList, matchedSequenceInfo);
				}
				else
				{
					List<Entry<String, String>> firstInCurrentLevel = currentLevelPosTagByTermList
							.get(0);
					List<Entry<String, String>> nextLevelAheadPosTagsByTerm = firstInCurrentLevel
							.subList(0, index);
					recursiveMatchWikipediaArticleTitle(
							Collections
									.singletonList(nextLevelAheadPosTagsByTerm),
							matchedSequenceInfo);
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

					recursiveMatchWikipediaArticleTitle(
							currentLevelRemainingPosTagByTermList,
							matchedSequenceInfo);
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
					List<Entry<String, String>> nextLevelRemainingPosTagsByTerm = lastInCurrentLevel
							.subList(startIndex, lastInCurrentLevel.size());

					recursiveMatchWikipediaArticleTitle(
							Collections
									.singletonList(nextLevelRemainingPosTagsByTerm),
							matchedSequenceInfo);
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
			nextLevelSequence = currentLevelSequence.subList(0,
					currentLevelSequence.size() - 1);

			nextLevelPosTagByTermList.add(nextLevelSequence);
		}

		nextLevelSequence = currentLevelSequence.subList(1,
				currentLevelSequence.size());
		nextLevelPosTagByTermList.add(nextLevelSequence);

		// recursively invoke next level
		recursiveMatchWikipediaArticleTitle(nextLevelPosTagByTermList,
				matchedSequenceInfo);
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
