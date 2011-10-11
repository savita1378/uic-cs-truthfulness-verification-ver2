package edu.uic.cs.t_verifier.score;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.uic.cs.t_verifier.common.AbstractWordOperations;
import edu.uic.cs.t_verifier.common.StatementType;
import edu.uic.cs.t_verifier.index.IndexConstants;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.misc.LogHelper;
import edu.uic.cs.t_verifier.score.data.AlternativeUnit;
import edu.uic.cs.t_verifier.score.data.Category;
import edu.uic.cs.t_verifier.score.data.MatchDetail;
import edu.uic.cs.t_verifier.score.data.MatchDetail.EachSpanDetail;
import edu.uic.cs.t_verifier.score.data.StatementMetadata;
import edu.uic.cs.t_verifier.score.span.explanation.TermsSpanComplexExplanation;

public abstract class AbstractStatementScorer extends AbstractWordOperations
		implements IndexConstants
{
	private static final Logger SCORE_DETAIL_LOGGER = LogHelper
			.getScoreDetailLogger();

	private static final Logger MATCHING_DETAIL_LOGGER = LogHelper
			.getMatchingDetailLogger();

	private File indexFolder = null;
	private IndexReader indexReader = null;
	private IndexSearcher indexSearcher = null;

	private IndexBy indexBy;

	protected AbstractStatementScorer(String indexFolder, IndexBy indexBy)
	{
		this.indexBy = indexBy;
		this.indexFolder = new File(indexFolder);

		try
		{
			Directory directory = FSDirectory.open(this.indexFolder);
			this.indexReader = IndexReader.open(directory);
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}

		this.indexSearcher = new IndexSearcher(indexReader);
		// Defined as default Similarity in Config.java
		// this.indexSearcher.setSimilarity(new StatementSimilarity());
	}

	public void close()
	{
		try
		{
			this.indexReader.close();
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	public List<StatementMetadata> retrieveStatementsMetadata(
			boolean printDetail)
	{
		AllDocCollector allDocCollector = new AllDocCollector();

		try
		{
			indexSearcher.search(new TermQuery(new Term(FIELD_NAME__DOC_TYPE,
					DOC_TYPE__STATEMENT_METADATA)), allDocCollector);

			List<ScoreDoc> allDocs = allDocCollector.getHits();
			List<StatementMetadata> result = new ArrayList<StatementMetadata>(
					allDocs.size());

			for (ScoreDoc scoreDoc : allDocs)
			{
				int docNumber = scoreDoc.doc;
				Document document = indexReader.document(docNumber);

				int statementId = Integer.parseInt(document
						.get(FIELD_NAME__STATEMENT_ID));
				StatementType statementType = StatementType.valueOf(document
						.get(FIELD_NAME__STATEMENT_TYPE));

				TermPositionVector termFreqVector = (TermPositionVector) indexReader
						.getTermFreqVector(docNumber,
								FIELD_NAME__ALL_TOPIC_UNIT_WORDS);
				TreeMap<Integer, String> stemmedNonstopTUWordsByPosition = new TreeMap<Integer, String>();
				for (String term : termFreqVector.getTerms())
				{
					int[] positions = termFreqVector
							.getTermPositions(termFreqVector.indexOf(term));
					for (int position : positions)
					{
						Assert.isNull(stemmedNonstopTUWordsByPosition.put(
								position, term));
					}
				}
				String[] stemmedNonstopTUWords = stemmedNonstopTUWordsByPosition
						.values().toArray(
								new String[stemmedNonstopTUWordsByPosition
										.size()]);

				String[] alternativeUnits = document
						.getValues(FIELD_NAME__AlTERNATIVE_UNIT);

				String[] matchedSubTopicUnits = document
						.getValues(FIELD_NAME__MATCHED_SUB_TU);

				if (printDetail)
				{
					System.out.println("ID:\t" + statementId);
					System.out.println("TU_0:\t"
							+ Arrays.toString(stemmedNonstopTUWords));
					System.out.println("AU:\t"
							+ Arrays.toString(alternativeUnits));
					for (String alternativeUnit : alternativeUnits)
					{
						String[] urls = document
								.getValues(FIELD_NAME__PREFIX__AlTERNATIVE_UNIT_URLS
										+ alternativeUnit);
						if (urls.length != 0)
						{
							// right now we ignore the URLs... 
							System.out
									.println('\t'
											+ Arrays.toString(urls)
											+ '\t'
											+ Arrays.asList(retrieveCategories(alternativeUnit)));
						}
					}

					System.out.println("SUB_TU:\t"
							+ Arrays.toString(matchedSubTopicUnits));
					for (String matchedTopicUnit : matchedSubTopicUnits)
					{
						String[] urls = document
								.getValues(FIELD_NAME__PREFIX__MATCHED_SUB_TU_URLS
										+ matchedTopicUnit);
						// right now we ignore the URLs... 
						System.out
								.println('\t'
										+ Arrays.toString(urls)
										+ '\t'
										+ Arrays.asList(retrieveCategories(matchedTopicUnit)));
					}
					System.out.println("============================\n\n");
				}

				StatementMetadata metadata = new StatementMetadata(statementId,
						stemmedNonstopTUWords, alternativeUnits,
						matchedSubTopicUnits, statementType);
				result.add(metadata);
			}

			if (printDetail)
			{
				System.out
						.println("==============================================================================================\n\n\n");
			}

			return result;

		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}

	}

	private void logScoreDetail(String detail)
	{
		SCORE_DETAIL_LOGGER.debug(detail);
	}

	private void logMatchingDetail(String detail)
	{
		MATCHING_DETAIL_LOGGER.debug(detail);
	}

	private float scoreAlternativeUnit(AlternativeUnit alternativeUnit,
			StatementMetadata metadata)
	{
		logMatchingDetail(LogHelper.LOG_LAYER_ONE_BEGIN + "AU["
				+ alternativeUnit.getString() + "] Score by AU only?["
				+ metadata.scoreByAlternativeUnitOnly() + "]");

		float finalScore = 0f;
		if (metadata.scoreByAlternativeUnitOnly())
		{
			float scoreOfAlternativeUnit = scoreByAlternativeUnit(
					alternativeUnit, metadata);
			if (scoreOfAlternativeUnit > 0f)
			{
				finalScore = scoreOfAlternativeUnit;
			}
			else
			{
				// if AU can't match anything, we need use TU instead 
				finalScore = scoreByTopicUnit(alternativeUnit, metadata);
			}
		}
		else
		{
			float scoreOfAlternativeUnit = scoreByAlternativeUnit(
					alternativeUnit, metadata);
			float scoreOfTopicUnit = scoreByTopicUnit(alternativeUnit, metadata);

			finalScore = (scoreOfAlternativeUnit > scoreOfTopicUnit) ? scoreOfAlternativeUnit
					: scoreOfTopicUnit;
		}

		logScoreDetail("FINAL SCORE for AU["
				+ alternativeUnit
				+ "]: "
				+ finalScore
				+ " ==========================================================================================================\n\n\n\n");

		logMatchingDetail(LogHelper.LOG_LAYER_ONE_END
				+ "AU["
				+ alternativeUnit.getString()
				+ "] ==========================================================================================================\n\n");

		return finalScore;
	}

	private float scoreByAlternativeUnit(AlternativeUnit alternativeUnit,
			StatementMetadata metadata)
	{
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term(FIELD_NAME__DOC_TYPE,
				DOC_TYPE__PAGE_CONTENT)), Occur.MUST);
		query.add(new TermQuery(new Term(FIELD_NAME__MATCHED_UNIT,
				alternativeUnit.getString())), Occur.MUST); // search only in one certain AlternativeUnit page

		String[] allStemmedNonstopWordsInTopicUnit = metadata
				.getStemmedNonstopTUWords();
		Query tpoicUnitQuery = prepareTopicUnitQuery(
				allStemmedNonstopWordsInTopicUnit, alternativeUnit.getWeight());

		/*List<String> stemmedNonStopWordsInAlternativeUnit = porterStemmingAnalyzeUsingDefaultStopWords(alternativeUnit
				.getString());
		Query tpoicUnitQuery = getAlternativeUnitAndNonSubTopicUnitQuery(
				stemmedNonStopWordsInAlternativeUnit,
				Arrays.asList(allStemmedNonstopWordsInTopicUnit), false,
				alternativeUnit.getWeight());*/

		query.add(tpoicUnitQuery, Occur.MUST);

		logScoreDetail("AU: ["
				+ alternativeUnit
				+ "] ============================================================");
		logScoreDetail(query.toString());

		////////////////////////////////////////////////////////////////////////
		MatchDetail matchDetail = doQueryAndScore(alternativeUnit.getString(),
				query);
		logMatchingDetail("By AU: " + alternativeUnit.getString());
		for (EachSpanDetail span : matchDetail.getSpanDetails())
		{
			logMatchingDetail(span.toString());
		}

		float score = matchDetail.getScore();
		logMatchingDetail("Score: " + score + " = Sqrt(" + score * score
				+ ")\n");

		logScoreDetail("AU SCORE for AU["
				+ alternativeUnit
				+ "]: "
				+ score
				+ " =======================================================\n\n");

		return score;
	}

	abstract protected Query prepareTopicUnitQuery(
			String[] allStemmedNonstopWordsInTopicUnit,
			int alternativeUnitWeight);

	private float scoreByTopicUnit(AlternativeUnit alternativeUnit,
			StatementMetadata metadata)
	{
		String[] subTopicUnits = metadata.getMatchedSubTopicUnits();
		String[] allStemmedNonstopWordsInTopicUnit = metadata
				.getStemmedNonstopTUWords();

		boolean isFrontPositionBetter = metadata.isFrontPositionBetter();

		float maxScore = 0f;
		for (String subTopicUnit : subTopicUnits)
		{
			logScoreDetail("AU: ["
					+ alternativeUnit.getString()
					+ "], SUB_TU: ["
					+ subTopicUnit
					+ "] ============================================================");
			MatchDetail matchDetail = scoreAlternativeUnitForOneSubTopicUnit(
					alternativeUnit, subTopicUnit,
					allStemmedNonstopWordsInTopicUnit, isFrontPositionBetter);

			logMatchingDetail("By Sub_TU: " + subTopicUnit);
			for (EachSpanDetail span : matchDetail.getSpanDetails())
			{
				logMatchingDetail(span.toString());
			}

			float score = matchDetail.getScore();
			logMatchingDetail("Score: " + score + " = Sqrt(" + score * score
					+ ")\n");

			logScoreDetail("SCORE: ["
					+ score
					+ "] =======================================================================\n");

			if (score > maxScore)
			{
				maxScore = score;
			}

		}

		logScoreDetail("TU SCORE for AU["
				+ alternativeUnit
				+ "]: "
				+ maxScore
				+ " =======================================================\n\n");

		return maxScore;
	}

	private MatchDetail scoreAlternativeUnitForOneSubTopicUnit(
			AlternativeUnit alternativeUnit, String subTopicUnit,
			String[] allStemmedNonstopWordsInTopicUnit,
			boolean isFrontPositionBetter)
	{
		Query alternativeUnitAndNonSubTopicUnitQuery = prepareAlternativeUnitAndNonSubTopicUnitQuery(
				alternativeUnit, subTopicUnit,
				allStemmedNonstopWordsInTopicUnit, isFrontPositionBetter);
		//		Query alternativeUnitQuery = prepareAlternativeUnitQuery(alternativeUnit);

		////////////////////////////////////////////////////////////////////////
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term(FIELD_NAME__DOC_TYPE,
				DOC_TYPE__PAGE_CONTENT)), Occur.MUST);
		query.add(new TermQuery(
				new Term(FIELD_NAME__MATCHED_UNIT, subTopicUnit)), Occur.MUST); // search only in one certain SUB_TU page
		// Alternative Unit must exist, but non sub topic unit may not exist
		query.add(alternativeUnitAndNonSubTopicUnitQuery, Occur.MUST);
		logScoreDetail(query.toString());

		////////////////////////////////////////////////////////////////////////
		return doQueryAndScore(subTopicUnit, query);
	}

	private MatchDetail doQueryAndScore(String unit, BooleanQuery query)
	{
		MatchDetail result = new MatchDetail(); // default empty result

		try
		{
			TopDocs topDocs = indexSearcher.search(query, 10);
			Assert.isTrue(topDocs.totalHits <= 1,
					"At most one document be matched for TU/AU[" + unit + "]! ");
			if (topDocs.totalHits == 1)
			{
				Explanation explanation = indexSearcher.explain(query,
						topDocs.scoreDocs[0].doc);
				logScoreDetail(explanation.toString());

				float score = topDocs.scoreDocs[0].score;
				Assert.isTrue(score >= 2.0f || score == 0f);

				TermsSpanComplexExplanation termsSpanComplexExplanation = findTermsSpanComplexExplanation(explanation);

				if (termsSpanComplexExplanation != null)
				{
					result = termsSpanComplexExplanation.getMatchDetail();
				}
				result.setScore(score >= 2.0f ? score - 2.0f : score);
			}
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}

		return result;
	}

	private TermsSpanComplexExplanation findTermsSpanComplexExplanation(
			Explanation explanation)
	{
		if (explanation.getDetails() == null)
		{
			return null;
		}

		for (Explanation innerExplanation : explanation.getDetails())
		{
			if (innerExplanation instanceof TermsSpanComplexExplanation)
			{
				return (TermsSpanComplexExplanation) innerExplanation;
			}

			TermsSpanComplexExplanation termsSpanComplexExplanation = findTermsSpanComplexExplanation(innerExplanation);
			if (termsSpanComplexExplanation != null)
			{
				return termsSpanComplexExplanation;
			}
		}

		return null;
	}

	private Query prepareAlternativeUnitAndNonSubTopicUnitQuery(
			AlternativeUnit alternativeUnit, String subTopicUnit,
			String[] allStemmedNonstopWordsInTopicUnit,
			boolean isFrontPositionBetter)
	{
		List<String> stemmedNonStopWordsInAlternativeUnit = porterStemmingAnalyzeUsingDefaultStopWords(alternativeUnit
				.getString());

		List<String> stemmedNonStopWordsInSubTopicUnit = porterStemmingAnalyzeUsingDefaultStopWords(subTopicUnit);
		List<String> stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit = new ArrayList<String>(
				Arrays.asList(allStemmedNonstopWordsInTopicUnit));
		Assert.isTrue(stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit
				.removeAll(stemmedNonStopWordsInSubTopicUnit));

		Query nonSubTopicUnitQuery = getAlternativeUnitAndNonSubTopicUnitQuery(
				stemmedNonStopWordsInAlternativeUnit,
				stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit,
				isFrontPositionBetter, alternativeUnit.getWeight());

		return nonSubTopicUnitQuery;
	}

	abstract protected Query getAlternativeUnitAndNonSubTopicUnitQuery(
			List<String> stemmedNonStopWordsInAlternativeUnit,
			List<String> stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit,
			boolean isFrontPositionBetter, int alternativeUnitWeight);

	protected String getIndexingFieldName()
	{
		return indexBy.toString();
	}

	public List<String> findTheMostMatchedAlternativeUnits(
			StatementMetadata metadata)
	{
		//		System.out.println("\n");
		//		System.out.println("ID:\t\t" + metadata.getStatementId());
		//		System.out.println("TU_0:\t\t"
		//				+ Arrays.toString(metadata.getStemmedNonstopTUWords()));
		//		System.out.println("AUs:\t\t" + metadata.getAlternativeUnits());
		//		System.out.println("SUB_TUs:\t"
		//				+ Arrays.toString(metadata.getMatchedSubTopicUnits()));

		List<String> mostMatchedAlternativeUnits = new ArrayList<String>();
		float maxScore = 0f;

		List<AlternativeUnit> alternativeUnits = metadata.getAlternativeUnits();
		//		System.out.print("SCOREs:\t\t");

		Entry<Category, List<AlternativeUnit>> alternativeUnitsByCategory = filterAlternativeUnitsByCategories(alternativeUnits);
		Category category = alternativeUnitsByCategory.getKey();
		alternativeUnits = alternativeUnitsByCategory.getValue();
		for (AlternativeUnit alternativeUnit : alternativeUnits)
		{
			float score = 0f;
			if (category == Category.CITY)
			{
				score = scoreAlternativeUnit(alternativeUnit, metadata);
			}
			else
			{
				score = scoreAlternativeUnit(alternativeUnit, metadata);
			}
			//			System.out.print("[" + alternativeUnit + "]:" + score + " | ");
			System.out.println(alternativeUnit + "\t" + score);
			//			System.out.println(alternativeUnit);
			if (score > maxScore)
			{
				maxScore = score;
				mostMatchedAlternativeUnits.clear();
				mostMatchedAlternativeUnits.add(alternativeUnit.getString());
			}
			else if (score == maxScore && score != 0f)
			{
				mostMatchedAlternativeUnits.add(alternativeUnit.getString());
			}
		}
		//		System.out.println();
		//
		//		if (!mostMatchedAlternativeUnits.isEmpty())
		//		{
		//			System.out.println("MATCHED_AU:\t" + mostMatchedAlternativeUnits
		//					+ ":" + maxScore);
		//		}
		//		else
		//		{
		//			System.out.println("NO AU MATCHED... ");
		//		}
		//
		//		System.out.print("============================");

		return mostMatchedAlternativeUnits;
	}

	private Entry<Category, List<AlternativeUnit>> filterAlternativeUnitsByCategories(
			List<AlternativeUnit> alternativeUnits)
	{
		Map<Category, List<AlternativeUnit>> alternativeUnitsByCategory = new HashMap<Category, List<AlternativeUnit>>();

		for (AlternativeUnit alternativeUnit : alternativeUnits)
		{
			String[] categories = retrieveCategories(alternativeUnit
					.getString());
			if (categories != null)
			{
				// System.out.println(Arrays.asList(categories));
				Category category = Category.matchCategory(categories);
				List<AlternativeUnit> alternativeUnitsOfCategory = alternativeUnitsByCategory
						.get(category);
				if (alternativeUnitsOfCategory == null)
				{
					alternativeUnitsOfCategory = new ArrayList<AlternativeUnit>();
					alternativeUnitsByCategory.put(category,
							alternativeUnitsOfCategory);
				}

				alternativeUnitsOfCategory.add(alternativeUnit);
			}

		}

		for (Category category : Category.values())
		{
			if (category == Category.OTHER)
			{
				continue;
			}

			List<AlternativeUnit> alternativeUnitsOfCertainCategory = alternativeUnitsByCategory
					.get(category);
			if (alternativeUnitsOfCertainCategory != null
					&& alternativeUnitsOfCertainCategory.size() >= 2) // at least 2/5 AUs are certain type
			{
				return new SimpleEntry<Category, List<AlternativeUnit>>(
						category, alternativeUnitsOfCertainCategory);
			}
		}

		return new SimpleEntry<Category, List<AlternativeUnit>>(Category.OTHER,
				alternativeUnits);
	}

	private String[] retrieveCategories(String alternativeUnitString)
	{
		try
		{
			BooleanQuery query = new BooleanQuery();
			query.add(new TermQuery(new Term(FIELD_NAME__DOC_TYPE,
					DOC_TYPE__PAGE_CONTENT)), Occur.MUST);
			query.add(new TermQuery(new Term(FIELD_NAME__MATCHED_UNIT,
					alternativeUnitString)), Occur.MUST);

			TopDocs topDocs = indexSearcher.search(query, 10);
			Assert.isTrue(topDocs.totalHits <= 1,
					"There should be at most one document matched for AU["
							+ alternativeUnitString + "]! ");

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
}