package edu.uic.cs.t_verifier.score.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.uic.cs.t_verifier.common.StatementType;

public class StatementMetadata
{
	private int statementId = 0;
	private String[] stemmedNonstopTUWords = null;
	private String[] alternativeUnits = null;
	private String[] matchedSubTopicUnits = null;
	private StatementType statementType = null;

	public StatementMetadata(int statementId, String[] stemmedNonstopTUWords,
			String[] alternativeUnits, String[] matchedSubTopicUnits,
			StatementType statementType)
	{
		this.statementId = statementId;
		this.stemmedNonstopTUWords = stemmedNonstopTUWords;
		this.alternativeUnits = alternativeUnits;
		this.matchedSubTopicUnits = matchedSubTopicUnits;
		this.statementType = statementType;
	}

	public int getStatementId()
	{
		return statementId;
	}

	public String[] getStemmedNonstopTUWords()
	{
		return stemmedNonstopTUWords;
	}

	public List<AlternativeUnit> getAlternativeUnits()
	{
		List<AlternativeUnit> result = new ArrayList<AlternativeUnit>(
				alternativeUnits.length);
		for (String auString : alternativeUnits)
		{
			result.add(new AlternativeUnit(auString, statementId));
		}

		if (statementType == StatementType.YEAR)
		{
			assignWeightToYearTypeAUs(result);
		}
		else
		{
			assignWeightToNonYearTypeAUs(result);
		}

		return result;
	}

	private void assignWeightToYearTypeAUs(List<AlternativeUnit> aus)
	{
		List<Integer> indexesOfCentury = new ArrayList<Integer>(2);
		List<Integer> indexesOfDecade = new ArrayList<Integer>(2);
		List<Integer> indexesOfYear = new ArrayList<Integer>(5);

		for (int index = 0; index < aus.size(); index++)
		{
			AlternativeUnit au = aus.get(index);

			if (au.getString().endsWith("00s"))
			{
				indexesOfCentury.add(index);
			}
			else if (au.getString().endsWith("0s"))
			{
				indexesOfDecade.add(index);
			}
			else
			{
				indexesOfYear.add(index);
			}

			au.setWeight(1);
		}

		if (!indexesOfCentury.isEmpty() && !indexesOfDecade.isEmpty())
		{
			for (Integer index : indexesOfYear)
			{
				aus.get(index).setWeight(3);
			}

			for (Integer index : indexesOfDecade)
			{
				aus.get(index).setWeight(2);
			}
		}
		else if (!indexesOfCentury.isEmpty() || !indexesOfDecade.isEmpty())
		{
			for (Integer index : indexesOfYear)
			{
				aus.get(index).setWeight(2);
			}
		}

	}

	private void assignWeightToNonYearTypeAUs(List<AlternativeUnit> aus)
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

	public String[] getMatchedSubTopicUnits()
	{
		return matchedSubTopicUnits;
	}

	public boolean scoreByAlternativeUnitOnly()
	{
		return statementType == StatementType.YEAR;
	}

	public StatementType getStatementType()
	{
		return statementType;
	}

	/*public boolean isFrontPositionBetter()
	{
		return statementType == StatementType.SUPERLATIVE_STRING;
	}*/

}
