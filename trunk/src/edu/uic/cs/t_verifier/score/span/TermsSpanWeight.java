package edu.uic.cs.t_verifier.score.span;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Explanation.IDFExplanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanWeight;

import edu.uic.cs.t_verifier.score.span.explanation.TermsSpanComplexExplanation;
import edu.uic.cs.t_verifier.score.span.explanation.TermsSpanExplanation;

@SuppressWarnings("deprecation")
public class TermsSpanWeight extends SpanWeight
{
	private static final long serialVersionUID = 1L;

	private Set<String> termsInQuery = null;
	private String fieldName = null;
	// private List<String> stemmedNonStopWordsInAlternativeUnit = null;

	private int alternativeUnitWeight;

	private IDFExplanation idfExp;

	public TermsSpanWeight(SpanQuery query, Searcher searcher,
			String fieldName, Set<String> termsInQuery,
			/*List<String> stemmedNonStopWordsInAlternativeUnit,*/
			int alternativeUnitWeight) throws IOException
	{
		super(query, searcher);
		idfExp = similarity.idfExplain(terms, searcher);

		this.fieldName = fieldName;
		this.termsInQuery = termsInQuery;
		/*this.stemmedNonStopWordsInAlternativeUnit = stemmedNonStopWordsInAlternativeUnit;*/
		this.alternativeUnitWeight = alternativeUnitWeight;
	}

	@Override
	public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
			boolean topScorer) throws IOException
	{
		// System.out.println(query);
		return new TermsSpanScorer(query.getSpans(reader), this, similarity,
				reader.norms(query.getField()), fieldName, termsInQuery,
				/*stemmedNonStopWordsInAlternativeUnit,*/reader,
				alternativeUnitWeight);
	}

	@Override
	public TermsSpanComplexExplanation explain(IndexReader reader, int doc)
			throws IOException
	{
		TermsSpanExplanation tfExpl = ((TermsSpanScorer) scorer(reader, true,
				false)).explain(doc);
		////////////////////////////////////////////////////////////////////////

		TermsSpanComplexExplanation result = new TermsSpanComplexExplanation(
				tfExpl.getMatchDetail());
		result.setDescription("weight(" + getQuery() + " in " + doc
				+ "), product of:");
		String field = ((SpanQuery) getQuery()).getField();

		Explanation idfExpl = new Explanation(idf, "idf(" + field + ": "
				+ idfExp.explain() + ")");

		// explain query weight
		Explanation queryExpl = new Explanation();
		queryExpl
				.setDescription("queryWeight(" + getQuery() + "), product of:");

		Explanation boostExpl = new Explanation(getQuery().getBoost(), "boost");
		if (getQuery().getBoost() != 1.0f)
			queryExpl.addDetail(boostExpl);
		queryExpl.addDetail(idfExpl);

		Explanation queryNormExpl = new Explanation(queryNorm, "queryNorm");
		queryExpl.addDetail(queryNormExpl);

		queryExpl.setValue(boostExpl.getValue() * idfExpl.getValue()
				* queryNormExpl.getValue());

		result.addDetail(queryExpl);

		// explain field weight
		TermsSpanComplexExplanation fieldExpl = new TermsSpanComplexExplanation(
				tfExpl.getMatchDetail());
		fieldExpl.setDescription("fieldWeight(" + field + ":"
				+ query.toString(field) + " in " + doc + "), product of:");

		fieldExpl.addDetail(tfExpl);
		fieldExpl.addDetail(idfExpl);

		Explanation fieldNormExpl = new Explanation();
		byte[] fieldNorms = reader.norms(field);
		float fieldNorm = fieldNorms != null ? similarity
				.decodeNormValue(fieldNorms[doc]) : 1.0f;
		fieldNormExpl.setValue(fieldNorm);
		fieldNormExpl.setDescription("fieldNorm(field=" + field + ", doc="
				+ doc + ")");
		fieldExpl.addDetail(fieldNormExpl);

		fieldExpl.setMatch(Boolean.valueOf(tfExpl.isMatch()));
		fieldExpl.setValue(tfExpl.getValue() * idfExpl.getValue()
				* fieldNormExpl.getValue());

		result.addDetail(fieldExpl);
		result.setMatch(fieldExpl.getMatch());

		// combine them
		result.setValue(queryExpl.getValue() * fieldExpl.getValue());

		if (queryExpl.getValue() == 1.0f)
		{
			return fieldExpl;
		}

		return result;
	}
}
