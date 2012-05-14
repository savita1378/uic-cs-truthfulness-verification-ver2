package edu.uic.cs.t_verifier.nlp;

import java.util.HashMap;
import java.util.List;

import edu.uic.cs.t_verifier.input.data.Statement;

public interface NLPAnalyzer
{

	String retrieveTopicTermIfSameTypeAsAU(Statement statement);

	String retrieveTopicTermIfSameTypeAsAU(String sentence,
			String alternativeUnit);

	HashMap<String, String> parseSentenceIntoTermsWithPosTag(String sentence,
			List<String> resultTerms);

	boolean hasAlternativeUnitDoneSomething(Statement statement);

	boolean hasAlternativeUnitDoneSomething(String sentence,
			String alternativeUint);
}