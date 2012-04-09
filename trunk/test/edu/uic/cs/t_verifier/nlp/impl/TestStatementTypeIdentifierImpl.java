package edu.uic.cs.t_verifier.nlp.impl;

import java.util.List;

import edu.uic.cs.t_verifier.EnhancedTestCase;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;

public class TestStatementTypeIdentifierImpl extends EnhancedTestCase
{
	// private StatementTypeIdentifierImpl identifier = new StatementTypeIdentifierImpl();
	NLPAnalyzerImpl2 analyzer = new NLPAnalyzerImpl2();

	public void testRestoreWordCasesForSentence()
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();
		for (Statement statement : statements)
		{
			List<String> allAlternativeUnits = statement.getAlternativeUnits();
			List<String> allAlternativeStatements = statement
					.getAllAlternativeStatements();
			for (int index = 0; index < allAlternativeStatements.size(); index++)
			{
				String sentence = allAlternativeStatements.get(index);
				String alternativeUnit = allAlternativeUnits.get(index);

				String actual = analyzer.restoreWordCasesForSentence(sentence,
						alternativeUnit);
				System.out.println(actual);
				assertEquals(sentence, actual.toLowerCase());
			}

			System.out.println();
		}
	}

}
