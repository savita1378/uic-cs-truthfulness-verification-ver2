package edu.uic.cs.t_verifier.ml.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.util.Span;

import org.apache.commons.io.IOUtils;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesSimple;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.ml.PersonNameIdentifier;
import edu.uic.cs.t_verifier.nlp.PersonNameMatcher;
import edu.uic.cs.t_verifier.nlp.PersonNameMatcher.NameType;
import edu.uic.cs.t_verifier.nlp.impl.NLPAnalyzerImpl4;
import edu.uic.cs.t_verifier.nlp.impl.OpenNLPChunker;
import edu.uic.cs.t_verifier.nlp.impl.OpenNLPChunker.ChunkType;
import edu.uic.cs.t_verifier.nlp.impl.PersonNameMatcherImpl;

public class PersonNameIdentifierImpl implements PersonNameIdentifier
{
	private static final String TRAINED_MODEL_NAME = "person.name.model";

	private static final String TRAINING_FILE_NAME = "personName-training.txt";

	private static final String PERSON_TAG_BEGIN = "<name>";
	private static final String PERSON_TAG_END = "</name>";

	private static final Pattern PERSON_PATTERN = Pattern
			.compile(PERSON_TAG_BEGIN + ".+?" + PERSON_TAG_END);

	private static final String NA = "NA";

	private static final String DATA_SET_NAME = "NP_PERSON_NAME";

	private static final String[] ALL_BOOLEAN_VALUES = { "true", "false" };

	private static final String[] ALL_POS_TAGS = { "CC", "CD", "DT", "EX",
			"FW", "IN", "JJ", /*"JJR", "JJS",*/"LS", "MD", "NN", /*"NNS", "NNP", "NNPS",*/
			"PDT", "POS", "PRP", /*"PRP$",*/"RB", /*"RBR", "RBS",*/"RP",
			"SYM", "TO", "UH", "VB", /*"VBD", "VBG", "VBN", "VBP", "VBZ",*/
			"WDT", "WP", /*"WP$",*/
			"WRB", PersonNameIdentifierImpl.NA };

	private static final int ATTRIBUTES_TOTAL_NUMBER = 12 + 1; // plus 1 for target

	private static final String ATTRIBUTE_NAME_NP_LENGTH = "NP_LENGTH";
	private static final String ATTRIBUTE_NAME_IS_FIRST = "IS_FIRST";
	private static final String ATTRIBUTE_NAME_IS_LAST = "IS_LAST";
	private static final String ATTRIBUTE_NAME_NAME_TYPE = "NAME_TYPE";
	private static final String ATTRIBUTE_NAME_FREQUENCY = "FREQUENCY";
	private static final String ATTRIBUTE_NAME_POS = "POS";
	private static final String ATTRIBUTE_NAME_PREVIOUS_NAME_TYPE = "PREVIOUS_NAME_TYPE";
	private static final String ATTRIBUTE_NAME_PREVIOUS_FREQUENCY = "PREVIOUS_FREQUENCY";
	private static final String ATTRIBUTE_NAME_PREVIOUS_POS = "PREVIOUS_POS";
	private static final String ATTRIBUTE_NAME_NEXT_NAME_TYPE = "NEXT_NAME_TYPE";
	private static final String ATTRIBUTE_NAME_NEXT_FREQUENCY = "NEXT_FREQUENCY";
	private static final String ATTRIBUTE_NAME_NEXT_POS = "NEXT_POS";
	private static final String ATTRIBUTE_NAME_IS_NAME = "IS_NAME";

	private static final FastVector ATTRIBUTE_DOMAIN_BOOLEAN = new FastVector(
			ALL_BOOLEAN_VALUES.length);
	static
	{
		for (String value : ALL_BOOLEAN_VALUES)
		{
			ATTRIBUTE_DOMAIN_BOOLEAN.addElement(value);
		}
	}

	private static final FastVector ATTRIBUTE_DOMAIN_NAME_TYPE = new FastVector(
			NameType.values().length);
	static
	{
		for (NameType nameType : NameType.values())
		{
			ATTRIBUTE_DOMAIN_NAME_TYPE.addElement(nameType.toString());
		}
	}

	private static final FastVector ATTRIBUTE_DOMAIN_POS_TAG = new FastVector(
			ALL_POS_TAGS.length);
	static
	{
		for (String posTag : ALL_POS_TAGS)
		{
			ATTRIBUTE_DOMAIN_POS_TAG.addElement(posTag);
		}
	}

	private NLPAnalyzerImpl4 nlpAnalyzer = null;
	private OpenNLPChunker openNLPChunker = new OpenNLPChunker();
	private PersonNameMatcher nameMatcher = new PersonNameMatcherImpl();

	private Classifier classifier = null;
	private FastVector attributeDefinitions = null;

	/**
	 * Notice: POS tag must be in basic form
	 */
	@Override
	public List<Integer> identifyNameTermsWithinNounPhrase(
			List<Entry<String, String>> tagsByTermInNP,
			Entry<String, String> tagsByTermBeforeNP,
			Entry<String, String> tagsByTermAfterNP)
	{
		if (classifier == null)
		{
			synchronized (this)
			{
				if (classifier == null)
				{
					classifier = getTrainedClassifierFromDisk();
				}
			}
		}

		if (attributeDefinitions == null)
		{
			synchronized (this)
			{
				if (attributeDefinitions == null)
				{
					attributeDefinitions = createAttributeDefinitions();
				}
			}
		}

		int npLength = tagsByTermInNP.size();
		List<Integer> result = new ArrayList<Integer>(npLength);

		Instances unlabeled = new Instances(DATA_SET_NAME,
				attributeDefinitions, npLength);
		unlabeled.setClassIndex(ATTRIBUTES_TOTAL_NUMBER - 1);

		for (int index = 0; index < npLength; index++)
		{
			boolean isFirstInNP = (index == 0);
			boolean isLastInNP = (index == npLength - 1);

			Entry<String, String> tagByTerm = tagsByTermInNP.get(index);
			String term = tagByTerm.getKey();
			NameType nameType = nameMatcher.typeOf(term);
			Double frequency = nameMatcher.getMaxFrequency(term);
			String pos = getNLPAnalyzer().mapPosTagToBasicForm(
					tagByTerm.getValue());

			Entry<String, String> previous = (index == 0) ? tagsByTermBeforeNP
					: tagsByTermInNP.get(index - 1);
			String previousTerm = previous != null ? previous.getKey() : null;
			NameType previousNameType = nameMatcher.typeOf(previousTerm);
			Double previousFrequency = nameMatcher
					.getMaxFrequency(previousTerm);
			String previousPos = previous != null ? getNLPAnalyzer()
					.mapPosTagToBasicForm(previous.getValue()) : NA;

			Entry<String, String> next = (index == tagsByTermInNP.size() - 1) ? tagsByTermAfterNP
					: tagsByTermInNP.get(index + 1);
			String nextTerm = next != null ? next.getKey() : null;
			NameType nextNameType = nameMatcher.typeOf(nextTerm);
			Double nextFrequency = nameMatcher.getMaxFrequency(nextTerm);
			String nextPos = next != null ? getNLPAnalyzer()
					.mapPosTagToBasicForm(next.getValue()) : NA;

			Instance instance = new Instance(ATTRIBUTES_TOTAL_NUMBER);
			instance.setDataset(unlabeled);

			int attIndex = 0;
			instance.setValue(attIndex++, npLength);
			instance.setValue(attIndex++, Boolean.toString(isFirstInNP));
			instance.setValue(attIndex++, Boolean.toString(isLastInNP));

			instance.setValue(attIndex++, nameType.toString());
			instance.setValue(attIndex++, frequency);
			instance.setValue(attIndex++, pos);

			instance.setValue(attIndex++, previousNameType.toString());
			instance.setValue(attIndex++, previousFrequency);
			instance.setValue(attIndex++, previousPos);

			instance.setValue(attIndex++, nextNameType.toString());
			instance.setValue(attIndex++, nextFrequency);
			instance.setValue(attIndex++, nextPos);

			try
			{
				double classLabel = classifier.classifyInstance(instance);
				String classValue = unlabeled.classAttribute().value(
						(int) classLabel);
				System.out.print(classValue + " ");

				if (Boolean.parseBoolean(classValue))
				{
					result.add(index);
				}
			}
			catch (Exception e)
			{
				throw new GeneralException(e);
			}
		}

		System.out.println();

		return result;
	}

	private synchronized NLPAnalyzerImpl4 getNLPAnalyzer()
	{
		if (nlpAnalyzer == null)
		{
			nlpAnalyzer = new NLPAnalyzerImpl4();
		}

		return nlpAnalyzer;
	}

	private void addSentenceIntoTrainingData(String sentence, Instances dataset)
	{
		sentence = sentence.trim();

		Set<String> nameTerms = new HashSet<String>();
		List<String[]> names = retrieveNames(sentence);
		if (names != null)
		{
			for (String[] name : names)
			{
				for (String nameTerm : name)
				{
					nameTerms.add(nameTerm);
				}
			}
		}

		////////////////////////////////////////////////////////////////////////
		sentence = sentence.replace(PERSON_TAG_BEGIN, "")
				.replace(PERSON_TAG_END, "").replaceAll("\\s{2,}", " ");

		List<Entry<String, String>> posTagsByTerm = getNLPAnalyzer()
				.parseIntoPosTagByTerms(sentence, false); // this one is not in basic form!!
		System.out.print(posTagsByTerm + "\t|\t");
		// split by the punctuation
		List<List<Entry<String, String>>> originalPosTagsByTermOfSubSentences = getNLPAnalyzer()
				.splitByPunctuations(posTagsByTerm); // for chunking

		for (List<Entry<String, String>> tagsByTerm : originalPosTagsByTermOfSubSentences)
		{
			// one sub-sentence

			List<Span> nounPhraseSpans = openNLPChunker.getChunkSpans(
					tagsByTerm, ChunkType.NP);

			for (Span nounPhraseSpan : nounPhraseSpans)
			{
				System.out.print(tagsByTerm.subList(nounPhraseSpan.getStart(),
						nounPhraseSpan.getEnd()) + "; ");
				mapAndAddNounPhraseIntoInstances(nounPhraseSpan, tagsByTerm,
						nameTerms, dataset);
			}
		}

		System.out.println();

	}

	private void mapAndAddNounPhraseIntoInstances(Span nounPhraseSpan,
			List<Entry<String, String>> tagsByTerm, Set<String> nameTerms,
			Instances dataset)
	{
		int npLength = nounPhraseSpan.length();

		for (int index = nounPhraseSpan.getStart(); index < nounPhraseSpan
				.getEnd(); index++)
		{
			boolean isFirstInNP = (index == nounPhraseSpan.getStart());
			boolean isLastInNP = (index == nounPhraseSpan.getEnd() - 1);

			Entry<String, String> entry = tagsByTerm.get(index);
			String term = entry.getKey();
			NameType nameType = nameMatcher.typeOf(term);
			Double frequency = nameMatcher.getMaxFrequency(term);
			String pos = getNLPAnalyzer()
					.mapPosTagToBasicForm(entry.getValue());

			Entry<String, String> previous = (index == 0) ? null : tagsByTerm
					.get(index - 1);
			String previousTerm = previous != null ? previous.getKey() : null;
			NameType previousNameType = nameMatcher.typeOf(previousTerm);
			Double previousFrequency = nameMatcher
					.getMaxFrequency(previousTerm);
			String previousPos = previous != null ? getNLPAnalyzer()
					.mapPosTagToBasicForm(previous.getValue()) : NA;

			Entry<String, String> next = (index == tagsByTerm.size() - 1) ? null
					: tagsByTerm.get(index + 1);
			String nextTerm = next != null ? next.getKey() : null;
			NameType nextNameType = nameMatcher.typeOf(nextTerm);
			Double nextFrequency = nameMatcher.getMaxFrequency(nextTerm);
			String nextPos = next != null ? getNLPAnalyzer()
					.mapPosTagToBasicForm(next.getValue()) : NA;

			Instance instance = new Instance(ATTRIBUTES_TOTAL_NUMBER);
			instance.setDataset(dataset);

			int attIndex = 0;
			instance.setValue(attIndex++, npLength);
			instance.setValue(attIndex++, Boolean.toString(isFirstInNP));
			instance.setValue(attIndex++, Boolean.toString(isLastInNP));

			instance.setValue(attIndex++, nameType.toString());
			instance.setValue(attIndex++, frequency);
			instance.setValue(attIndex++, pos);

			instance.setValue(attIndex++, previousNameType.toString());
			instance.setValue(attIndex++, previousFrequency);
			instance.setValue(attIndex++, previousPos);

			instance.setValue(attIndex++, nextNameType.toString());
			instance.setValue(attIndex++, nextFrequency);
			instance.setValue(attIndex++, nextPos);

			instance.setValue(attIndex++,
					Boolean.toString(nameTerms.contains(term))); // target

			dataset.add(instance);
		}
	}

	private List<String[]> retrieveNames(String sentence)
	{
		Matcher matcher = PERSON_PATTERN.matcher(sentence);

		List<String[]> result = null;
		while (matcher.find())
		{
			if (result == null)
			{
				result = new ArrayList<String[]>();
			}

			String name = matcher.group();
			name = name.replace(PERSON_TAG_BEGIN, "").replace(PERSON_TAG_END,
					"");
			result.add(name.split(" +"));
		}

		return result;
	}

	private FastVector createAttributeDefinitions()
	{
		FastVector attributeDefinitions = new FastVector(
				ATTRIBUTES_TOTAL_NUMBER);

		attributeDefinitions
				.addElement(new Attribute(ATTRIBUTE_NAME_NP_LENGTH));
		attributeDefinitions.addElement(new Attribute(ATTRIBUTE_NAME_IS_FIRST,
				ATTRIBUTE_DOMAIN_BOOLEAN));
		attributeDefinitions.addElement(new Attribute(ATTRIBUTE_NAME_IS_LAST,
				ATTRIBUTE_DOMAIN_BOOLEAN));

		attributeDefinitions.addElement(new Attribute(ATTRIBUTE_NAME_NAME_TYPE,
				ATTRIBUTE_DOMAIN_NAME_TYPE));
		attributeDefinitions
				.addElement(new Attribute(ATTRIBUTE_NAME_FREQUENCY));
		attributeDefinitions.addElement(new Attribute(ATTRIBUTE_NAME_POS,
				ATTRIBUTE_DOMAIN_POS_TAG));

		attributeDefinitions.addElement(new Attribute(
				ATTRIBUTE_NAME_PREVIOUS_NAME_TYPE, ATTRIBUTE_DOMAIN_NAME_TYPE));
		attributeDefinitions.addElement(new Attribute(
				ATTRIBUTE_NAME_PREVIOUS_FREQUENCY));
		attributeDefinitions.addElement(new Attribute(
				ATTRIBUTE_NAME_PREVIOUS_POS, ATTRIBUTE_DOMAIN_POS_TAG));

		attributeDefinitions.addElement(new Attribute(
				ATTRIBUTE_NAME_NEXT_NAME_TYPE, ATTRIBUTE_DOMAIN_NAME_TYPE));
		attributeDefinitions.addElement(new Attribute(
				ATTRIBUTE_NAME_NEXT_FREQUENCY));
		attributeDefinitions.addElement(new Attribute(ATTRIBUTE_NAME_NEXT_POS,
				ATTRIBUTE_DOMAIN_POS_TAG));

		attributeDefinitions.addElement(new Attribute(ATTRIBUTE_NAME_IS_NAME,
				ATTRIBUTE_DOMAIN_BOOLEAN));

		return attributeDefinitions;
	}

	@SuppressWarnings("unchecked")
	private Instances createDataset()
	{
		List<String> sentences = null;
		InputStream in = null;
		try
		{
			in = PersonNameIdentifierImpl.class
					.getResourceAsStream(TRAINING_FILE_NAME);
			sentences = IOUtils.readLines(in);
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
		finally
		{
			IOUtils.closeQuietly(in);
		}

		FastVector attributeDef = createAttributeDefinitions();
		Instances dataset = new Instances(DATA_SET_NAME, attributeDef,
				sentences.size() * 2);

		for (String sentence : sentences)
		{
			addSentenceIntoTrainingData(sentence, dataset);
		}

		dataset.setClassIndex(ATTRIBUTES_TOTAL_NUMBER - 1);

		return dataset;
	}

	private Classifier getTrainedClassifierFromDisk()
	{
		InputStream in = null;
		Classifier classifier = null;
		try
		{
			File file = new File(TRAINED_MODEL_NAME);

			if (!file.exists())
			{
				System.out.println("Model[" + TRAINED_MODEL_NAME
						+ "] does not exist, start training... ");

				Instances dataset = createDataset();

				classifier = new J48(); // J48 is better than others
				classifier.buildClassifier(dataset);

				SerializationHelper.write(TRAINED_MODEL_NAME, classifier);

				System.out.println("Finished training model["
						+ TRAINED_MODEL_NAME + "] by using " + classifier);
			}

			in = new FileInputStream(file);
			classifier = (Classifier) SerializationHelper.read(in);
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}
		finally
		{
			IOUtils.closeQuietly(in);
		}

		return classifier;
	}

	public static void main(String[] args)
	{
		PersonNameIdentifierImpl identifier = new PersonNameIdentifierImpl();
		List<Entry<String, String>> tagsByTermInNP = new ArrayList<Entry<String, String>>();
		tagsByTermInNP.add(new SimpleEntry<String, String>("aung", "NN"));
		tagsByTermInNP.add(new SimpleEntry<String, String>("san", "NN"));
		tagsByTermInNP.add(new SimpleEntry<String, String>("suu", "NN"));
		tagsByTermInNP.add(new SimpleEntry<String, String>("kyi", "NN"));

		Entry<String, String> tagsByTermBeforeNP = null;
		Entry<String, String> tagsByTermAfterNP = new SimpleEntry<String, String>(
				"won", "VB");

		List<Integer> nameTermIndices = identifier
				.identifyNameTermsWithinNounPhrase(tagsByTermInNP,
						tagsByTermBeforeNP, tagsByTermAfterNP);

		System.out.println(nameTermIndices);

		////////////////////////////////////////////////////////////////////////
		tagsByTermInNP = new ArrayList<Entry<String, String>>();
		tagsByTermInNP.add(new SimpleEntry<String, String>("orville", "JJ"));
		tagsByTermInNP.add(new SimpleEntry<String, String>("gibson", "NN"));

		tagsByTermBeforeNP = new SimpleEntry<String, String>("by", "IN");
		tagsByTermAfterNP = null;

		nameTermIndices = identifier.identifyNameTermsWithinNounPhrase(
				tagsByTermInNP, tagsByTermBeforeNP, tagsByTermAfterNP);

		System.out.println(nameTermIndices);

		////////////////////////////////////////////////////////////////////////
		tagsByTermInNP = new ArrayList<Entry<String, String>>();
		tagsByTermInNP.add(new SimpleEntry<String, String>("president", "NN"));
		tagsByTermInNP.add(new SimpleEntry<String, String>("grover", "NN"));
		tagsByTermInNP.add(new SimpleEntry<String, String>("cleveland", "JJ"));

		tagsByTermBeforeNP = new SimpleEntry<String, String>("is", "VB");
		tagsByTermAfterNP = new SimpleEntry<String, String>("'s", "POS");

		nameTermIndices = identifier.identifyNameTermsWithinNounPhrase(
				tagsByTermInNP, tagsByTermBeforeNP, tagsByTermAfterNP);

		System.out.println(nameTermIndices);
	}

	public static void main2(String[] args) throws Exception
	{
		PersonNameIdentifierImpl identifier = new PersonNameIdentifierImpl();
		Instances dataset = identifier.createDataset();
		Classifier classifier = identifier.getTrainedClassifierFromDisk();

		System.out.println(classifier.getClass().getName());
		Evaluation eval = new Evaluation(dataset);
		eval.crossValidateModel(classifier, dataset, 10, new Random(1));
		System.out.println(eval.toSummaryString());
	}

	public static void main3(String[] args) throws Exception
	{
		PersonNameIdentifierImpl identifier = new PersonNameIdentifierImpl();
		Instances dataset = identifier.createDataset();
		Classifier classifier = null;
		Evaluation eval = null;
		int folds = 10;

		classifier = new J48();
		System.out.println(classifier.getClass().getName());
		eval = new Evaluation(dataset);
		eval.crossValidateModel(classifier, dataset, folds, new Random(1));
		System.out.println(eval.toSummaryString());
		System.out.println("===============================================\n");

		classifier = new NaiveBayesSimple();
		System.out.println(classifier.getClass().getName());
		eval = new Evaluation(dataset);
		eval.crossValidateModel(classifier, dataset, folds, new Random(1));
		System.out.println(eval.toSummaryString());
		System.out.println("===============================================\n");

		classifier = new NaiveBayes();
		System.out.println(classifier.getClass().getName());
		eval = new Evaluation(dataset);
		eval.crossValidateModel(classifier, dataset, folds, new Random(1));
		System.out.println(eval.toSummaryString());
		System.out.println("===============================================\n");

		classifier = new LibSVM();
		System.out.println(classifier.getClass().getName());
		eval = new Evaluation(dataset);
		eval.crossValidateModel(classifier, dataset, folds, new Random(1));
		System.out.println(eval.toSummaryString());
		System.out.println("===============================================\n");
	}
}
