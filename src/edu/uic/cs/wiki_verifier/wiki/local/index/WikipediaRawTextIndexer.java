package edu.uic.cs.wiki_verifier.wiki.local.index;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.SAXException;

import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.misc.LogHelper;
import edu.uic.cs.wiki_verifier.wiki.local.misc.Config;

public class WikipediaRawTextIndexer implements WikipediaIndexConstants,
		IArticleFilter
{
	private static final Logger LOGGER = LogHelper
			.getLogger(WikipediaRawTextIndexer.class);

	private IndexWriter writer;

	public WikipediaRawTextIndexer()
	{
		if (!Config.ALLOW_REBUILD_INDEX)
		{
			throw new GeneralException(
					"Rebuild index is not allowed. Change the setting [allow_rebuild_index=true] in [config.properties].");
		}

		try
		{
			Directory directory = FSDirectory.open(new File(
					Config.WIKIPEDIA_INDEX_PATH));
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_36,
					new KeywordAnalyzer());
			iwc.setOpenMode(OpenMode.CREATE);
			writer = new IndexWriter(directory, iwc);
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	@Override
	public void process(WikiArticle article, Siteinfo siteinfo)
			throws SAXException
	{
		if (article.isMain())
		{
			indexArticle(article);
		}
	}

	private void indexArticle(WikiArticle article)
	{
		String id = article.getId();
		String title = article.getTitle();
		String revision = article.getRevisionId();
		String timestamp = article.getTimeStamp();
		String rawText = article.getText();

		Document document = new Document();
		// ID
		document.add(new Field(FIELD_NAME__PAGE_ID, id, Store.YES, Index.NO));
		// lower-case title, used for query
		document.add(new Field(FIELD_NAME__TITLE_IN_LOWER_CASE, title
				.toLowerCase(Locale.US), Store.NO, Index.NOT_ANALYZED_NO_NORMS));// not necessary to store it
		// normal-case title
		document.add(new Field(FIELD_NAME__TITLE_IN_NORMAL_CASE, title,
				Store.YES, Index.NO));
		// revision
		document.add(new Field(FIELD_NAME__PAGE_REVISION_ID, revision,
				Store.YES, Index.NO));
		document.add(new Field(FIELD_NAME__TIMESTAMP, timestamp, Store.YES,
				Index.NO));
		document.add(new Field(FIELD_NAME__RAW_TEXT, rawText, Store.YES,
				Index.NO));

		LOGGER.debug("Indexing [" + id + " | " + title + " | " + timestamp
				+ "]");
		try
		{
			writer.addDocument(document);
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	public void close()
	{
		commit();
		try
		{
			writer.close();
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	public void commit()
	{
		try
		{
			writer.commit();
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	public static void main(String[] args)
	{
		LOGGER.info("Begin indexing Wikipedia file ["
				+ Config.WIKIPEDIA_DUMP_FILE_PATH
				+ "]================================");
		WikipediaRawTextIndexer indexer = new WikipediaRawTextIndexer();

		boolean success = false;
		long start = System.currentTimeMillis();
		try
		{
			WikiXMLParser wxp = new WikiXMLParser(
					Config.WIKIPEDIA_DUMP_FILE_PATH, indexer);
			wxp.parse();

			success = true;
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}
		finally
		{
			indexer.close();

			long end = System.currentTimeMillis();
			long elapsed = end - start;
			LOGGER.info("========================================================");

			if (success)
			{
				LOGGER.info("Indexing is done.\tTime: " + (elapsed / 1000 / 60)
						+ "min");
			}
			else
			{
				LOGGER.info("Indexing is failed.\tTime: "
						+ (elapsed / 1000 / 60) + "min");
			}
		}

	}
}
