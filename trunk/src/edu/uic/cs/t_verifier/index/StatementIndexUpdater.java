package edu.uic.cs.t_verifier.index;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.uic.cs.t_verifier.common.StatementType;
import edu.uic.cs.t_verifier.html.WikipediaContentExtractor;
import edu.uic.cs.t_verifier.html.data.MatchedQueryKey;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.index.data.UrlWithDescription;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.ClassFactory;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.GeneralException;

public class StatementIndexUpdater implements IndexConstants
{
	private WikipediaContentExtractor wikipediaContentExtractor = ClassFactory
			.getInstance(Config.WIKIPEDIACONTENTEXTRACTOR_CLASS_NAME);

	private StatementIndexWriter indexWriter = new StatementIndexWriter(
			Config.INDEX_FOLDER, false); // DO NOT rebuild index

	private IndexSearcher indexSearcher;

	private static final StatementIndexUpdater INSTANCE = new StatementIndexUpdater();

	public static StatementIndexUpdater getInstance()
	{
		return INSTANCE;
	}

	private StatementIndexUpdater()
	{
		try
		{
			Directory directory = FSDirectory
					.open(new File(Config.INDEX_FOLDER));
			IndexReader indexReader = IndexReader.open(directory);
			this.indexSearcher = new IndexSearcher(indexReader);

		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}

	}

	private Map<String, List<String>> runtimeCache = new HashMap<String, List<String>>();

	/**
	 * NOT thread safety!
	 */
	public List<String> retrieveAndIndexUnitPage(String unit)
	{
		if (runtimeCache.containsKey(unit))
		{
			// if user didn't re-open indexer, she can't see the updated data
			return runtimeCache.get(unit);
		}

		checkUnitNotExistingInIndex(unit);

		MatchedQueryKey matchedQueryKey = wikipediaContentExtractor
				.matchQueryKey(unit);
		if (matchedQueryKey == null || !matchedQueryKey.isCertainly())
		{
			return null;
		}

		UrlWithDescription urlWithDescription = new UrlWithDescription(
				matchedQueryKey.getCertainPageUrl(), null,
				matchedQueryKey.getCategories());

		// extract content from each page
		List<Segment> segmentsInPage = wikipediaContentExtractor
				.extractSegmentsFromWikipedia(urlWithDescription,
						StatementType.match(unit) == StatementType.YEAR); // YEAR page is BulletinPage

		indexWriter.indexPageContentOfMatchedUnit(unit, Collections
				.singletonList(segmentsInPage), new HashSet<String>(
				urlWithDescription.getCategoriesBelongsTo()));

		indexWriter.commit();

		List<String> result = urlWithDescription.getCategoriesBelongsTo();
		runtimeCache.put(unit, result);

		return result;
	}

	private void checkUnitNotExistingInIndex(String unitString)
	{
		try
		{
			BooleanQuery query = new BooleanQuery();
			query.add(new TermQuery(new Term(FIELD_NAME__DOC_TYPE,
					DOC_TYPE__PAGE_CONTENT)), Occur.MUST);
			query.add(new TermQuery(new Term(FIELD_NAME__MATCHED_UNIT,
					unitString)), Occur.MUST);

			TopDocs topDocs = indexSearcher.search(query, 10);
			Assert.isTrue(topDocs.totalHits == 0,
					"There should be NO document matched for Unit["
							+ unitString + "]! ");
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}
}
