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
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesSimple;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.ml.AttributeGatherer;
import edu.uic.cs.t_verifier.score.Attributes;
import edu.uic.cs.t_verifier.score.IndexBy;
import edu.uic.cs.t_verifier.score.WindowScorer;
import edu.uic.cs.t_verifier.score.data.AlternativeUnit;
import edu.uic.cs.t_verifier.score.data.StatementMetadata;

public class AlternativeUnitClassifier
{
	public static void main(String[] args) throws Exception
	{
		AlternativeUnitClassifier alternativeUnitClassifier = new AlternativeUnitClassifier();
		Instances dataset = alternativeUnitClassifier.gatherDataset();

		alternativeUnitClassifier.runClassification(new NaiveBayesSimple(),
				dataset);
		alternativeUnitClassifier.runClassification(new NaiveBayes(), dataset);
		alternativeUnitClassifier.runClassification(new BayesNet(), dataset);
		alternativeUnitClassifier.runClassification(new J48(), dataset); // decision tree
		alternativeUnitClassifier.runClassification(new LibSVM(), dataset);
	}

	public void runClassification(Classifier classifier, Instances dataset)
			throws Exception
	{
		Evaluation evaluation = new Evaluation(dataset);
		evaluation.crossValidateModel(classifier, dataset, 10, new Random(1));

		System.out.println(evaluation.toSummaryString("\n"
				+ classifier.getClass().getSimpleName(), true));
		System.out.println("=================================================");
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

		Instances dataset = attributeGatherer.getDataset(classificationByAu,
				Attributes.ATTR_DOMAIN_TRUE_FALSE);

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
