package edu.uic.cs.t_verifier.index.data;

import java.util.ArrayList;
import java.util.List;


public class Segment
{
	private Heading heading = null;

	// TODO here we don't consider the order of paragraphs and tables
	private List<Paragraph> paragraphs = null;
	private List<Table> tables = null;

	public void addParagraph(Paragraph paragraph)
	{
		if (paragraphs == null)
		{
			paragraphs = new ArrayList<Paragraph>();
		}

		paragraphs.add(paragraph);
	}

	public void addTable(Table table)
	{
		if (tables == null)
		{
			tables = new ArrayList<Table>();
		}

		tables.add(table);
	}

	public void setHeading(Heading heading)
	{
		this.heading = heading;
	}

	public Heading getHeading()
	{
		return heading;
	}

	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();

		if (heading != null)
		{
			stringBuilder.append(heading).append("\n");
		}

		if (paragraphs != null)
		{
			for (Paragraph paragraph : paragraphs)
			{
				stringBuilder.append(paragraph).append("\n");
			}
		}

		if (tables != null)
		{
			for (Table table : tables)
			{
				stringBuilder.append(table).append("\n");
			}
		}

		return stringBuilder.toString();
	}

	public List<Paragraph> getParagraphs()
	{
		return paragraphs;
	}

	public List<Table> getTables()
	{
		return tables;
	}
}
