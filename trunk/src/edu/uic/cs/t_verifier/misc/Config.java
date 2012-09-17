package edu.uic.cs.t_verifier.misc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.lucene.search.Similarity;
import org.apache.lucene.util.Version;

import edu.uic.cs.t_verifier.score.StatementSimilarity;

public class Config
{
	static
	{
		// Since Config be used in both StatementIndexWriter and AbstractStatementScorer
		// this class is surely be loaded before those classes above
		Similarity.setDefault(new StatementSimilarity());
	}

	private static final Properties CONFIG = new Properties();
	static
	{
		InputStream inStream = ClassLoader
				.getSystemResourceAsStream("config.properties");

		try
		{
			CONFIG.load(inStream);
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	public static final boolean ALLOW_REBUILD_INDEX = Boolean
			.parseBoolean(CONFIG.getProperty("allow_rebuild_index"));

	public static final Version LUCENE_VERSION = Version.valueOf(CONFIG
			.getProperty("lucene_version"));

	public static long POLITENESS_INTERVAL = Long.parseLong(CONFIG
			.getProperty("politeness_interval"));

	////////////////////////////////////////////////////////////////////////////
	private static final String _IGNORING_COMMON_TEMPLATES = CONFIG
			.getProperty("ignoring_common_templates");
	private static final Set<String> _IGNORING_COMMON_TEMPLATES_SET = new HashSet<String>();
	static
	{
		StringTokenizer tokenizer = new StringTokenizer(
				_IGNORING_COMMON_TEMPLATES, "\t\n\r\f,");
		while (tokenizer.hasMoreTokens())
		{
			_IGNORING_COMMON_TEMPLATES_SET.add(tokenizer.nextToken());
		}
	}
	public static final String[] IGNORING_COMMON_TEMPLATES = _IGNORING_COMMON_TEMPLATES_SET
			.toArray(new String[_IGNORING_COMMON_TEMPLATES_SET.size()]);
	////////////////////////////////////////////////////////////////////////////

	public static final String INDEX_FOLDER = CONFIG
			.getProperty("index_folder");

	public static final String WORDNET_INDEX_FOLDER = CONFIG
			.getProperty("wordnet_index_folder");

	public static final int CONTENT_GAP = Integer.parseInt(CONFIG
			.getProperty("content_gap"));

	public static final int SEARCH_WINDOW = Integer.parseInt(CONFIG
			.getProperty("search_window"));

	public static final String WIKIPEDIACONTENTEXTRACTOR_CLASS_NAME = CONFIG
			.getProperty("WikipediaContentExtractor_class_name");

	public static final String WORDNET_FOLDER = CONFIG
			.getProperty("wordnet_folder");

	public static final double WORDNET_SYNSET_FREQUENCY_RATIO_TO_MAX_LOWER_BOUND = Double
			.parseDouble(CONFIG
					.getProperty("wordnet_synset_frequency_ratio_to_max_lower_bound"));

	public static final int MAX_WIKI_MATCHING_LENGTH = Integer.parseInt(CONFIG
			.getProperty("max_wiki_matching_length"));

}
