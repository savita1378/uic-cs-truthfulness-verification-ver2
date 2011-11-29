package edu.uic.cs.t_verifier.input;

import java.util.ArrayList;
import java.util.List;

import edu.uic.cs.t_verifier.EnhancedTestCase;
import edu.uic.cs.t_verifier.input.data.Statement;

public class TestAlternativeUnitsReader extends EnhancedTestCase
{
	public void testParseAllStatementsFromInputFiles()
	{
		List<Statement> actual = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();

		List<String> actualAll = new ArrayList<String>();
		for (Statement statement : actual)
		{
			System.out.println(statement.getId() + ":");
			for (String each : statement.getAllAlternativeStatements())
			{
				System.out.println(each);
			}

			actualAll.addAll(statement.getAllAlternativeStatements());
		}

		List<String> expected = getExpectedAsList("TestAlternativeUnitsReader.expected");
		assertEquals(expected, actualAll);

		actual = StatementCache.retrieveAllCachedStatements();
		assertEquals(expected, actualAll);

		actual = AlternativeUnitsReader.parseAllStatementsFromInputFiles();
		assertEquals(expected, actualAll);

		actual = AlternativeUnitsReader.parseAllStatementsFromInputFiles();
		assertEquals(expected, actualAll);

		actual = StatementCache.retrieveAllCachedStatements();
		assertEquals(expected, actualAll);
	}

}
