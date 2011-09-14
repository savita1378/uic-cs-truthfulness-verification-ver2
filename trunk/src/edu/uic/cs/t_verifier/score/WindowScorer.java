package edu.uic.cs.t_verifier.score;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.score.span.TermsSpanOrQuery;

public class WindowScorer extends AbstractStatementScorer
{
	private int slop = 0;
	private boolean inOrder = true;

	public WindowScorer(String indexFolder, IndexBy indexBy, int windowSize,
			boolean inOrder)
	{
		super(indexFolder, indexBy);
		this.slop = windowSize;
		this.inOrder = inOrder;
	}

	@Override
	protected Query getAlternativeUnitAndNonSubTopicUnitQuery(
			List<String> stemmedNonStopWordsInAlternativeUnit,
			List<String> stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit,
			boolean isFrontPositionBetter, int alternativeUnitWeight)
	{
		List<List<String>> powerset = powerset(
				stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit, false);

		SpanNearQuery alternativeUnitQuery = getAlternativeUnitQuery(stemmedNonStopWordsInAlternativeUnit);

		// if there's no TU words
		if (powerset.isEmpty())
		{
			return alternativeUnitQuery;
		}

		Set<String> termsInQuery = new HashSet<String>(
				stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit); // only consider TU now

		TermsSpanOrQuery result = getTopicUnitQuery(powerset,
				alternativeUnitQuery, termsInQuery,
				/*stemmedNonStopWordsInAlternativeUnit,*/alternativeUnitWeight);

		return result;
	}

	private static final Comparator<List<String>> LIST_REVERSE_SIZE_COMPARATOR = new Comparator<List<String>>()
	{
		@Override
		public int compare(List<String> o1, List<String> o2)
		{
			return o2.size() - o1.size();
		}
	};

	private TermsSpanOrQuery getTopicUnitQuery(List<List<String>> powerset,
			SpanNearQuery alternativeUnitQuery, Set<String> termsInQuery,
			/*List<String> stemmedNonStopWordsInAlternativeUnit,*/
			int alternativeUnitWeight)
	{
		TermsSpanOrQuery orQuery = new TermsSpanOrQuery(new SpanQuery[0],
				getIndexingFieldName(), termsInQuery,
				/*stemmedNonStopWordsInAlternativeUnit,*/alternativeUnitWeight);

		Collections.sort(powerset, LIST_REVERSE_SIZE_COMPARATOR);
		for (List<String> eachCombination : powerset)
		{
			SpanQuery[] terms = null;
			if (alternativeUnitQuery == null)
			{
				terms = new SpanQuery[eachCombination.size()];
			}
			else
			{
				terms = new SpanQuery[eachCombination.size() + 1];
			}

			for (int index = 0; index < eachCombination.size(); index++)
			{
				String word = eachCombination.get(index);
				terms[index] = new SpanTermQuery(new Term(
						getIndexingFieldName(), word));
			}

			if (alternativeUnitQuery != null)
			{
				terms[terms.length - 1] = alternativeUnitQuery;
			}

			/*TermsSpanNearQuery notInSubTopicUnitQuery = new TermsSpanNearQuery(
					terms, slop, inOrder, getIndexingFieldName(), termsInQuery,
					stemmedNonStopWordsInAlternativeUnit, alternativeUnitWeight);*/

			SpanNearQuery notInSubTopicUnitQuery = new SpanNearQuery(terms,
					slop, inOrder);

			orQuery.addClause(notInSubTopicUnitQuery);
		}

		return orQuery;
	}

	/*private SpanOrQuery getTopicUnitQuery(List<List<String>> powerset)
	{
		SpanOrQuery orQuery = new SpanOrQuery();
		for (List<String> eachCombination : powerset)
		{
			SpanQuery[] terms = new SpanQuery[eachCombination.size()];
			for (int index = 0; index < eachCombination.size(); index++)
			{
				String word = eachCombination.get(index);
				terms[index] = new SpanTermQuery(new Term(
						getIndexingFieldName(), word));
			}

			SpanNearQuery notInSubTopicUnitQuery = new SpanNearQuery(terms,
					slop, inOrder);
			// notInSubTopicUnitQuery.setBoost(terms.length); // useless!

			orQuery.addClause(notInSubTopicUnitQuery);
		}

		return orQuery;
	}*/

	private SpanNearQuery getAlternativeUnitQuery(
			List<String> stemmedNonStopWordsInAlternativeUnit)
	{
		SpanTermQuery[] wordsInAU = constructAuTerms(stemmedNonStopWordsInAlternativeUnit);

		// AU is considered as a whole, and no span within - (span=0, inOrder) 
		SpanNearQuery result = new SpanNearQuery(wordsInAU, 0, true);

		return result;
	}

	private SpanTermQuery[] constructAuTerms(
			List<String> stemmedNonStopWordsInAlternativeUnit)
	{
		SpanTermQuery[] wordsInAU = new SpanTermQuery[stemmedNonStopWordsInAlternativeUnit
				.size()];
		for (int index = 0; index < stemmedNonStopWordsInAlternativeUnit.size(); index++)
		{
			String word = stemmedNonStopWordsInAlternativeUnit.get(index);
			wordsInAU[index] = new SpanTermQuery(new Term(
					getIndexingFieldName(), word));
		}

		return wordsInAU;
	}

	/*private SpanNearQuery getAlternativeUnitFrontBetterQuery(
			List<String> stemmedNonStopWordsInAlternativeUnit)
	{
		SpanTermQuery[] wordsInAU = constructAuTerms(stemmedNonStopWordsInAlternativeUnit);

		return new FrontBetterSpanNearQuery(wordsInAU, 0, true);
	}*/

	private List<List<String>> powerset(Collection<String> list,
			boolean needEmptySet)
	{
		List<List<String>> ps = new ArrayList<List<String>>();
		ps.add(new ArrayList<String>()); // add the empty set

		// for every item in the original list
		for (String item : list)
		{
			List<List<String>> newPs = new ArrayList<List<String>>();

			for (List<String> subset : ps)
			{
				// copy all of the current powerset's subsets
				newPs.add(subset);

				// plus the subsets appended with the current item
				List<String> newSubset = new ArrayList<String>(subset);
				newSubset.add(item);
				newPs.add(newSubset);
			}

			// powerset is now powerset of list.subList(0, list.indexOf(item)+1)
			ps = newPs;
		}

		if (!needEmptySet)
		{
			ps.remove(0);
		}

		return ps;
	}

	@Override
	protected Query prepareTopicUnitQuery(
			String[] allStemmedNonstopWordsInTopicUnit,
			int alternativeUnitWeight)
	{
		List<String> allStemmedNonstopWordsInTopicUnitList = Arrays
				.asList(allStemmedNonstopWordsInTopicUnit);
		List<List<String>> powerset = powerset(
				allStemmedNonstopWordsInTopicUnitList, false);
		Assert.isTrue(!powerset.isEmpty());

		Set<String> termsInQuery = new HashSet<String>(
				allStemmedNonstopWordsInTopicUnitList);
		// @SuppressWarnings("unchecked")
		TermsSpanOrQuery result = getTopicUnitQuery(powerset, null,
				termsInQuery, /*Collections.EMPTY_LIST,*/alternativeUnitWeight);

		return result;
	}

}
