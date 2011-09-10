package edu.uic.cs.t_verifier.score.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import edu.uic.cs.t_verifier.EnhancedTestCase;

public class TestStatementMetadata extends EnhancedTestCase
{
	public void testAssignWeightToAUs_1()
	{
		List<String> alternativeUnits = new ArrayList<String>();
		alternativeUnits.add("a b c d");
		alternativeUnits.add("a b");
		alternativeUnits.add("c d");
		alternativeUnits.add("b c");
		alternativeUnits.add("a");
		alternativeUnits.add("b");
		alternativeUnits.add("c");

		StatementMetadata statement = new StatementMetadata(0, null,
				alternativeUnits.toArray(new String[alternativeUnits.size()]),
				null, null);

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
		List<String> alternativeUnits = new ArrayList<String>();
		alternativeUnits.add("a b c d");
		alternativeUnits.add("a b c");
		alternativeUnits.add("b c d");
		alternativeUnits.add("a b");
		alternativeUnits.add("c d");
		alternativeUnits.add("a");
		alternativeUnits.add("b");
		alternativeUnits.add("c");

		StatementMetadata statement = new StatementMetadata(0, null,
				alternativeUnits.toArray(new String[alternativeUnits.size()]),
				null, null);

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
		List<String> alternativeUnits = new ArrayList<String>();
		alternativeUnits.add("a b c d");
		alternativeUnits.add("a b c");
		alternativeUnits.add("b c d");
		alternativeUnits.add("a b");
		alternativeUnits.add("b a");
		alternativeUnits.add("a");
		alternativeUnits.add("b");
		alternativeUnits.add("c");

		StatementMetadata statement = new StatementMetadata(0, null,
				alternativeUnits.toArray(new String[alternativeUnits.size()]),
				null, null);

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
}
