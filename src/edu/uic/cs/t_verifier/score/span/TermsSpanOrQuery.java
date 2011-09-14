package edu.uic.cs.t_verifier.score.span;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;

@SuppressWarnings("deprecation")
public class TermsSpanOrQuery extends SpanOrQuery
{
	private static final long serialVersionUID = 1L;

	private Set<String> termsInQuery = null;
	private String fieldName = null;
	// private List<String> stemmedNonStopWordsInAlternativeUnit = null;

	private int alternativeUnitWeight;

	public TermsSpanOrQuery(SpanQuery[] clauses, String fieldName,
			Set<String> termsInQuery,
			/*List<String> stemmedNonStopWordsInAlternativeUnit,*/
			int alternativeUnitWeight)
	{
		super(clauses);

		this.fieldName = fieldName;
		this.termsInQuery = termsInQuery;
		// this.stemmedNonStopWordsInAlternativeUnit = stemmedNonStopWordsInAlternativeUnit;
		this.alternativeUnitWeight = alternativeUnitWeight;
	}

	@Override
	public Weight createWeight(Searcher searcher) throws IOException
	{
		return new TermsSpanWeight(this, searcher, fieldName, termsInQuery,
		/*stemmedNonStopWordsInAlternativeUnit,*/alternativeUnitWeight);
	}

}
