package edu.uic.cs.t_verifier.index.data;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class Table extends AbstractContent
{
	private List<String> sentences = null;

	public List<String> getSentences()
	{
		if (sentences != null)
		{
			return sentences;
		}

		String[] sentenceArray = StringUtils.split(getText(), "\n");

		sentences = Arrays.asList(sentenceArray);

		return sentences;
	}
}
