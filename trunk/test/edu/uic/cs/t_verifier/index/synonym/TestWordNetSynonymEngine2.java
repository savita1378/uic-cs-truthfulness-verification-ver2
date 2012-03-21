package edu.uic.cs.t_verifier.index.synonym;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import edu.uic.cs.t_verifier.EnhancedTestCase;

public class TestWordNetSynonymEngine2 extends EnhancedTestCase
{

	public void testGetSynonyms()
	{
		WordNetSynonymEngine2 synonymEngine = new WordNetSynonymEngine2();
		Set<String> actual = new TreeSet<String>(Arrays.asList(synonymEngine
				.getSynonyms("goodness")));

		Set<String> expected = new TreeSet<String>(
				getExpectedAsList("TestWordNetSynonymEngine2_testGetSynonyms.expected"));

		assertEquals(expected, actual);
	}

	public void testRetrieveSynonyms()
	{
		WordNetSynonymEngine2 synonymEngine = new WordNetSynonymEngine2();
		Set<String> actual = new TreeSet<String>();
		synonymEngine.retrieveSynonyms("good", actual);

		Set<String> expected = new TreeSet<String>(
				getExpectedAsList("TestWordNetSynonymEngine2_testRetrieveSynonyms.expected"));

		assertEquals(expected, actual);
	}
}
