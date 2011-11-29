package edu.uic.cs.t_verifier.score.data;

import org.apache.commons.lang.StringUtils;

public class AlternativeUnit
{
	private String auString = null;
	private String[] words = null;
	private int weight = 0;

	public AlternativeUnit(String auString)
	{
		this.auString = auString;
		this.words = StringUtils.split(auString);
	}

	@Override
	public String toString()
	{
		return auString + "[" + weight + "]";
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

}
