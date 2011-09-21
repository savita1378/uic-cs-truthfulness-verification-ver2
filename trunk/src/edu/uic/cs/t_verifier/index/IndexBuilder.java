package edu.uic.cs.t_verifier.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.uic.cs.t_verifier.common.StatementType;
import edu.uic.cs.t_verifier.html.WikipediaContentExtractor;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.index.data.UrlWithDescription;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.ClassFactory;
import edu.uic.cs.t_verifier.misc.Config;

public class IndexBuilder
{
	// analyze and get useful Topic Units for each Statement
	private static final StatementAnalyzer STATEMENT_ANALYZER = new StatementAnalyzer();

	// extract content from the Topic Unit pages
	private static final WikipediaContentExtractor WIKIPEDIA_CONTENT_EXTRACTOR = ClassFactory
			.getInstance(Config.WIKIPEDIACONTENTEXTRACTOR_CLASS_NAME);

	// write into index
	private static StatementIndexWriter INDEX_WRITER = null;

	public static void main(String[] args)
	{
		INDEX_WRITER = new StatementIndexWriter(Config.INDEX_FOLDER);

		// read from input file
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();

		// either TU or AU
		HashSet<String> indexedUnits = new HashSet<String>();
		try
		{
			for (int index = 0; index < statements.size(); index++)
			{
				Statement statement = statements.get(index);
				System.out.println("Processing Statement[" + (index + 1)
						+ "]... " + statement.getAllWordsInTopicUnits() + "//"
						+ statement.getAlternativeUnits());

				indexStatement(statement, indexedUnits);

				System.out
						.println("=========================================\n\n");
			}

			System.out
					.println("Done. All the needed data have been indexed into folder["
							+ Config.INDEX_FOLDER + "]. ");
		}
		finally
		{
			INDEX_WRITER.close();
		}
	}

	/**
	 * Alternative Unit's URL
	 * 
	 * @param statement
	 * @return  returning null, means we will also index alternative unit's page content
	 */
	private static Map<String, List<UrlWithDescription>> getUrlsByAlternativeUnit(
			Statement statement)
	{
		switch (statement.getType())
		{
			case NORMAL_STRING:
			case SUPERLATIVE_STRING:
			case NUMBER:
				return STATEMENT_ANALYZER.getUrlsByAlternativeUnit(statement,
						true);

				// case SUPERLATIVE_STRING: // no good to use AU for such type
			case YEAR:
				return STATEMENT_ANALYZER.getUrlsByAlternativeUnit(statement,
						false);

			default:
				Assert.isTrue(false);
				return null;
		}
	}

	private static void indexStatement(Statement statement,
			HashSet<String> indexedUnits)
	{
		// get useful topic units and their corresponding URLs
		Map<String, List<UrlWithDescription>> urlWithDescriptionsByTopicUnit = STATEMENT_ANALYZER
				.getUrlsByTopicUnit(statement);
		// get alternative units and their corresponding URLs
		Map<String, List<UrlWithDescription>> urlsByAlternativeUnit = getUrlsByAlternativeUnit(statement);

		// index metadata for the statement
		INDEX_WRITER.indexStatementMetadata(statement,
				urlWithDescriptionsByTopicUnit, urlsByAlternativeUnit);
		////////////////////////////////////////////////////////////////////////

		Map<String, List<UrlWithDescription>> allUnits = new HashMap<String, List<UrlWithDescription>>(
				urlWithDescriptionsByTopicUnit);
		if (urlsByAlternativeUnit != null)
		{
			allUnits.putAll(urlsByAlternativeUnit);
		}

		for (Entry<String, List<UrlWithDescription>> urlByUnit : allUnits
				.entrySet())
		{
			String matchedUnit = urlByUnit.getKey();
			if (indexedUnits.contains(matchedUnit))
			{
				System.out.println(" ### Topic/Alternative Unit[" + matchedUnit
						+ "] has already been indexed. ");
				continue;
			}
			else
			{
				indexedUnits.add(matchedUnit);
			}

			List<List<Segment>> segmentsOfMatchedPages = new ArrayList<List<Segment>>(
					urlByUnit.getValue().size());
			// disambiguations may cause multiple matched URLs
			for (UrlWithDescription urlWithDescription : urlByUnit.getValue())
			{
				// extract content from each page
				List<Segment> segmentsInPage = WIKIPEDIA_CONTENT_EXTRACTOR
						.extractPageContentFromWikipedia(
								urlWithDescription,
								StatementType.match(matchedUnit) == StatementType.YEAR).getSegments(); // YEAR page is BulletinPage

				segmentsOfMatchedPages.add(segmentsInPage);
			}

			// add the page content into index
			INDEX_WRITER.indexPageContentOfMatchedUnit(matchedUnit,
					segmentsOfMatchedPages);
		}
	}
}
