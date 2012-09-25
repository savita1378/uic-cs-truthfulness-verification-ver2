package edu.uic.cs.wiki_verifier.wiki.local.search;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.misc.LogHelper;
import edu.uic.cs.wiki_verifier.wiki.local.index.WikipediaIndexConstants;
import edu.uic.cs.wiki_verifier.wiki.local.misc.Config;

public class WikipediaRawTextSearcher implements WikipediaIndexConstants
{
	private static final Logger LOGGER = LogHelper
			.getLogger(WikipediaRawTextSearcher.class);

	private IndexSearcher searcher;

	public WikipediaRawTextSearcher()
	{
		Directory directory;
		try
		{
			directory = FSDirectory.open(new File(Config.WIKIPEDIA_INDEX_PATH));
			IndexReader indexReader = IndexReader.open(directory);

			searcher = new IndexSearcher(indexReader);
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	/**
	 * There may be more than one documents/pages matched due to the titles were indexed in lower-case. <br/>
	 * For example, both "CHINA" and "china" are indexed, when doing the retrieval, 
	 * we don't differentiate the case. So, we could retrieve both two pages above. 
	 * 
	 * @param title
	 * @return
	 */
	public Map<String, String> retrieveRawTextsByTitle(String title)
	{
		return retrieveIndexedFieldsByTitle(title, FIELD_NAME__RAW_TEXT);
	}

	/**
	 * There may be more than one documents/pages matched due to the titles were indexed in lower-case. <br/>
	 * For example, both "CHINA" and "china" are indexed, when doing the retrieval, 
	 * we don't differentiate the case. So, we could retrieve both two pages above. 
	 * 
	 * @param title
	 * @param fieldName
	 * @return
	 */
	public Map<String, String> retrieveIndexedFieldsByTitle(String title,
			String retrievingFieldName)
	{
		title = title.toLowerCase(Locale.US);

		/**
		 * ONLY FIELD_NAME__TITLE_IN_LOWER_CASE has been indexed!! :(
		 */
		TermQuery query = new TermQuery(new Term(
				FIELD_NAME__TITLE_IN_LOWER_CASE, title));

		try
		{
			TopDocs topDocs = searcher.search(query, 10);
			ScoreDoc[] docs = topDocs.scoreDocs;
			if (docs == null || docs.length == 0)
			{
				if (LOGGER.isDebugEnabled())
				{
					LOGGER.debug("No record been retrieved for queryKey ["
							+ title + "]. ");
				}
				return null;
			}

			HashMap<String, String> result = new HashMap<String, String>(
					docs.length);
			for (ScoreDoc doc : docs)
			{
				Document document = searcher.doc(doc.doc);
				String originalTitle = document
						.get(FIELD_NAME__TITLE_IN_NORMAL_CASE);
				String fieldValue = document.get(retrievingFieldName);

				Assert.isTrue(result.put(originalTitle, fieldValue) == null,
						"Title [" + originalTitle + "] is duplicate! ");

				if (LOGGER.isDebugEnabled())
				{
					LOGGER.debug("Record [title=" + originalTitle
							+ "] in field [" + retrievingFieldName
							+ "] has been retrieved. ");
				}
			}

			return result;
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	public static void main(String[] args)
	{
		Map<String, String> result = new WikipediaRawTextSearcher()
				.retrieveRawTextsByTitle("crime");
		for (Entry<String, String> textByTitle : result.entrySet())
		{
			System.out.println(textByTitle);
		}
	}

}
