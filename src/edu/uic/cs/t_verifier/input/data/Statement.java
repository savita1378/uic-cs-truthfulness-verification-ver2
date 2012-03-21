package edu.uic.cs.t_verifier.input.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import edu.uic.cs.t_verifier.common.AbstractWordOperations;
import edu.uic.cs.t_verifier.common.StatementType;
import edu.uic.cs.t_verifier.misc.Assert;

public class Statement extends AbstractWordOperations
{
	private static final int DEFAULT_ALTERNATIVE_UNIT_NUMBER = 5;

	private Integer id = null;

	//	private String wholeStatement = null;
	private List<String> alternativeUnits = null;
	private String topicUnitLeft = "";
	private String topicUnitRight = "";

	private List<String> allAlternativeStatements = null;
	private TreeSet<String> allWordsInTopicUnits = null;
	private TreeSet<String> allNonstopWordsInTopicUnits = null;

	public Statement(Integer id, String topicUnitLeft, String topicUnitRight)
	{
		this.id = id;
		this.alternativeUnits = new ArrayList<String>(
				DEFAULT_ALTERNATIVE_UNIT_NUMBER);

		this.topicUnitLeft = topicUnitLeft != null ? topicUnitLeft : "";
		this.topicUnitRight = topicUnitRight != null ? topicUnitRight : "";
	}

	public String getTopicUnitLeft()
	{
		return topicUnitLeft;
	}

	public String getTopicUnitRight()
	{
		return topicUnitRight;
	}

	public void addAlternativeUnit(String alternativeUnit)
	{
		alternativeUnits.add(alternativeUnit.toLowerCase(Locale.US));
	}

	public List<String> getAllAlternativeStatements()
	{
		if (allAlternativeStatements != null)
		{
			return allAlternativeStatements;
		}

		allAlternativeStatements = new ArrayList<String>(
				alternativeUnits.size());
		for (String au : alternativeUnits)
		{
			String alternativeStatement = topicUnitLeft + " " + au + " "
					+ topicUnitRight;
			allAlternativeStatements.add(alternativeStatement.trim());
		}

		return allAlternativeStatements;
	}

	/**
	 * Since the DU may appear in the middle of a sentence, like: 
	 * 'the life expectancy of an elephant is 60 years [60]'
	 * There may be at most two TUs,
	 * 'the life expectancy of an elephant is' and 'years'
	 * 
	 * @return all TUs of the statement
	 */
	public List<String> getTopicUnits()
	{
		ArrayList<String> result = new ArrayList<String>(2);
		if (topicUnitLeft != null && topicUnitLeft.trim().length() != 0)
		{
			result.add(topicUnitLeft.trim());
		}

		if (topicUnitRight != null && topicUnitRight.trim().length() != 0)
		{
			result.add(topicUnitRight.trim());
		}

		Assert.isTrue(result.size() > 0);

		return result/*.toArray(new String[result.size()])*/;
	}

	public TreeSet<String> getAllWordsInTopicUnits()
	{
		if (allWordsInTopicUnits != null)
		{
			return allWordsInTopicUnits;
		}

		allWordsInTopicUnits = new TreeSet<String>();
		for (String topicUnit : getTopicUnits())
		{
			List<String> words = standardAnalyzeWithoutRemovingStopWords(topicUnit); // DO NOT remove stop words
			allWordsInTopicUnits.addAll(words);
		}

		return allWordsInTopicUnits;
	}

	public TreeSet<String> getAllNonstopWordsInTopicUnits()
	{
		if (allNonstopWordsInTopicUnits != null)
		{
			return allNonstopWordsInTopicUnits;
		}

		allNonstopWordsInTopicUnits = new TreeSet<String>();
		for (String topicUnit : getTopicUnits())
		{
			List<String> words = standardAnalyzeUsingDefaultStopWords(topicUnit); // DO remove stop words
			allNonstopWordsInTopicUnits.addAll(words);
		}

		return allNonstopWordsInTopicUnits;
	}

	public List<String> getAlternativeUnits()
	{
		return alternativeUnits;
	}

	public Integer getId()
	{
		return id;
	}

	public StatementType getType()
	{
		StatementType statementType = StatementType.YEAR;

		for (String au : alternativeUnits)
		{
			StatementType auType = StatementType.match(au);
			if (auType == StatementType.NORMAL_STRING)
			{
				for (String tuWord : getAllWordsInTopicUnits())
				{
					if (StatementType.match(tuWord) == StatementType.SUPERLATIVE_STRING)
					{
						return StatementType.SUPERLATIVE_STRING;
					}
				}

				// if there is one string, return!
				return auType;
			}

			// if there is a number, then unless there are normal Strings
			// this statement is NUMBER type
			if (auType == StatementType.NUMBER)
			{
				statementType = StatementType.NUMBER;
			}
		}

		return statementType;
	}
}
