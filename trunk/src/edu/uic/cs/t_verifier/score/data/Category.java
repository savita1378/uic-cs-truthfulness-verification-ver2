package edu.uic.cs.t_verifier.score.data;

import java.util.ArrayList;
import java.util.List;

import edu.uic.cs.t_verifier.common.AbstractWordOperations;

public enum Category
{
	PEOPLE, CITY, OTHER;

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
				else if (word.equals("cities") || word.equals("city"))
				{
					// TODO whether it is a city, actually we can also identify from the TU 'city'
					return CITY;
				}
			}
		}

		return OTHER;
	}

	public static List<String> toStringList()
	{
		List<String> result = new ArrayList<String>(values().length);
		for (Category category : values())
		{
			result.add(category.toString());
		}

		return result;
	}
}
