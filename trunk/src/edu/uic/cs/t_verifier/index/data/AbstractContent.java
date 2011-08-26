package edu.uic.cs.t_verifier.index.data;

abstract class AbstractContent
{
	private String text = null;

	public void setText(String text)
	{
		this.text = text;
	}

	public String getText()
	{
		return text;
	}

	@Override
	public String toString()
	{
		return getText();
	}
}
