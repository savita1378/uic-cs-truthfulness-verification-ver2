package edu.uic.cs.wiki_verifier.wiki.local.search;

import info.bliki.wiki.model.WikiModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikipediaPageParseHelper
{
	private static final Pattern REDIRECT_PATTERN = Pattern.compile(
			"^#REDIRECT\\s+\\[\\[(.*?)\\]\\]", Pattern.CASE_INSENSITIVE); // #REDIRECT must at the begin of page

	private final static Pattern CATEGORY_PATTERN = Pattern.compile(
			"\\[\\[Category:(.*?)\\]\\]", Pattern.MULTILINE
					| Pattern.CASE_INSENSITIVE);

	private final static Pattern DISAMB_TEMPLATE_PATTERN = Pattern.compile(
			"\\{\\{(Disambig|Disambiguation(.*?)|Dab|Disamb)\\}\\}",
			Pattern.CASE_INSENSITIVE);

	static String parseRawText(String rawText)
	{
		return WikiModel.toHtml(rawText);
	}

	static List<String> parseCategories(String rawText)
	{
		List<String> categories = new ArrayList<String>();
		Matcher matcher = CATEGORY_PATTERN.matcher(rawText);
		while (matcher.find())
		{
			String[] temp = matcher.group(1).split("\\|");
			categories.add(temp[0]);
		}

		return categories;
	}

	static String parseRedirectToTitle(String rawText)
	{
		String redirectToTitle = null;
		Matcher matcher = REDIRECT_PATTERN.matcher(rawText);
		if (matcher.find())
		{
			if (matcher.groupCount() == 1)
			{
				redirectToTitle = matcher.group(1);
			}
		}
		return redirectToTitle;
	}

	/**
	 * Copied and modified from the <a
	 * href="http://code.google.com/p/gwtwiki/">Java Wikipedia API (Bliki engine)</a>.
	 * @param rawText
	 * @return
	 */
	static String parseInfoBox(String rawText)
	{
		String INFOBOX_CONST_STR = "{{Infobox";
		int startPos = rawText.indexOf(INFOBOX_CONST_STR);
		if (startPos < 0)
			return null;
		int bracketCount = 2;
		int endPos = startPos + INFOBOX_CONST_STR.length();

		if (endPos >= rawText.length())
		{
			return null;
		}
		for (; endPos < rawText.length(); endPos++)
		{
			switch (rawText.charAt(endPos))
			{
				case '}':
					bracketCount--;
					break;
				case '{':
					bracketCount++;
					break;
				default:
			}
			if (bracketCount == 0)
				break;
		}
		String infoBoxText;
		if (endPos >= rawText.length())
		{
			infoBoxText = rawText.substring(startPos);
		}
		else
		{
			infoBoxText = rawText.substring(startPos, endPos + 1);
		}
		infoBoxText = stripCite(infoBoxText); // strip clumsy {{cite}} tags
		// strip any html formatting
		infoBoxText = infoBoxText.replaceAll("&gt;", ">");
		infoBoxText = infoBoxText.replaceAll("&lt;", "<");
		infoBoxText = infoBoxText.replaceAll("<ref.*?>.*?</ref>", " ");
		infoBoxText = infoBoxText.replaceAll("</?.*?>", " ");

		return infoBoxText;
	}

	private static String stripCite(String text)
	{
		String CITE_CONST_STR = "{{cite";
		int startPos = text.indexOf(CITE_CONST_STR);
		if (startPos < 0)
			return text;
		int bracketCount = 2;
		int endPos = startPos + CITE_CONST_STR.length();
		for (; endPos < text.length(); endPos++)
		{
			switch (text.charAt(endPos))
			{
				case '}':
					bracketCount--;
					break;
				case '{':
					bracketCount++;
					break;
				default:
			}
			if (bracketCount == 0)
				break;
		}
		text = text.substring(0, startPos - 1) + text.substring(endPos);
		return stripCite(text);
	}

	static boolean isDisambiguation(String rawText)
	{
		Matcher matcher = DISAMB_TEMPLATE_PATTERN.matcher(rawText);
		return matcher.find();
	}

	public static void main(String[] args)
	{
		String rawText = "{{disambiguation}}";
		System.out.println(isDisambiguation(rawText));
	}
}
