package edu.uic.cs.t_verifier.nlp;

import java.util.List;

public interface TrueCaser
{
	String restoreCases(String sentence);

	String restoreCases(String sentence, List<List<String>> possibleNounPhrases);
}
