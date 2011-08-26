package edu.uic.cs.t_verifier.index;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.uic.cs.t_verifier.EnhancedTestCase;
import edu.uic.cs.t_verifier.index.StatementAnalyzer;
import edu.uic.cs.t_verifier.input.data.Statement;

public class TestStatementAnalyzer extends EnhancedTestCase
{
	private StatementAnalyzer analyzer = new StatementAnalyzer();

	public void testGetUrlsByTopicUnit_1()
	{
		String tu = "United Provinces of india and  new   granada";
		Statement statement = new Statement(null, tu, null);
		statement.addAlternativeUnit("");

		Map<String, List<String>> actual = analyzer
				.getUrlsByTopicUnit(statement);
		System.out.println(actual);

		assertEquals(3, actual.size());

		assertEquals(
				Collections
						.singletonList("http://en.wikipedia.org/w/index.php?title=Presidencies_and_provinces_of_British_India"),
				actual.get("provinces of india"));

		assertEquals(
				Collections
						.singletonList("http://en.wikipedia.org/w/index.php?title=New_Granada"),
				actual.get("new granada"));

		assertEquals(
				Collections
						.singletonList("http://en.wikipedia.org/w/index.php?title=United_Provinces_of_New_Granada"),
				actual.get("united provinces"));
	}

	public void testGetUrlsByTopicUnit_2()
	{
		String tu = "United Provinces of";
		Statement statement = new Statement(null, tu, null);
		statement.addAlternativeUnit("india and  granada");

		Map<String, List<String>> actual = analyzer
				.getUrlsByTopicUnit(statement);
		System.out.println(actual);

		assertEquals(1, actual.size());

		assertEquals(
				Arrays.asList(new String[] {
						"http://en.wikipedia.org/w/index.php?title=United_Provinces_of_Agra_and_Oudh",
						"http://en.wikipedia.org/w/index.php?title=United_Provinces_of_British_India",
						"http://en.wikipedia.org/w/index.php?title=United_Provinces_(1937-1950)",
						"http://en.wikipedia.org/w/index.php?title=United_Provinces_of_New_Granada" }),
				actual.get("united provinces"));
	}

	public void testGetUrlsByTopicUnit_3()
	{
		String tu = "United Provinces of";
		Statement statement = new Statement(null, tu, null);
		statement.addAlternativeUnit(" india  and  new     granada");

		Map<String, List<String>> actual = analyzer
				.getUrlsByTopicUnit(statement);
		System.out.println(actual);

		assertEquals(1, actual.size());

		assertEquals(
				Arrays.asList(new String[] { "http://en.wikipedia.org/w/index.php?title=United_Provinces_of_New_Granada" }),
				actual.get("united provinces"));
	}

}
