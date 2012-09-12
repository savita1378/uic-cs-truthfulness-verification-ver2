package edu.uic.cs.t_verifier.nlp.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

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

	protected boolean isPunctuation(String posTag)
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

}