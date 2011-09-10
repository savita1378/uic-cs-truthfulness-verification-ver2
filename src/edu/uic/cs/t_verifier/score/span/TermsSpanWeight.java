package edu.uic.cs.t_verifier.score.span;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanWeight;

@SuppressWarnings("deprecation")
public class TermsSpanWeight extends SpanWeight
{
	private static final long serialVersionUID = 1L;

	private Set<String> termsInQuery = null;
	private String fieldName = null;
	private List<String> stemmedNonStopWordsInAlternativeUnit = null;

	private int alternativeUnitWeight;

	public TermsSpanWeight(SpanQuery query, Searcher searcher,
			String fieldName, Set<String> termsInQuery,
			List<String> stemmedNonStopWordsInAlternativeUnit,
			int alternativeUnitWeight) throws IOException
	{
		super(query, searcher);

		this.fieldName = fieldName;
		this.termsInQuery = termsInQuery;
		this.stemmedNonStopWordsInAlternativeUnit = stemmedNonStopWordsInAlternativeUnit;
		this.alternativeUnitWeight = alternativeUnitWeight;
	}

	@Override
	public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
			boolean topScorer) throws IOException
	{
		// System.out.println(query);
		return new TermsSpanScorer(query.getSpans(reader), this, similarity,
				reader.norms(query.getField()), fieldName, termsInQuery,
				stemmedNonStopWordsInAlternativeUnit, reader,
				alternativeUnitWeight);
	}
}
