package edu.uic.cs.t_verifier.index.synonym;

/**
 * Copyright Manning Publications Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific lan      
*/

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.PorterStemmerExporter;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.uic.cs.t_verifier.score.AllDocCollector;

// From chapter 9
public class WordNetSynonymEngine implements SynonymEngine
{
	private IndexSearcher searcher;
	private Directory fsDir;
	private PorterStemmerExporter stemmer;

	public WordNetSynonymEngine(String index) throws IOException
	{
		fsDir = FSDirectory.open(new File(index));
		searcher = new IndexSearcher(fsDir);
		stemmer = new PorterStemmerExporter();
	}

	public void close() throws IOException
	{
		searcher.close();
		fsDir.close();
	}

	public String[] getSynonyms(String word) throws IOException
	{
		// List<String> synList = new ArrayList<String>();
		Set<String> synList = new TreeSet<String>();

		AllDocCollector collector = new AllDocCollector(); // #A

		String stemmedWord = stemmer.stem(word);
		// System.out.print("\n>>>>> " + word + " " + stemmedWord);

		Query originalWordQuery = new TermQuery(new Term("word", word));
		Query stemmedWordQuery = new TermQuery(new Term("word", stemmedWord));
		BooleanQuery query = new BooleanQuery();
		query.add(originalWordQuery, Occur.SHOULD);
		query.add(stemmedWordQuery, Occur.SHOULD);

		searcher.search(query, collector);

		for (ScoreDoc hit : collector.getHits())
		{ // #B
			Document doc = searcher.doc(hit.doc);

			String[] values = doc.getValues("syn");

			for (String syn : values)
			{ // #C
				synList.add(syn);
			}
		}

		return synList.toArray(new String[0]);
	}
}

/*
  #A Collect every matching document
  #B Iterate over matching documents
  #C Record synonyms
*/
