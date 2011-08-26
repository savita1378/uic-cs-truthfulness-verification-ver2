package edu.uic.cs.t_verifier.misc;

public abstract class ClassFactory
{
	public static <T> T getInstance(String className)
	{
		try
		{
			@SuppressWarnings("unchecked")
			T instance = (T) Class.forName(className).newInstance();
			return instance;
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}
	}
}
