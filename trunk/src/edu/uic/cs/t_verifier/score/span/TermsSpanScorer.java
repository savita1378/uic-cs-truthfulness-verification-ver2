package edu.uic.cs.t_verifier.score.span;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.spans.SpanScorer;
import org.apache.lucene.search.spans.Spans;

import edu.uic.cs.t_verifier.score.data.MatchDetail;
import edu.uic.cs.t_verifier.score.span.explanation.TermsSpanExplanation;

public class TermsSpanScorer extends SpanScorer
{
	private IndexReader reader = null;

	private String fieldName = null;
	private int totalTermsNumInQuery;

	private int alternativeUnitWeight;

	// the terms in TU
	private Set<String> tuTermsInQuery = null;
	// the words in AU should not be counted in a window
	private List<String> stemmedNonStopWordsInAlternativeUnit = null;

	//	private List<Entry<Integer, TreeSet<String>>> eachTimeMatchedTermsByNum = new ArrayList<Entry<Integer, TreeSet<String>>>();
	private Map<String, Integer> termsMatchedTimesByTermsInString = new TreeMap<String, Integer>();
	private Map<String, List<Integer>> termsMatchedWindowSizeByTermsInString = new TreeMap<String, List<Integer>>();

	private Set<String> alreadyProcessedSpans = new HashSet<String>();

	private Map<Integer, TreeMap<Integer, TreeSet<String>>> tuTermPositionsCache = new HashMap<Integer, TreeMap<Integer, TreeSet<String>>>();
	private Map<Integer, TreeMap<Integer, TreeSet<String>>> auTermPositionsCache = new HashMap<Integer, TreeMap<Integer, TreeSet<String>>>();

	private MatchDetail matchDetail = new MatchDetail();
	private int docID;

	protected TermsSpanScorer(int docID, Spans spans, Weight weight,
			Similarity similarity, byte[] norms, String fieldName,
			Set<String> termsInQuery,
			List<String> stemmedNonStopWordsInAlternativeUnit,
			IndexReader reader, int alternativeUnitWeight) throws IOException
	{
		super(spans, weight, similarity, norms);

		this.docID = docID;

		this.fieldName = fieldName;
		this.tuTermsInQuery = termsInQuery;
		this.totalTermsNumInQuery = termsInQuery.size();
		this.stemmedNonStopWordsInAlternativeUnit = stemmedNonStopWordsInAlternativeUnit;
		this.reader = reader;
		this.alternativeUnitWeight = alternativeUnitWeight;

		//		System.out.println("================================");
		//		System.out.println(tuTermsInQuery);
		//		System.out.println(stemmedNonStopWordsInAlternativeUnit);
		//		System.out.println();
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
		if (docID != doc)
		{
			return true;
		}

		TreeMap<Integer, TreeSet<String>> tuTermPositions = getTuTermPositions(doc);
		// System.out.println("\t" + tuTermPositions);
		TreeMap<Integer, TreeSet<String>> auTermPositions = getAuTermPositions(doc);
		// System.out.println("\t" + auTermPositions);
		//		int auTermsStartPosition = getAuStartTermPositions(doc, spans.start(),
		//				spans.end());
		//		int auTermsEndPosition = auTermsStartPosition
		//				+ stemmedNonStopWordsInAlternativeUnit.size() - 1;

		freq = 0.0f;
		termsMatchedTimesByTermsInString.clear();
		termsMatchedWindowSizeByTermsInString.clear();

		alreadyProcessedSpans.clear();

		do
		{
			boolean auInEitherSideOfWindow = isAuInEitherSideOfWindow(
					auTermPositions, spans.start(), spans.end());

			int matchLength = spans.end() - spans.start();
			if (auInEitherSideOfWindow)
			{
				matchLength = matchLength
						- stemmedNonStopWordsInAlternativeUnit.size();
			}

			String processingSpanID = doc + "|" + spans.start() + "|"
					+ spans.end();
			// System.out.println("\t" + processingSpanID);
			// TODO I am not sure why the same span may be processed may times...
			// seems related to synonyms
			if (!alreadyProcessedSpans.contains(processingSpanID))
			{
				alreadyProcessedSpans.add(processingSpanID);

				// not including the terms in AU
				TreeSet<String> matchedTerms = computeMatchedTerms(
						spans.start(), spans.end(), tuTermPositions);
				int matchedTermsNumber = matchedTerms.size();
				/*if (matchedTerms.isEmpty())
				{
					matchedTerms.add("AU"
							+ stemmedNonStopWordsInAlternativeUnit);
				}*/

				if (!matchedTerms.isEmpty())
				{
					String matchedTermsInString = matchedTerms.toString();
					Integer matchedTimes = termsMatchedTimesByTermsInString
							.get(matchedTermsInString);
					if (matchedTimes == null)
					{
						termsMatchedTimesByTermsInString.put(
								matchedTermsInString, Integer.valueOf(1));
						termsMatchedWindowSizeByTermsInString.put(
								matchedTermsInString, new ArrayList<Integer>());
						termsMatchedWindowSizeByTermsInString.get(
								matchedTermsInString).add(matchLength);
					}
					else
					{
						termsMatchedTimesByTermsInString.put(
								matchedTermsInString,
								Integer.valueOf(matchedTimes.intValue() + 1));
						termsMatchedWindowSizeByTermsInString.get(
								matchedTermsInString).add(matchLength);
					}

					// not use this matchedRato since it is too mall, would cause the final score hard to differentiate
					//			float matchedRato = ((matchedTermsNumber + 1) / (totalTermsNumInQuery + 1));
					//			Assert.isTrue(matchedRato <= 1.0F);
					float matchedRatio = ((float) matchedTermsNumber)
							/ totalTermsNumInQuery;

					// matchedRato^2 since the final score is computed by tf(){ Math.sqrt(freq); }

					float score = alternativeUnitWeight
							* getSimilarity().sloppyFreq(matchLength)
							* (matchedRatio * matchedRatio * matchedRatio * matchedRatio);// matchedRatio is more important!

					matchDetail.addSpanDetail(matchedTerms, tuTermsInQuery,
							matchedRatio, matchLength, score, spans.doc(),
							spans.start(), spans.end());

					// use the sum of scores
					// freq += score;

					// use the max score
					if (freq < score)
					{
						freq = score;
					}
				}
			}

			more = spans.next();
		}
		while (more && (doc == spans.doc()));

		return true;
	}

	private boolean isAuInEitherSideOfWindow(
			TreeMap<Integer, TreeSet<String>> auTermPositions, int spanStart,
			int spanEnd)
	{
		int auLength = stemmedNonStopWordsInAlternativeUnit.size();

		boolean auAtBegin = true;
		for (int index = 0; index < auLength; index++)
		{
			TreeSet<String> termsInPosition = auTermPositions.get(index
					+ spanStart);
			if (termsInPosition == null
					|| !termsInPosition
							.contains(stemmedNonStopWordsInAlternativeUnit
									.get(index)))
			{
				auAtBegin = false;
				break;
			}
		}
		if (auAtBegin)
		{
			return true;
		}

		boolean auAtEnd = true;
		for (int index = auLength - 1; index >= 0; index--)
		{
			TreeSet<String> termsInPosition = auTermPositions.get(spanEnd - 1
					- index);
			if (termsInPosition == null
					|| !termsInPosition
							.contains(stemmedNonStopWordsInAlternativeUnit
									.get(auLength - 1 - index)))
			{
				auAtEnd = false;
				break;
			}
		}
		if (auAtEnd)
		{
			return true;
		}

		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected TermsSpanExplanation explain(int doc) throws IOException
	{
		TermsSpanExplanation tfExplanation = new TermsSpanExplanation(
				this.matchDetail);

		int expDoc = advance(doc);

		float phraseFreq = (expDoc == doc) ? freq : 0.0f;
		tfExplanation.setValue(getSimilarity().tf(phraseFreq));

		int matchedTime = 0;
		List<String> termsStringWithTimes = new ArrayList<String>();
		for (Entry<String, Integer> timesByTerm : termsMatchedTimesByTermsInString
				.entrySet())
		{
			matchedTime += timesByTerm.getValue();
			termsStringWithTimes.add(timesByTerm.getKey()
					+ "*"
					+ timesByTerm.getValue()
					+ termsMatchedWindowSizeByTermsInString.get(timesByTerm
							.getKey()));
		}

		tfExplanation.setDescription("tf(phraseFreq=" + phraseFreq
				+ "); matched [" + matchedTime + "] spans, with times "
				+ termsStringWithTimes);

		return tfExplanation;
	}

	//	private int[] getMinAndMaxIndexInDoc(int doc) throws IOException
	//	{
	//		TermPositionVector termFreqVector = (TermPositionVector) reader
	//				.getTermFreqVector(doc, fieldName);
	//		String[] allTerms = termFreqVector.getTerms();
	//
	//		int min = Integer.MAX_VALUE;
	//		int max = -1;
	//		for (String term : allTerms)
	//		{
	//			int[] positions = termFreqVector.getTermPositions(termFreqVector
	//					.indexOf(term));
	//			for (int p : positions)
	//			{
	//				if (p > max)
	//				{
	//					max = p;
	//				}
	//
	//				if (p < min)
	//				{
	//					min = p;
	//				}
	//			}
	//		}
	//
	//		return new int[] { min, max };
	//	}

	private TreeSet<String> computeMatchedTerms(int start, int end,
			TreeMap<Integer, TreeSet<String>> tuTermPositions)
	{
		TreeSet<String> matchedTerms = new TreeSet<String>();

		// System.out.println(start + "~" + end);
		for (int index = start; index < end; index++)
		{
			TreeSet<String> termsInPosition = tuTermPositions.get(Integer
					.valueOf(index));
			if (termsInPosition != null)
			{
				matchedTerms.addAll(termsInPosition);
			}
		}

		// return matchedTerms.size();
		return matchedTerms;
	}

	private TreeMap<Integer, TreeSet<String>> getTermPositions(int doc,
			Collection<String> terms,
			Map<Integer, TreeMap<Integer, TreeSet<String>>> termPositionsCache)
			throws IOException
	{
		TreeMap<Integer, TreeSet<String>> termPositions = termPositionsCache
				.get(Integer.valueOf(doc));
		if (termPositions == null)
		{
			termPositions = new TreeMap<Integer, TreeSet<String>>();
			TermPositionVector termFreqVector = (TermPositionVector) reader
					.getTermFreqVector(doc, fieldName);

			for (String term : terms)
			{
				int[] positions = termFreqVector
						.getTermPositions(termFreqVector.indexOf(term));
				for (int pos : positions)
				{
					TreeSet<String> termsInOnePosition = termPositions
							.get(Integer.valueOf(pos));
					if (termsInOnePosition == null)
					{
						termsInOnePosition = new TreeSet<String>();
						termPositions.put(pos, termsInOnePosition);
					}
					// due to synonym, may add one position multiple times
					termsInOnePosition.add(term);
				}
			}

			termPositionsCache.put(Integer.valueOf(doc), termPositions);
		}

		return termPositions;
	}

	private TreeMap<Integer, TreeSet<String>> getTuTermPositions(int doc)
			throws IOException
	{
		return getTermPositions(doc, tuTermsInQuery, tuTermPositionsCache);
	}

	private TreeMap<Integer, TreeSet<String>> getAuTermPositions(int doc)
			throws IOException
	{
		return getTermPositions(doc, stemmedNonStopWordsInAlternativeUnit,
				auTermPositionsCache);
	}

	//	private int getAuStartTermPositions(int doc, int spanStart, int spanEnd)
	//			throws IOException
	//	{
	//		TreeMap<Integer, TreeSet<String>> auTermPositions = getAuTermPositions(doc);
	//		if (auTermPositions.isEmpty())
	//		{
	//			Assert.isTrue(stemmedNonStopWordsInAlternativeUnit.isEmpty());
	//			return -1;
	//		}
	//
	//		System.out.println("======== " + spanStart + "  " + spanEnd);
	//		TreeMap<Integer, TreeSet<String>> tuTermsPositions = getTuTermPositions(doc);
	//		for (int index = spanStart; index < spanEnd; index++)
	//		{
	//			System.out.print(tuTermsPositions.get(index) + " ");
	//		}
	//		System.out.println();
	//		System.out.println(auTermPositions);
	//		System.out.println(stemmedNonStopWordsInAlternativeUnit);
	//		System.out.println();
	//
	//		int auEndIndex = -1;
	//		String lastTermInAu = stemmedNonStopWordsInAlternativeUnit
	//				.get(stemmedNonStopWordsInAlternativeUnit.size() - 1);
	//		for (int index = spanEnd - 1; index >= spanStart; index--)
	//		{
	//			TreeSet<String> termsInPosition = auTermPositions.get(Integer
	//					.valueOf(index));
	//			if (termsInPosition != null
	//					&& termsInPosition.contains(lastTermInAu))
	//			{
	//				auEndIndex = index;
	//				break;
	//			}
	//		}
	//
	//		int auBeginIndex = auEndIndex + 1
	//				- stemmedNonStopWordsInAlternativeUnit.size();
	//		System.out.println("[" + auBeginIndex + ", " + auEndIndex + "]");
	//		for (int index = auBeginIndex; index <= auEndIndex; index++)
	//		{
	//			System.out.println("** " + index);
	//			TreeSet<String> termsInPosition = auTermPositions.get(Integer
	//					.valueOf(index));
	//			Assert.isTrue(termsInPosition
	//					.contains(stemmedNonStopWordsInAlternativeUnit.get(index
	//							- auBeginIndex)));
	//		}
	//
	//		return auBeginIndex;
	//	}
}
