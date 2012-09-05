package edu.uic.cs.t_verifier.nlp.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TrueCaseTextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.uic.cs.t_verifier.TrecTopicsReaderWrapper;
import edu.uic.cs.t_verifier.misc.Assert;

public class TestStanfordTrueCaser
{
	private StanfordCoreNLP trueCaser;
	private StanfordCoreNLP tokenizer;

	private TestStanfordTrueCaser()
	{
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, truecase");
		trueCaser = new StanfordCoreNLP(props);

		props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos");
		tokenizer = new StanfordCoreNLP(props);
	}

	private List<String> getTrueCaseTokens(String textStringInLowerCase)
	{
		Annotation text = new Annotation(textStringInLowerCase);
		trueCaser.annotate(text);

		List<String> result = new ArrayList<String>();
		for (CoreLabel token : text.get(TokensAnnotation.class))
		{
			String pos = token.get(PartOfSpeechAnnotation.class);
			if (!StringUtils.isAlpha(pos)) // non-word
			{
				continue;
			}

			String trueCase = token.get(TrueCaseTextAnnotation.class);
			result.add(trueCase);
		}

		return result;
	}

	private List<String> getOriginalCasedTokens(String casedTextString)
	{
		Annotation text = new Annotation(casedTextString);
		tokenizer.annotate(text);

		List<String> result = new ArrayList<String>();
		for (CoreLabel token : text.get(TokensAnnotation.class))
		{
			String pos = token.get(PartOfSpeechAnnotation.class);
			if (!StringUtils.isAlpha(pos)) // non-word
			{
				continue;
			}

			String trueCase = token.get(TextAnnotation.class);
			result.add(trueCase);
		}

		return result;
	}

	public static void main(String[] args)
	{
		TestStanfordTrueCaser tester = new TestStanfordTrueCaser();

		TrecTopicsReaderWrapper trecTopicsReader = new TrecTopicsReaderWrapper();
		List<String> sentences = trecTopicsReader
				.readDescriptions("08.qa.questions.txt");
		sentences.addAll(trecTopicsReader
				.readDescriptions("09.qa.questions.txt"));

		int totalNumOfTerms = 0;
		int correctCasedNum = 0;

		int totalNumOfTrueNonLower = 0;
		int totalNumOfRecognizedNonLower = 0;
		int correctNonLowerNum = 0;

		int totalNumOfTrueLower = 0;
		int totalNumOfRecognizedLower = 0;
		int correctLowerNum = 0;

		for (int index = 0; index < sentences.size(); index++)
		{
			String sentence = sentences.get(index);
			System.out.println((index + 1) + "\t" + sentence);

			List<String> originalTokens = tester
					.getOriginalCasedTokens(sentence);
			// System.out.println(originalTokens);

			List<String> trueCasedTokens = tester.getTrueCaseTokens(sentence
					.toLowerCase());
			// System.out.println(trueCasedTokens);

			Assert.isTrue(originalTokens.size() == trueCasedTokens.size());

			// consider non-lower as positive
			List<String> truePositive = new ArrayList<String>(); // correct
			List<String> trueNegative = new ArrayList<String>(); // correct
			List<String> falseNegative = new ArrayList<String>();
			List<String> falsePositive = new ArrayList<String>();

			for (int termIndex = 0; termIndex < originalTokens.size(); termIndex++)
			{
				if (termIndex == 0) // ignore the first term which is always capitalized
				{
					continue;
				}

				totalNumOfTerms++;

				String original = originalTokens.get(termIndex);
				String trueCased = trueCasedTokens.get(termIndex);

				boolean isOriginalInLowerCase = original.equals(original
						.toLowerCase());
				boolean isSameCase = original.equals(trueCased);

				if (!isOriginalInLowerCase) // term should be non-lower case; i.e. POSITIVE
				{
					totalNumOfTrueNonLower++;

					if (isSameCase) // identified as non-lower
					{
						totalNumOfRecognizedNonLower++;
						truePositive.add(original);
					}
					else
					// identified as lower case
					{
						totalNumOfRecognizedLower++;
						falseNegative.add(trueCased);
					}
				}
				else
				// term should be lower case; i.e. NEGATIVE
				{
					totalNumOfTrueLower++;

					if (isSameCase) // identified as lower case
					{
						totalNumOfRecognizedLower++;
						trueNegative.add(original);
					}
					else
					// but be identified as non-lower
					{
						totalNumOfRecognizedNonLower++;
						falsePositive.add(trueCased);
					}
				}
			}

			correctCasedNum += (truePositive.size() + trueNegative.size());
			correctNonLowerNum += truePositive.size();
			correctLowerNum += trueNegative.size();
			if (!falseNegative.isEmpty() || !falsePositive.isEmpty())
			{
				System.out.println(">>>\t" + falseNegative + " | "
						+ falsePositive);
			}

			System.out.println();
		}

		System.out.println("=================");
		System.out.printf("\tTotal accuracy: %d/%d=%.3f\n", correctCasedNum,
				totalNumOfTerms, ((double) correctCasedNum) / totalNumOfTerms);

		System.out.printf(
				"\tNon-lower case Precision: %d/%d=%.3f  Recall: %d/%d=%.3f\n",
				correctNonLowerNum, totalNumOfRecognizedNonLower,
				((double) correctNonLowerNum) / totalNumOfRecognizedNonLower,
				correctNonLowerNum, totalNumOfTrueNonLower,
				((double) correctNonLowerNum) / totalNumOfTrueNonLower);
		System.out.printf(
				"\tLower case Precision: %d/%d=%.3f  Recall: %d/%d=%.3f\n",
				correctLowerNum, totalNumOfRecognizedLower,
				((double) correctLowerNum) / totalNumOfRecognizedLower,
				correctLowerNum, totalNumOfTrueLower,
				((double) correctLowerNum) / totalNumOfTrueLower);
	}
}
