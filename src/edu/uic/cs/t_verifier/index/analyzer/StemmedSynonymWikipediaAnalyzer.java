package edu.uic.cs.t_verifier.index.analyzer;

import java.io.IOException;
import java.io.Reader;

import lia.analysis.AnalyzerUtils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;

import edu.uic.cs.t_verifier.index.synonym.SynonymEngine;
import edu.uic.cs.t_verifier.index.synonym.SynonymFilter;
import edu.uic.cs.t_verifier.index.synonym.WordNetSynonymEngine2;
import edu.uic.cs.t_verifier.misc.Config;

public abstract class StemmedSynonymWikipediaAnalyzer extends Analyzer
{
	private SynonymEngine engine;

	public StemmedSynonymWikipediaAnalyzer() throws IOException
	{
		// this.engine = new WordNetSynonymEngine(Config.WORDNET_INDEX_FOLDER);
		this.engine = new WordNetSynonymEngine2(Config.WORDNET_FOLDER);
	}

	public TokenStream tokenStream(String fieldName, Reader reader)
	{
		StopFilter stopFilter = new StopFilter(
				Config.LUCENE_VERSION,
				new LowerCaseFilter(Config.LUCENE_VERSION, new StandardFilter(
						Config.LUCENE_VERSION, new WikipediaTokenizer(reader))),
				StopAnalyzer.ENGLISH_STOP_WORDS_SET, false);
		stopFilter.setEnablePositionIncrements(false);

		TokenStream result = new PorterStemFilter(new SynonymFilter(
				new NumberFormatFilter(stopFilter), engine));

		return result;
	}

	@Override
	public abstract int getPositionIncrementGap(String fieldName);

	public static void main(String[] args) throws Exception
	{
		StemmedSynonymWikipediaAnalyzer analyzer = new StemmedSynonymWikipediaAnalyzer()
		{
			@Override
			public int getPositionIncrementGap(String fieldName)
			{
				return 0;
			}

		};

		// WikipediaTokenizer cann't distinguish citation, so we need to remove those [xxx] first
		// String INPUT = "luminosity luminous the brightest star visible from earth is light nuclear states state electric guitar actress primary primarily language lead Alexey Leonov aleksei leonov place McDonald's hamburger 1964-10-16 Australia smaller 100 2,000, wanghong.nanjing@gmail.com The quick brown fox[1][2] jumps over[3] the lazy[citation needed] dog stemming algorithms sunk age aging";
		// String INPUT = "1964-10-16 1,000, 90s";
		// String INPUT = "Population luminosity and its proximity to the observer. Below are listed the 91 brightest individual stars in order of their apparent magnitudes in the visible spectrum as seen from Earth";
		String INPUT = "history famous famou histori mount climb mountain mexico";

		AnalyzerUtils.displayTokensWithPositions(analyzer, INPUT);
	}
}
