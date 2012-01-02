package edu.uic.cs.t_verifier.score.data;

import org.apache.commons.lang.StringUtils;

public class AlternativeUnit
{
	private String auString = null;
	private String[] words = null;
	private int weight = 0;

	private int statementIdBelongsTo;

	public AlternativeUnit(String auString, int statementIdBelongsTo)
	{
		this.auString = auString;
		this.words = StringUtils.split(auString);
		this.statementIdBelongsTo = statementIdBelongsTo;
	}

	@Override
	public String toString()
	{
		return statementIdBelongsTo + " : " + auString /*+ "[" + weight + "]"*/;
	}

	String[] getWords()
	{
		return words;
	}

	public void setWeight(int weight)
	{
		this.weight = weight;
	}

	public int getWeight()
	{
		return weight;
	}

	public String getString()
	{
		return auString;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((auString == null) ? 0 : auString.hashCode());
		result = prime * result + statementIdBelongsTo;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AlternativeUnit other = (AlternativeUnit) obj;
		if (auString == null)
		{
			if (other.auString != null)
				return false;
		}
		else if (!auString.equals(other.auString))
			return false;
		if (statementIdBelongsTo != other.statementIdBelongsTo)
			return false;
		return true;
	}

}
