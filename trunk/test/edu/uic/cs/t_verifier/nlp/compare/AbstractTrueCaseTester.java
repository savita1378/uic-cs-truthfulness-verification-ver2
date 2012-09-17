package edu.uic.cs.t_verifier.nlp.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.nlp.impl.AbstractNLPOperations;

public abstract class AbstractTrueCaseTester extends AbstractNLPOperations
{
	protected static Logger logger = Logger.getLogger("TRUECASER_EVALUATION");
	private StanfordCoreNLP tokenizer;

	public AbstractTrueCaseTester()
	{
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos");
		tokenizer = new StanfordCoreNLP(props);
	}

	protected List<List<String>> getOriginalCasedTokens(String casedTextString)
	{
		Annotation document = new Annotation(casedTextString);
		tokenizer.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		List<List<String>> result = new ArrayList<List<String>>(
				sentences.size());
		for (CoreMap sentence : sentences)
		{
			List<String> sentenceTokens = new ArrayList<String>();
			result.add(sentenceTokens);

			for (CoreLabel token : sentence.get(TokensAnnotation.class))
			{
				String pos = mapPosTagToBasicForm(token
						.get(PartOfSpeechAnnotation.class));
				if (!StringUtils.isAlpha(pos)) // non-word
				{
					continue;
				}

				String tokenString = token.get(TextAnnotation.class);
				sentenceTokens.add(tokenString);
			}
		}

		return result;
	}

	protected void evaluate(List<String> testSequences)
	{
		int totalNumOfTerms = 0;
		int correctCasedNum = 0;

		int totalNumOfTrueNonLower = 0;
		int totalNumOfRecognizedNonLower = 0;
		int correctNonLowerNum = 0;

		int totalNumOfTrueLower = 0;
		int totalNumOfRecognizedLower = 0;
		int correctLowerNum = 0;

		for (int index = 0; index < testSequences.size(); index++)
		{
			//			if (index + 1 != 1282)
			//			{
			//				continue;
			//			}

			String testSequence = testSequences.get(index);
			logger.info((index + 1) + "\t" + testSequence);

			try
			{
				List<List<String>> originalSentence = getOriginalCasedTokens(testSequence);
				List<List<String>> trueCasedSentence = getTrueCaseTokens(testSequence
						.toLowerCase());

				Assert.isTrue(originalSentence.size() == trueCasedSentence
						.size());

				// consider non-lower as positive
				List<String> truePositive = new ArrayList<String>(); // correct
				List<String> trueNegative = new ArrayList<String>(); // correct
				List<String> falseNegative = new ArrayList<String>();
				List<String> falsePositive = new ArrayList<String>();

				for (int i = 0; i < originalSentence.size(); i++)
				{
					List<String> originalTokens = originalSentence.get(i);
					// println(originalTokens);

					List<String> trueCasedTokens = trueCasedSentence.get(i);
					// println(trueCasedTokens);

					if (originalTokens.size() != trueCasedTokens.size())
					{
						alignment(originalTokens, trueCasedTokens);
					}

					for (int termIndex = 0; termIndex < originalTokens.size(); termIndex++)
					{
						if (termIndex == 0) // ignore the first term which is always capitalized
						{
							continue;
						}

						String original = originalTokens.get(termIndex);
						String trueCased = trueCasedTokens.get(termIndex);

						boolean isOriginalInLowerCase = original
								.equals(original.toLowerCase());

						boolean isSameCase = original.equals(trueCased);
						Assert.isTrue(
								original.equalsIgnoreCase(trueCased),
								"Original["
										+ original
										+ "] should equals ignore case to True-cased["
										+ trueCased + "]");

						if (isPunctuation(original))
						{
							continue;
						}

						totalNumOfTerms++;

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
					} // for one sentence
				} // for all sentences

				correctCasedNum += (truePositive.size() + trueNegative.size());
				correctNonLowerNum += truePositive.size();
				correctLowerNum += trueNegative.size();
				if (!falseNegative.isEmpty() || !falsePositive.isEmpty())
				{
					logger.info(">>>\t" + falseNegative + " | " + falsePositive);
				}

			}
			catch (Throwable th)
			{
				logger.error(index + 1, th);
				commit();
			}

			logger.info("");
		}

		logger.info("=================");

		logger.info(String.format("\tTotal accuracy: %d/%d=%.3f\n",
				correctCasedNum, totalNumOfTerms, ((double) correctCasedNum)
						/ totalNumOfTerms));

		logger.info(String.format(
				"\tNon-lower case Precision: %d/%d=%.3f  Recall: %d/%d=%.3f\n",
				correctNonLowerNum, totalNumOfRecognizedNonLower,
				((double) correctNonLowerNum) / totalNumOfRecognizedNonLower,
				correctNonLowerNum, totalNumOfTrueNonLower,
				((double) correctNonLowerNum) / totalNumOfTrueNonLower));
		logger.info(String.format(
				"\tLower case Precision: %d/%d=%.3f  Recall: %d/%d=%.3f\n",
				correctLowerNum, totalNumOfRecognizedLower,
				((double) correctLowerNum) / totalNumOfRecognizedLower,
				correctLowerNum, totalNumOfTrueLower,
				((double) correctLowerNum) / totalNumOfTrueLower));
	}

	private boolean isPunctuation(String original)
	{
		original = original.trim();
		for (char ch : original.toCharArray())
		{
			if (Character.isLetterOrDigit(ch))
			{
				return false;
			}
		}

		return true;
	}

	private void alignment(List<String> originalTokens,
			List<String> trueCasedTokens)
	{
		System.out.println(originalTokens);
		System.out.println(trueCasedTokens);
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

		/**
		 * [Morris, ', departure, raised, fears, that, Clinton, would, veer, more, to, the, left, in, a, second, term]
		 * [Morris, departure, raised, fears, that, Clinton, would, veer, more, to, the, left, in, a, second, term]
		 */
		if (originalTokens.get(0).equals("Morris")
				&& originalTokens.get(2).equals("departure"))
		{
			trueCasedTokens.add(1, "'");
			System.out.println(originalTokens);
			System.out.println(trueCasedTokens);
		}

		Assert.isTrue(originalTokens.size() == trueCasedTokens.size(), "\n"
				+ originalTokens + "\n" + trueCasedTokens);
	}

	abstract protected List<List<String>> getTrueCaseTokens(String lowerCase);

	abstract protected void commit();

}