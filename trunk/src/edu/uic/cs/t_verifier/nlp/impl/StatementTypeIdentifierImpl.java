package edu.uic.cs.t_verifier.nlp.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.uic.cs.t_verifier.index.IndexConstants;
import edu.uic.cs.t_verifier.index.StatementIndexUpdater;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.nlp.CategoryMapper;
import edu.uic.cs.t_verifier.nlp.StatementTypeIdentifier;

public class StatementTypeIdentifierImpl implements StatementTypeIdentifier,
		IndexConstants
{
	private static boolean PRINT_DETAIL = false;

	private static final double MINMUM_MAX_RATIO = 0.2;

	private NLPAnalyzerImpl3 nlpAnalyzer = new NLPAnalyzerImpl3();

	private static final String SERIALIZED_CLASSIFIER_PATH_7_CLASSES = "StanfordNer_classifiers/muc.7class.distsim.crf.ser.gz";
	@SuppressWarnings("unchecked")
	private AbstractSequenceClassifier<CoreLabel> classifier_7_classes = CRFClassifier
			.getClassifierNoExceptions(SERIALIZED_CLASSIFIER_PATH_7_CLASSES);

	//	private static final String SERIALIZED_CLASSIFIER_PATH_4_CLASSES = "StanfordNer_classifiers/conll.4class.distsim.crf.ser.gz";
	//	@SuppressWarnings("unchecked")
	//	private AbstractSequenceClassifier<CoreLabel> classifier_4_classes = CRFClassifier
	//			.getClassifierNoExceptions(SERIALIZED_CLASSIFIER_PATH_4_CLASSES);

	private static final String SERIALIZED_CLASSIFIER_PATH_3_CLASSES = "StanfordNer_classifiers/all.3class.distsim.crf.ser.gz";
	@SuppressWarnings("unchecked")
	private AbstractSequenceClassifier<CoreLabel> classifier_3_classes = CRFClassifier
			.getClassifierNoExceptions(SERIALIZED_CLASSIFIER_PATH_3_CLASSES);

	private static final Set<String> FIRST_NAMES;
	private static final Set<String> LAST_NAMES;

	private static final String CATEGORY_KEYWORD_OCCUPATION = "occupation";
	static
	{
		FIRST_NAMES = loadNames(ClassLoader.getSystemResource(
				"personNames/NameList.Census.FirstName.Male").getPath());
		FIRST_NAMES.addAll(loadNames(ClassLoader.getSystemResource(
				"personNames/NameList.Census.FirstName.Female").getPath()));

		LAST_NAMES = loadNames(ClassLoader.getSystemResource(
				"personNames/NameList.Census.LastName").getPath());
	}

	private IndexReader indexReader = null;
	private IndexSearcher indexSearcher = null;

	private StatementIndexUpdater indexUpdater = null;

	private CategoryMapper categoryMapper = new FileBasedCategoryMapperImpl();

	public static void setPrintDetail(boolean printDetail)
	{
		StatementTypeIdentifierImpl.PRINT_DETAIL = printDetail;
	}

	public StatementTypeIdentifierImpl()
	{
		try
		{
			Directory directory = FSDirectory
					.open(new File(Config.INDEX_FOLDER));
			this.indexReader = IndexReader.open(directory);

		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}

		this.indexSearcher = new IndexSearcher(indexReader);
	}

	private static Set<String> loadNames(String fileName)
	{
		try
		{
			@SuppressWarnings("unchecked")
			List<String> names = FileUtils.readLines(new File(fileName));

			return new HashSet<String>(names);
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	@Override
	public StatementType identifyType(Statement statement)
	{
		StatementType result = identifyTypeByNER(statement,
				classifier_7_classes); //  try 7 classes first
		/*if (result == StatementType.OTHER)
		{
			result = identifyTypeByNER(statement, classifier_4_classes); //  try 4 classes again 
		}*/

		if (result == StatementType.OTHER)
		{
			List<String> allAlternativeUnits = statement.getAlternativeUnits();
			if (isNumber(allAlternativeUnits)) // Number?
			{
				return StatementType.NUMBER;
			}
			else if (isPerson(statement))
			{
				return StatementType.PERSON;
			}
		}

		return result;

	}

	private StatementType identifyTypeByNER(Statement statement,
			AbstractSequenceClassifier<CoreLabel> classifier)
	{
		List<String> allAlternativeUnits = statement.getAlternativeUnits();
		List<String> allAlternativeStatements = statement
				.getAllAlternativeStatements();

		HashMap<StatementType, Integer> numberOfType = new HashMap<StatementType, Integer>(
				5);
		for (int index = 0; index < allAlternativeStatements.size(); index++)
		{
			String alternativeUnit = allAlternativeUnits.get(index);
			String sentence = allAlternativeStatements.get(index);

			sentence = nlpAnalyzer.restoreWordCasesForSentence(sentence,
					alternativeUnit);

			// System.out.println(sentence);

			List<List<CoreLabel>> terms = classifier.classify(sentence);
			Assert.isTrue(terms.size() == 1);

			if (PRINT_DETAIL)
			{
				System.out.print("[" + statement.getId() + "]\t" + sentence
						+ "\t|\t");
			}

			for (CoreLabel term : terms.get(0))
			{
				String termString = term.word();
				String typeString = term.get(AnswerAnnotation.class);
				StatementType statementType = StatementType.parse(typeString);

				if (!alternativeUnit.contains(termString.toLowerCase(Locale.US)
						.trim()))
				{
					if (PRINT_DETAIL && statementType != StatementType.OTHER)
					{
						System.out
								.print(termString + "/" + statementType + " ");
					}
					continue; // not AU term
				}

				//				String typeString = term.get(AnswerAnnotation.class);
				//				StatementType statementType = StatementType.parse(typeString);
				if (statementType == StatementType.OTHER)
				{
					continue;
				}

				if (PRINT_DETAIL)
				{
					System.out.print("*" + termString + "/" + statementType
							+ "* ");
				}
				increaseNumberOfType(numberOfType, statementType);

			}

			if (PRINT_DETAIL)
			{
				System.out.println();
			}
		}

		StatementType result = StatementType.OTHER;
		int totalAuNumber = statement.getAlternativeUnits().size();
		double maxRatio = MINMUM_MAX_RATIO; // at least two AU in this type
		for (Entry<StatementType, Integer> entry : numberOfType.entrySet())
		{
			double ratio = entry.getValue().doubleValue() / totalAuNumber;
			if (ratio > maxRatio)
			{
				result = entry.getKey();
				maxRatio = ratio;
			}
		}

		return result;
	}

	private boolean isPerson(Statement statement)
	{
		List<String> allAlternativeUnits = statement.getAlternativeUnits();
		double count = 0.0;
		for (String alternativeUnit : allAlternativeUnits)
		{
			if (alternativeUnit.contains(" "))
			{
				String[] parts = alternativeUnit.split(" ");
				for (String part : parts)
				{
					if (FIRST_NAMES.contains(part) || LAST_NAMES.contains(part))
					{
						count++;
						break;
					}
				}
			}
			else
			{
				if (FIRST_NAMES.contains(alternativeUnit)
						|| LAST_NAMES.contains(alternativeUnit))
				{
					count++;
				}
			}
		}

		return (count / allAlternativeUnits.size() > MINMUM_MAX_RATIO) // it is a name of person
				&& (nlpAnalyzer.hasAlternativeUnitDoneSomething(statement) || hasOccupation(statement));
	}

	private boolean hasOccupation(Statement statement)
	{
		String topicTerm = nlpAnalyzer
				.retrieveTopicTermIfSameTypeAsAU(statement);
		if (topicTerm == null)
		{
			return false;
		}

		return isTermAnOccupation(topicTerm);
	}

	private boolean isTermAnOccupation(String topicTerm)
	{
		String[] categories = retrieveCategoryAndInsertIntoIndexIfNotExist(topicTerm);

		for (String category : categories)
		{
			category = category.toLowerCase(Locale.US);
			if (category.contains(CATEGORY_KEYWORD_OCCUPATION))
			{
				return true;
			}
		}

		return false;
	}

	private String[] retrieveCategoryAndInsertIntoIndexIfNotExist(
			String topicTerm)
	{
		String[] categories = retrieveCategories(topicTerm);
		if (categories == null)
		{
			System.out.println("\tUn-indexed term [" + topicTerm + "]");

			if (indexUpdater == null)
			{
				indexUpdater = StatementIndexUpdater.getInstance();
			}

			List<String> insertedCategories = indexUpdater
					.retrieveAndIndexUnitPage(topicTerm);
			if (insertedCategories == null)
			{
				return new String[] {};
			}

			categories = insertedCategories
					.toArray(new String[insertedCategories.size()]);
		}
		return categories;
	}

	private String[] retrieveCategories(String unitString)
	{
		try
		{
			BooleanQuery query = new BooleanQuery();
			query.add(new TermQuery(new Term(FIELD_NAME__DOC_TYPE,
					DOC_TYPE__PAGE_CONTENT)), Occur.MUST);
			query.add(new TermQuery(new Term(FIELD_NAME__MATCHED_UNIT,
					unitString)), Occur.MUST);

			TopDocs topDocs = indexSearcher.search(query, 10);
			Assert.isTrue(topDocs.totalHits <= 1,
					"There should be at most one document matched for AU["
							+ unitString + "]! ");

			if (topDocs.totalHits == 1)
			{
				ScoreDoc scoreDoc = topDocs.scoreDocs[0];
				int docNumber = scoreDoc.doc;
				Document document = indexReader.document(docNumber);
				String[] categories = document
						.getValues(FIELD_NAME__CATEGORIES);

				return categories;
			}

			return null;
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	private boolean isNumber(List<String> allAlternativeUnits)
	{
		double count = 0.0;
		for (String alternativeUnit : allAlternativeUnits)
		{
			try
			{
				Double.parseDouble(alternativeUnit);
				count++;
			}
			catch (NumberFormatException e)
			{
				// not the number
			}
		}

		if (count / allAlternativeUnits.size() > MINMUM_MAX_RATIO)
		{
			return true;
		}

		return false;
	}

	private void increaseNumberOfType(
			HashMap<StatementType, Integer> numberOfType,
			StatementType statementType)
	{
		Integer number = numberOfType.get(statementType);
		if (number == null)
		{
			numberOfType.put(statementType, Integer.valueOf(1));
		}
		else
		{
			numberOfType.put(statementType,
					Integer.valueOf(number.intValue() + 1)); //number++
		}
	}

	//	public static void main3(String[] args)
	//	{
	//		List<Statement> statements = AlternativeUnitsReader
	//				.parseAllStatementsFromInputFiles();
	//		StatementTypeIdentifier typeIdentifier = new StatementTypeIdentifierImpl();
	//		StatementTypeIdentifierImpl.PRINT_DETAIL = true;
	//
	//		NLPAnalyzerImpl2 analyzer = new NLPAnalyzerImpl2();
	//		for (Statement statement : statements)
	//		{
	//			if (analyzer.retrieveTopicTermIfSameTypeAsAU(statement) == null)
	//			{
	//				continue;
	//			}
	//
	//			StatementType type = typeIdentifier.identifyType(statement);
	//
	//			if (StatementTypeIdentifierImpl.PRINT_DETAIL)
	//			{
	//				System.out.println("[" + type
	//						+ "] ===========================================\n");
	//			}
	//			else
	//			{
	//				System.out.println(statement.getId() + "\t[" + type + "]\t"
	//						+ statement.getAllAlternativeStatements().get(0));
	//			}
	//		}
	//	}

	public static void main(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();
		StatementTypeIdentifier typeIdentifier = new StatementTypeIdentifierImpl();
		StatementTypeIdentifierImpl.PRINT_DETAIL = true;

		for (Statement statement : statements)
		{
			List<String> allAlternativeUnits = statement.getAlternativeUnits();
			List<String> allAlternativeStatements = statement
					.getAllAlternativeStatements();

			for (int index = 0; index < allAlternativeStatements.size(); index++)
			{
				String alternativeUnit = allAlternativeUnits.get(index);
				String sentence = allAlternativeStatements.get(index);

				StatementType statementType = typeIdentifier.identifyType(
						sentence, alternativeUnit);

				System.out.println("["
						+ statement.getId()
						+ "] "
						+ sentence.replace(alternativeUnit, "["
								+ alternativeUnit + "]") + "\t["
						+ statementType + "]");
			}

			System.out.println();
		}
	}

	public static void main2(String[] args)
	{
		StatementTypeIdentifier typeIdentifier = new StatementTypeIdentifierImpl();
		String sentence = "harry is lead actress in the movie sleepless in seattle";
		String alternativeUnit = "harry";
		System.out.println(typeIdentifier.identifyType(sentence,
				alternativeUnit));
	}

	@Override
	public StatementType identifyType(String originalSentence,
			String alternativeUnit)
	{
		StatementType result = StatementType.OTHER;

		String counterPartOfAU = nlpAnalyzer.retrieveTopicTermIfSameTypeAsAU(
				originalSentence, alternativeUnit); // this method will restore cases

		if (counterPartOfAU != null)
		{
			result = identifyTypeByTopic(classifier_7_classes,
					originalSentence, alternativeUnit, counterPartOfAU);
			if (result == StatementType.OTHER) // try 3 class classifier
			{
				result = identifyTypeByTopic(classifier_3_classes,
						originalSentence, alternativeUnit, counterPartOfAU);
			}

			if (result == StatementType.OTHER)
			{
				result = identifyTypeByCategory(counterPartOfAU);
			}
		}

		if (result == StatementType.OTHER)
		{
			result = identifyTypeByNER(classifier_7_classes, originalSentence,
					alternativeUnit);
			if (result == StatementType.OTHER) // try 3 class classifier
			{
				result = identifyTypeByNER(classifier_3_classes,
						originalSentence, alternativeUnit);
			}
		}

		if (result == StatementType.OTHER)
		{
			if (isNumber(alternativeUnit)) // Number?
			{
				return StatementType.NUMBER;
			}
			else if (isPerson(originalSentence, alternativeUnit))
			{
				return StatementType.PERSON;
			}
		}

		return result;
	}

	private boolean isPerson(String originalSentence, String alternativeUnit)
	{
		String[] parts = alternativeUnit.toLowerCase().trim().split(" ");
		int matchCount = 0;
		for (String part : parts)
		{
			if (FIRST_NAMES.contains(part) || LAST_NAMES.contains(part))
			{
				matchCount++;
			}
		}

		return parts.length == matchCount // it is a name of person
				&& (nlpAnalyzer.hasAlternativeUnitDoneSomething(
						originalSentence, alternativeUnit) || hasOccupation(
						originalSentence, alternativeUnit));
	}

	private boolean hasOccupation(String originalSentence,
			String alternativeUnit)
	{
		String topicTerm = nlpAnalyzer.retrieveTopicTermIfSameTypeAsAU(
				originalSentence, alternativeUnit);
		if (topicTerm == null)
		{
			return false;
		}

		return isTermAnOccupation(topicTerm);
	}

	private boolean isNumber(String alternativeUnit)
	{
		try
		{
			Double.parseDouble(alternativeUnit);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	private StatementType identifyTypeByNER(
			AbstractSequenceClassifier<CoreLabel> classifier,
			String originalSentence, String alternativeUnit)
	{
		StatementType result = StatementType.OTHER;

		String restoredSentence = nlpAnalyzer.restoreWordCasesForSentence(
				originalSentence, alternativeUnit);

		List<List<CoreLabel>> terms = classifier.classify(restoredSentence);
		Assert.isTrue(terms.size() == 1);
		for (CoreLabel term : terms.get(0))
		{
			String termString = term.word();
			if (StringUtils.containsIgnoreCase(alternativeUnit, termString))
			{
				String typeString = term.get(AnswerAnnotation.class);
				result = StatementType.parse(typeString);
				if (result != StatementType.OTHER)
				{
					break;
				}
			}
		}

		return result;
	}

	private StatementType identifyTypeByCategory(String counterPartOfAU)
	{
		// System.out.println("counterPartOfAU: " + counterPartOfAU);
		String[] categories = retrieveCategoryAndInsertIntoIndexIfNotExist(counterPartOfAU);
		List<String> categoryList = new ArrayList<String>();
		categoryList.add(counterPartOfAU);
		categoryList.addAll(Arrays.asList(categories));

		return categoryMapper.mapCategoriesIntoStatementType(categoryList);
	}

	private StatementType identifyTypeByTopic(
			AbstractSequenceClassifier<CoreLabel> classifier,
			String originalSentence, String alternativeUnit,
			String counterPartOfAU)
	{
		String restoredSentence = nlpAnalyzer.restoreWordCasesForSentence(
				originalSentence, alternativeUnit);

		List<List<CoreLabel>> terms = classifier.classify(restoredSentence);
		Assert.isTrue(terms.size() == 1);

		StatementType alternativeUnitStatementType = StatementType.OTHER;
		StatementType counterPartStatementType = StatementType.OTHER;

		String[] alternativeUnitTerms = alternativeUnit.split(" ");
		outter: for (CoreLabel term : terms.get(0))
		{
			String termString = term.word();
			String typeString = term.get(AnswerAnnotation.class);
			StatementType statementType = StatementType.parse(typeString);
			if (statementType == StatementType.OTHER)
			{
				continue;
			}

			for (String auTerm : alternativeUnitTerms)
			{
				if (auTerm.equalsIgnoreCase(termString))
				{
					alternativeUnitStatementType = statementType;
					continue outter;
				}
			}

			if (counterPartOfAU.equalsIgnoreCase(termString))
			{
				counterPartStatementType = statementType;
			}
		}

		if (alternativeUnitStatementType == counterPartStatementType)
		{
			return alternativeUnitStatementType;
		}
		else if (counterPartStatementType != StatementType.OTHER)
		{
			return counterPartStatementType;
		}
		else
		{
			return alternativeUnitStatementType;
		}
	}
}
