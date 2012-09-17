package edu.uic.cs.t_verifier.nlp.compare;

import java.util.ArrayList;
import java.util.List;

import edu.uic.cs.t_verifier.TrecTopicsReaderWrapper;
import edu.uic.cs.t_verifier.nlp.impl.HybridTrueCaserImpl;

public class TestHybridTrueCaserImpl extends AbstractTrueCaseTester
{
	private HybridTrueCaserImpl trueCaser = new HybridTrueCaserImpl();

	protected List<List<String>> getTrueCaseTokens(String textStringInLowerCase)
	{
		String trueCased = trueCaser.restoreCases(textStringInLowerCase);

		return getOriginalCasedTokens(trueCased);
	}

	protected void commit()
	{
		trueCaser.commitCache();
	}

	public static void main(String[] args)
	{
		TestHybridTrueCaserImpl tester = new TestHybridTrueCaserImpl();

		TrecTopicsReaderWrapper trecTopicsReader = new TrecTopicsReaderWrapper();
		List<String> testSequences = new ArrayList<String>();

		//		testSequences.addAll(trecTopicsReader
		//				.readDescriptions("04.robust.testset"));
		testSequences.addAll(trecTopicsReader
				.readDescriptions("08.qa.questions.txt"));
		testSequences.addAll(trecTopicsReader
				.readDescriptions("09.qa.questions.txt"));

		try
		{
			tester.evaluate(testSequences);
		}
		finally
		{
			tester.commit(); // DO NOT forget this!
		}
	}
}
