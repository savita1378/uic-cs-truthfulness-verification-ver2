package edu.uic.cs.t_verifier.misc;

import org.apache.log4j.Logger;

public class LogHelper
{
	public static final String LOG_LAYER_THREE = "********* ";
	public static final String LOG_LAYER_THREE_BEGIN = "*********BEGIN*** ";
	public static final String LOG_LAYER_THREE_END = "*********END***** ";

	public static final String LOG_LAYER_TWO = "******";
	public static final String LOG_LAYER_TWO_BEGIN = "******BEGIN*** ";
	public static final String LOG_LAYER_TWO_END = "******END***** ";

	public static final String LOG_LAYER_ONE = "***";
	public static final String LOG_LAYER_ONE_BEGIN = "***BEGIN*** ";
	public static final String LOG_LAYER_ONE_END = "***END***** ";

	public static Logger getLogger(Class<?> clazz)
	{
		Assert.notNull(clazz);
		return Logger.getLogger(clazz);
	}

	public static Logger getScoreDetailLogger()
	{
		return Logger.getLogger("SCORE_DETAIL");
	}

	public static Logger getMatchingDetailLogger()
	{
		return Logger.getLogger("MATCHING_DETAIL");
	}
}
