package edu.uic.cs.t_verifier.html.impl;

import edu.uic.cs.t_verifier.EnhancedTestCase;

public class TestWikitextExtractor extends EnhancedTestCase
{
	private WikitextExtractor wikitextExtractor = new WikitextExtractor();

	public void testExtractWikitext()
	{
		String actual = wikitextExtractor
				.extractWikitext("http://en.wikipedia.org/w/index.php?title=Sorting_algorithm");
		System.out.println(actual);

		String expected = getExpectedAsString("TestWikitextExtractor_testExtractWikitext.expected");

		// TODO The StringUtils use '\r\n', but the string extract from web don't contain '\r'
		// Also, JWP can't parse string contains '/r'
		expected = expected.replace("\r\n", "\n");

		assertEquals(expected, actual);
	}

}
