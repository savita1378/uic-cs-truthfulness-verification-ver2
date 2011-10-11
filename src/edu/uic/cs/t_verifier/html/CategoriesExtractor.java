package edu.uic.cs.t_verifier.html;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.Div;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.GeneralException;

public abstract class CategoriesExtractor implements HtmlConstants
{
	private static final String HTML_TAG_DIV_ATTRIBUTE_ID = "id";
	private static final String HTML_TAG_DIV_ATTRIBUTE_ID_CATEGORIES = "catlinks";
	private static final String HTML_TAG_DIV_ATTRIBUTE_ID_NORMAL_CATEGORIES = "mw-normal-catlinks";

	private static final String KEY_WORD_CATEGORIES = "Categories:";
	private static final int KEY_WORD_CATEGORIES_LENGTH = KEY_WORD_CATEGORIES
			.length();

	protected static class ContentLtrDivFilter implements NodeFilter
	{
		private static final long serialVersionUID = 1L;

		private static final String HTML_TAG_DIV_ATTRIBUTE_CLASS = "class";
		private static final String HTML_TAG_DIV_ATTRIBUTE_CLASS_CONTENT = "mw-content-ltr";

		public ContentLtrDivFilter()
		{
		}

		@Override
		public boolean accept(Node node)
		{
			if (node instanceof Div)
			{
				Div div = (Div) node;
				String id = div.getAttribute(HTML_TAG_DIV_ATTRIBUTE_CLASS);
				if (HTML_TAG_DIV_ATTRIBUTE_CLASS_CONTENT.equals(id))
				{
					return true;
				}
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

	protected List<String> extractCategoriesFromBodyContentDiv(
			Node bodyContentDiv)
	{
		List<String> categories = new ArrayList<String>();

		SimpleNodeIterator iterator = bodyContentDiv.getChildren().elements();
		while (iterator.hasMoreNodes())
		{
			Node child = iterator.nextNode();
			if (child instanceof Div
					&& HTML_TAG_DIV_ATTRIBUTE_ID_CATEGORIES
							.equals(((Div) child)
									.getAttribute(HTML_TAG_DIV_ATTRIBUTE_ID)))
			{
				Node firstChild = child.getFirstChild();
				String id = ((Div) firstChild)
						.getAttribute(HTML_TAG_DIV_ATTRIBUTE_ID);
				if (HTML_TAG_DIV_ATTRIBUTE_ID_NORMAL_CATEGORIES.equals(id))
				{
					for (Node node : firstChild.getChildren().toNodeArray())
					{
						if (node instanceof BulletList)
						{
							for (Node bullet : node.getChildren().toNodeArray())
							{
								categories.add(bullet.toPlainTextString()
										.trim());
							}
						}
					}

					if (categories.isEmpty())
					{
						String categoriesString = firstChild
								.toPlainTextString();
						int index = categoriesString
								.indexOf(KEY_WORD_CATEGORIES);
						Assert.isTrue(index == 0);
						categoriesString = categoriesString
								.substring(KEY_WORD_CATEGORIES_LENGTH);

						String[] categoriesArray = StringUtils.split(
								categoriesString, '|');
						for (String category : categoriesArray)
						{
							categories.add(category.trim());
						}
					}

					break;
				}
			}
		}

		return categories;
	}

	public List<String> extractCategoriesFromPage(String url)
	{
		Node htmlNode = null;
		try
		{
			parser.setResource(url);
			htmlNode = parser.parse(new TagNameFilter(HTML_TAG_HTML))
					.elementAt(0);
		}
		catch (ParserException e)
		{
			throw new GeneralException(e);
		}

		Node bodyContentDiv = htmlNode.getChildren()
				.extractAllNodesThatMatch(new BodyContentDivFilter(), true)
				.elementAt(0);

		return extractCategoriesFromBodyContentDiv(bodyContentDiv);
	}

	public static void main(String[] args)
	{
		String pageUrl = "http://en.wikipedia.org/wiki/Frances_Folsom_Cleveland_Preston";
		//		String pageUrl = "http://en.wikipedia.org/wiki/Big_Mac";
		//		String pageUrl = "http://en.wikipedia.org/w/index.php?title=Languages_of_the_Philippines";
		//		String pageUrl = "http://en.wikipedia.org/wiki/1949";
		//		String pageUrl = "http://en.wikipedia.org/w/index.php?title=1800%E2%80%931809";
		//		String pageUrl = "http://en.wikipedia.org/wiki/Sleepless_in_seattle";
		//		String pageUrl = "http://en.wikipedia.org/wiki/Filipino";
		//		String pageUrl = "http://en.wikipedia.org/wiki/Adolph_Rickenbacker";
		//		String pageUrl = "http://en.wikipedia.org/wiki/Naples";

		List<String> categories = new CategoriesExtractor()
		{
		}.extractCategoriesFromPage(pageUrl);

		for (String category : categories)
		{
			System.out.println(category);
		}
	}
}
