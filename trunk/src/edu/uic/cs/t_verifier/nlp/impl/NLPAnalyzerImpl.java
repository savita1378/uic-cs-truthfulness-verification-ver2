package edu.uic.cs.t_verifier.nlp.impl;

import java.util.List;
import java.util.Locale;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.uic.cs.t_verifier.common.AbstractWordOperations;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.nlp.NLPAnalyzer;

public class NLPAnalyzerImpl extends AbstractWordOperations implements
		NLPAnalyzer
{
	private static final String GRAMMAR = "englishPCFG.ser.gz";

	private static final String AU_SUBSTITUTE = "AU";

	private LexicalizedParser lexicalizedParser;

	private GrammaticalStructureFactory grammaticalStructureFactory;

	public NLPAnalyzerImpl()
	{
		lexicalizedParser = new LexicalizedParser(GRAMMAR);
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		grammaticalStructureFactory = tlp.grammaticalStructureFactory();
	}

	/* (non-Javadoc)
	 * @see edu.uic.cs.t_verifier.nlp.NLPAnalyzer1#retrieveSubjectIfSameTypeAsAU(edu.uic.cs.t_verifier.input.data.Statement)
	 */
	@Override
	public String retrieveSubjectIfSameTypeAsAU(Statement statement)
	{
		String sentence = statement.getAllAlternativeStatements().get(0);
		String alternativeUint = statement.getAlternativeUnits().get(0);
		if (alternativeUint.contains(" ")) // if AU contains more than one term, use "AU" instead. 
		{
			sentence = sentence.replace(alternativeUint, AU_SUBSTITUTE);
			alternativeUint = AU_SUBSTITUTE.toLowerCase(Locale.US);
		}

		char[] chars = sentence.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		sentence = new String(chars);

		Tree parsedTree = lexicalizedParser.apply(sentence);
		GrammaticalStructure gs = grammaticalStructureFactory
				.newGrammaticalStructure(parsedTree);
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

		String subject = retrieveSubjectInternal(alternativeUint, tdl);
		if (subject == null)
		{
			return null;
		}

		for (TypedDependency typedDependency : tdl)
		{
			String gov = typedDependency.gov().toString("value")
					.toLowerCase(Locale.US);
			String dep = typedDependency.dep().toString("value")
					.toLowerCase(Locale.US);

			// if one is Subject, the other is alternative-unit, then return the Subject. 
			if ((subject.equals(gov) && alternativeUint.equals(dep))
					|| (subject.equals(dep) && alternativeUint.equals(gov)))
			{
				return subject;
			}
		}

		return null;
	}

	public String retrieveSubject(Statement statement)
	{
		String sentence = statement.getAllAlternativeStatements().get(0);
		String alternativeUint = statement.getAlternativeUnits().get(0);
		if (alternativeUint.contains(" ")) // if AU contains more than one term, use "AU" instead. 
		{
			sentence = sentence.replace(alternativeUint, AU_SUBSTITUTE);
			alternativeUint = AU_SUBSTITUTE.toLowerCase(Locale.US);
		}

		char[] chars = sentence.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		sentence = new String(chars);

		Tree parsedTree = lexicalizedParser.apply(sentence);
		GrammaticalStructure gs = grammaticalStructureFactory
				.newGrammaticalStructure(parsedTree);
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

		return retrieveSubjectInternal(alternativeUint, tdl);

	}

	private String retrieveSubjectInternal(String alternativeUint,
			List<TypedDependency> tdl)
	{
		for (TypedDependency typedDependency : tdl)
		{
			GrammaticalRelation grammaticalRelation = typedDependency.reln();
			if (grammaticalRelation.getShortName().equals("nsubj")
					|| grammaticalRelation.getShortName().equals("nsubjpass"))
			{
				String gov = typedDependency.gov().toString("value")
						.toLowerCase(Locale.US);
				String dep = typedDependency.dep().toString("value")
						.toLowerCase(Locale.US);

				if (!alternativeUint.equals(dep))
				{
					return dep;
				}

				if (!alternativeUint.equals(gov))
				{
					return gov;
				}
			}
		}

		return null;
	}

	/*public List<List<String>> retrieveNouns(Statement statement)
	{
		ArrayList<List<String>> nounsInStatement = new ArrayList<List<String>>();

		String sentence = statement.getAllAlternativeStatements().get(0);
		String alternativeUint = statement.getAlternativeUnits().get(0);
		List<String> alternativeUnitTerms = standardAnalyzeWithoutRemovingStopWords(alternativeUint);
		Tree parsedTree = lexicalizedParser.apply(sentence);

		boolean newSlot = true;
		for (TaggedWord taggedWord : parsedTree.taggedYield())
		{
			if (taggedWord.tag().startsWith("NN")
					&& !alternativeUnitTerms.contains(taggedWord.word()))
			{
				if (newSlot)
				{
					nounsInStatement.add(new ArrayList<String>());
				}

				nounsInStatement.get(nounsInStatement.size() - 1).add(
						taggedWord.word());

				newSlot = false;
			}
			else
			{
				newSlot = true;
			}
		}

		return nounsInStatement;
	}*/

	public static void main(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();

		NLPAnalyzer analyzer = new NLPAnalyzerImpl();
		for (Statement statement : statements)
		{
			System.out.print(statement.getId() + "\t");
			/*System.out.println(analyzer.retrieveSubject(statement));*/
			System.out.println("["
					+ analyzer.retrieveSubjectIfSameTypeAsAU(statement) + "]\t"
					+ statement.getAllAlternativeStatements().get(0));
		}
	}
}
