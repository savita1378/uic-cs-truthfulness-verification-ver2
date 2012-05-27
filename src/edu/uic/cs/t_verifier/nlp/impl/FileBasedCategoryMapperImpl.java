package edu.uic.cs.t_verifier.nlp.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import edu.uic.cs.t_verifier.common.AbstractWordOperations;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.nlp.CategoryMapper;
import edu.uic.cs.t_verifier.nlp.StatementTypeIdentifier.StatementType;

public class FileBasedCategoryMapperImpl extends AbstractWordOperations
		implements CategoryMapper
{
	private static FileBasedCategoryMapperImpl WORD_OPERATIONS = new FileBasedCategoryMapperImpl()
	{
	};

	private static final Set<String> CATEGORY_KEYWORDS_LOCATION;
	private static final Set<String> CATEGORY_KEYWORDS_PERSON;
	private static final Set<String> CATEGORY_KEYWORDS_ORGANIZATION;

	static
	{
		CATEGORY_KEYWORDS_LOCATION = loadKeywords(ClassLoader
				.getSystemResource("categoriesKeywords/location.keywords")
				.getPath());

		CATEGORY_KEYWORDS_PERSON = loadKeywords(ClassLoader.getSystemResource(
				"categoriesKeywords/person.keywords").getPath());

		CATEGORY_KEYWORDS_ORGANIZATION = loadKeywords(ClassLoader
				.getSystemResource("categoriesKeywords/organization.keywords")
				.getPath());
	}

	private static Set<String> loadKeywords(String fileName)
	{
		try
		{
			@SuppressWarnings("unchecked")
			List<String> keywords = FileUtils.readLines(new File(fileName));
			HashSet<String> result = new HashSet<String>();
			for (String keyword : keywords)
			{
				// stem the keyword
				result.add(WORD_OPERATIONS.stem(keyword));
			}

			return result;
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	@Override
	public StatementType mapCategoryIntoStatementType(String category)
	{
		List<String> categoryTerms = standardAnalyzeUsingDefaultStopWords(category);
		HashSet<String> stemmedTerms = new HashSet<String>();
		for (String term : categoryTerms)
		{
			stemmedTerms.add(stem(term));
		}

		if (!Collections.disjoint(CATEGORY_KEYWORDS_LOCATION, stemmedTerms))
		{
			return StatementType.LOCATION;
		}
		else if (!Collections.disjoint(CATEGORY_KEYWORDS_PERSON, stemmedTerms))
		{
			return StatementType.PERSON;
		}
		else if (!Collections.disjoint(CATEGORY_KEYWORDS_ORGANIZATION,
				stemmedTerms))
		{
			return StatementType.ORGANIZATION;
		}

		return StatementType.OTHER;
	}

	@Override
	public StatementType mapCategoriesIntoStatementType(
			Collection<String> categories)
	{
		HashMap<StatementType, Integer> countByStatementType = new HashMap<StatementType, Integer>();

		for (String category : categories)
		{
			StatementType statementType = mapCategoryIntoStatementType(category);
			if (statementType == StatementType.OTHER)
			{
				continue;
			}

			Integer count = countByStatementType.get(statementType);
			if (count == null)
			{
				countByStatementType.put(statementType, Integer.valueOf(1));
			}
			else
			{
				countByStatementType.put(statementType,
						Integer.valueOf(count.intValue() + 1));
			}
		}

		if (countByStatementType.isEmpty())
		{
			return StatementType.OTHER;
		}

		int maxCount = 0;
		StatementType result = null;
		for (Entry<StatementType, Integer> entry : countByStatementType
				.entrySet())
		{
			int count = entry.getValue().intValue();
			if (count > maxCount)
			{
				maxCount = count;
				result = entry.getKey();
			}
		}

		Assert.notNull(result);
		return result;
	}

}
