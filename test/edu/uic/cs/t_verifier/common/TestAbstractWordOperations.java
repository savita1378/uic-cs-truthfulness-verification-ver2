package edu.uic.cs.t_verifier.common;

import java.util.List;

import edu.uic.cs.t_verifier.EnhancedTestCase;

public class TestAbstractWordOperations extends EnhancedTestCase
{
	private AbstractWordOperations abstractWordOperations = new AbstractWordOperations()
	{
	};

	public void testStandardAnalyzeUsingDefaultStopWords()
	{
		String rawString = "Leverages Lucene's 3.1 and it's inherent optimizations and bug fixes as well as new analysis capabilities.";
		List<String> actual = abstractWordOperations
				.standardAnalyzeUsingDefaultStopWords(rawString);
		System.out.println(actual);

		List<String> expected = getExpectedAsList("TestAbstractWordOperations_testStandardAnalyzeUsingDefaultStopWords.expected");

		assertEquals(expected, actual);
	}

	public void testStandardAnalyzeWithoutRemovingStopWords()
	{
		String rawString = "Leverages Lucene's 3.1 and it's inherent optimizations and bug fixes as well as new analysis capabilities.";
		List<String> actual = abstractWordOperations
				.standardAnalyzeWithoutRemovingStopWords(rawString);
		System.out.println(actual);

		List<String> expected = getExpectedAsList("TestAbstractWordOperations_testStandardAnalyzeWithoutRemovingStopWords.expected");

		assertEquals(expected, actual);
	}

	public void testTrimStopWordsInBothSides_1()
	{
		String orginal = "is the leading actor in";
		String expected = "leading actor";
		String actual = abstractWordOperations
				.trimStopWordsInBothSides(orginal);

		assertEquals(expected, actual);
	}

	public void testTrimStopWordsInBothSides_2()
	{
		String orginal = "this is the only way";
		String expected = "only way";
		String actual = abstractWordOperations
				.trimStopWordsInBothSides(orginal);

		assertEquals(expected, actual);
	}

	public void testTrimStopWordsInBothSides_3()
	{
		String orginal = " He is the leading actor in";
		String expected = "he is the leading actor";
		String actual = abstractWordOperations
				.trimStopWordsInBothSides(orginal);

		assertEquals(expected, actual);
	}

	public void testTrimStopWordsInBothSides_4()
	{
		String orginal = "he is not the   leader";
		String expected = "he is not the leader";
		String actual = abstractWordOperations
				.trimStopWordsInBothSides(orginal);

		assertEquals(expected, actual);
	}

	public void testTrimStopWordsInBothSides_5()
	{
		String orginal = "father  he is not";
		String expected = "father he";
		String actual = abstractWordOperations
				.trimStopWordsInBothSides(orginal);

		assertEquals(expected, actual);
	}

	public void testTrimStopWordsInBothSides_6()
	{
		String orginal = "  ";
		String expected = "";
		String actual = abstractWordOperations
				.trimStopWordsInBothSides(orginal);

		assertEquals(expected, actual);
	}

	public void testPorterStemmingAnalyzeUsingDefaultStopWords()
	{
		String rawString = "Leverages Lucene's 3.1 and it's inherent optimizations and bug fixes as well as new analysis capabilities, 233,123,001.01.";
		List<String> actual = abstractWordOperations
				.porterStemmingAnalyzeUsingDefaultStopWords(rawString);
		System.out.println(actual);
		List<String> expected = getExpectedAsList("TestAbstractWordOperations_testPorterStemmingAnalyzeUsingDefaultStopWords.expected");

		assertEquals(expected, actual);
	}

}
