package edu.uic.cs.wiki_verifier.wiki.local.search;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import edu.uic.cs.t_verifier.misc.GeneralException;

public class WikipediaPage
{
	private String title;
	private String rawText;

	private String redirectToTitle;
	private boolean isDisambiguation = false;

	private List<String> categories;
	private String infoBoxRawText;

	private String htmlText;
	private String infoBoxHtmlText;

	//	public static void main(String[] args) throws UnsupportedEncodingException
	//	{
	//		String s = "UTC%2B08:00 %27 he llo %28sds %289834";
	//		//s = URLEncoder.encode(s, "UTF-8");
	//		System.out.println(URLDecoder.decode(s, "ISO-8859-1"));
	//	}

	//	private static final Map<String, String> PERCENT_ENCODING_DECODE_MAPPING = new HashMap<String, String>();
	//	static
	//	{
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%21", "!");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%23", "#");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%24", "$");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%26", "&");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%27", "'");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%28", "(");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%29", ")");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%2A", "*");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%2B", "+");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%2C", ",");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%2F", "/");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%3A", ":");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%3B", ";");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%3D", "=");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%3F", "?");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%40", "@");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%5B", "[");
	//		PERCENT_ENCODING_DECODE_MAPPING.put("%5D", "]");
	//	}

	public WikipediaPage(String title, String rawText)
	{
		this.title = title;
		this.rawText = rawText;

		this.redirectToTitle = WikipediaPageParseHelper
				.parseRedirectToTitle(rawText);
		if (this.redirectToTitle != null)
		{
			// some page use "Committee_on_Culture_and_Education" as redirect title
			try
			{
				redirectToTitle = redirectToTitle.replace("+", "%2B");
				redirectToTitle = URLDecoder.decode(redirectToTitle, "UTF-8")
						.replace("_", " ").trim();
				redirectToTitle = redirectToTitle.replaceAll("\\s{2,}", " ");
			}
			catch (UnsupportedEncodingException e)
			{
				throw new GeneralException(e);
			}
		}

		this.isDisambiguation = WikipediaPageParseHelper
				.isDisambiguation(rawText);
	}

	public String getTitle()
	{
		return title;
	}

	public String getRawText()
	{
		return rawText;
	}

	public boolean isRedirectPage()
	{
		return getRedirectToTitle() != null;
	}

	public String getRedirectToTitle()
	{
		return redirectToTitle;
	}

	public boolean isDisambiguation()
	{
		return isDisambiguation;
	}

	public String getInfoBoxRawText()
	{
		// lazy initialization, since it is not used very often now
		if (infoBoxRawText == null)
		{
			synchronized (this)
			{
				if (infoBoxRawText == null)
				{
					infoBoxRawText = WikipediaPageParseHelper
							.parseInfoBox(rawText);
				}
			}
		}

		return infoBoxRawText;
	}

	public List<String> getCategories()
	{
		if (categories == null)
		{
			synchronized (this)
			{
				if (categories == null)
				{
					categories = WikipediaPageParseHelper
							.parseCategories(rawText);
				}
			}
		}

		return categories;
	}

	public String getHtmlText()
	{
		if (htmlText == null)
		{
			synchronized (this)
			{
				if (htmlText == null)
				{
					htmlText = WikipediaPageParseHelper.parseRawText(rawText);
				}
			}
		}

		return htmlText;
	}

	public String getInfoBoxHtmlText()
	{
		if (getInfoBoxRawText() == null)
		{
			return null;
		}

		if (infoBoxHtmlText == null)
		{
			synchronized (this)
			{
				if (infoBoxHtmlText == null)
				{
					infoBoxHtmlText = WikipediaPageParseHelper
							.parseRawText(getInfoBoxRawText());
				}
			}
		}

		return infoBoxHtmlText;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WikipediaPage other = (WikipediaPage) obj;
		if (title == null)
		{
			if (other.title != null)
				return false;
		}
		else if (!title.equals(other.title))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "WikipediaRawPage [title="
				+ title
				+ ", isDisambiguation="
				+ isDisambiguation
				+ (isRedirectPage() ? ", redirectToTitle=" + redirectToTitle
						: "") + "]";
	}

}
