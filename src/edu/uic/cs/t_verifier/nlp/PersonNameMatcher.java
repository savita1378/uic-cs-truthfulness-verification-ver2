package edu.uic.cs.t_verifier.nlp;

public interface PersonNameMatcher
{
	boolean isName(String firstName, String lastName);

	boolean isName(String firstName, String middleName, String lastName);
}
