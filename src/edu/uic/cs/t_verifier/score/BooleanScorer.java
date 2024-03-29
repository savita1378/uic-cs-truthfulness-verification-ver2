package edu.uic.cs.t_verifier.score;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class BooleanScorer extends AbstractStatementScorer
{
	public BooleanScorer(String indexFolder)
	{
		// every field should be the same since it's boolean query 
		super(indexFolder, IndexBy.SENTENCE);
	}

	@Override
	protected Query getAlternativeUnitAndNonSubTopicUnitQuery(int docID,
			List<String> stemmedNonStopWordsInAlternativeUnit,
			List<String> stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit,
			boolean isFrontPositionBetter, int alternativeUnitWeight)
	{
		// ignore isFrontPositionBetter

		BooleanQuery query = new BooleanQuery();

		for (String word : stemmedNonStopWordsInAlternativeUnit)
		{
			query.add(new TermQuery(new Term(getIndexingFieldName(), word)),
					Occur.SHOULD);
		}

		for (String word : stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit)
		{
			query.add(new TermQuery(new Term(getIndexingFieldName(), word)),
					Occur.SHOULD);
		}

		return query;
	}

	@Override
	protected Query prepareTopicUnitQuery(int docID,
			String[] allStemmedNonstopWordsInTopicUnit,
			int alternativeUnitWeight)
	{
		BooleanQuery query = new BooleanQuery();
		for (String word : allStemmedNonstopWordsInTopicUnit)
		{
			query.add(new TermQuery(new Term(getIndexingFieldName(), word)),
					Occur.SHOULD);
		}

		return query;
	}

}
