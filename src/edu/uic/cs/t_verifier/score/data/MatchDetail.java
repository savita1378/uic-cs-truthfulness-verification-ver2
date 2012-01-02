package edu.uic.cs.t_verifier.score.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MatchDetail
{
	private float finalScore = 0f;

	private List<EachSpanDetail> spanDetails = new ArrayList<EachSpanDetail>();

	public List<EachSpanDetail> getSpanDetails()
	{
		Collections.sort(spanDetails, new Comparator<EachSpanDetail>()
		{
			@Override
			public int compare(EachSpanDetail o1, EachSpanDetail o2)
			{
				return Float.compare(o1.score, o2.score);
			}
		});

		return spanDetails;
	}

	public void setScore(float score)
	{
		this.finalScore = score;
	}

	public float getScore()
	{
		return finalScore;
	}

	public static final class EachSpanDetail
	{
		private float score;
		private float matchedRatio;
		private int matchLength;
		private TreeSet<String> notMatchedTermsInQuery;
		private TreeSet<String> matchedTermsInQuery;

		private int docID;
		private int start;
		private int end;

		private EachSpanDetail(float score, float matchedRatio,
				int matchLength, TreeSet<String> matchedTermsInQuery,
				TreeSet<String> notMatchedTermsInQuery, int docID, int start,
				int end)
		{
			this.score = score;
			this.matchedRatio = matchedRatio;
			this.matchLength = matchLength;
			this.matchedTermsInQuery = matchedTermsInQuery;
			this.notMatchedTermsInQuery = notMatchedTermsInQuery;
			this.docID = docID;
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString()
		{
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("Score=").append(score);
			stringBuilder.append("\tMatchedRatio=").append(matchedRatio);
			stringBuilder.append("\tMatchLength=").append(matchLength);
			stringBuilder.append("\tMatchedTermsInQuery=").append(
					matchedTermsInQuery);
			stringBuilder.append("\tNotMatchedTermsInQuery=").append(
					notMatchedTermsInQuery);

			return stringBuilder.toString();
		}

		public int getMatchedNumber()
		{
			return matchedTermsInQuery.size();
		}

		public int getNotMatchedNumber()
		{
			return notMatchedTermsInQuery.size();
		}

		public int getTotalTermsNumber()
		{
			return getMatchedNumber() + getNotMatchedNumber();
		}

		public int getMatchWindowSize()
		{
			return matchLength;
		}

		public int getDocID()
		{
			return docID;
		}

		public int getStart()
		{
			return start;
		}

		public int getEnd()
		{
			return end;
		}

	}

	public void addSpanDetail(TreeSet<String> matchedTerms,
			Set<String> termsInQuery, float matchedRatio, int matchLength,
			float score, int docID, int start, int end)
	{
		TreeSet<String> notMatchedTermsInQuery = new TreeSet<String>(
				termsInQuery);
		notMatchedTermsInQuery.removeAll(matchedTerms);

		EachSpanDetail eachSpanDetail = new EachSpanDetail(score, matchedRatio,
				matchLength, matchedTerms, notMatchedTermsInQuery, docID,
				start, end);
		spanDetails.add(eachSpanDetail);
	}

}
