package edu.uic.cs.t_verifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;

import edu.uic.cs.t_verifier.misc.GeneralException;

public class TrecTopicsReaderWrapper
{
	private static final String TREC_TOPICS_FOLDER_NAME = "trecTopics";

	private static final String TREC_TOPICS_FOLDER_PATH = TrecTopicsReaderWrapper.class
			.getClassLoader().getResource(".").getFile()
			+ File.separator + TREC_TOPICS_FOLDER_NAME + File.separator;

	private TrecTopicsReader trecTopicsReader = new TrecTopicsReader();

	public List<String> readDescriptions(String fileName)
	{
		QualityQuery[] trecQueries = null;
		try
		{
			trecQueries = trecTopicsReader.readQueries(new BufferedReader(
					new InputStreamReader(new FileInputStream(
							TREC_TOPICS_FOLDER_PATH + fileName), "UTF-8")));
		}
		catch (Exception e)
		{
			throw new GeneralException(e);
		}

		if (trecQueries == null)
		{
			return Collections.emptyList();
		}

		List<String> result = new ArrayList<String>(trecQueries.length);
		for (QualityQuery query : trecQueries)
		{
			result.add(query.getValue("description").replaceAll(" {2,}", " "));
		}

		return result;
	}

	public static void main(String[] args)
	{
		TrecTopicsReaderWrapper topicsReaderWrapper = new TrecTopicsReaderWrapper();
		int count = 1;
		for (String des : topicsReaderWrapper.readDescriptions("04.testset"))
		{
			System.out.println(count++ + "\t" + des);
		}
	}
}
