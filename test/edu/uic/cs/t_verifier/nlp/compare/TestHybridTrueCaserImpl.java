package edu.uic.cs.t_verifier.nlp.compare;

import java.util.ArrayList;
import java.util.List;

import edu.uic.cs.t_verifier.Conll03NERReader;
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
		List<String> testSequences = new ArrayList<String>();

		//		TrecTopicsReaderWrapper trecTopicsReader = new TrecTopicsReaderWrapper();
		//		testSequences.addAll(trecTopicsReader
		//				.readDescriptions("04.robust.testset"));
		//		testSequences.addAll(trecTopicsReader
		//				.readDescriptions("08.qa.questions.txt"));
		//		testSequences.addAll(trecTopicsReader
		//				.readDescriptions("09.qa.questions.txt"));

		Conll03NERReader conll03nerReader = new Conll03NERReader();
		testSequences.addAll(conll03nerReader.readSentences("eng.testa"));

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
