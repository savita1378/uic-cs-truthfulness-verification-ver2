package edu.uic.cs;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import edu.uic.cs.t_verifier.index.analyzer.StemmedSynonymWikipediaAnalyzer;

public class Test
{
	public static void main(String[] args) throws Exception
	{
		Directory directory = new RAMDirectory();

		PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(
				new KeywordAnalyzer());
		wrapper.addAnalyzer("SEGMENTS", new StemmedSynonymWikipediaAnalyzer()
		{
			@Override
			public int getPositionIncrementGap(String fieldName)
			{
				return 10;
			}
		});

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_30,
				wrapper);

		IndexWriter indexWriter = new IndexWriter(directory, config);

		Document document = new Document();
		document.add(new Field("AU", "au_2", Store.YES,
				Index.NOT_ANALYZED_NO_NORMS));
		document.add(new Field("AU", "au_1", Store.YES,
				Index.NOT_ANALYZED_NO_NORMS));
		document.add(new Field("AU", "au_3", Store.YES,
				Index.NOT_ANALYZED_NO_NORMS));

		document.add(new Field("TU", "tu_1", Store.YES,
				Index.NOT_ANALYZED_NO_NORMS));

		document.add(new Field("TU", "tu_2", Store.YES,
				Index.NOT_ANALYZED_NO_NORMS));

		document.add(new Field("SUB_TU", "sub_tu_1", Store.YES,
				Index.NOT_ANALYZED_NO_NORMS));

		for (int index = 0; index < 100; index++)
		{
			document.add(new Field("SEGMENTS", "" + index, Store.NO,
					Index.ANALYZED_NO_NORMS));
		}

		indexWriter.addDocument(document);
		indexWriter.close();

		IndexSearcher indexSearcher = new IndexSearcher(directory);
		//		IndexReader indexReader = indexSearcher.getIndexReader();
		//
		//		FieldCache fieldCache = FieldCache.DEFAULT;
		//		String[] result = fieldCache.getStrings(indexReader, "AU");
		//		System.out.println(Arrays.toString(result));

		//		QueryParser queryParser = new QueryParser(Version.LUCENE_30, "AU",
		//				new KeywordAnalyzer());
		//		Query query = queryParser.parse("*:*");

		//		Query query = new MatchAllDocsQuery();
		//		TopDocs topDocs = indexSearcher.search(query, 10);
		//		for (ScoreDoc scoreDoc : topDocs.scoreDocs)
		//		{
		//			Document doc = indexSearcher.doc(scoreDoc.doc);
		//			String[] aus = doc.getValues("AU");
		//			System.out.println(Arrays.toString(aus));
		//		}
		SpanTermQuery[] queries = new SpanTermQuery[] {
				new SpanTermQuery(new Term("SEGMENTS", "3")),
				new SpanTermQuery(new Term("SEGMENTS", "1")),
				new SpanTermQuery(new Term("SEGMENTS", "2")) };
		SpanNearQuery nearQuery = new SpanNearQuery(queries, 19, false);
		TopDocs topDocs = indexSearcher.search(nearQuery, 10);
		System.out.println(topDocs.totalHits);

		nearQuery = new SpanNearQuery(queries, 20, false);
		topDocs = indexSearcher.search(nearQuery, 10);
		System.out.println(topDocs.totalHits);

		nearQuery = new SpanNearQuery(queries, 1000, false);
		topDocs = indexSearcher.search(nearQuery, 10);
		System.out.println(topDocs.totalHits);

	}
}
