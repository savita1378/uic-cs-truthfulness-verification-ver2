package edu.uic.cs.t_verifier.score;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.score.span.TermsSpanNearQuery;

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
			boolean isFrontPositionBetter)
	{
		List<List<String>> powerset = powerset(
				stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit, false);

		SpanNearQuery alternativeUnitQuery = null;
		//		if (isFrontPositionBetter)
		//		{
		alternativeUnitQuery = getAlternativeUnitQuery(stemmedNonStopWordsInAlternativeUnit);
		//		}
		//		else
		//		{
		// 			// XXX doesn't work
		//			alternativeUnitQuery = getAlternativeUnitFrontBetterQuery(stemmedNonStopWordsInAlternativeUnit);
		//		}

		// if there's no TU words
		if (powerset.isEmpty())
		{
			return alternativeUnitQuery;
		}

		SpanOrQuery topicUnitQuery = getTopicUnitQuery(powerset);

		// AU near {powerset of TU (OR)}
		SpanQuery[] auQueryAndAllCombinationsOfTuQuery = new SpanQuery[] {
				alternativeUnitQuery, topicUnitQuery };
		Set<String> termsInQuery = new HashSet<String>(
				stemmedNonStopWordsInTopicUnitButNotInSubTopicUnit); // only consider TU now

		TermsSpanNearQuery result = new TermsSpanNearQuery(
				auQueryAndAllCombinationsOfTuQuery, slop, false,
				getIndexingFieldName(), termsInQuery,
				stemmedNonStopWordsInAlternativeUnit); // not in order

		//		SpanNearQuery auNearTuQuery = new SpanNearQuery(
		//				auQueryAndAllCombinationsOfTuQuery, slop, false); // not in order
		//
		//		TermsSpanOrQuery result = new TermsSpanOrQuery(getIndexingFieldName(),
		//				termsInQuery, stemmedNonStopWordsInAlternativeUnit,
		//				auNearTuQuery, alternativeUnitQuery);

		return result;
	}

	private SpanOrQuery getTopicUnitQuery(List<List<String>> powerset)
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
	}

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
			String[] allStemmedNonstopWordsInTopicUnit)
	{
		List<String> allStemmedNonstopWordsInTopicUnitList = Arrays
				.asList(allStemmedNonstopWordsInTopicUnit);
		List<List<String>> powerset = powerset(
				allStemmedNonstopWordsInTopicUnitList, false);
		Assert.isTrue(!powerset.isEmpty());

		SpanOrQuery topicUnitQuery = getTopicUnitQuery(powerset);
		SpanQuery[] allCombinationsOfTuQuery = new SpanQuery[] { topicUnitQuery };
		Set<String> termsInQuery = new HashSet<String>(
				allStemmedNonstopWordsInTopicUnitList);

		@SuppressWarnings("unchecked")
		TermsSpanNearQuery result = new TermsSpanNearQuery(
				allCombinationsOfTuQuery, slop, false, getIndexingFieldName(),
				termsInQuery, Collections.EMPTY_LIST); // not in order

		return result;
	}

}
