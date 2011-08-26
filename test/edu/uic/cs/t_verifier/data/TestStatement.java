package edu.uic.cs.t_verifier.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.uic.cs.t_verifier.EnhancedTestCase;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.AlternativeUnit;
import edu.uic.cs.t_verifier.input.data.Statement;

public class TestStatement extends EnhancedTestCase
{

	public void testGetAllWordsInTopicUnits_1()
	{
		Statement statement = new Statement(null,
				"the life expectancy of an elephant is", "the years");
		Set<String> actual = statement.getAllWordsInTopicUnits();
		TreeSet<String> expected = new TreeSet<String>(
				Arrays.asList(new String[] { "an", "elephant", "expectancy",
						"is", "life", "of", "the", "years" }));

		assertEquals(expected, actual);
	}

	public void testGetAllWordsInTopicUnits_2()
	{
		Statement statement = new Statement(null,
				"the life expectancy of an elephant is", "it's five years");
		Set<String> actual = statement.getAllWordsInTopicUnits();
		TreeSet<String> expected = new TreeSet<String>(
				Arrays.asList(new String[] { "an", "elephant", "expectancy",
						"five", "it", "is", "life", "of", /*"s",*/"the",
						"years" }));

		assertEquals(expected, actual);
	}

	public void testAssignWeightToAUs_1()
	{
		Statement statement = new Statement(null, null, null);
		statement.addAlternativeUnit("a b c d");
		statement.addAlternativeUnit("a b");
		statement.addAlternativeUnit("c d");
		statement.addAlternativeUnit("b c");
		statement.addAlternativeUnit("a");
		statement.addAlternativeUnit("b");
		statement.addAlternativeUnit("c");

		TreeSet<String> actual = new TreeSet<String>();
		for (AlternativeUnit au : statement.getAlternativeUnits())
		{
			System.out.println(au.toString());
			actual.add(au.toString());
		}

		TreeSet<String> expected = new TreeSet<String>(
				Arrays.asList(new String[] { "a[1]", "b[1]", "c[1]", "a b[2]",
						"c d[2]", "b c[2]", "a b c d[3]" }));

		assertEquals(expected, actual);
	}

	public void testAssignWeightToAUs_2()
	{
		Statement statement = new Statement(null, null, null);
		statement.addAlternativeUnit("a b c d");
		statement.addAlternativeUnit("a b c");
		statement.addAlternativeUnit("b c d");
		statement.addAlternativeUnit("a b");
		statement.addAlternativeUnit("c d");
		statement.addAlternativeUnit("a");
		statement.addAlternativeUnit("b");
		statement.addAlternativeUnit("c");

		TreeSet<String> actual = new TreeSet<String>();
		for (AlternativeUnit au : statement.getAlternativeUnits())
		{
			System.out.println(au.toString());
			actual.add(au.toString());
		}

		TreeSet<String> expected = new TreeSet<String>(
				Arrays.asList(new String[] { "a[1]", "b[1]", "c[1]", "a b[2]",
						"c d[2]", "a b c[3]", "b c d[3]", "a b c d[4]" }));

		assertEquals(expected, actual);
	}

	public void testAssignWeightToAUs_3()
	{
		Statement statement = new Statement(null, null, null);
		statement.addAlternativeUnit("a b c d");
		statement.addAlternativeUnit("a b c");
		statement.addAlternativeUnit("b c d");
		statement.addAlternativeUnit("a b");
		statement.addAlternativeUnit("b a");
		statement.addAlternativeUnit("a");
		statement.addAlternativeUnit("b");
		statement.addAlternativeUnit("c");

		TreeSet<String> actual = new TreeSet<String>();
		for (AlternativeUnit au : statement.getAlternativeUnits())
		{
			System.out.println(au.toString());
			actual.add(au.toString());
		}

		TreeSet<String> expected = new TreeSet<String>(
				Arrays.asList(new String[] { "a[1]", "b[1]", "c[1]", "a b[2]",
						"b a[2]", "a b c[3]", "b c d[2]", "a b c d[4]" }));

		assertEquals(expected, actual);
	}

	public void testGetType()
	{
		List<Statement> actual = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();

		List<String> actualAll = new ArrayList<String>();
		for (Statement statement : actual)
		{
			String ss = statement.getId() + ":" + statement.getType();
			System.out.println(ss);
			actualAll.add(ss);
		}

		List<String> expected = getExpectedAsList("TestStatement_testGetType.expected");
		assertEquals(expected, actualAll);
	}

}
