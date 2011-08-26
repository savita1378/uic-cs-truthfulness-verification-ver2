package edu.uic.cs.t_verifier.score;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.Explanation.IDFExplanation;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.spans.SpanScorer;

@SuppressWarnings("deprecation")
public class StatementSimilarity extends DefaultSimilarity
{
	private static final long serialVersionUID = 1L;

	/** Implemented as <code>1/sqrt(sumOfSquaredWeights)</code>. */
	@Override
	public float queryNorm(float sumOfSquaredWeights)
	{
		//		return (float) (1.0 / Math.sqrt(sumOfSquaredWeights));
		return 1.0f;
	}

	/** Implemented as <code>sqrt(freq)</code>. */
	@Override
	public float tf(float freq)
	{
		return (float) Math.sqrt(freq);
	}

	/**
	 * Implemented as <code>1 / (distance + 1)</code>. 
	 * <br>
	 * The longer the span is, the value is smaller
	 * for one matched span, this value will be send to {@link #tf(float)} to compute tf. 
	 * <br>
	 * If there are multiple matched span, each tf will add up together. 
	 * @see SpanScorer#setFreqCurrentDoc()
	 */
	@Override
	public float sloppyFreq(int distance)
	{
		return 1.0f / (distance + 1);
	}

	/** Do not consider this document frequency */
	@Override
	public float idf(int docFreq, int numDocs)
	{
		//		return (float) (Math.log(numDocs / (double) (docFreq + 1)) + 1.0);
		return 1.0f;
	}

	/**
	 * return the max idf among terms
	 */
	@Override
	public IDFExplanation idfExplain(Collection<Term> terms, Searcher searcher)
			throws IOException
	{
		IDFExplanation result = null;
		float maxIdf = 0.0F;

		IDFExplanation explanation = null;
		for (Term term : terms)
		{
			explanation = idfExplain(term, searcher);
			if (explanation.getIdf() >= maxIdf)
			{
				result = explanation;
				maxIdf = explanation.getIdf();
			}
		}

		if (result == null)
		{
			result = new IDFExplanation()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public float getIdf()
				{
					return 1.0F;
				}

				@Override
				public String explain()
				{
					return "";
				}
			};
		}

		return result;
	}
}
