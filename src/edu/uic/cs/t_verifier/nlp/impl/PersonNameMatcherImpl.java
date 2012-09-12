package edu.uic.cs.t_verifier.nlp.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import edu.mit.jwi.item.POS;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.nlp.PersonNameMatcher;
import edu.uic.cs.t_verifier.nlp.WordNetReader;
import edu.uic.cs.t_verifier.nlp.impl.AbstractNLPOperations.RecursiveMatcher;

public class PersonNameMatcherImpl implements PersonNameMatcher,
		RecursiveMatcher<String>
{
	//	private static final Set<String> FIRST_NAMES;
	//	private static final Set<String> LAST_NAMES;
	//	static
	//	{
	//		FIRST_NAMES = loadNames(ClassLoader.getSystemResource(
	//				"personNames/NameList.Census.FirstName.Male").getPath());
	//		FIRST_NAMES.addAll(loadNames(ClassLoader.getSystemResource(
	//				"personNames/NameList.Census.FirstName.Female").getPath()));
	//
	//		LAST_NAMES = loadNames(ClassLoader.getSystemResource(
	//				"personNames/NameList.Census.LastName").getPath());
	//	}

	private static final Map<String, Double> FREQUENCIES_BY_MALE_FIRST_NAME;
	private static final Map<String, Double> FREQUENCIES_BY_FEMALE_FIRST_NAME;
	private static final Map<String, Double> FREQUENCIES_BY_LAST_NAME;
	static
	{
		FREQUENCIES_BY_MALE_FIRST_NAME = loadNamesAndFrequencies(ClassLoader
				.getSystemResource("personNames/dist.male.first").getPath());
		FREQUENCIES_BY_FEMALE_FIRST_NAME = loadNamesAndFrequencies(ClassLoader
				.getSystemResource("personNames/dist.female.first").getPath());
		FREQUENCIES_BY_LAST_NAME = loadNamesAndFrequencies(ClassLoader
				.getSystemResource("personNames/dist.all.last").getPath());
	}

	//private int gramLength;
	//
	//	public PersonNameMatcherImpl(int gramLength)
	//	{
	//		this.gramLength = gramLength;
	//	}

	//	private static Set<String> loadNames(String fileName)
	//	{
	//		try
	//		{
	//			@SuppressWarnings("unchecked")
	//			List<String> names = FileUtils.readLines(new File(fileName));
	//
	//			return new HashSet<String>(names);
	//		}
	//		catch (IOException e)
	//		{
	//			throw new GeneralException(e);
	//		}
	//	}

	private static Map<String, Double> loadNamesAndFrequencies(String fileName)
	{
		try
		{
			@SuppressWarnings("unchecked")
			List<String> lines = FileUtils.readLines(new File(fileName));
			HashMap<String, Double> result = new HashMap<String, Double>(
					lines.size());
			for (String line : lines)
			{
				String[] parts = line.split(" +");
				Assert.isTrue(parts.length == 4,
						"Actual is " + Arrays.toString(parts));

				result.put(parts[0].toLowerCase(Locale.US),
						Double.valueOf(parts[1]));
			}

			return result;
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	//	public static void main(String[] args)
	//	{
	//		for (Entry<String, Double> entry : FREQUENCIES_BY_MALE_FIRST_NAME
	//				.entrySet())
	//		{
	//			System.out.println(entry);
	//		}
	//
	//		System.out.println("======================================");
	//
	//		for (Entry<String, Double> entry : FREQUENCIES_BY_FEMALE_FIRST_NAME
	//				.entrySet())
	//		{
	//			System.out.println(entry);
	//		}
	//
	//		System.out.println("======================================");
	//
	//		for (Entry<String, Double> entry : FREQUENCIES_BY_LAST_NAME.entrySet())
	//		{
	//			System.out.println(entry);
	//		}
	//	}

	private WordNetReader wordNetReader = new WordNetReaderImpl();

	@Override
	public boolean isName(String firstName, String lastName)
	{
		return isFirstName(firstName) && isLastName(lastName);
	}

	private boolean isLastName(String lastName)
	{
		lastName = lastName.toLowerCase(Locale.US);
		return FREQUENCIES_BY_LAST_NAME.containsKey(lastName);
	}

	private boolean isFirstName(String firstName)
	{
		firstName = firstName.toLowerCase(Locale.US);
		return FREQUENCIES_BY_MALE_FIRST_NAME.containsKey(firstName)
				|| FREQUENCIES_BY_FEMALE_FIRST_NAME.containsKey(firstName);
	}

	@Override
	public boolean isName(String firstName, String middleName, String lastName)
	{
		return isName(firstName, lastName)
				&& (isFirstName(middleName) || isLastName(middleName));
	}

	@Override
	public boolean isName(String name)
	{
		return isFirstName(name) || isLastName(name);
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
		if (length == 1)
		{
			String name = currentLevelPosTagsByTermSequence.get(0).getKey()
					.toLowerCase();
			String termInWordNet = wordNetReader.retrieveTermInStandardCase(
					name, POS.NOUN); // use lowercase to search if capitalized return

			if (termInWordNet == null && isName(name)) // person name usually doesn't exist in WordNet
			{
				matchedName = StringUtils.capitalize(name);
				return true;
			}
		}
		else if (length == 2)
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

	@Override
	public NameType typeOf(String name)
	{
		if (name == null)
		{
			return NameType.NA;
		}

		boolean isFirst = isFirstName(name);
		boolean isLast = isLastName(name);

		if (isFirst && isLast)
		{
			return NameType.BOTH;
		}

		if (isFirst)
		{
			return NameType.FIRST;
		}

		if (isLast)
		{
			return NameType.LAST;
		}

		return NameType.NA;
	}

	@Override
	public Double getMaxFrequency(String name)
	{
		if (name == null)
		{
			return 0.0;
		}

		Double maleFirstFrequency = FREQUENCIES_BY_MALE_FIRST_NAME.get(name);
		if (maleFirstFrequency == null)
		{
			maleFirstFrequency = 0.0;
		}

		Double femaleFirstFrequency = FREQUENCIES_BY_FEMALE_FIRST_NAME
				.get(name);
		if (femaleFirstFrequency == null)
		{
			femaleFirstFrequency = 0.0;
		}

		Double lastFrequency = FREQUENCIES_BY_LAST_NAME.get(name);
		if (lastFrequency == null)
		{
			lastFrequency = 0.0;
		}

		return Math.max(lastFrequency,
				Math.max(maleFirstFrequency, femaleFirstFrequency));
	}

}
