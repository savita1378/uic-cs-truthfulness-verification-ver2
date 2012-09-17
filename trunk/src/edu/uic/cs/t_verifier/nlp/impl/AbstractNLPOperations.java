package edu.uic.cs.t_verifier.nlp.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.lucene.analysis.StopAnalyzer;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.uic.cs.t_verifier.common.AbstractWordOperations;
import edu.uic.cs.t_verifier.nlp.WordNetReader;

public abstract class AbstractNLPOperations extends AbstractWordOperations
{
	private static final List<String> PUNCTUATIONS = Arrays
			.asList(new String[] { ".", ",", ":" });

	protected static final String POSTAG_POSSESSIVE = "POS";
	protected static final String POSTAG_DETERMINER = "DT";
	protected static final String POSTAG_DOLLAR = "$";
	protected static final String POSTAG_NOUN = "NN";

	protected static final String PARENTHESES_LEFT = "-lrb-";
	protected static final String PARENTHESES_RIGHT = "-rrb-";
	protected static final List<String> PARENTHESES_VALUES = Arrays.asList(
			PARENTHESES_LEFT, PARENTHESES_RIGHT);

	public static interface RecursiveMatcher<T>
	{
		boolean isMatched(
				List<Entry<String, String>> currentLevelPosTagsByTermSequence);

		T getMatchedInfo();
	}

	protected WordNetReader wordNetReader = new WordNetReaderImpl();

	protected String mapPosTagToBasicForm(String posTag)
	{
		if ("JJR".equals(posTag) || "JJS".equals(posTag))
		{
			return "JJ";
		}

		if ("NNS".equals(posTag) || "NNP".equals(posTag)
				|| "NNPS".equals(posTag))
		{
			return "NN";
		}

		if ("PRP$".equals(posTag))
		{
			return "PRP";
		}

		if ("RBR".equals(posTag) || "RBS".equals(posTag))
		{
			return "RB";
		}

		if ("VBD".equals(posTag) || "VBG".equals(posTag)
				|| "VBN".equals(posTag) || "VBP".equals(posTag)
				|| "VBZ".equals(posTag))
		{
			return "VB";
		}

		if ("WP$".equals(posTag))
		{
			return "WP";
		}

		return posTag;
	}

	protected List<List<Entry<String, String>>> splitByPunctuations(
			List<Entry<String, String>> posTagsByTerm)
	{
		List<List<Entry<String, String>>> result = new ArrayList<List<Entry<String, String>>>();

		List<Entry<String, String>> subSentence = null;
		boolean newSubSentence = true;
		for (Entry<String, String> pair : posTagsByTerm)
		{
			if (!isSplitSentencePunctuation(pair.getValue()))
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

	private boolean isSplitSentencePunctuation(String posTag)
	{
		return PUNCTUATIONS.contains(posTag);
	}

	//	private List<Entry<String, String>> parseIntoPosTagByTerms(String sentence)
	//	{
	//		return parseIntoPosTagByTerms(sentence, true);
	//	}

	protected List<Entry<String, String>> parseIntoPosTagByTerms(
			String sentence, boolean mapTagToBasicForm)
	{
		Annotation document = new Annotation(sentence);
		getAnnotater().annotate(document);

		List<Entry<String, String>> result = new ArrayList<Entry<String, String>>();
		for (CoreLabel token : document.get(TokensAnnotation.class))
		{
			String pos = mapTagToBasicForm ? mapPosTagToBasicForm(token
					.get(PartOfSpeechAnnotation.class)) : token
					.get(PartOfSpeechAnnotation.class);
			String tokenString = token.get(TextAnnotation.class);
			tokenString = tokenString.replace("\\/", "/");

			result.add(new SimpleEntry<String, String>(tokenString, pos));
		}

		return result;
	}

	private StanfordCoreNLP annotater = null;

	protected StanfordCoreNLP getAnnotater()
	{
		if (annotater == null)
		{
			synchronized (AbstractNLPOperations.class)
			{
				if (annotater == null)
				{
					Properties props = new Properties();
					props.put("annotators", "tokenize, ssplit, pos");
					annotater = new StanfordCoreNLP(props);
				}
			}
		}

		return annotater;
	}

	protected <T> void recursiveMatchTerms(RecursiveMatcher<T> matcher,
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
				if (!isSplitSentencePunctuation(posTag)
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

}