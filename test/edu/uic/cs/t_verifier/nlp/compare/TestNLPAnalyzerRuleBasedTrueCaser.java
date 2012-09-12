package edu.uic.cs.t_verifier.nlp.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.uic.cs.t_verifier.TrecTopicsReaderWrapper;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.nlp.impl.NLPAnalyzerImpl4;

public class TestNLPAnalyzerRuleBasedTrueCaser
{
	private NLPAnalyzerImpl4 analyzer = new NLPAnalyzerImpl4();
	private StanfordCoreNLP tokenizer;

	private TestNLPAnalyzerRuleBasedTrueCaser()
	{
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos");
		tokenizer = new StanfordCoreNLP(props);
	}

	private List<String> getTrueCaseTokens(String textStringInLowerCase)
	{
		String trueCased = analyzer.capitalizeProperNounTerms(
				textStringInLowerCase, null);

		return getTokens(trueCased);
	}

	private List<String> getOriginalCasedTokens(String casedTextString)
	{
		return getTokens(casedTextString);
	}

	private List<String> getTokens(String textString)
	{
		Annotation text = new Annotation(textString);
		tokenizer.annotate(text);

		List<String> result = new ArrayList<String>();
		for (CoreLabel token : text.get(TokensAnnotation.class))
		{
			String pos = token.get(PartOfSpeechAnnotation.class);
			if (!StringUtils.isAlpha(pos)) // non-word
			{
				continue;
			}

			String tokenString = token.get(TextAnnotation.class);
			result.add(tokenString);
		}

		return result;
	}

	private void commit()
	{
		analyzer.commitCache();
	}

	public static void main(String[] args)
	{
		TestNLPAnalyzerRuleBasedTrueCaser tester = new TestNLPAnalyzerRuleBasedTrueCaser();

		TrecTopicsReaderWrapper trecTopicsReader = new TrecTopicsReaderWrapper();
		List<String> sentences = new ArrayList<String>();

		//		sentences
		//				.addAll(trecTopicsReader.readDescriptions("04.robust.testset"));
		sentences.addAll(trecTopicsReader
				.readDescriptions("08.qa.questions.txt"));
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
		try
		{
			for (int index = 0; index < sentences.size(); index++)
			{
				String sentence = sentences.get(index);
				System.out.println((index + 1) + "\t" + sentence);

				List<String> originalTokens = tester
						.getOriginalCasedTokens(sentence);
				// System.out.println(originalTokens);

				List<String> trueCasedTokens = tester
						.getTrueCaseTokens(sentence.toLowerCase());
				// System.out.println(trueCasedTokens);

				if (originalTokens.size() != trueCasedTokens.size())
				{
					// lower case "r&d" could be tokenized as "r & d"
					int tokenIndex = 0;
					while (originalTokens.get(tokenIndex).equalsIgnoreCase(
							trueCasedTokens.get(tokenIndex)))
					{
						tokenIndex++;
					} // find the unequal one

					String originalToken = originalTokens.get(tokenIndex);
					originalTokens.remove(tokenIndex);
					for (int charIndex = originalToken.length() - 1; charIndex >= 0; charIndex--)
					{
						char originalChar = originalToken.charAt(charIndex);
						originalTokens.add(tokenIndex, "" + originalChar);
					}

					Assert.isTrue(
							originalTokens.size() == trueCasedTokens.size(),
							"\n" + originalTokens + "\n" + trueCasedTokens);
				}

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
					Assert.isTrue(
							original.equalsIgnoreCase(trueCased),
							"Original["
									+ original
									+ "] should equals ignore case to True-cased["
									+ trueCased + "]");

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
			System.out.printf("\tTotal accuracy: %d/%d=%.3f\n",
					correctCasedNum, totalNumOfTerms,
					((double) correctCasedNum) / totalNumOfTerms);

			System.out
					.printf("\tNon-lower case Precision: %d/%d=%.3f  Recall: %d/%d=%.3f\n",
							correctNonLowerNum, totalNumOfRecognizedNonLower,
							((double) correctNonLowerNum)
									/ totalNumOfRecognizedNonLower,
							correctNonLowerNum, totalNumOfTrueNonLower,
							((double) correctNonLowerNum)
									/ totalNumOfTrueNonLower);
			System.out.printf(
					"\tLower case Precision: %d/%d=%.3f  Recall: %d/%d=%.3f\n",
					correctLowerNum, totalNumOfRecognizedLower,
					((double) correctLowerNum) / totalNumOfRecognizedLower,
					correctLowerNum, totalNumOfTrueLower,
					((double) correctLowerNum) / totalNumOfTrueLower);
		}
		finally
		{
			tester.commit(); // DO NOT forget this!
		}
	}
}
