package edu.uic.cs.t_verifier.nlp;

import java.util.Set;

import edu.mit.jwi.item.POS;

public interface WordNetReader
{
	Set<String> retrieveSynonyms(String term, Set<String> excludeTerms);

	/**
	 * Some term may be capitalized in the WordNet. 
	 * So our goal is to find such 'standard case' in WordNet.
	 * 
	 * @return
	 */
	String retrieveTermInStandardCase(String term, POS pos);
}
