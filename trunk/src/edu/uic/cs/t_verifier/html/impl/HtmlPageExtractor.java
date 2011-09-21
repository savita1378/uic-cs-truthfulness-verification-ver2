package edu.uic.cs.t_verifier.html.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.RemarkNode;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import edu.uic.cs.t_verifier.html.WikipediaContentExtractor;
import edu.uic.cs.t_verifier.html.data.PageContent;
import edu.uic.cs.t_verifier.index.data.Heading;
import edu.uic.cs.t_verifier.index.data.Paragraph;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.index.data.Table;
import edu.uic.cs.t_verifier.index.data.UrlWithDescription;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.GeneralException;

public class HtmlPageExtractor extends WikipediaContentExtractor
{
	private static Pattern REFERENCE_SYMBOL_PATTERN = Pattern
			.compile("(\\[(\\w| )+\\])+\\s");

	private static final String HTML_TAG_DIV_ATTRIBUTE_ID = "id";
	private static final String HTML_TAG_DIV_ATTRIBUTE_ID_CATEGORIES = "catlinks";
	private static final String HTML_TAG_DIV_ATTRIBUTE_ID_NORMAL_CATEGORIES = "mw-normal-catlinks";

	private static final String KEY_WORD_CATEGORIES = "Categories:";
	private static final int KEY_WORD_CATEGORIES_LENGTH = KEY_WORD_CATEGORIES
			.length();

	@Override
	public PageContent extractPageContentFromWikipedia(
			UrlWithDescription urlWithDescription, boolean isBulletinPage)
	{
		PageContent result = null;
		try
		{
			parser.setResource(urlWithDescription.getUrl());
			Node htmlNode = parser.parse(new TagNameFilter(HTML_TAG_HTML))
					.elementAt(0);
			Node bodyContentDiv = htmlNode.getChildren()
					.extractAllNodesThatMatch(new BodyContentDivFilter(), true)
					.elementAt(0);

			Node[] contents = bodyContentDiv.getChildren().toNodeArray();
			result = parseContents(contents, isBulletinPage, urlWithDescription);
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}

		return result;
	}

	private static PageContent parseContents(Node[] contents,
			boolean isBulletinPage, UrlWithDescription urlWithDescription)
	{
		List<Segment> segments = new ArrayList<Segment>();
		List<String> categories = new ArrayList<String>();

		for (Node content : contents)
		{
			if (content instanceof HeadingTag)
			{
				String headingString = toPlainTextStringWithoutReferenceSymbol(content);
				if (StringUtils.isBlank(headingString))
				{
					continue;
				}

				Heading heading = new Heading(headingString);

				// heading means new segment begins
				Segment segment = new Segment();
				segments.add(segment);

				segment.setHeading(heading);
			}
			else if (content instanceof ParagraphTag)
			{
				String paragraphString = toPlainTextStringWithoutReferenceSymbol(content);
				if (StringUtils.isBlank(paragraphString))
				{
					continue;
				}

				Paragraph paragraph = new Paragraph();
				paragraph.setText(paragraphString);

				if (segments.isEmpty())
				{
					segments.add(new Segment());
				}
				Segment segment = segments.get(segments.size() - 1);
				segment.addParagraph(paragraph);
			}
			else if (content instanceof BulletList)
			{
				if (isBulletinPage) // each Bullet is considered as a paragraph
				{
					NodeList allBullets = content.getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("li"));
					for (int index = 0; index < allBullets.size(); index++)
					{
						recursiveExtractBullets("",
								(Bullet) allBullets.elementAt(index), segments);
					}

				}
				else
				{
					String paragraphString = toPlainTextStringWithoutReferenceSymbol(content);
					if (StringUtils.isBlank(paragraphString))
					{
						continue;
					}

					if (segments.isEmpty())
					{
						segments.add(new Segment());
					}
					Segment segment = segments.get(segments.size() - 1);

					Paragraph paragraph = null;
					List<Paragraph> paragraphs = segment.getParagraphs();
					// if there's no paragraph exist, create one
					if (paragraphs == null || paragraphs.isEmpty())
					{
						paragraph = new Paragraph();
						paragraph.setText(paragraphString);
						segment.addParagraph(paragraph);
					}
					else
					// there's exist one in the segment, append content into that previous one
					{
						paragraph = paragraphs.get(paragraphs.size() - 1);
						StringBuilder stringBuilder = new StringBuilder(
								paragraph.getText());
						stringBuilder.append('\n').append(paragraphString);
						paragraph.setText(stringBuilder.toString());
					}
				}
			}
			else if (content instanceof TableTag)
			{
				Table table = parseTable((TableTag) content);
				if (table == null)
				{
					continue;
				}

				if (segments.isEmpty())
				{
					segments.add(new Segment());
				}
				Segment segment = segments.get(segments.size() - 1);
				segment.addTable(table);
			}
			else if (content instanceof Div
					&& HTML_TAG_DIV_ATTRIBUTE_ID_CATEGORIES
							.equals(((Div) content)
									.getAttribute(HTML_TAG_DIV_ATTRIBUTE_ID)))
			{
				Node firstChild = content.getFirstChild();
				String id = ((Div) firstChild)
						.getAttribute(HTML_TAG_DIV_ATTRIBUTE_ID);
				if (HTML_TAG_DIV_ATTRIBUTE_ID_NORMAL_CATEGORIES.equals(id))
				{
					String categoriesString = firstChild.toPlainTextString();
					int index = categoriesString.indexOf(KEY_WORD_CATEGORIES);
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
			}
			else if (content instanceof TextNode
					|| content instanceof RemarkNode /*|| content instanceof Div*/
					|| content instanceof TagNode)
			{
				// ignore... 
			}
			else
			{
				throw new GeneralException("Unsupported Content instance["
						+ content.getClass() + "]. ");
			}
		}

		if (urlWithDescription.isDisambiguationLink())
		{
			// description from disambiguation link
			Segment segment = new Segment();

			Paragraph paragraph = new Paragraph();
			paragraph.setText(urlWithDescription.getDescription());
			segment.addParagraph(paragraph);

			// add this description in a new segment
			segments.add(segment);
		}

		return new PageContent(segments, categories);
	}

	private static void recursiveExtractBullets(String textInUpperLevels,
			Bullet currentBullet, List<Segment> segments)
	{
		if (textInUpperLevels.length() != 0)
		{
			textInUpperLevels = textInUpperLevels + " ";
		}
		NodeList subBulletList = currentBullet.getChildren()
				.extractAllNodesThatMatch(new TagNameFilter("ul"));
		if (subBulletList.size() == 0) // lowest level Bullet
		{
			String bulletString = toPlainTextStringWithoutReferenceSymbol(currentBullet);
			String finalString = textInUpperLevels + bulletString;

			// store it! and return
			if (segments.isEmpty())
			{
				segments.add(new Segment());
			}
			Segment segment = segments.get(segments.size() - 1);

			Paragraph paragraph = new Paragraph();
			paragraph.setText(finalString);

			segment.addParagraph(paragraph);

			// System.out.println(finalString);
			return;
		}

		Assert.isTrue(subBulletList.size() == 1);
		NodeList subBullets = subBulletList.elementAt(0).getChildren()
				.extractAllNodesThatMatch(new TagNameFilter("li"));

		currentBullet.removeChild(currentBullet.getChildren().indexOf(
				subBulletList.elementAt(0)));
		String bulletString = toPlainTextStringWithoutReferenceSymbol(currentBullet);

		for (int index = 0; index < subBullets.size(); index++)
		{
			Bullet bullet = (Bullet) subBullets.elementAt(index);
			recursiveExtractBullets(textInUpperLevels + bulletString, bullet,
					segments);
		}

	}

	private static String toPlainTextStringWithoutReferenceSymbol(Node content)
	{
		String contentString = REFERENCE_SYMBOL_PATTERN.matcher(
				content.toPlainTextString()).replaceAll(" ");

		if (contentString != null)
		{
			contentString = StringEscapeUtils.unescapeHtml(contentString)
					.trim();
		}

		return contentString;
	}

	private static Table parseTable(TableTag table)
	{
		StringBuilder sb = new StringBuilder();

		NodeList nl = table.getChildren().extractAllNodesThatMatch(
				new TagNameFilter("caption"), true);
		if (nl.size() != 0)
		{
			String captionString = null;
			Node captionTagNode = nl.elementAt(0);
			do
			{
				captionTagNode = captionTagNode.getNextSibling();
				captionString = toPlainTextStringWithoutReferenceSymbol(captionTagNode);
			}
			while (StringUtils.isBlank(captionString));

			//			System.out.println("==" + captionString + "==");
			// consider caption as a table row
			sb.append(captionString).append('\n');
		}

		TableRow[] rows = table.getRows();
		for (TableRow row : rows)
		{
			String rowString = toPlainTextStringWithoutReferenceSymbol(row);
			// ignore TOC
			if (rowString != null && !rowString.startsWith("Contents"))
			{
				rowString = rowString.replace('\n', ' ');
				//				System.out.println(rowString);
				//				result.addSentence(rowString);
				sb.append(rowString).append('\n');
			}

		}

		//		System.out
		//				.println("======================================================");

		String text = sb.toString().trim();
		if (text.length() != 0)
		{
			Table result = new Table();
			result.setText(text);
			return result;
		}

		return null;
	}

	//	private StringBuilder builder = new StringBuilder();
	//
	//	private void recursiveExtractNode(Node node, int indent)
	//	{
	//		for (int i = 0; i < indent; i++)
	//		{
	//			System.out.print(" ");
	//			builder.append(' ');
	//		}
	//		System.out.println(node.getClass().getSimpleName() + " ["
	//				+ node.getText().trim() + "]");
	//		builder.append(node.getClass().getSimpleName() + " ["
	//				+ StringEscapeUtils.unescapeHtml(node.getText()).trim() + "]\n");
	//
	//		NodeList children = node.getChildren();
	//		if (children != null)
	//		{
	//			for (Node child : children.toNodeArray())
	//			{
	//				recursiveExtractNode(child, indent + 1);
	//			}
	//		}
	//
	//	}

	public static void main2(String[] args)
	{
		String str = "100 2,000, wanghong.nanjing@gmail.com The quick brown[  sda sf 1 ] fox[1][2]\n jumps over[3] the lazy[citation needed] dog stemming algorithms sunk age aging";
		String str2 = REFERENCE_SYMBOL_PATTERN.matcher(str).replaceAll(" ");

		System.out.println(str2);
	}

	public static void main(String[] args) throws Exception
	{
		//		String str = "100 2,000, wanghong.nanjing@gmail.com The quick brown[  sda sf 1 ] fox[1][2] jumps over[3] the lazy[citation needed] dog stemming algorithms sunk age aging";
		//		//str = StringUtils.remove(str, "[*]");
		//		String str2 = REFERENCE_SYMBOL_PATTERN.matcher(str).replaceAll(" ");
		//
		//		System.out.println(str2);
		//		System.out
		//				.println(str2
		//						.equals("100 2,000, wanghong.nanjing@gmail.com The quick brown fox jumps over the lazy dog stemming algorithms sunk age aging"));

		HtmlPageExtractor extractor = new HtmlPageExtractor();
		//		String pageUrl = "http://en.wikipedia.org/wiki/Sorting_algorithm";
		//		String pageUrl = "http://en.wikipedia.org/wiki/Big_Mac";
		//		String pageUrl = "http://en.wikipedia.org/w/index.php?title=Languages_of_the_Philippines";
		//		String pageUrl = "http://en.wikipedia.org/wiki/1949";
		//		String pageUrl = "http://en.wikipedia.org/w/index.php?title=1800%E2%80%931809";
		//		String pageUrl = "http://en.wikipedia.org/wiki/Sleepless_in_seattle";
		//		String pageUrl = "http://en.wikipedia.org/wiki/Filipino";
		String pageUrl = "http://en.wikipedia.org/wiki/Adolph_Rickenbacker";
		PageContent result = extractor
				.extractPageContentFromWikipedia(
						new UrlWithDescription(pageUrl, null
						/*"Filipino language, the national language of the Philippines, based primarily on Tagalog"*/),
						true);

		for (Segment segment : result.getSegments())
		{
			if (segment.getHeading() != null)
			{
				System.out.println("HEADING: [" + segment.getHeading() + "]");
				System.out.println("----------------");
			}

			if (segment.getParagraphs() != null)
			{
				for (Paragraph paragraph : segment.getParagraphs())
				{
					System.out.println("PARAGRAPH: [" + paragraph + "]");
					System.out.println("-----");
					//					for (String sentence : paragraph.getSentences())
					//					{
					//						System.out.println("[" + sentence + "]");
					//					}
				}
				System.out.println("----------------");
			}

			if (segment.getTables() != null)
			{
				for (Table table : segment.getTables())
				{
					System.out.println("TABLE: [" + table + "]");
					System.out.println("-----");
				}
			}
			System.out.println("===========================================");
		}

		for (String category : result.getCategoriesBelongsTo())
		{
			System.out.println(category);
		}

	}
}