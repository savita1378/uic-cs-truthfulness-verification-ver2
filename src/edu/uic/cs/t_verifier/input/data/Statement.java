package edu.uic.cs.t_verifier.input.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

	public Statement(Integer id, String topicUnitLeft, String topicUnitRight)
	{
		this.id = id;
		this.alternativeUnits = new ArrayList<String>(
				DEFAULT_ALTERNATIVE_UNIT_NUMBER);

		this.topicUnitLeft = topicUnitLeft != null ? topicUnitLeft : "";
		this.topicUnitRight = topicUnitRight != null ? topicUnitRight : "";
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

	public List<AlternativeUnit> getAlternativeUnits()
	{
		List<AlternativeUnit> result = new ArrayList<AlternativeUnit>(
				alternativeUnits.size());
		for (String auString : alternativeUnits)
		{
			result.add(new AlternativeUnit(auString));
		}

		assignWeightToAUs(result);

		return result;
	}

	private void assignWeightToAUs(List<AlternativeUnit> aus)
	{
		Collections.sort(aus, new Comparator<AlternativeUnit>()
		{
			@Override
			public int compare(AlternativeUnit au1, AlternativeUnit au2)
			{
				return au1.getWords().length - au2.getWords().length;
			}
		});

		////////////////////////////////////////////////////////////////////////
		int initialLength = aus.get(0).getWords().length;
		for (int index = 0; index < aus.size(); index++)
		{
			AlternativeUnit au_current = aus.get(index);
			if (au_current.getWords().length == initialLength)
			{
				au_current.setWeight(1);
				continue;
			}

			AlternativeUnit au_previous = null;
			for (int j = 0; j < index; j++)
			{
				au_previous = aus.get(j);
				if (au_current.getWords().length == au_previous.getWords().length)
				{
					continue;
				}

				if (Arrays.asList(au_current.getWords()).containsAll(
						Arrays.asList(au_previous.getWords())))
				{
					if (au_current.getWeight() <= au_previous.getWeight())
					{
						au_current.setWeight(au_previous.getWeight() + 1);
					}
				}
				else if (au_current.getWeight() < au_previous.getWeight())
				{
					au_current.setWeight(au_previous.getWeight());
				}
			}
		}
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
