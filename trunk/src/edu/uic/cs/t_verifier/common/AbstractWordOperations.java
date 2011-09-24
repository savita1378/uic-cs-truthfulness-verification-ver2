package edu.uic.cs.t_verifier.common;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.PorterStemmerExporter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.GeneralException;

public abstract class AbstractWordOperations
{
	private PorterStemmerExporter porterStemmer = new PorterStemmerExporter();

	public List<String> standardAnalyzeUsingDefaultStopWords(String rawString)
	{
		return standardAnalyze(rawString, StandardAnalyzer.STOP_WORDS_SET);
	}

	public List<String> standardAnalyzeWithoutRemovingStopWords(String rawString)
	{
		return standardAnalyze(rawString, null);
	}

	protected List<String> porterStemmingAnalyzeUsingDefaultStopWords(
			String rawString)
	{
		TokenStream tokenStream = constructStandardTokenStream(rawString,
				StandardAnalyzer.STOP_WORDS_SET);
		tokenStream = new PorterStemFilter(tokenStream);
		CharTermAttribute termAttribute = tokenStream
				.addAttribute(CharTermAttribute.class);

		List<String> result = new ArrayList<String>();
		try
		{
			while (tokenStream.incrementToken())
			{
				// replace ',' for number like '3,000,230'
				result.add(termAttribute.toString().replace(",", ""));
			}
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}

		return result;
	}

	protected String stem(String word)
	{
		return porterStemmer.stem(word);
	}

	/**
	 * NO stem! 
	 * 
	 * StandardTokenizer -> StandardFilter -> LowerCaseFilter -> StopFilter
	 */
	private List<String> standardAnalyze(String rawString, Set<?> stopWords)
	{
		TokenStream tokenStream = constructStandardTokenStream(rawString,
				stopWords);
		CharTermAttribute termAttribute = tokenStream
				.addAttribute(CharTermAttribute.class);

		List<String> result = new ArrayList<String>();
		try
		{
			while (tokenStream.incrementToken())
			{
				// replace ',' for number like '3,000,230'
				result.add(termAttribute.toString().replace(",", ""));
			}
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}

		return result;
	}

	private TokenStream constructStandardTokenStream(String rawString,
			Set<?> stopWords)
	{
		StandardAnalyzer standardAnalyzer = new StandardAnalyzer(
				Config.LUCENE_VERSION, stopWords); // Use LUCENE_30 to support "'s"

		TokenStream tokenStream = standardAnalyzer.tokenStream(null,
				new StringReader(rawString));

		return tokenStream;
	}

	protected String trimStopWordsInBothSides(String rawString)
	{
		List<String> words = standardAnalyzeWithoutRemovingStopWords(rawString);

		int firstNonStopWordIndex = words.size();
		int lastNonStopWordIndex = -1;
		for (int index = 0; index < words.size(); index++)
		{
			String word = words.get(index);
			if (!isStopWord(word))
			{
				firstNonStopWordIndex = index;
				break;
			}
		}

		for (int index = words.size() - 1; index >= 0; index--)
		{
			String word = words.get(index);
			if (!isStopWord(word))
			{
				lastNonStopWordIndex = index;
				break;
			}
		}

		StringBuilder stringBuilder = new StringBuilder();
		if (lastNonStopWordIndex >= 0)
		{
			for (int index = firstNonStopWordIndex; index < lastNonStopWordIndex; index++)
			{
				stringBuilder.append(words.get(index));
				stringBuilder.append(' ');
			}
			stringBuilder.append(words.get(lastNonStopWordIndex));
		}

		return stringBuilder.toString();
	}

	protected boolean isStopWord(String word)
	{
		return StandardAnalyzer.STOP_WORDS_SET.contains(word);
	}

}
