package edu.uic.cs.t_verifier.nlp;

import edu.uic.cs.t_verifier.input.data.Statement;

public interface StatementTypeIdentifier
{
	enum StatementType
	{
		/*TIME,*/ LOCATION, /*ORGANIZATION,*/ PERSON, /*MONEY, PERCENT,*/ DATE,
		///////////////////////////////////////////////////////////
		NUMBER, OTHER;

		public static StatementType parse(String str)
		{
			StatementType type = OTHER;

			try
			{
				type = StatementType.valueOf(str);
			}
			catch (IllegalArgumentException exception)
			{
				// ignore
			}

			return type;
		}
	}

	StatementType identifyType(Statement statement);
}
