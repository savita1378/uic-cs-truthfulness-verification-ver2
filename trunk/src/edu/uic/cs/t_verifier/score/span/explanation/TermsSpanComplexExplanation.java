package edu.uic.cs.t_verifier.score.span.explanation;

import org.apache.lucene.search.ComplexExplanation;

import edu.uic.cs.t_verifier.score.data.MatchDetail;

public class TermsSpanComplexExplanation extends ComplexExplanation
{
	private static final long serialVersionUID = 1L;

	private MatchDetail matchDetail = null;

	public TermsSpanComplexExplanation(MatchDetail matchDetail)
	{
		this.matchDetail = matchDetail;
	}

	public MatchDetail getMatchDetail()
	{
		return matchDetail;
	}
}
