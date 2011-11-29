package edu.uic.cs.t_verifier.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.uic.cs.t_verifier.input.data.Statement;

public class StatementCache
{
	private static final List<Statement> STATEMENTS = new ArrayList<Statement>();

	public static void reflash()
	{
		STATEMENTS.clear();
	}

	public static void addAll(Collection<Statement> statementsToAdd)
	{
		STATEMENTS.addAll(statementsToAdd);
	}

	public static List<Statement> retrieveAllCachedStatements()
	{
		return Collections.unmodifiableList(STATEMENTS);
	}
}
