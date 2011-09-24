package edu.uic.cs.t_verifier.score.data;

import java.util.List;

import edu.uic.cs.t_verifier.common.AbstractWordOperations;

public enum Category
{
	PEOPLE, OTHER;

	private static final AbstractWordOperations WORD_OPERATIONS = new AbstractWordOperations()
	{
	};

	public static Category matchCategory(String[] categoryStrings)
	{
		for (String categoryString : categoryStrings)
		{
			List<String> words = WORD_OPERATIONS
					.standardAnalyzeUsingDefaultStopWords(categoryString);
			for (String word : words)
			{
				if (word.equals("people"))
				{
					return PEOPLE;
				}
			}
		}

		return OTHER;
	}
}
