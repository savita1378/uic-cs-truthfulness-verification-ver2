package edu.uic.cs.t_verifier.index.analyzer;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;

class NumberFormatFilter extends TokenFilter
{
	private static final String[] DATE_FORMATS = { /*"yyyy.MM.dd",*/
	"yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy", "yyyy/MM/dd", /*"yyyy.MM",*/
	"yyyy-MM", "yyyy/MM", "MM/yyyy" };
	//	private static final String DATE_FORMAT_DELIMITER = "./-";

	private final CharTermAttribute termAtt;
	private final PositionIncrementAttribute posIncrAtt;

	private ArrayList<String> yearSynonym = new ArrayList<String>();
	private AttributeSource.State current;

	NumberFormatFilter(TokenStream input)
	{
		super(input);
		this.termAtt = addAttribute(CharTermAttribute.class);
		this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	}

	@Override
	public boolean incrementToken() throws IOException
	{
		if (!yearSynonym.isEmpty())
		{
			String part = yearSynonym.remove(0);
			restoreState(current);
			termAtt.copyBuffer(part.toCharArray(), 0, part.length());
			posIncrAtt.setPositionIncrement(0);
			return true;
		}

		if (!input.incrementToken())
		{
			return false;
		}

		String token = termAtt.toString();
		token = token.replace(",", "");

		String year = extractYear(token);
		if (year != null)
		{
			current = captureState();
			yearSynonym.add(year);
			// last two digits of year
			//			System.out
			//					.println("***************** [" + token + ">" + year + "]");
			// TODO, not only support 19XX, 20XX
			if (year.length() == 4
					&& (year.startsWith("19") || year.startsWith("20")))
			{
				yearSynonym
						.add(year.substring(year.length() - 2, year.length()));
			}
		}
		else
		{
			termAtt.copyBuffer(token.toCharArray(), 0, token.length());
		}

		return true;
	}

	private String extractYear(String token)
	{
		String year = null;
		try
		{
			Date date = DateUtils.parseDateStrictly(token, DATE_FORMATS);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			int yearInt = calendar.get(Calendar.YEAR);

			return "" + yearInt;
		}
		catch (ParseException e)
		{
			// ignore
		}

		return year;
	}

}
