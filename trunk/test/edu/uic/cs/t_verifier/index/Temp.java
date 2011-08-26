package edu.uic.cs.t_verifier.index;

import java.io.File;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.uic.cs.t_verifier.index.IndexConstants;
import edu.uic.cs.t_verifier.misc.Config;

public class Temp
{
	public static void main(String[] args) throws Exception
	{
		Directory directory = FSDirectory.open(new File(Config.INDEX_FOLDER));
		IndexSearcher searcher = new IndexSearcher(directory);
		Document document = searcher.doc(0);

		String[] tus = document
				.getValues(IndexConstants.FIELD_NAME__ALL_TOPIC_UNIT_WORDS);
		for (String tu : tus)
		{
			System.out.println(tu);
		}

		TermFreqVector vector = searcher.getIndexReader().getTermFreqVector(0,
				IndexConstants.FIELD_NAME__ALL_TOPIC_UNIT_WORDS);
		for (String term : vector.getTerms())
		{
			System.out.println(term);
		}
	}
}
