package edu.uic.cs.t_verifier.nlp;

import java.util.HashMap;
import java.util.List;

import edu.uic.cs.t_verifier.input.data.Statement;

public interface NLPAnalyzer
{

	String retrieveTopicTermIfSameTypeAsAU(Statement statement);

	HashMap<String, String> parseSentenceIntoTermsWithPosTag(String sentence,
			List<String> resultTerms);

	boolean hasAlternativeUnitDoneSomething(Statement statement);
}