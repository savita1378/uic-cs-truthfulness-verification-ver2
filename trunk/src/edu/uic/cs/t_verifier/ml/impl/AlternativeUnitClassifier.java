package edu.uic.cs.t_verifier.ml.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.ml.AttributeGatherer;
import edu.uic.cs.t_verifier.ml.Attributes;
import edu.uic.cs.t_verifier.score.IndexBy;
import edu.uic.cs.t_verifier.score.WindowScorer;
import edu.uic.cs.t_verifier.score.data.AlternativeUnit;
import edu.uic.cs.t_verifier.score.data.StatementMetadata;

public class AlternativeUnitClassifier implements Attributes
{
	public static void main(String[] args) throws Exception
	{
		AlternativeUnitClassifier alternativeUnitClassifier = new AlternativeUnitClassifier();
		Instances dataset = alternativeUnitClassifier.gatherDataset();

		//		alternativeUnitClassifier.runClassification(new NaiveBayesSimple(),
		//				dataset);
		//		alternativeUnitClassifier.runClassification(new NaiveBayes(), dataset);
		//		alternativeUnitClassifier.runClassification(new BayesNet(), dataset);
		alternativeUnitClassifier.runClassification(new J48(), dataset); // decision tree
		// alternativeUnitClassifier.runClassification(new LibSVM(), dataset);
	}

	public void runClassification(Classifier classifier, Instances dataset)
			throws Exception
	{
		//		Evaluation evaluation = new Evaluation(dataset);
		//		evaluation.crossValidateModel(classifier, dataset, 10, new Random(1));
		//
		//		System.out.println(evaluation.toSummaryString("\n"
		//				+ classifier.getClass().getSimpleName(), true));

		System.out.println(classifier.getClass().getSimpleName()
				+ "============================================");
		crossValidateModel(classifier, dataset, 10, new Random(1));
		System.out.println("===============================================\n");
	}

	private void crossValidateModel(Classifier classifier, Instances data,
			int numFolds, Random random) throws Exception
	{
		// Make a copy of the data we can reorder
		data = new Instances(data);
		data.randomize(random);

		double ratioSum = 0.0;
		// Do the folds
		for (int i = 0; i < numFolds; i++)
		{
			Instances train = data.trainCV(numFolds, i, random);
			Classifier copiedClassifier = Classifier.makeCopy(classifier);
			copiedClassifier.buildClassifier(train);
			Instances test = data.testCV(numFolds, i);
			System.out.println("Fold [" + (i + 1) + "], training instances ["
					+ train.numInstances() + "], testing instances ["
					+ test.numInstances() + "], total ["
					+ (train.numInstances() + test.numInstances()) + "] ");
			double correctNum = evaluateModel(copiedClassifier, test);
			double correctRatio = correctNum / test.numInstances();
			ratioSum += correctRatio;

			System.out.println("Correction ratio [" + correctRatio + "] \n");
		}

		System.out.println("Average correction ratio [" + (ratioSum / numFolds)
				+ "] ");
	}

	private int evaluateModel(Classifier classifier, Instances data)
	{
		int correctNum = 0;
		double predictions[] = new double[data.numInstances()];

		for (int i = 0; i < data.numInstances(); i++)
		{
			predictions[i] = evaluateModelOnceAndRecordPrediction(classifier,
					data.instance(i));

			if (predictions[i] == 0.0)
			{
				System.out.println(data.instance(i));
			}
			else if (predictions[i] == 1.0)
			{
				correctNum++;
			}
			else
			{
				Assert.isTrue(false);
			}
		}

		return correctNum;
	}

	private double evaluateModelOnceAndRecordPrediction(Classifier classifier,
			Instance instance)
	{

		Instance classMissing = (Instance) instance.copy();
		double pred = 0;
		classMissing.setDataset(instance.dataset());
		classMissing.setClassMissing();

		try
		{
			pred = classifier.classifyInstance(classMissing);
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}
		return pred;
	}

	private Instances gatherDataset()
	{
		AttributeGatherer attributeGatherer = new AttributeGathererImpl();

		WindowScorer scorer = new WindowScorer(Config.INDEX_FOLDER,
				IndexBy.PARAGRAPH, Config.SEARCH_WINDOW, false);
		scorer.setAttributeGather(attributeGatherer);

		List<StatementMetadata> metadataList = scorer
				.retrieveStatementsMetadata(false);

		Map<String, String> trainingResult = getTrainingResults("training_result.txt");

		Map<AlternativeUnit, String> classificationByAu = new LinkedHashMap<AlternativeUnit, String>();
		for (StatementMetadata metadata : metadataList)
		{
			for (AlternativeUnit alternativeUnit : metadata
					.getAlternativeUnits())
			{
				String idString = metadata.getStatementId() + "_"
						+ alternativeUnit.getString();
				String classification = trainingResult.get(idString);
				Assert.notNull(classification);

				classificationByAu.put(alternativeUnit, classification);
			}

			scorer.findTheMostMatchedAlternativeUnits(metadata);
		}

		HashMap<String, String> unsignedValueByName = new HashMap<String, String>();
		unsignedValueByName.put(ATTR_MATCHED_BY_AU.name, ATTR_VALUE_FALSE);
		unsignedValueByName.put(ATTR_MATCHED_BY_TU.name, ATTR_VALUE_FALSE);

		Instances dataset = attributeGatherer.getDataset(classificationByAu,
				Attributes.ATTR_DOMAIN_TRUE_FALSE, unsignedValueByName);

		System.out
				.println("\n\n=================================================");
		System.out.println(dataset.toSummaryString());
		System.out.println("=================================================");

		return dataset;
	}

	private Map<String, String> getTrainingResults(String expectedFileName)
	{
		try
		{
			@SuppressWarnings("unchecked")
			List<String> lines = FileUtils.readLines(new File(
					getPathOfFile(expectedFileName)));

			Map<String, String> result = new HashMap<String, String>();
			for (String line : lines)
			{
				String[] parts = line.split("=");
				result.put(parts[0], parts[1]);
			}

			return result;
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}

	}

	private String getPathOfFile(String fileName)
	{
		return this.getClass().getResource(fileName).getPath();
	}
}
