package edu.uic.cs.t_verifier.html;

import org.htmlparser.Parser;
import org.htmlparser.util.ParserException;

import edu.uic.cs.t_verifier.misc.Config;

public class PoliteParser extends Parser
{
	private static final long serialVersionUID = 1L;

	@Override
	public void setResource(String resource) throws ParserException
	{
		super.setResource(resource);

		try
		{
			Thread.sleep(Config.POLITENESS_INTERVAL);
		}
		catch (InterruptedException e)
		{
			// ignore
		}
	}

}
