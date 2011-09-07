package edu.uic.cs.t_verifier.index;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.uic.cs.t_verifier.EnhancedTestCase;
import edu.uic.cs.t_verifier.common.StatementType;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;

public class TestStatementAnalyzer2 extends EnhancedTestCase
{
	private static final List<Statement> STATEMENTS = AlternativeUnitsReader
			.parseAllStatementsFromInputFiles();

	private StatementAnalyzer analyzer = new StatementAnalyzer();

	public void testGetUrlsByAlternativeUnit()
	{
		for (Statement statement : STATEMENTS)
		{
			// only test YEAR type
			if (statement.getType() != StatementType.YEAR)
			{
				continue;
			}

			Map<String, List<String>> urlsByAlternativeUnit = analyzer
					.getUrlsByAlternativeUnit(statement, false);

			assertEquals(statement.getAlternativeUnits().size(),
					urlsByAlternativeUnit.size());

			for (Entry<String, List<String>> entry : urlsByAlternativeUnit
					.entrySet())
			{
				System.out.print("\t" + entry.getKey() + " : ");
				System.out.println(entry.getValue());

				assertNotNull(entry.getValue());
				assertFalse(entry.getValue().isEmpty());
				assertTrue(entry.getValue().size() <= 2);
			}

			System.out
					.println("===========================================\n\n");

		}

	}
}
