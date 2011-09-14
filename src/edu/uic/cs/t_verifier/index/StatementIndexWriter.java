package edu.uic.cs.t_verifier.index;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.uic.cs.t_verifier.index.analyzer.StemmedStandardAnalyzer;
import edu.uic.cs.t_verifier.index.analyzer.StemmedSynonymWikipediaAnalyzer;
import edu.uic.cs.t_verifier.index.data.Heading;
import edu.uic.cs.t_verifier.index.data.Paragraph;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.index.data.Table;
import edu.uic.cs.t_verifier.index.data.UrlWithDescription;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.GeneralException;

class StatementIndexWriter implements IndexConstants
{
	private File indexFolder = null;
	private IndexWriter indexWriter = null;

	StatementIndexWriter(String indexFolder)
	{
		if (!Config.ALLOW_REBUILD_INDEX)
		{
			throw new GeneralException(
					"Rebuid index is not allowed! "
							+ "Change [allow_rebuild_index] in config.properties to [true] "
							+ "if you want to rebuld the index. ");
		}

		this.indexFolder = new File(indexFolder);

		try
		{
			Directory directory = FSDirectory.open(this.indexFolder);

			Analyzer analyzer = initializeAnalyzers();

			IndexWriterConfig config = new IndexWriterConfig(
					Config.LUCENE_VERSION, analyzer);
			config.setOpenMode(OpenMode.CREATE);
			// Defined as default Similarity in Config.java
			// config.setSimilarity(new StatementSimilarity());
			LogMergePolicy logMergePolicy = (LogMergePolicy) config
					.getMergePolicy();
			logMergePolicy.setUseCompoundFile(true);

			indexWriter = new IndexWriter(directory, config);

			indexWriter.deleteAll();
			indexWriter.commit();
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}
	}

	private Analyzer initializeAnalyzers() throws IOException
	{
		PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(
				new KeywordAnalyzer());

		////////////////////////////////////////////////////////////////////////
		Analyzer wikipediaAnalyzer = new StemmedSynonymWikipediaAnalyzer()
		{
			@Override
			public int getPositionIncrementGap(String fieldName)
			{
				return Config.CONTENT_GAP;
			}
		};
		wrapper.addAnalyzer(FIELD_NAME__CONTENT_INDEXED_BY_SEGMENT,
				wikipediaAnalyzer);
		wrapper.addAnalyzer(FIELD_NAME__CONTENT_INDEXED_BY_PARAGRAPH,
				wikipediaAnalyzer);
		wrapper.addAnalyzer(FIELD_NAME__CONTENT_INDEXED_BY_SENTENCE,
				wikipediaAnalyzer);

		////////////////////////////////////////////////////////////////////////
		wrapper.addAnalyzer(FIELD_NAME__ALL_TOPIC_UNIT_WORDS,
				new StemmedStandardAnalyzer());

		return wrapper;
	}

	public void close()
	{
		try
		{
			if (indexWriter != null)
			{
				indexWriter.optimize();
				indexWriter.commit();
			}
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}
		finally
		{
			try
			{
				if (indexWriter != null)
				{
					indexWriter.close();
				}
			}
			catch (Exception e)
			{
				throw new GeneralException(e);
			}
		}
	}

	public void indexStatementMetadata(Statement statement,
			Map<String, List<UrlWithDescription>> urlsByMatchedSubTopicUnit,
			Map<String, List<UrlWithDescription>> urlsByAlternativeUnit)
	{
		Document document = new Document();

		document.add(new Field(FIELD_NAME__STATEMENT_ID, statement.getId()
				.toString(), Store.YES, Index.NOT_ANALYZED_NO_NORMS)); // not analyzed
		document.add(new Field(FIELD_NAME__STATEMENT_TYPE, statement.getType()
				.toString(), Store.YES, Index.NOT_ANALYZED_NO_NORMS)); // not analyzed
		document.add(new Field(FIELD_NAME__DOC_TYPE,
				DOC_TYPE__STATEMENT_METADATA, Store.YES,
				Index.NOT_ANALYZED_NO_NORMS)); // not analyzed

		// TU //////////////////////////////////////////////////////////////////
		for (Entry<String, List<UrlWithDescription>> urlWithDescriptionByMatchedSubTopicUnit : urlsByMatchedSubTopicUnit
				.entrySet())
		{
			String matchedTopicUnit = urlWithDescriptionByMatchedSubTopicUnit
					.getKey();
			document.add(new Field(FIELD_NAME__MATCHED_SUB_TU,
					matchedTopicUnit, Store.YES, Index.NOT_ANALYZED_NO_NORMS));

			// use ["MATCHED_SUB_TU_URLS__" + URL] as field name 
			String fieldNameForUrls = FIELD_NAME__PREFIX__MATCHED_SUB_TU_URLS
					+ matchedTopicUnit;
			for (UrlWithDescription urlWithDescription : urlWithDescriptionByMatchedSubTopicUnit
					.getValue())
			{
				document.add(new Field(fieldNameForUrls, urlWithDescription
						.getUrl(), Store.YES, Index.NOT_ANALYZED_NO_NORMS));
			}
		}

		List<String> topicUnits = statement.getTopicUnits();
		for (String topicUnit : topicUnits)
		{
			document.add(new Field(FIELD_NAME__ALL_TOPIC_UNIT_WORDS, topicUnit,
					Store.YES, Index.ANALYZED_NO_NORMS,
					TermVector.WITH_POSITIONS_OFFSETS)); // analyzed into stemmed non-stop words
		}

		// AU //////////////////////////////////////////////////////////////////
		List<String> alternativeUnits = statement.getAlternativeUnits();
		for (String auString : alternativeUnits)
		{
			document.add(new Field(FIELD_NAME__AlTERNATIVE_UNIT, auString,
					Store.YES, Index.NOT_ANALYZED_NO_NORMS)); // not analyzed
			// TODO here, we ignore the weight of the Alternative Units

			if (urlsByAlternativeUnit == null) // no need to record the AU pages
			{
				continue;
			}

			List<UrlWithDescription> urlsForAu = urlsByAlternativeUnit
					.get(auString);
			if (urlsForAu == null) // if AU cann't match a page or pages, this URL list is null
			{
				continue;
			}

			// use ["AlTERNATIVE_UNIT_URLS__" + URL] as field name 
			String fieldNameForUrls = FIELD_NAME__PREFIX__AlTERNATIVE_UNIT_URLS
					+ auString;
			for (UrlWithDescription urlWithDescription : urlsForAu)
			{
				document.add(new Field(fieldNameForUrls, urlWithDescription
						.getUrl(), Store.YES, Index.NOT_ANALYZED_NO_NORMS));
			}
		}

		////////////////////////////////////////////////////////////////////////
		try
		{
			indexWriter.addDocument(document);
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}
	}

	/**
	 * One topic/alternative unit may match many pages(when there are disambiguations)
	 * Right now, all the disambiguations pages are grouped together and considered as one page
	 * 
	 * @param matchedUnit
	 * @param segmentsOfMatchedPages
	 */
	public void indexPageContentOfMatchedUnit(String matchedUnit,
			List<List<Segment>> segmentsOfMatchedPages)
	{
		// one document for one topic-unit
		Document document = new Document();
		document.add(new Field(FIELD_NAME__DOC_TYPE, DOC_TYPE__PAGE_CONTENT,
				Store.YES, Index.NOT_ANALYZED_NO_NORMS)); // not analyzed

		addMatchedUnitContentIntoDocument(document, matchedUnit,
				segmentsOfMatchedPages);

		try
		{
			indexWriter.addDocument(document);
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}

	}

	private void addMatchedUnitContentIntoDocument(Document document,
			String matchedUnit, List<List<Segment>> segmentsOfMatchedPages)
	{
		document.add(new Field(FIELD_NAME__MATCHED_UNIT, matchedUnit,
				Store.YES, Index.NOT_ANALYZED_NO_NORMS));

		// One topic unit may match many pages(when there are disambiguations)
		// each page appends to another, as they are one page
		for (List<Segment> segmentsInOnePage : segmentsOfMatchedPages)
		{
			for (Segment segment : segmentsInOnePage)
			{
				// for segment
				document.add(new Field(FIELD_NAME__CONTENT_INDEXED_BY_SEGMENT,
						segment.toString(), Store.NO, Index.ANALYZED_NO_NORMS,
						TermVector.WITH_POSITIONS_OFFSETS));

				///////////////////////////////////////////////////////////////
				// for paragraph/sentence
				Heading heading = segment.getHeading();
				List<Paragraph> paragraphs = segment.getParagraphs();
				List<Table> tables = segment.getTables();

				if (heading != null)
				{
					// heading be considered as a single paragraph
					document.add(new Field(
							FIELD_NAME__CONTENT_INDEXED_BY_PARAGRAPH, heading
									.toString(), Store.NO,
							Index.ANALYZED_NO_NORMS,
							TermVector.WITH_POSITIONS_OFFSETS));

					// heading be considered as a single sentence
					document.add(new Field(
							FIELD_NAME__CONTENT_INDEXED_BY_SENTENCE, heading
									.toString(), Store.NO,
							Index.ANALYZED_NO_NORMS,
							TermVector.WITH_POSITIONS_OFFSETS));
				}

				if (paragraphs != null)
				{
					for (Paragraph paragraph : paragraphs)
					{
						document.add(new Field(
								FIELD_NAME__CONTENT_INDEXED_BY_PARAGRAPH,
								paragraph.toString(), Store.NO,
								Index.ANALYZED_NO_NORMS,
								TermVector.WITH_POSITIONS_OFFSETS));

						////////////////////////////////////////////////////////
						// for sentence
						for (String sentence : paragraph.getSentences())
						{
							document.add(new Field(
									FIELD_NAME__CONTENT_INDEXED_BY_SENTENCE,
									sentence, Store.NO,
									Index.ANALYZED_NO_NORMS,
									TermVector.WITH_POSITIONS_OFFSETS));
						}
					}
				}

				if (tables != null)
				{
					for (Table table : tables)
					{
						document.add(new Field(
								FIELD_NAME__CONTENT_INDEXED_BY_PARAGRAPH, table
										.toString(), Store.NO,
								Index.ANALYZED_NO_NORMS,
								TermVector.WITH_POSITIONS_OFFSETS));

						////////////////////////////////////////////////////////
						// for sentence
						for (String sentence : table.getSentences())
						{
							document.add(new Field(
									FIELD_NAME__CONTENT_INDEXED_BY_SENTENCE,
									sentence, Store.NO,
									Index.ANALYZED_NO_NORMS,
									TermVector.WITH_POSITIONS_OFFSETS));
						}
					}
				}

			}
		}

	}
}
