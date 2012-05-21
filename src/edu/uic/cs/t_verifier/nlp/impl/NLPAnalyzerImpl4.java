package edu.uic.cs.t_verifier.nlp.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.Tree;
import edu.uic.cs.t_verifier.html.WikipediaContentExtractor;
import edu.uic.cs.t_verifier.html.data.MatchedQueryKey;
import edu.uic.cs.t_verifier.misc.ClassFactory;
import edu.uic.cs.t_verifier.misc.Config;

public class NLPAnalyzerImpl4 extends NLPAnalyzerImpl3
{
	private static final String POSTAG_POSSESSIVE = "POS";

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
		NLPAnalyzerImpl4 impl = new NLPAnalyzerImpl4();

		String sentence = "microsoft's corporate headquarters locates in redmond, it is a beautiful place. ";
		impl.capitalizeProperNounTerms(sentence, null);
	}

	@Override
	protected String capitalizeProperNounTerms(String sentence,
			List<List<String>> nounPhrases)
	{
		sentence = StringUtils.capitalize(sentence.trim());
		List<Entry<String, String>> posTagsByTerm = parseIntoPosTagByTerms(sentence);
		// split by the punctuations
		List<List<Entry<String, String>>> posTagsByTermOfSubSentences = splitByPunctuations(posTagsByTerm);

		List<Entry<List<Entry<String, String>>, MatchedQueryKey>> matchedSequenceInfo = new ArrayList<Entry<List<Entry<String, String>>, MatchedQueryKey>>();
		for (List<Entry<String, String>> posTagsByTermOfSubSentence : posTagsByTermOfSubSentences)
		{
			recursiveMatchWikipediaArticleTitle(
					Collections.singletonList(posTagsByTermOfSubSentence),
					matchedSequenceInfo);
		}

		if (matchedSequenceInfo.isEmpty())
		{
			return sentence;
		}

		//		System.out.println();
		for (Entry<List<Entry<String, String>>, MatchedQueryKey> entry : matchedSequenceInfo)
		{
			String matchedTerms = concatenateTerms(entry.getKey());
			String matchedURL = entry.getValue().getCertainPageUrl();
			//			String matchedKeyword = entry.getValue().getKeyWord();
			//			System.out.println(matchedTerms);
			//			System.out.println(matchedKeyword);
			//			System.out.println(matchedURL);
			//			System.out.println();

			// FIXME be aware of the POSSESSIVE symbol
		}

		// FIXME nounPhrases needs to fill

		// TODO Auto-generated method stub
		return null;
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

	private String concatenateTerms(List<Entry<String, String>> posTagsByTerm)
	{
		StringBuilder stringBuilder = new StringBuilder(posTagsByTerm.get(0)
				.getKey());
		for (int index = 1; index < posTagsByTerm.size(); index++)
		{
			String posTag = posTagsByTerm.get(index).getValue();
			String term = posTagsByTerm.get(index).getKey();

			if (POSTAG_POSSESSIVE.equals(posTag))
			{
				stringBuilder.append(term);
			}
			else
			{
				stringBuilder.append(" ").append(term);
			}
		}

		return stringBuilder.toString();
	}
}
