package edu.uic.cs.t_verifier.score;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import edu.uic.cs.t_verifier.misc.LogHelper;
import edu.uic.cs.t_verifier.score.data.AlternativeUnit;
import edu.uic.cs.t_verifier.score.data.MatchDetail;
import edu.uic.cs.t_verifier.score.data.StatementMetadata;
import edu.uic.cs.t_verifier.score.data.MatchDetail.EachSpanDetail;

public class StatisticsMultipleWindowSizeScorer extends
		AbstractStatisticsScorer
{
	private static final int MAX_ALLOWED_WINDOW_SIZE_MULTIPLIER = 24;

	public StatisticsMultipleWindowSizeScorer(String indexFolder)
	{
		super(indexFolder, IndexBy.PARAGRAPH);
	}

	protected int scoreNormalAlternativeUnit(AlternativeUnit alternativeUnit,
			StatementMetadata metadata)
	{
		logMatchingDetail(LogHelper.LOG_LAYER_ONE_BEGIN + "AU["
				+ alternativeUnit.getString() + "] Score by AU only?["
				+ metadata.scoreByAlternativeUnitOnly() + "]");

		int winSize; // MAX_VALUE means not matched, < MAX_VALUE means matched in winSize
		if (metadata.scoreByAlternativeUnitOnly())
		{
			// use original logic
			winSize = super.scoreByAlternativeUnit(alternativeUnit, metadata);
		}
		else
		{
			int winSizeOfAlternativeUnit = scoreByAlternativeUnit(
					alternativeUnit, metadata);
			int winSizeOfTopicUnit = scoreByTopicUnit(alternativeUnit, metadata);

			// Need both AU and TU matched all terms ///////////////////////////
			if (winSizeOfAlternativeUnit == Integer.MAX_VALUE
					|| winSizeOfTopicUnit == Integer.MAX_VALUE)
			{
				return Integer.MAX_VALUE;
			}
			////////////////////////////////////////////////////////////////////

			// the smaller win. size, the better
			winSize = (winSizeOfAlternativeUnit < winSizeOfTopicUnit) ? winSizeOfAlternativeUnit
					: winSizeOfTopicUnit;
		}

		logScoreDetail("FINAL SCORE for AU["
				+ alternativeUnit
				+ "]: "
				+ winSize
				+ " ==========================================================================================================\n\n\n\n");

		logMatchingDetail(LogHelper.LOG_LAYER_ONE_END
				+ "AU["
				+ alternativeUnit.getString()
				+ "] ==========================================================================================================\n\n");

		return winSize;
	}

	protected int scoreByAlternativeUnit(AlternativeUnit alternativeUnit,
			StatementMetadata metadata)
	{
		int docID = retrieveDocID(alternativeUnit.getString());

		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term(FIELD_NAME__DOC_TYPE,
				DOC_TYPE__PAGE_CONTENT)), Occur.MUST);
		query.add(new TermQuery(new Term(FIELD_NAME__MATCHED_UNIT,
				alternativeUnit.getString())), Occur.MUST); // search only in one certain AlternativeUnit page

		String[] allStemmedNonstopWordsInTopicUnit = metadata
				.getStemmedNonstopTUWords();
		Query tpoicUnitQuery = prepareTopicUnitQuery(docID,
				allStemmedNonstopWordsInTopicUnit, alternativeUnit.getWeight());

		query.add(tpoicUnitQuery, Occur.MUST);

		logScoreDetail("AU: ["
				+ alternativeUnit
				+ "] ============================================================");
		logScoreDetail(query.toString());

		////////////////////////////////////////////////////////////////////////
		MatchDetail matchDetail = doQueryAndScore(alternativeUnit.getString(),
				query);
		logMatchingDetail("By AU: " + alternativeUnit.getString());

		int winSize = Integer.MAX_VALUE;
		EachSpanDetail bestSpan = logAndFindBestSpan(null, matchDetail);
		if (bestSpan != null)
		{
			if (bestSpan.getMatchedNumber() == bestSpan.getTotalTermsNumber())
			{
				if (bestSpan.getMatchWindowSize() <= (MAX_ALLOWED_WINDOW_SIZE_MULTIPLIER * bestSpan
						.getMatchedNumber()))
				{
					// all matched
					winSize = bestSpan.getMatchWindowSize();
				}
			}
		}

		logScoreDetail("AU SCORE for AU["
				+ alternativeUnit
				+ "]: "
				+ winSize
				+ " =======================================================\n\n");

		return winSize;
	}

	protected int scoreByTopicUnit(AlternativeUnit alternativeUnit,
			StatementMetadata metadata)
	{
		String[] subTopicUnits = metadata.getMatchedSubTopicUnits();
		String[] allStemmedNonstopWordsInTopicUnit = metadata
				.getStemmedNonstopTUWords();

		int minWinSize = Integer.MAX_VALUE;
		EachSpanDetail bestSpan = null;
		for (String subTopicUnit : subTopicUnits)
		{
			logScoreDetail("AU: ["
					+ alternativeUnit.getString()
					+ "], SUB_TU: ["
					+ subTopicUnit
					+ "] ============================================================");
			MatchDetail matchDetail = scoreAlternativeUnitForOneSubTopicUnit(
					alternativeUnit, subTopicUnit,
					allStemmedNonstopWordsInTopicUnit, false);

			logMatchingDetail("By Sub_TU: " + subTopicUnit);
			bestSpan = logAndFindBestSpan(bestSpan, matchDetail);
			if (bestSpan != null
					&& bestSpan.getMatchedNumber() == bestSpan
							.getTotalTermsNumber())
			{
				if (bestSpan.getMatchWindowSize() <= (MAX_ALLOWED_WINDOW_SIZE_MULTIPLIER * bestSpan
						.getMatchedNumber()))
				{
					int winSize = bestSpan.getMatchWindowSize();
					if (winSize < minWinSize)
					{
						minWinSize = winSize;
					}
				}
			}

		}

		logScoreDetail("TU SCORE for AU["
				+ alternativeUnit
				+ "]: "
				+ minWinSize
				+ " =======================================================\n\n");

		return minWinSize;
	}

}
