package edu.uic.cs.t_verifier.score.data;

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

	public String[] getAlternativeUnits()
	{
		return alternativeUnits;
	}

	public String[] getMatchedSubTopicUnits()
	{
		return matchedSubTopicUnits;
	}

	public boolean isUseAuToScore()
	{
		return statementType == StatementType.YEAR;
	}

	public boolean isFrontPositionBetter()
	{
		return statementType == StatementType.SUPERLATIVE_STRING;
	}

}
