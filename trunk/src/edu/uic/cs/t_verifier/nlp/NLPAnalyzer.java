package edu.uic.cs.t_verifier.nlp;

import edu.uic.cs.t_verifier.input.data.Statement;

public interface NLPAnalyzer
{

	String retrieveTopicTermIfSameTypeAsAU(Statement statement);

}