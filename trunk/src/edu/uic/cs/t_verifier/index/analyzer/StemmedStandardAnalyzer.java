package edu.uic.cs.t_verifier.index.analyzer;

import java.io.Reader;

import lia.analysis.AnalyzerUtils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import edu.uic.cs.t_verifier.misc.Config;

public class StemmedStandardAnalyzer extends Analyzer
{
	public TokenStream tokenStream(String fieldName, Reader reader)
	{
		StopFilter stopFilter = new StopFilter(Config.LUCENE_VERSION,
				new LowerCaseFilter(Config.LUCENE_VERSION, new StandardFilter(
						Config.LUCENE_VERSION, new StandardTokenizer(
								Config.LUCENE_VERSION, reader))),
				StopAnalyzer.ENGLISH_STOP_WORDS_SET);
		stopFilter.setEnablePositionIncrements(false);

		TokenStream result = new PorterStemFilter(new NumberFormatFilter(
				stopFilter));

		return result;
	}

	public static void main(String[] args) throws Exception
	{
		StemmedStandardAnalyzer analyzer = new StemmedStandardAnalyzer();

		String INPUT = "100 2,000, wanghong.nanjing@gmail.com The quick brown fox jumps over the lazy dog stemming algorithms sunk age aging";
		AnalyzerUtils.displayTokensWithPositions(analyzer, INPUT);
	}
}
