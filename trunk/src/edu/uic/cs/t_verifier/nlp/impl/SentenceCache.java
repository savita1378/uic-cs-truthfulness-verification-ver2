package edu.uic.cs.t_verifier.nlp.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import edu.uic.cs.t_verifier.html.data.MatchedQueryKey;
import edu.uic.cs.t_verifier.misc.GeneralException;

public class SentenceCache
{
	//	List<Entry<List<Entry<String, String>>, MatchedQueryKey>> matchedSequenceInfo = new ArrayList<Entry<List<Entry<String, String>>, MatchedQueryKey>>();
	//	List<Entry<Entry<String, String>, MatchedQueryKey>> matchedSingleInfo = new ArrayList<Entry<Entry<String, String>, MatchedQueryKey>>();

	private static boolean MODIFIED = false;

	private static final String WIKIPEDIA_CACHE_FILE_NAME = "cache/wikipedia.cache";

	private static Map<String, Entry<List<Entry<List<Entry<String, String>>, MatchedQueryKey>>, List<Entry<Entry<String, String>, MatchedQueryKey>>>> WIKIPEDIA_SENTENCE_CACHE;

	public Entry<List<Entry<List<Entry<String, String>>, MatchedQueryKey>>, List<Entry<Entry<String, String>, MatchedQueryKey>>> retrieveWikipeidaSentenceCache(
			String sentence)
	{
		setializeCacheIfNotExists();

		return WIKIPEDIA_SENTENCE_CACHE.get(sentence.toLowerCase());
	}

	private static final SentenceCache INSTANCE = new SentenceCache();

	private SentenceCache()
	{
	}

	public static SentenceCache getInstance()
	{
		return INSTANCE;
	}

	@SuppressWarnings("unchecked")
	private void setializeCacheIfNotExists()
	{
		if (WIKIPEDIA_SENTENCE_CACHE == null)
		{
			synchronized (SentenceCache.class)
			{
				if (WIKIPEDIA_SENTENCE_CACHE == null)
				{
					ObjectInputStream ois = null;
					try
					{
						BufferedInputStream stream = new BufferedInputStream(
								new FileInputStream(WIKIPEDIA_CACHE_FILE_NAME));
						ois = new ObjectInputStream(stream);

						WIKIPEDIA_SENTENCE_CACHE = (Map<String, Entry<List<Entry<List<Entry<String, String>>, MatchedQueryKey>>, List<Entry<Entry<String, String>, MatchedQueryKey>>>>) ois
								.readObject();
					}
					catch (Exception exception)
					{
						System.out.println("No cache. ");
						WIKIPEDIA_SENTENCE_CACHE = new HashMap<String, Map.Entry<List<Entry<List<Entry<String, String>>, MatchedQueryKey>>, List<Entry<Entry<String, String>, MatchedQueryKey>>>>();
					}
					finally
					{
						IOUtils.closeQuietly(ois);
					}
				}
			}
		}
	}

	public void addToCache(
			String sentence,
			List<Entry<List<Entry<String, String>>, MatchedQueryKey>> matchedSequenceInfo,
			List<Entry<Entry<String, String>, MatchedQueryKey>> matchedSingleInfo)
	{
		Entry<List<Entry<List<Entry<String, String>>, MatchedQueryKey>>, List<Entry<Entry<String, String>, MatchedQueryKey>>> pair = new AbstractMap.SimpleEntry<List<Entry<List<Entry<String, String>>, MatchedQueryKey>>, List<Entry<Entry<String, String>, MatchedQueryKey>>>(
				matchedSequenceInfo, matchedSingleInfo);

		setializeCacheIfNotExists();
		WIKIPEDIA_SENTENCE_CACHE.put(sentence.toLowerCase(), pair);

		MODIFIED = true;
	}

	public void writeCache()
	{
		if (!MODIFIED)
		{
			return;
		}

		FileUtils.deleteQuietly(new File(WIKIPEDIA_CACHE_FILE_NAME));

		ObjectOutputStream oos = null;
		try
		{
			BufferedOutputStream stream = new BufferedOutputStream(
					new FileOutputStream(WIKIPEDIA_CACHE_FILE_NAME));
			oos = new ObjectOutputStream(stream);
			oos.writeObject(WIKIPEDIA_SENTENCE_CACHE);
			oos.flush();
		}
		catch (Exception e)
		{
			System.out.println("Fail to write cache into disk! ");
			throw new GeneralException(e);
		}
		finally
		{
			IOUtils.closeQuietly(oos);
		}
	}

}
