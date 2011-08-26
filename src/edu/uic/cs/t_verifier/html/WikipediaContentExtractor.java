package edu.uic.cs.t_verifier.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

import edu.uic.cs.t_verifier.html.data.MatchedQueryKey;
import edu.uic.cs.t_verifier.html.data.MatchedQueryKey.DisambiguationEntry;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.misc.LogHelper;

public abstract class WikipediaContentExtractor implements HtmlConstants
{
	private static final Logger LOGGER = LogHelper
			.getLogger(WikipediaContentExtractor.class);

	private static final String WIKI_SEARCH_URL_PREFIX = "http://en.wikipedia.org/wiki/Special:Search/";
	private static final String WIKI_URL_PREFIX = "/wiki/";
	private static final int WIKI_URL_PREFIX_LENGTH = WIKI_URL_PREFIX.length();

	private static final String HTML_TEXT_DISAMBIGUATION = " may refer to:";

	private static class DisambiguationTagNodeFilter implements NodeFilter
	{
		private static final long serialVersionUID = 1L;

		@Override
		public boolean accept(Node node)
		{
			if (node instanceof Text
					&& node.toPlainTextString()
							.equals(HTML_TEXT_DISAMBIGUATION))
			{
				return true;
			}

			return false;
		}
	}

	private static class PossibleKeyWordsFilter implements NodeFilter
	{
		private static final long serialVersionUID = 1L;

		@Override
		public boolean accept(Node node)
		{
			if (node instanceof Tag
					&& ((Tag) node).getRawTagName().equals("li")
					&& (node.getChildren() != null || node.getChildren().size() > 0))
			{
				return true;
			}

			return false;
		}
	}

	protected static class BodyContentDivFilter implements NodeFilter
	{
		private static final long serialVersionUID = 1L;

		private static final String HTML_TAG_DIV_ATTRIBUTE_ID = "id";
		private static final String HTML_TAG_DIV_ATTRIBUTE_ID_BODYCONTENT = "bodyContent";

		public BodyContentDivFilter()
		{
		}

		@Override
		public boolean accept(Node node)
		{
			if (node instanceof Div)
			{
				Div div = (Div) node;
				String id = div.getAttribute(HTML_TAG_DIV_ATTRIBUTE_ID);
				if (HTML_TAG_DIV_ATTRIBUTE_ID_BODYCONTENT.equals(id))
				{
					return true;
				}
			}

			return false;
		}
	}

	protected PoliteParser parser = new PoliteParser();

	public MatchedQueryKey matchQueryKey(String queryWords)
	{
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug(LogHelper.LOG_LAYER_ONE_BEGIN + "Trying key word["
					+ queryWords + "]. ");
		}

		MatchedQueryKey result = matchQueryKeyInternal(queryWords);

		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug(LogHelper.LOG_LAYER_ONE_END + "Trying key word["
					+ queryWords + "]. Result[" + result + "]. ");
		}

		return result;
	}

	private MatchedQueryKey matchQueryKeyInternal(String queryWords)
	{
		String url = WIKI_SEARCH_URL_PREFIX
				+ queryWords.trim().replace(' ', '_');

		try
		{
			System.out.print(url);
			parser.setResource(url);

			// this means the input query words can't exactly match a page 
			if (url.equals(parser.getURL()))
			{
				System.out.println(" ×");
				return null;
			}

			Node htmlNode = parser.parse(new TagNameFilter(HTML_TAG_HTML))
					.elementAt(0);

			Node headNode = htmlNode.getChildren()
					.extractAllNodesThatMatch(new TagNameFilter(HTML_TAG_HEAD))
					.elementAt(0);
			Node titleNode = headNode
					.getChildren()
					.extractAllNodesThatMatch(new TagNameFilter(HTML_TAG_TITLE))
					.elementAt(0);

			// successfully retrieved title looks like this:  
			// Sleepless in Seattle - Wikipedia, the free encyclopedia
			String titleString = titleNode.toPlainTextString();
			titleString = titleString
					.substring(0, titleString.lastIndexOf('-')).trim();

			Node bodyContentDiv = htmlNode.getChildren()
					.extractAllNodesThatMatch(new BodyContentDivFilter(), true)
					.elementAt(0);
			// there may be ambiguous key words
			List<DisambiguationEntry> disambiguationEntries = extractDisambiguationEntries(bodyContentDiv
					.getChildren());

			MatchedQueryKey result = new MatchedQueryKey(
					titleString.replaceAll(" ", "_"), disambiguationEntries);
			if (result.isCertainly())
			{
				System.out.println(" √");
			}
			else
			{
				System.out.println(" ?");
			}

			return result;

		}
		catch (ParserException e)
		{
			throw new GeneralException(e);
		}
	}

	private List<DisambiguationEntry> extractDisambiguationEntries(
			NodeList nodeListInBodyContentDiv)
	{
		if (!isThereDisambiguationKeyWords(nodeListInBodyContentDiv))
		{
			// there's no disambiguation key words
			return Collections.emptyList();
		}

		NodeList allAmbiguousBulletList = nodeListInBodyContentDiv
				.extractAllNodesThatMatch(new TagNameFilter("ul"));
		NodeList allAmbiguousbullets = allAmbiguousBulletList
				.extractAllNodesThatMatch(new PossibleKeyWordsFilter(), true);

		List<DisambiguationEntry> result = new ArrayList<DisambiguationEntry>();
		for (int index = 0; index < allAmbiguousbullets.size(); index++)
		{
			Bullet bullet = (Bullet) allAmbiguousbullets.elementAt(index);
			String alternativeKeyWord = extractAlternativeKeyWord(bullet);
			String description = extractDescription(bullet);

			// break the last entry if it ends with "(disambiguation)"
			if (index == allAmbiguousbullets.size() - 1
					&& description.toLowerCase(Locale.US).endsWith(
							"(disambiguation)"))
			{
				break;
			}

			result.add(new DisambiguationEntry(alternativeKeyWord, description));
		}

		return result;
	}

	private String extractDescription(Bullet bullet)
	{
		StringBuilder stringRepresentation = new StringBuilder();
		for (SimpleNodeIterator e = bullet.children(); e.hasMoreNodes();)
		{
			Node node = e.nextNode();
			// Only process one level
			if (node instanceof BulletList)
			{
				continue;
			}
			stringRepresentation.append(node.toPlainTextString());
		}

		String description = StringEscapeUtils
				.unescapeHtml(stringRepresentation.toString());

		return description.trim();
	}

	private String extractAlternativeKeyWord(Bullet bullet)
	{
		LinkTag linkTag = (LinkTag) bullet.getChildren()
				.extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0);
		if (linkTag == null)
		{
			return null;
		}

		String link = linkTag.getLink().trim();

		int brginIndex = link.indexOf(WIKI_URL_PREFIX);
		if (brginIndex < 0)
		{
			return null;
		}
		else
		{
			return link.substring(brginIndex + WIKI_URL_PREFIX_LENGTH);
		}
	}

	private boolean isThereDisambiguationKeyWords(
			NodeList nodeListInBodyContentDiv)
	{
		NodeList ambiguityTag = nodeListInBodyContentDiv
				.extractAllNodesThatMatch(new DisambiguationTagNodeFilter(),
						true);

		return ambiguityTag.size() != 0;
	}

	abstract public List<Segment> extractPageContentFromWikipedia(String url,
			boolean isBulletinPage);

}
