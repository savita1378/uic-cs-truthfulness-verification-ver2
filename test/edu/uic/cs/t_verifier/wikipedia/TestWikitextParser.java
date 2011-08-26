package edu.uic.cs.t_verifier.wikipedia;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import edu.uic.cs.t_verifier.EnhancedTestCase;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.wikipedia.WikitextParser;

public class TestWikitextParser extends EnhancedTestCase
{
	public void testParse()
	{
		String wikitext = getExpectedAsString("TestWikitextParser_testParse.fixture");
		wikitext = StringEscapeUtils.unescapeHtml(wikitext);
		List<Segment> actual = WikitextParser.parse(wikitext);

		for (Segment segment : actual)
		{
			System.out.println(segment);
			System.out
					.println("================================================================");
		}
	}

}
