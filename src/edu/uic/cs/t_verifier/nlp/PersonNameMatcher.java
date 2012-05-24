package edu.uic.cs.t_verifier.nlp;

public interface PersonNameMatcher
{
	boolean isName(String firstName, String lastName);

	boolean isName(String firstName, String middleName, String lastName);

	@Deprecated
	/**
	 * this one is not accurate, because it only compare one part of a name.
	 * @param name
	 * @return
	 */
	boolean isName(String name);
}
