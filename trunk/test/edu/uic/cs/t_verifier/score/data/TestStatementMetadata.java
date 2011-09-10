package edu.uic.cs.t_verifier.score.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import edu.uic.cs.t_verifier.EnhancedTestCase;
import edu.uic.cs.t_verifier.common.StatementType;

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

	public void testAssignWeightToAUs_4()
	{
		List<String> alternativeUnits = new ArrayList<String>();
		alternativeUnits.add("1964");
		alternativeUnits.add("1550");
		alternativeUnits.add("1980");
		alternativeUnits.add("1996");
		alternativeUnits.add("1960");

		StatementMetadata statement = new StatementMetadata(0, null,
				alternativeUnits.toArray(new String[alternativeUnits.size()]),
				null, StatementType.YEAR);

		TreeSet<String> actual = new TreeSet<String>();
		for (AlternativeUnit au : statement.getAlternativeUnits())
		{
			System.out.println(au.toString());
			actual.add(au.toString());
		}

		TreeSet<String> expected = new TreeSet<String>(
				Arrays.asList(new String[] { "1964[1]", "1550[1]", "1980[1]",
						"1996[1]", "1960[1]" }));

		assertEquals(expected, actual);
	}

	public void testAssignWeightToAUs_5()
	{
		List<String> alternativeUnits = new ArrayList<String>();
		alternativeUnits.add("1964");
		alternativeUnits.add("1550");
		alternativeUnits.add("1980");
		alternativeUnits.add("1996");
		alternativeUnits.add("1960s");

		StatementMetadata statement = new StatementMetadata(0, null,
				alternativeUnits.toArray(new String[alternativeUnits.size()]),
				null, StatementType.YEAR);

		TreeSet<String> actual = new TreeSet<String>();
		for (AlternativeUnit au : statement.getAlternativeUnits())
		{
			System.out.println(au.toString());
			actual.add(au.toString());
		}

		TreeSet<String> expected = new TreeSet<String>(
				Arrays.asList(new String[] { "1964[2]", "1550[2]", "1980[2]",
						"1996[2]", "1960s[1]" }));

		assertEquals(expected, actual);
	}

	public void testAssignWeightToAUs_6()
	{
		List<String> alternativeUnits = new ArrayList<String>();
		alternativeUnits.add("1964");
		alternativeUnits.add("1550");
		alternativeUnits.add("1980");
		alternativeUnits.add("1996");
		alternativeUnits.add("1900s");

		StatementMetadata statement = new StatementMetadata(0, null,
				alternativeUnits.toArray(new String[alternativeUnits.size()]),
				null, StatementType.YEAR);

		TreeSet<String> actual = new TreeSet<String>();
		for (AlternativeUnit au : statement.getAlternativeUnits())
		{
			System.out.println(au.toString());
			actual.add(au.toString());
		}

		TreeSet<String> expected = new TreeSet<String>(
				Arrays.asList(new String[] { "1964[2]", "1550[2]", "1980[2]",
						"1996[2]", "1900s[1]" }));

		assertEquals(expected, actual);
	}

	public void testAssignWeightToAUs_7()
	{
		List<String> alternativeUnits = new ArrayList<String>();
		alternativeUnits.add("1964");
		alternativeUnits.add("1550");
		alternativeUnits.add("1990");
		alternativeUnits.add("1990s");
		alternativeUnits.add("1900s");

		StatementMetadata statement = new StatementMetadata(0, null,
				alternativeUnits.toArray(new String[alternativeUnits.size()]),
				null, StatementType.YEAR);

		TreeSet<String> actual = new TreeSet<String>();
		for (AlternativeUnit au : statement.getAlternativeUnits())
		{
			System.out.println(au.toString());
			actual.add(au.toString());
		}

		TreeSet<String> expected = new TreeSet<String>(
				Arrays.asList(new String[] { "1964[3]", "1550[3]", "1990[3]",
						"1990s[2]", "1900s[1]" }));

		assertEquals(expected, actual);
	}
}
