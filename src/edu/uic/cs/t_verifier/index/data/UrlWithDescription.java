package edu.uic.cs.t_verifier.index.data;

import java.util.List;

import edu.uic.cs.t_verifier.misc.Assert;

public class UrlWithDescription
{
	private String url;
	private String description;

	private List<String> categoriesBelongsTo;

	public UrlWithDescription(String url, String description,
			List<String> categoriesBelongsTo)
	{
		url = url.replace("&amp;", "%26").replace("+", "%2B");
		this.url = url;
		this.description = description;
		Assert.notNull(categoriesBelongsTo);
		this.categoriesBelongsTo = categoriesBelongsTo;
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

	public List<String> getCategoriesBelongsTo()
	{
		return categoriesBelongsTo;
	}
}
