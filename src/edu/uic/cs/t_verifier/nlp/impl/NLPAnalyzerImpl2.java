package edu.uic.cs.t_verifier.nlp.impl;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;

import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.nlp.NLPAnalyzer;

public class NLPAnalyzerImpl2 implements NLPAnalyzer
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
	public String retrieveTopicTermIfSameTypeAsAU(Statement statement)
	{
		String sentence = statement.getAllAlternativeStatements().get(0);
		String alternativeUint = statement.getAlternativeUnits().get(0);

		return retrieveTopicTermIfSameTypeAsAU(sentence, alternativeUint);
	}

	public String retrieveTopicTermIfSameTypeAsAU(String sentence,
			String alternativeUint)
	{
		boolean doubleCheckSubject = true;
		if (alternativeUint.contains(" ")) // if AU contains more than one term, use "AU" instead. 
		{
			sentence = sentence.replace(alternativeUint, AU_SUBSTITUTE);
			alternativeUint = AU_SUBSTITUTE.toLowerCase(Locale.US);
			doubleCheckSubject = false;
		}

		sentence = StringUtils.capitalize(sentence);

		return retrieveTopicTermIfSameTypeAsAUInternal(sentence,
				alternativeUint, doubleCheckSubject);
	}

	private String retrieveTopicTermIfSameTypeAsAUInternal(String sentence,
			String alternativeUint, boolean doubleCheckSubject)
	{
		Tree parsedTree = lexicalizedParser.apply(sentence);
		GrammaticalStructure gs = grammaticalStructureFactory
				.newGrammaticalStructure(parsedTree);
		Collection<TypedDependency> typedDependencyList = gs
				.typedDependencies();

		AtomicBoolean isPassive = new AtomicBoolean(false);
		String subjectTerm = retrieveSubjectTerm(alternativeUint, parsedTree,
				typedDependencyList, isPassive);
		if (subjectTerm == null)
		{
			if (doubleCheckSubject) // check it again. This time, we use AU to replace single AU term. 
			{
				sentence = sentence.replace(alternativeUint,
						StringUtils.capitalize(alternativeUint));
				subjectTerm = retrieveTopicTermIfSameTypeAsAUInternal(sentence,
						alternativeUint, false);
			}

			//			if (subjectTerm == null && isPassive.get())
			//			{
			//				String objectTerm = retrieveObjectTerm(alternativeUint,
			//						parsedTree, typedDependencyList);
			//
			//				return objectTerm;
			//			}
		}

		return subjectTerm;
	}

	/*private String retrieveObjectTerm(String alternativeUint, Tree parsedTree,
			Collection<TypedDependency> typedDependencyList)
	{
		List<TreeGraphNode> allPossibleObjects = retrieveObjectTermInternal(typedDependencyList);
		if (allPossibleObjects.isEmpty())
		{
			return null;
		}

		String auTag = null;
		int auIndex = -1;
		List<TaggedWord> taggedWords = parsedTree.taggedYield();
		// find AU
		for (int index = 0; index < taggedWords.size(); index++)
		{
			TaggedWord taggedWord = taggedWords.get(index);
			String term = taggedWord.word().toLowerCase(Locale.US);
			if (term.equals(alternativeUint))
			{
				auTag = mapPosTagToBasicForm(taggedWord.tag());
				auIndex = index;
				break;
			}
		}

		Assert.notNull(auTag);

		// String objectTerm = null;
		// int smallestDiff = Integer.MAX_VALUE;
		for (TreeGraphNode objectNode : allPossibleObjects)
		{
			int index = objectNode.label().get(IndexAnnotation.class)
					.intValue() - 1;
			TaggedWord object = taggedWords.get(index);

			int indexDiff = Math.abs(auIndex - index);
			if (indexDiff != 0 && indexDiff < smallestDiff
					&& auTag.equals(mapPosTagToBasicForm(object.tag())))
			{
				objectTerm = objectNode.toString("value")
						.toLowerCase(Locale.US);
				smallestDiff = indexDiff;
			}

			if (auIndex != index
					&& auTag.equals(mapPosTagToBasicForm(object.tag())))
			{
				// return the first one matched
				return objectNode.toString("value").toLowerCase(Locale.US);
			}
		}

		// return objectTerm;
		return null;
	}

	private List<TreeGraphNode> retrieveObjectTermInternal(
			Collection<TypedDependency> typedDependencyList)
	{
		List<TreeGraphNode> result = new ArrayList<TreeGraphNode>();

		for (TypedDependency typedDependency : typedDependencyList)
		{
			GrammaticalRelation grammaticalRelation = typedDependency.reln();
			String shortName = grammaticalRelation.getShortName();
			if (shortName.equals("dobj") || shortName.equals("iobj")
					|| shortName.equals("pobj"))
			{
				TreeGraphNode dep = typedDependency.dep();
				result.add(dep);
			}
		}

		return result;
	}*/

	private String retrieveSubjectTerm(String alternativeUint, Tree parsedTree,
			Collection<TypedDependency> typedDependencyList,
			AtomicBoolean isPassive)
	{
		TreeGraphNode[] auAndCounterpart = retrieveSubjectInternal(
				alternativeUint, typedDependencyList, isPassive);
		if (auAndCounterpart == null)
		{
			return null;
		}

		TreeGraphNode auNode = auAndCounterpart[0];
		TreeGraphNode subjectNode = auAndCounterpart[1];

		if (haveSamePosTag(auNode, subjectNode, parsedTree))
		{
			return subjectNode.toString("value").toLowerCase(Locale.US);
		}

		return null;
	}

	private boolean haveSamePosTag(TreeGraphNode oneNode,
			TreeGraphNode otherNode, Tree parsedTree)
	{
		Integer oneIndex = oneNode.label().get(IndexAnnotation.class);
		Integer otherIndex = otherNode.label().get(IndexAnnotation.class);

		List<TaggedWord> taggedWords = parsedTree.taggedYield();

		TaggedWord oneTaggedWord = taggedWords.get(oneIndex.intValue() - 1);
		TaggedWord otherTaggedWord = taggedWords.get(otherIndex.intValue() - 1);

		String auTag = mapPosTagToBasicForm(oneTaggedWord.tag());
		String subjectTag = mapPosTagToBasicForm(otherTaggedWord.tag());

		return auTag.equals(subjectTag);
	}

	/**
	 * TreeGraphNode[] {AU, counterpart}
	 */
	private TreeGraphNode[] retrieveSubjectInternal(String alternativeUint,
			Collection<TypedDependency> tdl, AtomicBoolean isPassive)
	{
		TreeGraphNode[] result = null;

		for (TypedDependency typedDependency : tdl)
		{
			GrammaticalRelation grammaticalRelation = typedDependency.reln();
			String shortName = grammaticalRelation.getShortName();
			boolean isNsubj = shortName.equals("nsubj");
			boolean isNsubjpass = shortName.equals("nsubjpass");
			if (isNsubj || isNsubjpass)
			{
				Assert.isTrue(result == null,
						"Only one subject related to AU in the sentecne. ");

				isPassive.getAndSet(isNsubjpass);

				TreeGraphNode gov = typedDependency.gov();
				TreeGraphNode dep = typedDependency.dep();

				String govValue = gov.toString("value").toLowerCase(Locale.US);
				String depValue = dep.toString("value").toLowerCase(Locale.US);

				if (alternativeUint.equals(govValue))
				{
					result = new TreeGraphNode[] { gov, dep };
				}
				else if (alternativeUint.equals(depValue))
				{
					result = new TreeGraphNode[] { dep, gov };
				}

				// System.out.println("\t" + shortName + ": " + depValue);

			}
		}

		return result;
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
			/*System.out.println(statement.getId() + "\t"
					+ statement.getAllAlternativeStatements().get(0));
			
			analyzer.retrieveTopicTermIfSameTypeAsAU(statement);
			System.out.println();*/

			System.out.print(statement.getId() + "\t");
			System.out.println("["
					+ analyzer.retrieveTopicTermIfSameTypeAsAU(statement)
					+ "]\t" + statement.getAllAlternativeStatements().get(0));
		}

		//		System.out.println(analyzer.retrieveTopicTermIfSameTypeAsAU(
		//				"poseidon is known as the greek god of sea", "poseidon"));
		//
		//		System.out
		//				.println(analyzer
		//						.retrieveTopicTermIfSameTypeAsAU(
		//								"ronald reagan is known as the first private citizen to fly in space",
		//								"ronald reagan"));
	}
}
