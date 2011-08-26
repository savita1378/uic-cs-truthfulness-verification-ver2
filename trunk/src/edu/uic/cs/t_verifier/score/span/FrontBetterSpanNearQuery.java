package edu.uic.cs.t_verifier.score.span;

import java.io.IOException;

import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;

@SuppressWarnings("deprecation")
public class FrontBetterSpanNearQuery extends SpanNearQuery
{
	private static final long serialVersionUID = 1L;

	public FrontBetterSpanNearQuery(SpanQuery[] clauses, int slop,
			boolean inOrder)
	{
		super(clauses, slop, inOrder);
	}

	@Override
	public Weight createWeight(Searcher searcher) throws IOException
	{
		return new FrontBetterSpanWeight(this, searcher);
	}

}
