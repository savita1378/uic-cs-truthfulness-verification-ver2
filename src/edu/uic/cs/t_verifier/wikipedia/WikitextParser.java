package edu.uic.cs.t_verifier.wikipedia;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import be.devijver.wikipedia.parser.ast.Content;
import be.devijver.wikipedia.parser.ast.Document;
import be.devijver.wikipedia.parser.ast.Indent;
import be.devijver.wikipedia.parser.ast.Literal;
import be.devijver.wikipedia.parser.ast.OrderedListItem;
import be.devijver.wikipedia.parser.ast.SpecialConstruct;
import be.devijver.wikipedia.parser.ast.UnorderedListItem;
import be.devijver.wikipedia.parser.wikitext.MarkupParser;
import edu.uic.cs.t_verifier.index.data.Heading;
import edu.uic.cs.t_verifier.index.data.Paragraph;
import edu.uic.cs.t_verifier.index.data.Segment;
import edu.uic.cs.t_verifier.index.data.Table;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.Config;
import edu.uic.cs.t_verifier.misc.GeneralException;

public class WikitextParser
{
	private static final String XML_COMMENT_END = "-->";
	private static final int XML_COMMENT_END_LENGTH = XML_COMMENT_END.length();
	private static final String XML_COMMENT_BEGIN = "<!--";

	//	private static final int XML_COMMENT_BEGIN_LENGTH = XML_COMMENT_BEGIN
	//			.length();

	public static List<Segment> parse(String wikitext)
	{
		// make sure there's no '\r' in the string, which can't be processed by JWP
		wikitext = wikitext.replace("\r\n", "\n");

		Document doc = new MarkupParser(wikitext).parseDocument();

		List<Segment> result = parseContents(doc.getContent());

		return result;
	}

	private static List<Segment> parseContents(Content[] contents)
	{
		List<Segment> segments = new ArrayList<Segment>();

		boolean inComment = false;
		boolean newParagraph = true;
		for (Content content : contents)
		{
			if (content instanceof be.devijver.wikipedia.parser.ast.Heading)
			{
				Heading heading = new Heading(content.toString());

				// heading means new segment begins
				Segment segment = new Segment();
				segments.add(segment);

				segment.setHeading(heading);
				newParagraph = true;
			}
			else if (content instanceof be.devijver.wikipedia.parser.ast.Paragraph)
			{
				Paragraph paragraph = new Paragraph();
				Table table = new Table();
				inComment = parseParagraph(
						(be.devijver.wikipedia.parser.ast.Paragraph) content,
						paragraph, table, inComment);

				if (paragraph.getText() != null)
				{
					if (segments.isEmpty())
					{
						segments.add(new Segment());
					}

					Segment segment = segments.get(segments.size() - 1);

					segment.addParagraph(paragraph);
				}

				if (table.getText() != null)
				{
					if (segments.isEmpty())
					{
						segments.add(new Segment());
					}

					Segment segment = segments.get(segments.size() - 1);

					segment.addTable(table);
				}

				newParagraph = true;

			}
			else if (content instanceof UnorderedListItem
					|| content instanceof OrderedListItem
					|| content instanceof Indent)
			{
				if (segments.isEmpty())
				{
					segments.add(new Segment());
				}

				Segment segment = segments.get(segments.size() - 1);

				Paragraph paragraph = null;
				if (newParagraph || segment.getParagraphs().isEmpty())
				{
					// consider it as Paragraph
					paragraph = new Paragraph();
					segment.addParagraph(paragraph);
				}
				else
				{
					// use last Paragraph
					List<Paragraph> paragraphs = segment.getParagraphs();
					paragraph = paragraphs.get(paragraphs.size() - 1);
				}

				paragraph.setText(content.toString());

				newParagraph = false;

			}
			else if (content instanceof Literal)
			{
				// ignore
			}
			else
			{
				throw new GeneralException("Unsupported Content instance["
						+ content.getClass() + "]. ");
			}
		}

		return segments;
	}

	private static boolean parseParagraph(
			be.devijver.wikipedia.parser.ast.Paragraph para,
			Paragraph paragraph, Table table, boolean inComment)
	{
		StringBuilder text = new StringBuilder();
		for (Content content : para.getContent())
		{
			if (content instanceof SpecialConstruct)
			{
				// for SpecialConstruct, we process Table
				if ('|' == ((SpecialConstruct) content).getCharacter())
				{
					Assert.isTrue(table.getText() == null,
							"One paragraph contains only one Table. ");

					String tableWikitext = ((SpecialConstruct) content)
							.getContent();
					if (!inComment)
					{
						table.setText("{|" + tableWikitext + "|}");
					}
				}
				// SOME Templates
				else if ('{' == ((SpecialConstruct) content).getCharacter())
				{
					String templateWikitext = ((SpecialConstruct) content)
							.getContent();
					// System.out.println(templateWikitext);
					if (!startsWithAnyIgnoreCase(templateWikitext,
							Config.IGNORING_COMMON_TEMPLATES))
					{
						if (!inComment)
						{
							table.setText("{{" + templateWikitext + "}}");
						}
					}
				}
			}
			else if (content != null)
			{
				String wikitext = content.toString();
				int commentIndexBegin = wikitext.indexOf(XML_COMMENT_BEGIN);
				int commentIndexEnd = wikitext.indexOf(XML_COMMENT_END);

				// comment in the same line
				if (commentIndexBegin != -1 && commentIndexEnd != -1)
				{
					Assert.isTrue(commentIndexBegin < commentIndexEnd,
							"DO NOT support two or more comments in a same line["
									+ wikitext + "]");

					String begin = wikitext.substring(0, commentIndexBegin);
					String end = wikitext.substring(commentIndexEnd
							+ XML_COMMENT_END_LENGTH);

					wikitext = begin + end;

					inComment = false;
				}
				// just has <!--
				else if (commentIndexBegin != -1 && commentIndexEnd == -1)
				{
					wikitext = wikitext.substring(0, commentIndexBegin);

					// Assert.isTrue(!inComment); //
					inComment = true;
				}
				// just has -->
				else if (commentIndexBegin == -1 && commentIndexEnd != -1)
				{
					wikitext = wikitext.substring(commentIndexEnd
							+ XML_COMMENT_END_LENGTH);

					// Assert.isTrue(inComment); //
					inComment = false;
				}
				//				else
				//				{
				//					inComment = false;
				//				}

				if (!inComment)
				{
					text.append(wikitext);
				}
			}
		}

		String wikitext = text.toString().trim();
		if (wikitext.length() != 0)
		{
			paragraph.setText(wikitext);
		}

		return inComment;
	}

	private static boolean startsWithAnyIgnoreCase(String string,
			String[] searchStrings)
	{
		if (StringUtils.isEmpty(string) || ArrayUtils.isEmpty(searchStrings))
		{
			return false;
		}
		for (int i = 0; i < searchStrings.length; i++)
		{
			String searchString = searchStrings[i];
			if (StringUtils.startsWithIgnoreCase(string, searchString))
			{
				return true;
			}
		}
		return false;
	}

	//	public static void main(String[] args)
	//	{
	//		String wikitext = "-10123456789";
	//		int commentIndexBegin = wikitext.indexOf("-101");
	//		int commentIndexEnd = wikitext.indexOf("67");
	//		String begin = wikitext.substring(0, commentIndexBegin);
	//		String middle = wikitext.substring(commentIndexBegin + "-101".length(),
	//				commentIndexEnd);
	//		String end = wikitext.substring(commentIndexEnd + "67".length());
	//		System.out.println(begin + middle + end);
	//	}
}
