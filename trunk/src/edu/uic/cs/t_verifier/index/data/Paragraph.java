package edu.uic.cs.t_verifier.index.data;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class Paragraph extends AbstractContent
{
	private List<String> sentences = null;

	public List<String> getSentences()
	{
		if (sentences != null)
		{
			return sentences;
		}

		String[] sentenceArray = StringUtils.split(getText(), "\n.?!");

		sentences = Arrays.asList(sentenceArray);

		return sentences;
	}

	//	public static void main(String[] args)
	//	{
	//		String paragraph = "hello! this is Hong Wang. how are you? I'm fine, and you?";
	//		String[] sentenceArray = StringUtils.split(paragraph, ".?!");
	//		for (String sentence : sentenceArray)
	//		{
	//			System.out.println(sentence);
	//		}
	//
	//	}
}
