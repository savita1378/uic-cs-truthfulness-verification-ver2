package edu.uic.cs;

import java.io.File;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.uic.cs.t_verifier.index.IndexConstants;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.Config;

public class TestSynonymQuery implements IndexConstants
{
	public static void main2(String[] args) throws Exception
	{
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term(FIELD_NAME__DOC_TYPE,
				DOC_TYPE__PAGE_CONTENT)), Occur.MUST);
		query.add(new TermQuery(new Term(FIELD_NAME__MATCHED_UNIT, "big mac")),
				Occur.MUST); // search only in one certain SUB_TU page

		SpanTermQuery alternativeUnitQuery = new SpanTermQuery(new Term(
				FIELD_NAME__CONTENT_INDEXED_BY_SENTENCE, "mcdonald"));
		SpanTermQuery topicUnitQuery = new SpanTermQuery(new Term(
				FIELD_NAME__CONTENT_INDEXED_BY_SENTENCE, "beefburg"));
		SpanQuery[] auQueryAndAllCombinationsOfTuQuery = new SpanQuery[] {
				alternativeUnitQuery, topicUnitQuery };
		SpanNearQuery spanQUery = new SpanNearQuery(
				auQueryAndAllCombinationsOfTuQuery, 3, false); // not in order

		query.add(spanQUery, Occur.MUST);

		Directory directory = FSDirectory.open(new File(Config.INDEX_FOLDER));
		IndexSearcher indexSearcher = new IndexSearcher(directory);
		TopDocs topDocs = indexSearcher.search(query, 10);
		Assert.isTrue(topDocs.totalHits == 1);
		Explanation explanation = indexSearcher.explain(query,
				topDocs.scoreDocs[0].doc);
		System.out.println(explanation);
	}

	public static void main(String[] args) throws Exception
	{
		// locat
		Directory directory = FSDirectory.open(new File(Config.INDEX_FOLDER));
		IndexSearcher indexSearcher = new IndexSearcher(directory);

		TermPositionVector termFreqVector = (TermPositionVector) indexSearcher
				.getIndexReader().getTermFreqVector(150,
						FIELD_NAME__CONTENT_INDEXED_BY_SENTENCE);
		int[] positions = termFreqVector.getTermPositions(termFreqVector
				.indexOf("kilimanjaro"));
		for (int pos : positions)
		{
			System.out.println(pos);
		}
	}
}
