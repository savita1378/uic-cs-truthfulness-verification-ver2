package edu.uic.cs.t_verifier.common;

import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

public enum StatementType
{
	NORMAL_STRING, SUPERLATIVE_STRING, NUMBER, YEAR;

	// 1989
	private static final Pattern PATTERN_SINGLE_YEAR = Pattern
			.compile("^(1|2)\\d{3}$");

	// 1980s
	private static final Pattern PATTERN_DECADE = Pattern
			.compile("^(1|2)\\d{2}0s$");

	// 1900s
	private static final Pattern PATTERN_DECADE_OR_CENTURY = Pattern
			.compile("^(1|2)\\d{1}00s$");

	// normal number
	private static final Pattern PATTERN_NUMBER = Pattern.compile("^\\d+$");

	private static final int YEAR_STRING_LENGTH = 4; // 1xxx, 2xxx

	private static final int CURRENT_YEAR = Calendar.getInstance().get(
			Calendar.YEAR);

	public static StatementType match(String unit)
	{
		unit = unit.toLowerCase(Locale.US);

		if (PATTERN_SINGLE_YEAR.matcher(unit).matches()
				|| PATTERN_DECADE.matcher(unit).matches()
				|| PATTERN_DECADE_OR_CENTURY.matcher(unit).matches())
		{
			// year not greater than current!
			String yearString = unit.substring(0, YEAR_STRING_LENGTH);
			if (Integer.parseInt(yearString) > CURRENT_YEAR)
			{
				return NUMBER;
			}
			else
			{
				return YEAR;
			}
		}
		else if (PATTERN_NUMBER.matcher(unit).matches())
		{
			return NUMBER;
		}
		// FIXME right now we consider "least" into.
		// And don't consider irregular words like "worst" 
		else if (unit.equals("most") || unit.equals("least")
				|| unit.endsWith("est"))
		{
			return SUPERLATIVE_STRING;
		}
		else
		{
			return NORMAL_STRING;
		}
	}
}
