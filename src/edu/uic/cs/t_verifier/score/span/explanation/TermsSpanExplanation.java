package edu.uic.cs.t_verifier.score.span.explanation;

import org.apache.lucene.search.Explanation;

import edu.uic.cs.t_verifier.score.data.MatchDetail;

public class TermsSpanExplanation extends Explanation
{
	private static final long serialVersionUID = 1L;

	private MatchDetail matchDetail = null;

	public TermsSpanExplanation(MatchDetail matchDetail)
	{
		this.matchDetail = matchDetail;
	}

	public MatchDetail getMatchDetail()
	{
		return matchDetail;
	}

}
