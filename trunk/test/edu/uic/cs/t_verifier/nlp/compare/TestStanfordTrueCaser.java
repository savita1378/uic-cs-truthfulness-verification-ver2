package edu.uic.cs.t_verifier.nlp.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TrueCaseTextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.uic.cs.t_verifier.Conll03NERReader;

public class TestStanfordTrueCaser extends AbstractTrueCaseTester
{
	private StanfordCoreNLP trueCaser;

	private TestStanfordTrueCaser()
	{
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, truecase");
		trueCaser = new StanfordCoreNLP(props);
	}

	protected List<List<String>> getTrueCaseTokens(String textStringInLowerCase)
	{
		Annotation document = new Annotation(textStringInLowerCase);
		trueCaser.annotate(document);

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

				String trueCase = token.get(TrueCaseTextAnnotation.class);
				sentenceTokens.add(trueCase);
			}
		}

		return result;
	}

	public static void main(String[] args)
	{
		TestStanfordTrueCaser tester = new TestStanfordTrueCaser();
		List<String> testSequences = new ArrayList<String>();

		//		TrecTopicsReaderWrapper trecTopicsReader = new TrecTopicsReaderWrapper();
		//		testSequences.addAll(trecTopicsReader
		//				.readDescriptions("04.robust.testset"));
		//		testSequences.addAll(trecTopicsReader
		//				.readDescriptions("08.qa.questions.txt"));
		//		testSequences.addAll(trecTopicsReader
		//				.readDescriptions("09.qa.questions.txt"));

		Conll03NERReader conll03nerReader = new Conll03NERReader();
		testSequences.addAll(conll03nerReader.readSentences("eng.testa"));

		tester.evaluate(testSequences);
	}

	@Override
	protected void commit()
	{
		// do nothing

	}
}
