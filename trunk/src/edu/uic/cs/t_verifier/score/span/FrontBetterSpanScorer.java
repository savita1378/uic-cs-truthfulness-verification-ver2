package edu.uic.cs.t_verifier.score.span;

import java.io.IOException;

import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.spans.SpanScorer;
import org.apache.lucene.search.spans.Spans;

public class FrontBetterSpanScorer extends SpanScorer
{
	protected FrontBetterSpanScorer(Spans spans, Weight weight,
			Similarity similarity, byte[] norms) throws IOException
	{
		super(spans, weight, similarity, norms);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected boolean setFreqCurrentDoc() throws IOException
	{
		if (!more)
		{
			return false;
		}
		doc = spans.doc();
		freq = 0.0f;
		do
		{
			int matchLength = spans.end() - spans.start();
			freq += (getSimilarity().sloppyFreq(matchLength) * 10000 / spans
					.start());
			more = spans.next();
		}
		while (more && (doc == spans.doc()));
		return true;
	}

	//	@SuppressWarnings("deprecation")
	//	@Override
	//	protected Explanation explain(int doc) throws IOException
	//	{
	//		Explanation tfExplanation = new Explanation();
	//
	//		int expDoc = advance(doc);
	//
	//		float phraseFreq = (expDoc == doc) ? freq : 0.0f;
	//		tfExplanation.setValue(getSimilarity().tf(phraseFreq));
	//
	//		int matchedTime = 0;
	//		List<String> termsStringWithTimes = new ArrayList<String>();
	//		for (Entry<String, Integer> entry : termsMatchedTimesByTermsInString
	//				.entrySet())
	//		{
	//			matchedTime += entry.getValue();
	//			termsStringWithTimes.add(entry.getKey() + "*" + entry.getValue());
	//		}
	//
	//		tfExplanation.setDescription("tf(phraseFreq=" + phraseFreq
	//				+ "); matched [" + matchedTime + "] spans, with times "
	//				+ termsStringWithTimes);
	//
	//		return tfExplanation;
	//	}

}
