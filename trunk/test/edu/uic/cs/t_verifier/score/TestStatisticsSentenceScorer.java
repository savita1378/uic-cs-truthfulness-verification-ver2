package edu.uic.cs.t_verifier.score;

import java.util.List;

import junit.framework.AssertionFailedError;

import edu.uic.cs.t_verifier.EnhancedTestCase;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.score.data.StatementMetadata;

public class TestStatisticsSentenceScorer extends EnhancedTestCase
{
	private static final StatisticsSentenceScorer SCORER = new StatisticsSentenceScorer(
			Config.INDEX_FOLDER);

	private static final List<StatementMetadata> ALL_METADATA = SCORER
			.retrieveStatementsMetadata(false);

	private List<String> allExpected = getExpectedAsList("TestWindowScorer.expected");

	private void runTestInternal()
	{
		int index = getIndexFromTestCaseName();
		String expected = allExpected.get(index);

		List<String> actual = SCORER
				.findTheMostMatchedAlternativeUnits(ALL_METADATA.get(index));

		try
		{
			assertTrue("Expected: [" + expected + "], but actual: " + actual,
					actual.contains(expected));
		}
		catch (AssertionFailedError e)
		{
			//			System.out.print(" ×   | Expected: [" + expected + "]");
			System.out.println("========================");
			throw e;
		}

		if (actual.size() > 1)
		{
			//			System.out.print(" ≈√  | Expected: [" + expected + "]");
		}
		else
		{
			//			System.out.print(" √   | Expected: [" + expected + "]");
		}

		System.out.println("========================");
	}

	public void testFindTheMostMatchedAlternativeUnits_01()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_02()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_03()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_04()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_05()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_06()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_07()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_08()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_09()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_10()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_11()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_12()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_13()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_14()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_15()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_16()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_17()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_18()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_19()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_20()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_21()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_22()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_23()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_24()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_25()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_26()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_27()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_28()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_29()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_30()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_31()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_32()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_33()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_34()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_35()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_36()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_37()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_38()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_39()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_40()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_41()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_42()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_43()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_44()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_45()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_46()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_47()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_48()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_49()
	{
		runTestInternal();
	}

	public void testFindTheMostMatchedAlternativeUnits_50()
	{
		runTestInternal();
	}

}
