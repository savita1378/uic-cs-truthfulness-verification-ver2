package edu.uic.cs.t_verifier.ml.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.uic.cs.t_verifier.ml.PersonNameIdentifier;
import edu.uic.cs.t_verifier.nlp.StatementTypeIdentifier.StatementType;

public class PersonNameIdentifierStanfordNERImpl implements
		PersonNameIdentifier
{

	private static final String SERIALIZED_CLASSIFIER_PATH_4_CLASSES_CASELESS = "StanfordNer_classifiers/english.conll.4class.caseless.distsim.crf.ser.gz";
	@SuppressWarnings("unchecked")
	private static final CRFClassifier<CoreLabel> CLASSIFIER_4_CLASSES_CASELESS = CRFClassifier
			.getClassifierNoExceptions(SERIALIZED_CLASSIFIER_PATH_4_CLASSES_CASELESS);

	//	private static final String SERIALIZED_CLASSIFIER_PATH_3_CLASSES_CASELESS = "StanfordNer_classifiers/english.all.3class.caseless.distsim.crf.ser.gz";
	//	@SuppressWarnings("unchecked")
	//	private static final CRFClassifier<CoreLabel> CLASSIFIER_3_CLASSES_CASELESS = CRFClassifier
	//			.getClassifierNoExceptions(SERIALIZED_CLASSIFIER_PATH_3_CLASSES_CASELESS);

	/*private NLPAnalyzerImpl4 nlpAnalyzer;

	private synchronized NLPAnalyzerImpl4 getNLPAnalyzer()
	{
		if (nlpAnalyzer == null)
		{
			nlpAnalyzer = new NLPAnalyzerImpl4();
		}

		return nlpAnalyzer;
	}*/

	@Override
	public List<Integer> identifyNameTermsWithinNounPhrase(
			List<Entry<String, String>> tagsByTermInSentence,
			Entry<String, String> tagsByTermBeforeNP,
			Entry<String, String> tagsByTermAfterNP)
	{
		List<CoreLabel> sentence = new ArrayList<CoreLabel>();
		for (Entry<String, String> tagByTerm : tagsByTermInSentence)
		{
			CoreLabel label = new CoreLabel();
			label.setWord(tagByTerm.getKey());
			label.setTag(tagByTerm.getValue());
			sentence.add(label);
		}

		/*String sentence = getNLPAnalyzer().concatenateTerms(
				tagsByTermInSentence);*/

		List<CoreLabel> classifiedLabels = CLASSIFIER_4_CLASSES_CASELESS
				.classify(sentence);
		//		List<CoreLabel> classifiedLabels = CLASSIFIER_3_CLASSES_CASELESS
		//				.classify(sentence);

		// Assert.isTrue(classifiedLabels.size() == 1);
		List<Integer> result = new ArrayList<Integer>();
		int index = 0;
		for (CoreLabel label : classifiedLabels/*.get(0)*/)
		{
			String typeString = label.get(AnswerAnnotation.class);
			// System.out.println(typeString);
			StatementType statementType = StatementType.parse(typeString);
			if (statementType == StatementType.PERSON)
			{
				result.add(index);
				// System.out.print(" " + label.word());
			}

			index++;
		}
		//		if (!result.isEmpty())
		//		{
		//			System.out.println();
		//		}

		return result;
	}

}
