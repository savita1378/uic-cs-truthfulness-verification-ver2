package edu.uic.cs.t_verifier.score.span;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanWeight;

@SuppressWarnings("deprecation")
public class FrontBetterSpanWeight extends SpanWeight
{
	private static final long serialVersionUID = 1L;

	public FrontBetterSpanWeight(SpanQuery query, Searcher searcher)
			throws IOException
	{
		super(query, searcher);
	}

	@Override
	public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
			boolean topScorer) throws IOException
	{
		return new FrontBetterSpanScorer(query.getSpans(reader), this,
				similarity, reader.norms(query.getField()));
	}
}
