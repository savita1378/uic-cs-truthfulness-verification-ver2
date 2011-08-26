package edu.uic.cs.t_verifier.score.copy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import edu.uic.cs.t_verifier.score.data.StatementMetadata;

public abstract class AbstractStatementScorer extends AbstractWordOperations
		implements IndexConstants
{
	private static final Logger SCORE_DETAIL_LOGGER = LogHelper
			.getScoreDetailLogger();

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
							System.out.println('\t' + Arrays.toString(urls));
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
						System.out.println('\t' + Arrays.toString(urls));
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

	private float scoreAlternativeUnit(String alternativeUnit,
			StatementMetadata metadata)
	{
		if (metadata.isUseAuToScore())
		{
			return scoreByAlternativeUnit(alternativeUnit, metadata);
			// FIXME if AU cann't match anything, we need use TU instead 
		}
		else
		{
			return scoreByTopicUnit(alternativeUnit, metadata);
		}
	}

	private float scoreByAlternativeUnit(String alternativeUnit,
			StatementMetadata metadata)
	{
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term(FIELD_NAME__DOC_TYPE,
				DOC_TYPE__PAGE_CONTENT)), Occur.MUST);
		query.add(new TermQuery(new Term(FIELD_NAME__MATCHED_UNIT,
				alternativeUnit)), Occur.MUST); // search only in one certain AlternativeUnit page

		String[] allStemmedNonstopWordsInTopicUnit = metadata
				.getStemmedNonstopTUWords();
		Query tpoicUnitQuery = prepareTopicUnitQuery(allStemmedNonstopWordsInTopicUnit);
		query.add(tpoicUnitQuery, Occur.MUST);

		logScoreDetail("AU: ["
				+ alternativeUnit
				+ "] ============================================================");
		logScoreDetail(query.toString());

		////////////////////////////////////////////////////////////////////////
		float score = doQueryAndScore(alternativeUnit, query);

		logScoreDetail("SCORE for AU["
				+ alternativeUnit
				+ "]: "
				+ score
				+ " =======================================================\n\n\n");

		return score;
	}

	abstract protected Query prepareTopicUnitQuery(
			String[] allStemmedNonstopWordsInTopicUnit);

	private float scoreByTopicUnit(String alternativeUnit,
			StatementMetadata metadata)
	{
		String[] subTopicUnits = metadata.getMatchedSubTopicUnits();
		String[] allStemmedNonstopWordsInTopicUnit = metadata
				.getStemmedNonstopTUWords();

		float finalScore = 0;
		for (String subTopicUnit : subTopicUnits)
		{
			logScoreDetail("AU: ["
					+ alternativeUnit
					+ "], SUB_TU: ["
					+ subTopicUnit
					+ "] ============================================================");
			float score = scoreAlternativeUnitForOneSubTopicUnit(
					alternativeUnit, subTopicUnit,
					allStemmedNonstopWordsInTopicUnit);

			finalScore += score;

			logScoreDetail("SCORE: ["
					+ score
					+ "] =======================================================================\n");
		}

		logScoreDetail("TOTAL SCORE for AU["
				+ alternativeUnit
				+ "]: "
				+ finalScore
				+ " =======================================================\n\n\n");

		return finalScore;
	}

	private float scoreAlternativeUnitForOneSubTopicUnit(
			String alternativeUnit, String subTopicUnit,
			String[] allStemmedNonstopWordsInTopicUnit)
	{
		Query alternativeUnitAndNonSubTopicUnitQuery = prepareAlternativeUnitAndNonSubTopicUnitQuery(
				alternativeUnit, subTopicUnit,
				allStemmedNonstopWordsInTopicUnit);
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

	private float doQueryAndScore(String unit, BooleanQuery query)
	{
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

				return topDocs.scoreDocs[0].score;
			}
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}

		return 0;
	}

	private Query prepareAlternativeUnitAndNonSubTopicUnitQuery(
			String alternativeUnit, String subTopicUnit,
			String[] allStemmedNonstopWordsInTopicUnit)
	{
		List<String> stemmedNonStopWordsInAlternativeUnit = porterStemmingAnalyzeUsingDefaultStopWords(alternativeUnit);

		List<String> stemmedNonStopWordsInSubTopicUnit = porterStemmingAnalyzeUsingDefaultStopWords(subTopicUnit);
		List<String> stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit = new ArrayList<String>(
				Arrays.asList(allStemmedNonstopWordsInTopicUnit));
		Assert.isTrue(stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit
				.removeAll(stemmedNonStopWordsInSubTopicUnit));

		Query nonSubTopicUnitQuery = getAlternativeUnitAndNonSubTopicUnitQuery(
				stemmedNonStopWordsInAlternativeUnit,
				stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit);

		return nonSubTopicUnitQuery;
	}

	abstract protected Query getAlternativeUnitAndNonSubTopicUnitQuery(
			List<String> stemmedNonStopWordsInAlternativeUnit,
			List<String> stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit);

	protected String getIndexingFieldName()
	{
		return indexBy.toString();
	}

	public List<String> findTheMostMatchedAlternativeUnits(
			StatementMetadata metadata)
	{
		System.out.println("\n");
		System.out.println("ID:\t\t" + metadata.getStatementId());
		System.out.println("TU_0:\t\t"
				+ Arrays.toString(metadata.getStemmedNonstopTUWords()));
		System.out.println("AUs:\t\t"
				+ Arrays.toString(metadata.getAlternativeUnits()));
		System.out.println("SUB_TUs:\t"
				+ Arrays.toString(metadata.getMatchedSubTopicUnits()));

		List<String> mostMatchedAlternativeUnits = new ArrayList<String>();
		float maxScore = 0F;

		String[] alternativeUnits = metadata.getAlternativeUnits();
		System.out.print("SCOREs:\t\t");
		for (String alternativeUnit : alternativeUnits)
		{
			float score = scoreAlternativeUnit(alternativeUnit, metadata);
			System.out.print("[" + alternativeUnit + "]:" + score + " | ");
			//			System.out.println(score);
			if (score > maxScore)
			{
				maxScore = score;
				mostMatchedAlternativeUnits.clear();
				mostMatchedAlternativeUnits.add(alternativeUnit);
			}
			else if (score == maxScore && score != 0F)
			{
				mostMatchedAlternativeUnits.add(alternativeUnit);
			}
		}
		System.out.println();

		if (!mostMatchedAlternativeUnits.isEmpty())
		{
			System.out.println("MATCHED_AU:\t" + mostMatchedAlternativeUnits
					+ ":" + maxScore);
		}
		else
		{
			System.out.println("NO AU MATCHED... ");
		}

		System.out.print("============================");

		return mostMatchedAlternativeUnits;
	}

}
