package edu.uic.cs.t_verifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.formats.Conll02NameSampleStream;
import opennlp.tools.formats.Conll03NameSampleStream;
import opennlp.tools.formats.Conll03NameSampleStream.LANGUAGE;
import opennlp.tools.namefind.NameSample;
import edu.uic.cs.t_verifier.misc.GeneralException;

public class Conll03NERReader
{
	private static final String CONLL_TESTDATA_FOLDER_NAME = "ner_conll03";

	private static final String CONLL_TESTDATA_FOLDER_PATH = Conll03NERReader.class
			.getClassLoader().getResource(".").getFile()
			+ File.separator + CONLL_TESTDATA_FOLDER_NAME + File.separator;

	private static int TYPES_TO_GENERATE = 0;
	static
	{
		TYPES_TO_GENERATE = TYPES_TO_GENERATE
				| Conll02NameSampleStream.GENERATE_PERSON_ENTITIES;
		TYPES_TO_GENERATE = TYPES_TO_GENERATE
				| Conll02NameSampleStream.GENERATE_ORGANIZATION_ENTITIES;
		TYPES_TO_GENERATE = TYPES_TO_GENERATE
				| Conll02NameSampleStream.GENERATE_LOCATION_ENTITIES;
		TYPES_TO_GENERATE = TYPES_TO_GENERATE
				| Conll02NameSampleStream.GENERATE_MISC_ENTITIES;
	}

	//	private static String PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

	public List<String> readSentences(String fileName)
	{
		Conll03NameSampleStream sampleStream = new Conll03NameSampleStream(
				LANGUAGE.EN, CmdLineUtil.openInFile(new File(
						CONLL_TESTDATA_FOLDER_PATH + fileName)),
				TYPES_TO_GENERATE);

		List<String> result = new ArrayList<String>();
		NameSample nameSample = null;
		try
		{
			while ((nameSample = sampleStream.read()) != null)
			{
				String[] termsArrayInSentence = nameSample.getSentence();
				String sentence = concatenateTerms(termsArrayInSentence);

				sentence = sentence.replace("$ ", " $");

				result.add(sentence);
			}
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}

		return result;
	}

	private String concatenateTerms(String[] termsArrayInSentence)
	{
		if (termsArrayInSentence == null)
		{
			return null;
		}

		if (termsArrayInSentence.length == 0)
		{
			return "";
		}

		StringBuilder sb = new StringBuilder(termsArrayInSentence[0]);
		for (int index = 1; index < termsArrayInSentence.length; index++)
		{
			String term = termsArrayInSentence[index];
			if (term == null || term.length() == 0)
			{
				continue;
			}

			if (!isPunctuation(term))
			{
				sb.append(" ");
			}

			// TODO the true-caser could replace "Centre" with "Center"
			if (term.equals("organisation"))
			{
				term = "organization";
			}
			else if (term.equals("Organisation"))
			{
				term = "Organization";
			}
			else if (term.equals("centre"))
			{
				term = "center";
			}
			else if (term.equals("Centre"))
			{
				term = "Center";
			}

			sb.append(term);
		}

		return sb.toString();

	}

	private boolean isPunctuation(String term)
	{
		return (term.length() == 1 && !Character
				.isLetterOrDigit(term.charAt(0)))
				|| term.equalsIgnoreCase("'s");
	}

	public static void main(String[] args)
	{
		String fileName = "eng.testa";
		Conll03NERReader reader = new Conll03NERReader();

		List<String> sentences = reader.readSentences(fileName);
		for (String sentence : sentences)
		{
			System.out.println(sentence);
		}

		System.out.println("=================================================");
		System.out.println("Sentence No.:\t" + sentences.size());
	}

	//	public static void main(String[] args) throws Exception
	//	{
	//		String fileName = "eng.testa";
	//
	//		Conll03NameSampleStream sampleStream = new Conll03NameSampleStream(
	//				LANGUAGE.EN, CmdLineUtil.openInFile(new File(
	//						CONLL_TESTDATA_FOLDER_PATH + fileName)),
	//				TYPES_TO_GENERATE);
	//
	//		NameSample nameSample = null;
	//		while ((nameSample = sampleStream.read()) != null)
	//		{
	//			System.out.println(Arrays.asList(nameSample.getSentence()));
	//			System.out.println(Arrays.asList(nameSample.getNames()));
	//			System.out.println();
	//		}
	//	}

}
