package edu.uic.cs.t_verifier.nlp.impl;

import java.util.List;
import java.util.Locale;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.uic.cs.t_verifier.common.AbstractWordOperations;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.nlp.NLPAnalyzer;

public class NLPAnalyzerImpl2 extends AbstractWordOperations implements
		NLPAnalyzer
{
	private static final String GRAMMAR = "englishPCFG.ser.gz";

	private static final String AU_SUBSTITUTE = "AU";

	private LexicalizedParser lexicalizedParser;

	private GrammaticalStructureFactory grammaticalStructureFactory;

	public NLPAnalyzerImpl2()
	{
		lexicalizedParser = new LexicalizedParser(GRAMMAR);
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		grammaticalStructureFactory = tlp.grammaticalStructureFactory();
	}

	/* (non-Javadoc)
	 * @see edu.uic.cs.t_verifier.nlp.NLPAnalyzer#retrieveSubjectIfSameTypeAsAU(edu.uic.cs.t_verifier.input.data.Statement)
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

		TreeGraphNode[] auAndCounterpart = retrieveSubjectInternal(
				alternativeUint, tdl);
		if (auAndCounterpart == null)
		{
			return null;
		}

		TreeGraphNode auNode = auAndCounterpart[0];
		TreeGraphNode subjectNode = auAndCounterpart[1];

		Integer auNodeIndex = auNode.label().get(IndexAnnotation.class);
		Integer subjectNodeIndex = subjectNode.label().get(
				IndexAnnotation.class);

		List<TaggedWord> taggedWords = parsedTree.taggedYield();
		TaggedWord auTaggedWord = taggedWords.get(auNodeIndex.intValue() - 1);
		TaggedWord subjectTaggedWord = taggedWords.get(subjectNodeIndex
				.intValue() - 1);
		String auTag = mapPosTagToBasicForm(auTaggedWord.tag());
		String subjectTag = mapPosTagToBasicForm(subjectTaggedWord.tag());
		if (auTag.equals(subjectTag))
		{
			return subjectNode.toString("value").toLowerCase(Locale.US);
		}

		return null;
	}

	/**
	 * TreeGraphNode[] {AU, counterpart}
	 */
	private TreeGraphNode[] retrieveSubjectInternal(String alternativeUint,
			List<TypedDependency> tdl)
	{
		for (TypedDependency typedDependency : tdl)
		{
			GrammaticalRelation grammaticalRelation = typedDependency.reln();
			if (grammaticalRelation.getShortName().equals("nsubj")
					|| grammaticalRelation.getShortName().equals("nsubjpass"))
			{
				TreeGraphNode gov = typedDependency.gov();
				TreeGraphNode dep = typedDependency.dep();

				String govValue = gov.toString("value").toLowerCase(Locale.US);
				String depValue = dep.toString("value").toLowerCase(Locale.US);

				if (alternativeUint.equals(govValue))
				{
					return new TreeGraphNode[] { gov, dep };
				}

				if (alternativeUint.equals(depValue))
				{
					return new TreeGraphNode[] { dep, gov };
				}

			}
		}

		return null;
	}

	private String mapPosTagToBasicForm(String posTag)
	{
		if ("JJR".equals(posTag) || "JJS".equals(posTag))
		{
			return "JJ";
		}

		if ("NNS".equals(posTag) || "NNP".equals(posTag)
				|| "NNPS".equals(posTag))
		{
			return "NN";
		}

		if ("PRP$".equals(posTag))
		{
			return "PRP";
		}

		if ("RBR".equals(posTag) || "RBS".equals(posTag))
		{
			return "RB";
		}

		if ("VBD".equals(posTag) || "VBG".equals(posTag)
				|| "VBN".equals(posTag) || "VBP".equals(posTag)
				|| "VBZ".equals(posTag))
		{
			return "VB";
		}

		if ("WP$".equals(posTag))
		{
			return "WP";
		}

		return posTag;
	}

	public static void main(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();

		NLPAnalyzerImpl2 analyzer = new NLPAnalyzerImpl2();
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
