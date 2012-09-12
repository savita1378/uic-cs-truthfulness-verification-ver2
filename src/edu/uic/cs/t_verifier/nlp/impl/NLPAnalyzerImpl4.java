package edu.uic.cs.t_verifier.nlp.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import edu.uic.cs.t_verifier.nlp.TrueCaser;

public class NLPAnalyzerImpl4 extends NLPAnalyzerImpl3
{
	//	private static final Logger LOGGER = LogHelper
	//			.getLogger(NLPAnalyzerImpl4.class);

	private static Map<String, Entry<String, List<List<String>>>> RESTORED_SENTENCE_CACHE = new ConcurrentHashMap<String, Entry<String, List<List<String>>>>();

	private TrueCaser trueCaser = new RuleBasedTrueCaserImpl();

	protected String restoreWordCasesForSentence(String sentence,
			String alternativeUnit, List<List<String>> nounPhrases)
	{
		Entry<String, List<List<String>>> restoredSentenceAndNPs = RESTORED_SENTENCE_CACHE
				.get(sentence);
		if (restoredSentenceAndNPs == null)
		{
			synchronized (RESTORED_SENTENCE_CACHE)
			{
				restoredSentenceAndNPs = RESTORED_SENTENCE_CACHE.get(sentence);
				if (restoredSentenceAndNPs == null)
				{
					List<List<String>> nounPhrasesToStore = new ArrayList<List<String>>();
					String resultSentence = capitalizeProperNounTerms(sentence,
							nounPhrasesToStore);

					restoredSentenceAndNPs = new SimpleEntry<String, List<List<String>>>(
							resultSentence, nounPhrasesToStore);
					RESTORED_SENTENCE_CACHE.put(sentence,
							restoredSentenceAndNPs);
				}
			}
		}

		nounPhrases.addAll(restoredSentenceAndNPs.getValue());
		return restoredSentenceAndNPs.getKey();
	}

	@Override
	public String capitalizeProperNounTerms(String sentence,
			List<List<String>> possibleNounPhrases)
	{
		return trueCaser.restoreCases(sentence, possibleNounPhrases);
	}
}
