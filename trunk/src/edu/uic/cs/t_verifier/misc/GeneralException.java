package edu.uic.cs.t_verifier.misc;

public class GeneralException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public GeneralException()
	{
		super();
	}

	public GeneralException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public GeneralException(String message)
	{
		super(message);
	}

	public GeneralException(Throwable cause)
	{
		super(cause);
	}

}
