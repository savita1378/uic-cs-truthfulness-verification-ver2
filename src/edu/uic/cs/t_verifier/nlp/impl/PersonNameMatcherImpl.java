package edu.uic.cs.t_verifier.nlp.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.nlp.PersonNameMatcher;
import edu.uic.cs.t_verifier.nlp.impl.NLPAnalyzerImpl4.RecursiveMatcher;

public class PersonNameMatcherImpl implements PersonNameMatcher,
		RecursiveMatcher<String>
{
	private static final Set<String> FIRST_NAMES;
	private static final Set<String> LAST_NAMES;

	static
	{
		FIRST_NAMES = loadNames(ClassLoader.getSystemResource(
				"personNames/NameList.Census.FirstName.Male").getPath());
		FIRST_NAMES.addAll(loadNames(ClassLoader.getSystemResource(
				"personNames/NameList.Census.FirstName.Female").getPath()));

		LAST_NAMES = loadNames(ClassLoader.getSystemResource(
				"personNames/NameList.Census.LastName").getPath());
	}

	//private int gramLength;
	//
	//	public PersonNameMatcherImpl(int gramLength)
	//	{
	//		this.gramLength = gramLength;
	//	}

	private static Set<String> loadNames(String fileName)
	{
		try
		{
			@SuppressWarnings("unchecked")
			List<String> names = FileUtils.readLines(new File(fileName));

			return new HashSet<String>(names);
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	@Override
	public boolean isName(String firstName, String lastName)
	{
		firstName = firstName.toLowerCase();
		lastName = lastName.toLowerCase();

		return FIRST_NAMES.contains(firstName) && LAST_NAMES.contains(lastName);
	}

	@Override
	public boolean isName(String firstName, String middleName, String lastName)
	{
		return isName(firstName, lastName)
				&& (FIRST_NAMES.contains(middleName) || LAST_NAMES
						.contains(middleName));
	}

	@Override
	public boolean isName(String name)
	{
		return FIRST_NAMES.contains(name) || LAST_NAMES.contains(name);
	}

	private String matchedName = null;

	@Override
	public boolean isMatched(
			List<Entry<String, String>> currentLevelPosTagsByTermSequence)
	{
		matchedName = null;

		int length = currentLevelPosTagsByTermSequence.size();
		//		if (gramLength < length)
		//		{
		//			return false;
		//		}

		if (length == 2)
		{
			String firstName = currentLevelPosTagsByTermSequence.get(0)
					.getKey();
			String lastName = currentLevelPosTagsByTermSequence.get(1).getKey();

			if (isName(firstName, lastName))
			{
				matchedName = StringUtils.capitalize(firstName) + " "
						+ StringUtils.capitalize(lastName);
				return true;
			}
		}
		else if (length == 3)
		{
			String firstName = currentLevelPosTagsByTermSequence.get(0)
					.getKey();
			String middleName = currentLevelPosTagsByTermSequence.get(1)
					.getKey();
			String lastName = currentLevelPosTagsByTermSequence.get(2).getKey();

			if (isName(firstName, middleName, lastName))
			{
				matchedName = StringUtils.capitalize(firstName) + " "
						+ StringUtils.capitalize(middleName) + " "
						+ StringUtils.capitalize(lastName);
				return true;
			}
		}

		return false;
	}

	@Override
	public String getMatchedInfo()
	{
		Assert.notNull(matchedName);
		return matchedName;
	}

}
