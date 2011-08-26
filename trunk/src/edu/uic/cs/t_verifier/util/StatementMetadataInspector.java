package edu.uic.cs.t_verifier.util;

import java.util.List;

import org.apache.lucene.search.Query;

import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.score.AbstractStatementScorer;
import edu.uic.cs.t_verifier.score.IndexBy;

public class StatementMetadataInspector
{
	public static void main(String[] args)
	{
		new AbstractStatementScorer(Config.INDEX_FOLDER, IndexBy.PARAGRAPH)
		{
			@Override
			protected Query getAlternativeUnitAndNonSubTopicUnitQuery(
					List<String> stemmedNonStopWordsInAlternativeUnit,
					List<String> stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit,
					boolean isFrontPositionBetter)
			{
				return null;
			}

			@Override
			protected Query prepareTopicUnitQuery(
					String[] allStemmedNonstopWordsInTopicUnit)
			{
				return null;
			}

		}.retrieveStatementsMetadata(true);
	}
}
