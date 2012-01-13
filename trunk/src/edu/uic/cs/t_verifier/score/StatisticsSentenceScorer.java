package edu.uic.cs.t_verifier.score;

import edu.uic.cs.t_verifier.misc.LogHelper;
import edu.uic.cs.t_verifier.score.data.AlternativeUnit;
import edu.uic.cs.t_verifier.score.data.StatementMetadata;

public class StatisticsSentenceScorer extends AbstractStatisticsScorer
{
	public StatisticsSentenceScorer(String indexFolder)
	{
		super(indexFolder, IndexBy.SENTENCE);
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
			winSize = scoreByAlternativeUnit(alternativeUnit, metadata);
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

}
