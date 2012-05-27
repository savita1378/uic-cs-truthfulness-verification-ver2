package edu.uic.cs.t_verifier.html.data;

import java.io.Serializable;
import java.util.List;

public class MatchedQueryKey implements Serializable
{
	private static final long serialVersionUID = 1L;

	//	public static final String WIKI_ADDRESS_PREFIX = "http://en.wikipedia.org/wiki/";
	private static final String WIKI_ADDRESS_PREFIX = "http://en.wikipedia.org/w/index.php?title=";
	//	private static final String WIKI_ADDRESS_EDIT_POSTFIX = "&action=edit";

	private String keyWord = null;
	private List<DisambiguationEntry> disambiguationEntries = null;
	private List<String> categories = null;

	public static class DisambiguationEntry implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private String keyWord = null;
		private String description = null;

		public DisambiguationEntry(String keyWord, String description)
		{
			this.keyWord = keyWord;
			this.description = description;
		}

		public String getKeyWord()
		{
			return keyWord;
		}

		public String getDescription()
		{
			return description;
		}

		@Override
		public String toString()
		{
			return "?" + keyWord/* + "=[" + description + "]"*/;
		}

	}

	public MatchedQueryKey(String keyWord,
			List<DisambiguationEntry> disambiguationEntries,
			List<String> categories)
	{
		this.keyWord = keyWord;
		this.disambiguationEntries = disambiguationEntries;
		this.categories = categories;
	}

	public String getKeyWord()
	{
		return keyWord;
	}

	public List<DisambiguationEntry> getDisambiguationEntries()
	{
		return disambiguationEntries;
	}

	public boolean isCertainly()
	{
		return (disambiguationEntries == null || disambiguationEntries
				.isEmpty());
	}

	public String getCertainPageUrl()
	{
		if (isCertainly())
		{
			return constructPageAddress(keyWord);
		}

		return null;
	}

	@Override
	public String toString()
	{
		return getCertainPageUrl();
	}

	public static String constructPageAddress(String keyWord)
	{
		return WIKI_ADDRESS_PREFIX + keyWord /* + WIKI_ADDRESS_EDIT_POSTFIX*/;
	}

	public List<String> getCategories()
	{
		return categories;
	}
}
