package edu.uic.cs.t_verifier.index.data;

public class UrlWithDescription
{
	private String url;
	private String description;

	public UrlWithDescription(String url, String description)
	{
		this.url = url;
		this.description = description;
	}

	public String getUrl()
	{
		return url;
	}

	public String getDescription()
	{
		return description;
	}

	public boolean isDisambiguationLink()
	{
		return description != null && description.trim().length() != 0;
	}

	@Override
	public String toString()
	{
		return url + (isDisambiguationLink() ? (" | " + description) : "");
	}
}
