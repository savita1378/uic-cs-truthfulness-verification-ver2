package edu.uic.cs.t_verifier.ml;

import java.util.List;
import java.util.Map.Entry;

public interface PersonNameIdentifier
{
	/**
	 * Notice: POS tag must be in basic form
	 * 
	 * @param tagsByTermInNP
	 * @param tagsByTermBeforeNP
	 * @param tagsByTermAfterNP
	 * @return the index of possible name term within the parameter "tagsByTermInNP"
	 */
	List<Integer> identifyNameTermsWithinNounPhrase(
			List<Entry<String, String>> tagsByTerm,
			Entry<String, String> tagsByTermBeforeNP,
			Entry<String, String> tagsByTermAfterNP);
}
