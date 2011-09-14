package edu.uic.cs.t_verifier.html.impl;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.ParserException;

import edu.uic.cs.t_verifier.html.WikipediaContentExtractor;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.index.data.UrlWithDescription;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.wikipedia.WikitextParser;

public class WikitextExtractor extends WikipediaContentExtractor
{
	private static final String WIKI_ADDRESS_EDIT_POSTFIX = "&action=edit";

	// for test
	String extractWikitext(String url)
	{
		if (!url.endsWith(WIKI_ADDRESS_EDIT_POSTFIX))
		{
			url = url + WIKI_ADDRESS_EDIT_POSTFIX;
		}

		try
		{
			parser.setResource(url);
			Node wikitextNode = parser.parse(new TagNameFilter("textarea"))
					.elementAt(0);

			String wikitext = StringEscapeUtils.unescapeHtml(wikitextNode
					.toPlainTextString());

			return wikitext.trim();
		}
		catch (ParserException e)
		{
			throw new GeneralException(e);
		}
	}

	@Override
	public List<Segment> extractPageContentFromWikipedia(
			UrlWithDescription url, boolean isBulletinPage)
	{
		// TODO here we ignore BulletinPage...
		String wikitext = extractWikitext(url.getUrl());
		List<Segment> segmentsInPage = WikitextParser.parse(wikitext);

		return segmentsInPage;
	}

}
