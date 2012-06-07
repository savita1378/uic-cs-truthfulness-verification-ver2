package edu.uic.cs.t_verifier.nlp.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
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

import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.uic.cs.t_verifier.html.WikipediaContentExtractor;
import edu.uic.cs.t_verifier.html.data.MatchedQueryKey;
import edu.uic.cs.t_verifier.index.IndexConstants;
import edu.uic.cs.t_verifier.index.StatementIndexUpdater;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.ClassFactory;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.nlp.CategoryMapper;
import edu.uic.cs.t_verifier.nlp.PersonNameMatcher;
import edu.uic.cs.t_verifier.nlp.StatementTypeIdentifier;
import edu.uic.cs.t_verifier.nlp.WordNetReader;
import edu.uic.cs.t_verifier.nlp.impl.WordNetReaderImpl.HypernymSet;

public class StatementTypeIdentifierImpl implements StatementTypeIdentifier,
		IndexConstants
{
	private static final String WORDNET_HYPERNYM_PERSON = "person";
	private static final String WORDNET_HYPERNYM_CELESTIAL_BODY = "celestial body";

	//	private static final Set<String> WORDNET_GLOSS_KEY_WORDS_COUNTRY_CITY = new HashSet<String>(
	//			Arrays.asList(new String[] { "country", "city", "state", "nation",
	//					"republic" }));

	private static boolean PRINT_DETAIL = false;

	private static final double MINMUM_MAX_RATIO = 0.2;

	private NLPAnalyzerImpl4 nlpAnalyzer = new NLPAnalyzerImpl4();

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

	private PersonNameMatcher personNameMatcher = new PersonNameMatcherImpl();

	private static final Set<String> STAR_NAMES;

	private static final String CATEGORY_KEYWORD_OCCUPATION = "occupation";
	static
	{
		// http://www.astro.wisc.edu/~dolan/constellations/starname_list.html
		STAR_NAMES = loadNames(ClassLoader.getSystemResource(
				"starNames/starNames").getPath());
	}

	private IndexReader indexReader = null;
	private IndexSearcher indexSearcher = null;

	private StatementIndexUpdater indexUpdater = null;

	private CategoryMapper categoryMapper = new FileBasedCategoryMapperImpl();
	private WordNetReader wordNetReader = new WordNetReaderImpl();

	private WikipediaContentExtractor wikipediaContentExtractor = ClassFactory
			.getInstance(Config.WIKIPEDIACONTENTEXTRACTOR_CLASS_NAME);

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
	@Deprecated
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

	@SuppressWarnings("deprecation")
	private boolean isPerson(Statement statement)
	{
		List<String> allAlternativeUnits = statement.getAlternativeUnits();
		double count = 0.0;
		for (String alternativeUnit : allAlternativeUnits)
		{
			if (alternativeUnit.contains(" "))
			{
				String[] parts = alternativeUnit.split(" ");

				if (parts.length == 2
						&& personNameMatcher.isName(parts[0], parts[1]))
				{
					count++;
				}
				else if (parts.length == 3
						&& personNameMatcher.isName(parts[0], parts[1],
								parts[2]))
				{
					count++;
				}
				else if (parts.length > 3)
				{
					continue;
				}
			}
			else
			{
				if (personNameMatcher.isName(alternativeUnit))
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

		SentenceCache.getInstance().writeCache();
	}

	public static void main2(String[] args)
	{
		StatementTypeIdentifier typeIdentifier = new StatementTypeIdentifierImpl();
		String sentence = "burgh island hotel was used for a setting of the agatha christie novel, and then there were none";
		String alternativeUnit = "burgh island hotel";
		System.out.println(typeIdentifier.identifyType(sentence,
				alternativeUnit));
	}

	@Override
	public StatementType identifyType(String originalSentence,
			String alternativeUnit)
	{
		alternativeUnit = alternativeUnit.trim();

		StatementType result = StatementType.OTHER;

		String counterPartOfAU = nlpAnalyzer.retrieveTopicTermIfSameTypeAsAU(
				originalSentence, alternativeUnit); // this method will restore cases

		if (counterPartOfAU != null) // AA is BB
		{
			if (isPersonName(alternativeUnit)) // maybe person name
			{
				boolean moreThanOneTerms = alternativeUnit.contains(" ");
				if (identifyTypeByHypernym(counterPartOfAU,
						WORDNET_HYPERNYM_PERSON, /*!moreThanOneTerms*/false))
				{
					if (!moreThanOneTerms
							&& isCountryOrCityName(alternativeUnit))
					{
						result = StatementType.LOCATION;
					}
					else
					{
						result = StatementType.PERSON;
					}
				}
			}

			// stars
			if (result == StatementType.OTHER
					&& identifyTypeByHypernym(counterPartOfAU,
							WORDNET_HYPERNYM_CELESTIAL_BODY, false)
					&& isStarName(alternativeUnit))
			{
				result = StatementType.LOCATION;
			}

			//			StatementType statementTypeIdentifiedByHypernyms = identifyTypeByHypernym(counterPartOfAU);
			//			if (statementTypeIdentifiedByHypernyms == StatementType.PERSON
			//					&& isPersonName(alternativeUnit)) // person
			//			{
			//				result = StatementType.PERSON;
			//			}
			//			else if (statementTypeIdentifiedByHypernyms == StatementType.LOCATION
			//					&& isStarName(alternativeUnit)) // star
			//			{
			//				result = StatementType.LOCATION;
			//			}

			if (result == StatementType.OTHER)
			{
				result = identifyTypeByTopic(classifier_7_classes,
						originalSentence, alternativeUnit, counterPartOfAU);
				if (result == StatementType.OTHER) // try 3 class classifier
				{
					result = identifyTypeByTopic(classifier_3_classes,
							originalSentence, alternativeUnit, counterPartOfAU);
				}
			}

			if (result == StatementType.OTHER)
			{
				result = identifyTypeByCategory(counterPartOfAU);
			}

			//			else if (result == StatementType.ORGANIZATION
			//					|| result == StatementType.DATE
			//					|| result == StatementType.LOCATION)
			//			{
			//				if (identifyTypeByHypernym(counterPartOfAU) == StatementType.PERSON)
			//				{
			//					result = StatementType.PERSON;
			//				}
			//			}
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

	//	private boolean isCountryOrCityName(String alternativeUnit)
	//	{
	//		List<String> glosses = wordNetReader.retrieveGlosses(alternativeUnit,
	//				POS.NOUN);
	//		if (glosses == null)
	//		{
	//			return false;
	//		}
	//
	//		for (String gloss : glosses)
	//		{
	//			for (String keyword : WORDNET_GLOSS_KEY_WORDS_COUNTRY_CITY)
	//			{
	//				if (StringUtils.containsIgnoreCase(gloss, keyword))
	//				{
	//					return true;
	//				}
	//			}
	//		}
	//
	//		return false;
	//	}

	private boolean isCountryOrCityName(String alternativeUnit)
	{
		return identifyTypeByCategory(alternativeUnit) == StatementType.LOCATION;
	}

	private boolean isStarName(String alternativeUnit)
	{
		return STAR_NAMES.contains(alternativeUnit.toLowerCase().trim());
	}

	//	private StatementType identifyTypeByHypernym(String counterPartOfAU)
	//	{
	//		LinkedHashSet<HypernymSet> hypernyms = wordNetReader.retrieveHypernyms(
	//				counterPartOfAU, POS.NOUN);
	//		if (recursiveFindHypernym(hypernyms, WORDNET_HYPERNYM_PERSON, true)) // all hypernyms must be person
	//		{
	//			return StatementType.PERSON;
	//		}
	//		else if (recursiveFindHypernym(hypernyms,
	//				WORDNET_HYPERNYM_CELESTIAL_BODY, false))
	//		{
	//			return StatementType.LOCATION;
	//		}
	//
	//		return StatementType.OTHER;
	//	}

	private boolean identifyTypeByHypernym(String counterPartOfAU,
			String typeHypernymKeyWord, boolean requireAllHypernymsMatched)
	{
		LinkedHashSet<HypernymSet> hypernyms = wordNetReader.retrieveHypernyms(
				counterPartOfAU, POS.NOUN);

		return recursiveFindHypernym(hypernyms, typeHypernymKeyWord,
				requireAllHypernymsMatched);
	}

	private static boolean recursiveFindHypernym(
			LinkedHashSet<HypernymSet> hypernyms, String hypernymToFind,
			boolean requireAllHypernymsMatched)
	{
		for (HypernymSet hypernym : hypernyms)
		{
			//			System.out.println(hypernym.getTerms());

			if (requireAllHypernymsMatched)
			{
				if (hypernym.getTerms().contains(hypernymToFind))
				{
					continue;
				}

				LinkedHashSet<HypernymSet> childHypernyms = hypernym
						.getHyperHypernyms();
				if (childHypernyms.isEmpty()
						|| !recursiveFindHypernym(childHypernyms,
								hypernymToFind, requireAllHypernymsMatched))
				{
					return false;
				}
			}
			else
			{
				if (hypernym.getTerms().contains(hypernymToFind))
				{
					return true;
				}

				if (recursiveFindHypernym(hypernym.getHyperHypernyms(),
						hypernymToFind, requireAllHypernymsMatched))
				{
					return true;
				}
			}
		}

		return requireAllHypernymsMatched;
	}

	@SuppressWarnings("deprecation")
	private boolean isPersonName(String alternativeUnit)
	{
		String[] parts = alternativeUnit.toLowerCase().trim().split(" ");

		if ((parts.length == 1 && personNameMatcher.isName(parts[0]))
				|| (parts.length == 2 && personNameMatcher.isName(parts[0],
						parts[1]))
				|| (parts.length == 3 && personNameMatcher.isName(parts[0],
						parts[1], parts[2])))
		{
			return true;
		}

		return false;
	}

	private boolean isPerson(String originalSentence, String alternativeUnit)
	{
		return isPersonName(alternativeUnit)
				&& (nlpAnalyzer.hasAlternativeUnitDoneSomething(
						originalSentence, alternativeUnit)
				/*|| hasOccupation(originalSentence, alternativeUnit)*/);
	}

	//	private boolean hasOccupation(String originalSentence,
	//			String alternativeUnit)
	//	{
	//		String topicTerm = nlpAnalyzer.retrieveTopicTermIfSameTypeAsAU(
	//				originalSentence, alternativeUnit);
	//		if (topicTerm == null)
	//		{
	//			return false;
	//		}
	//
	//		return isTermAnOccupation(topicTerm);
	//	}

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
		String restoredSentence = nlpAnalyzer.restoreWordCasesForSentence(
				originalSentence, alternativeUnit);

		List<List<CoreLabel>> terms = classifier.classify(restoredSentence);
		Assert.isTrue(terms.size() == 1);

		StatementType alternativeUnitStatementType = StatementType.OTHER;
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
					break outter;
				}
			}

		}

		return alternativeUnitStatementType;
	}

	private StatementType identifyTypeByCategory(String term)
	{
		// System.out.println("counterPartOfAU: " + counterPartOfAU);
		//		String[] categories = retrieveCategoryAndInsertIntoIndexIfNotExist(term);

		// here we need use the categories for un-disambiguated term
		MatchedQueryKey matchedQueryKey = wikipediaContentExtractor
				.matchQueryKey(term, false);
		if (matchedQueryKey == null || !matchedQueryKey.isCertainly())
		{
			return StatementType.OTHER;
		}

		List<String> categories = matchedQueryKey.getCategories();

		List<String> categoryList = new ArrayList<String>();
		categoryList.add(term);
		categoryList.addAll(categories);

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
