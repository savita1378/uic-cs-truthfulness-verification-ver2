package edu.uic.cs.t_verifier.nlp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.nlp.StatementTypeIdentifier.StatementType;
import edu.uic.cs.t_verifier.nlp.impl.StatementTypeIdentifierImpl;

@SuppressWarnings("unchecked")
public class StatementTypeIdentifierEvaluator
{
	private static StatementTypeIdentifier typeIdentifier = new StatementTypeIdentifierImpl();
	private static final Random RANDOM = new Random();

	private static final int NUMBER_OF_WALK_THROUGH = 10;

	private static List<String> ALL_EXPECTED_TYPES = new ArrayList<String>();
	static
	{
		try
		{
			ALL_EXPECTED_TYPES
					.addAll(FileUtils
							.readLines(new File(
									getPathOfFile("StatementTypeIdentifierEvaluator.expected"))));
		}
		catch (IOException e)
		{
			throw new GeneralException(e);
		}
	}

	private static String getPathOfFile(String fileName)
	{
		return StatementTypeIdentifierEvaluator.class.getResource(fileName)
				.getPath();
	}

	private static double evaluate(int numberOfWalkThrough)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();

		double correctRatioSum = 0.0;
		for (int time = 0; time < numberOfWalkThrough; time++)
		{
			int correctCount = 0;

			for (int index = 0; index < statements.size(); index++)
			{
				Statement statement = statements.get(index);
				statement = randomlyKeepOneAlternative(statement);

				StatementType type = typeIdentifier.identifyType(statement);
				System.out.print("[" + type + "]");

				String expected = ALL_EXPECTED_TYPES.get(index);
				if (expected.equals(type.toString()))
				{
					System.out
							.println(" = ["
									+ expected
									+ "] √ ===========================================");
					correctCount++;
				}
				else
				{
					System.out
							.println(" ≠ ["
									+ expected
									+ "] × ===========================================");
				}
			}

			double correctRatio = ((double) correctCount) / statements.size();
			correctRatioSum += correctRatio;
			System.out.println(">>>>>>>>>>>  [" + (time + 1)
					+ "] Correct ratio: " + correctRatio + "\n\n");
		}

		return correctRatioSum / numberOfWalkThrough;
	}

	private static Statement randomlyKeepOneAlternative(Statement statement)
	{
		List<String> allAlternativeUnits = statement.getAlternativeUnits();

		int size = allAlternativeUnits.size();
		int randomIndex = RANDOM.nextInt(size);

		Statement result = new Statement(statement.getId(),
				statement.getTopicUnitLeft(), statement.getTopicUnitRight());

		result.addAlternativeUnit(allAlternativeUnits.get(randomIndex));

		return result;
	}

	public static void main(String[] args)
	{
		double averageCorrectRatio = evaluate(NUMBER_OF_WALK_THROUGH);

		System.out.println();
		System.out.println(">>>>> Average correct ratio: "
				+ averageCorrectRatio);
	}
}
