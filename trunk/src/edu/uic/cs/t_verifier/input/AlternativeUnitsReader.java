package edu.uic.cs.t_verifier.input;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.GeneralException;

public class AlternativeUnitsReader
{
	private static final String AU_LEFT_SYMBOL = "['";
	private static final int AU_LEFT_SYMBOL_LENGTH = AU_LEFT_SYMBOL.length();
	private static final String AU_RIGHT_SYMBOL = "']";

	private static final String INPUT_FOLDER_NAME = "input";

	private static String INPUT_FOLDER_PATH = AlternativeUnitsReader.class
			.getClassLoader().getResource(".").getFile()
			+ File.separator + INPUT_FOLDER_NAME + File.separator;

	private static final char[] DELIMITERS = " \t\n\r\f".toCharArray();

	public static List<Statement> parseAllStatementsFromInputFiles()
	{
		StatementCache.reflash();

		File inputFolder = new File(INPUT_FOLDER_PATH);
		Assert.isTrue(inputFolder.isDirectory());

		File[] inputFiles = inputFolder.listFiles();
		if (inputFiles == null)
		{
			return Collections.emptyList();
		}

		for (File file : inputFiles)
		{
			Collection<Statement> statements = parseStatementsFromOneInputFile(file);
			StatementCache.addAll(statements);
		}

		return StatementCache.retrieveAllCachedStatements();
	}

	private static Collection<Statement> parseStatementsFromOneInputFile(
			File file)
	{
		TreeMap<Integer, Statement> statementsById = new TreeMap<Integer, Statement>();
		try
		{
			@SuppressWarnings("unchecked")
			List<String> lines = FileUtils.readLines(file);

			for (String line : lines)
			{
				int idIndexEnd = StringUtils.indexOfAny(line, DELIMITERS);
				Assert.isTrue(idIndexEnd > 0);
				Integer id = Integer.valueOf(line.substring(0, idIndexEnd)
						.trim());
				Statement statement = statementsById.get(id);
				if (statement == null)
				{
					statement = parseStatement(id, line, idIndexEnd);
					statementsById.put(id, statement);
				}
				else
				{
					statement.addAlternativeUnit(parseAlternativeUnit(line));
				}

			}

		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}

		return statementsById.values();
	}

	private static Statement parseStatement(Integer id, String line,
			int idIndexEnd)
	{
		int begin = idIndexEnd;
		int end = line.lastIndexOf(AU_LEFT_SYMBOL);

		String statementString = line.substring(begin, end).trim();
		String alternativeUnit = parseAlternativeUnit(line, end);

		int auIndex = statementString.indexOf(alternativeUnit);
		Assert.isTrue(auIndex >= 0, "Can not mathch AU[" + alternativeUnit
				+ "] in [" + statementString + "]. ");

		String left = statementString.substring(0, auIndex).trim();
		String right = statementString.substring(
				auIndex + alternativeUnit.length()).trim();

		Statement result = new Statement(id, left, right);
		result.addAlternativeUnit(alternativeUnit);

		return result;
	}

	private static String parseAlternativeUnit(String line)
	{
		int begin = line.lastIndexOf(AU_LEFT_SYMBOL);
		return parseAlternativeUnit(line, begin);

	}

	private static String parseAlternativeUnit(String line, int begin)
	{
		int end = line.lastIndexOf(AU_RIGHT_SYMBOL);
		return line.substring(begin + AU_LEFT_SYMBOL_LENGTH, end);
	}
}
