package edu.uic.cs;

import java.io.File;

import org.apache.commons.io.FileUtils;

import be.devijver.wikipedia.parser.ast.Content;
import be.devijver.wikipedia.parser.ast.ContentHolder;
import be.devijver.wikipedia.parser.ast.Document;
import be.devijver.wikipedia.parser.ast.Literal;
import be.devijver.wikipedia.parser.ast.SingleContentHolder;
import be.devijver.wikipedia.parser.ast.SpecialConstruct;
import be.devijver.wikipedia.parser.wikitext.MarkupParser;

public class TestMarkupParser
{
	public static void main(String[] args) throws Exception
	{
		String inputString = FileUtils
				.readFileToString(getFile("TestMarkupParser.txt"));
		inputString = inputString.replace("\r", "");
		//		System.out.println(inputString);

		Document doc = new MarkupParser(inputString).parseDocument();

		System.out.println("====================================");
		recursiveParseContents(doc.getContent(), 0);

		//		System.out.println("====================================");
		//
		//		StringWriter sw = new StringWriter();
		//		SmartLinkResolver smartLinkResolver = new SmartLinkResolver()
		//		{
		//			@Override
		//			public String resolve(String smartLink)
		//			{
		//				return "*" + smartLink + "*";
		//			}
		//		};
		//		//		Visitor visitor = new HtmlVisitor(sw, smartLinkResolver, true);
		//		//		new DefaultASTParser(new MarkupParser(inputString).parseDocument())
		//		//				.parse(visitor);
		//		Parser.toHtml(inputString, smartLinkResolver, sw);
		//		String html = sw.toString();
		//
		//		System.out.println(html);
	}

	private static void recursiveParseContents(Content[] contents, int indent)
	{
		for (Content content : contents)
		{
			if (content instanceof Literal)
			{
				System.out.println("=====> " + content);
			}

			if (content instanceof ContentHolder)
			{
				Content[] innerContents = ((ContentHolder) content)
						.getContent();
				recursiveParseContents(innerContents, indent + 1);

				for (int index = 0; index < indent; index++)
				{
					System.out.print('\t');
				}
				System.out.println(content != null ? content.getClass()
						: "null");
			}
			else if (content instanceof SingleContentHolder)
			{
				Content innerContent = ((SingleContentHolder) content)
						.getContent();
				recursiveParseContents(new Content[] { innerContent },
						indent + 1);

				for (int index = 0; index < indent; index++)
				{
					System.out.print('\t');
				}
				System.out.println(content != null ? content.getClass()
						: "null");
			}
			else if (content instanceof SpecialConstruct)
			{
				System.out.println("!!! "
						+ ((SpecialConstruct) content).getCharacter() + " || "
						+ ((SpecialConstruct) content).getContent());
				System.out.println(content.toString());
			}
			else
			{
				for (int index = 0; index < indent; index++)
				{
					System.out.print('\t');
				}

				System.out.println(content != null ? content.getClass()
						: "null");
				//				if (content != null)
				//				{
				//					System.out.print(content + " ");
				//				}
			}
		}
	}

	private static File getFile(String fileName)
	{
		String path = TestMarkupParser.class.getResource(fileName).getPath();
		return new File(path);
	}
}
