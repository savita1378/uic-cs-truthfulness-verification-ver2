package edu.uic.cs.t_verifier.score;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import edu.uic.cs.t_verifier.common.StatementType;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.LogHelper;
import edu.uic.cs.t_verifier.score.data.AlternativeUnit;
import edu.uic.cs.t_verifier.score.data.Category;
import edu.uic.cs.t_verifier.score.data.MatchDetail;
import edu.uic.cs.t_verifier.score.data.MatchDetail.EachSpanDetail;
import edu.uic.cs.t_verifier.score.data.StatementMetadata;

public class StatisticsScorer extends WindowScorer
{
	public StatisticsScorer(String indexFolder)
	{
		super(indexFolder, IndexBy.PARAGRAPH, Config.SEARCH_WINDOW, false);
	}

	@Override
	public List<String> findTheMostMatchedAlternativeUnits(
			StatementMetadata metadata)
	{
		List<String> bestMatchedAlternativeUnits;

		List<AlternativeUnit> alternativeUnits = metadata.getAlternativeUnits();
		Entry<Category, List<AlternativeUnit>> alternativeUnitsByCategory = filterAlternativeUnitsByCategories(alternativeUnits);
		Category category = alternativeUnitsByCategory.getKey();
		alternativeUnits = alternativeUnitsByCategory.getValue();

		if (category.equals(Category.CITY)
				&& metadata.getStatementType() == StatementType.SUPERLATIVE_STRING)
		{
			bestMatchedAlternativeUnits = bestMatchedCityAlternativeUnits(
					alternativeUnits, metadata);
		}
		else
		{
			bestMatchedAlternativeUnits = bestMatchedNormalAlternativeUnits(
					alternativeUnits, metadata);
		}

		return bestMatchedAlternativeUnits;
	}

	private List<String> bestMatchedNormalAlternativeUnits(
			List<AlternativeUnit> alternativeUnits, StatementMetadata metadata)
	{
		List<String> bestMatchedAlternativeUnits = new ArrayList<String>();
		int minWinSize = Integer.MAX_VALUE;

		for (AlternativeUnit alternativeUnit : alternativeUnits)
		{
			int winSize = scoreNormalAlternativeUnit(alternativeUnit, metadata);
			System.out.println(alternativeUnit + "\t"
					+ ((winSize == Integer.MAX_VALUE) ? "N/A" : winSize));

			if (winSize < minWinSize)
			{
				minWinSize = winSize;
				bestMatchedAlternativeUnits.clear();
				bestMatchedAlternativeUnits.add(alternativeUnit.getString());
			}
			else if (winSize == minWinSize && winSize != Integer.MAX_VALUE)
			{
				bestMatchedAlternativeUnits.add(alternativeUnit.getString());
			}
		}

		return bestMatchedAlternativeUnits;
	}

	private List<String> bestMatchedCityAlternativeUnits(
			List<AlternativeUnit> alternativeUnits, StatementMetadata metadata)
	{
		List<String> bestMatchedAlternativeUnits = new ArrayList<String>();
		int maxPopulation = 0;

		for (AlternativeUnit alternativeUnit : alternativeUnits)
		{
			logMatchingDetail(LogHelper.LOG_LAYER_ONE_BEGIN + "AU<City>["
					+ alternativeUnit.getString() + "], StatementType["
					+ metadata.getStatementType() + "]");

			int population = scoreByPopulationOfCity(alternativeUnit, metadata);
			System.out.println(alternativeUnit + "\t"
					+ (population == 0 ? "N/A" : population));

			logScoreDetail("FINAL SCORE for AU<City>["
					+ alternativeUnit
					+ "]: "
					+ population
					+ " ==========================================================================================================\n\n\n\n");

			logMatchingDetail(LogHelper.LOG_LAYER_ONE_END
					+ "AU<City>["
					+ alternativeUnit.getString()
					+ "] ==========================================================================================================\n\n");

			if (population > maxPopulation)
			{
				maxPopulation = population;
				bestMatchedAlternativeUnits.clear();
				bestMatchedAlternativeUnits.add(alternativeUnit.getString());
			}
			else if (population == maxPopulation && population != 0)
			{
				bestMatchedAlternativeUnits.add(alternativeUnit.getString());
			}
		}

		return bestMatchedAlternativeUnits;
	}

	private int scoreNormalAlternativeUnit(AlternativeUnit alternativeUnit,
			StatementMetadata metadata)
	{
		logMatchingDetail(LogHelper.LOG_LAYER_ONE_BEGIN + "AU["
				+ alternativeUnit.getString() + "] Score by AU only?["
				+ metadata.scoreByAlternativeUnitOnly() + "]");

		int winSize; // MAX_VALUE means not matched, < MAX_VALUE means matched in winSize
		if (metadata.scoreByAlternativeUnitOnly())
		{
			winSize = scoreByAlternativeUnit(alternativeUnit, metadata);
		}
		else
		{
			int winSizeOfAlternativeUnit = scoreByAlternativeUnit(
					alternativeUnit, metadata);
			int winSizeOfTopicUnit = scoreByTopicUnit(alternativeUnit, metadata);

			// Need both AU and TU matched all terms ///////////////////////////
			/*if (winSizeOfAlternativeUnit == Integer.MAX_VALUE
					|| winSizeOfTopicUnit == Integer.MAX_VALUE)
			{
				return Integer.MAX_VALUE;
			}*/
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

	private int scoreByAlternativeUnit(AlternativeUnit alternativeUnit,
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
				// all matched
				winSize = bestSpan.getMatchWindowSize();
			}
		}

		logScoreDetail("AU SCORE for AU["
				+ alternativeUnit
				+ "]: "
				+ winSize
				+ " =======================================================\n\n");

		return winSize;
	}

	private int scoreByTopicUnit(AlternativeUnit alternativeUnit,
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
				int winSize = bestSpan.getMatchWindowSize();
				if (winSize < minWinSize)
				{
					minWinSize = winSize;
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
