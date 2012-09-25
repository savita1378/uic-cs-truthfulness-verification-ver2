package edu.uic.cs.wiki_verifier.wiki.local.misc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import edu.uic.cs.t_verifier.misc.GeneralException;

public class Config
{
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

	public static final String WIKIPEDIA_DUMP_FILE_PATH = CONFIG
			.getProperty("wikipedia_dump_file_path");

	public static final String WIKIPEDIA_INDEX_PATH = CONFIG
			.getProperty("wikipedia_index_path");
}
