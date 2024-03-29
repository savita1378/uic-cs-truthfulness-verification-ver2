package edu.uic.cs.t_verifier.nlp.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import edu.mit.jwi.item.POS;
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
import edu.uic.cs.t_verifier.misc.GeneralException;
import edu.uic.cs.t_verifier.nlp.NLPAnalyzer;

public class NLPAnalyzerImpl3 extends AbstractNLPOperations implements
		NLPAnalyzer
{
	// private static final String GRAMMAR = "/edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";

	private static final String AU_SUBSTITUTE = "AU";

	private static final List<String> PUNCTUATIONS = Arrays
			.asList(new String[] { ".", ",", ":" });

	protected static final LexicalizedParser lexicalizedParser = LexicalizedParser
			.loadModel(/*GRAMMAR*/);
	private static final GrammaticalStructureFactory grammaticalStructureFactory;
	static
	{
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		grammaticalStructureFactory = tlp.grammaticalStructureFactory();
	}

	/* (non-Javadoc)
	 * @see edu.uic.cs.t_verifier.nlp.NLPAnalyzer#retrieveSubjectIfSameTypeAsAU(edu.uic.cs.t_verifier.input.data.Statement)
	 */
	@Override
	public String retrieveTopicTermIfSameTypeAsAU(Statement statement)
	{
		List<String> allAlternativeUnits = statement.getAlternativeUnits();
		List<String> allAlternativeStatements = statement
				.getAllAlternativeStatements();
		for (int index = 0; index < allAlternativeStatements.size(); index++)
		{
			String sentence = allAlternativeStatements.get(index);
			String alternativeUnit = allAlternativeUnits.get(index);

			String topicTerm = retrieveTopicTermIfSameTypeAsAU(sentence,
					alternativeUnit);
			if (topicTerm != null)
			{
				return topicTerm;
			}
		}

		return null;
	}

	@Override
	public String retrieveTopicTermIfSameTypeAsAU(String sentence,
			String alternativeUnit)
	{
		boolean doubleCheckSubject = true;
		if (alternativeUnit.contains(" ")) // if AU contains more than one term, use "AU" instead. 
		{
			sentence = sentence.replace(alternativeUnit, AU_SUBSTITUTE);
			alternativeUnit = AU_SUBSTITUTE;
			doubleCheckSubject = false;
		}

		List<List<String>> nounPhrases = new ArrayList<List<String>>();
		sentence = restoreWordCasesForSentence(sentence, alternativeUnit,
				nounPhrases);
		// System.out.println("\t" + sentence); 
		alternativeUnit = StringUtils.capitalize(alternativeUnit); // TODO ?? is it ok?

		String topicTerm = retrieveTopicTermIfSameTypeAsAUInternal(sentence,
				alternativeUnit, doubleCheckSubject, nounPhrases);

		return topicTerm;
	}

	private String retrieveTopicTermIfSameTypeAsAUInternal(String sentence,
			String alternativeUint, boolean doubleCheckSubject,
			List<List<String>> nounPhrases)
	{
		Tree parsedTree = lexicalizedParser.apply(sentence);
		GrammaticalStructure gs = grammaticalStructureFactory
				.newGrammaticalStructure(parsedTree);
		Collection<TypedDependency> typedDependencyList = gs
				.typedDependencies();

		System.out.println(sentence);
		AtomicBoolean isPassive = new AtomicBoolean(false);
		String subjectTerm = retrieveSubjectTerm(alternativeUint, parsedTree,
				typedDependencyList, isPassive, nounPhrases);
		if (subjectTerm == null)
		{
			if (doubleCheckSubject) // check it again. This time, we use AU to replace single AU term. 
			{
				sentence = sentence.replace(alternativeUint, AU_SUBSTITUTE);
				subjectTerm = retrieveTopicTermIfSameTypeAsAUInternal(sentence,
						alternativeUint, false, nounPhrases);
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
			AtomicBoolean isPassive, List<List<String>> nounPhrases)
	{
		TreeGraphNode[] auAndCounterpart = retrieveSubjectInternal(
				alternativeUint, typedDependencyList, isPassive, nounPhrases);
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
	 * @param nounPhrases 
	 */
	private TreeGraphNode[] retrieveSubjectInternal(String alternativeUint,
			Collection<TypedDependency> tdl, AtomicBoolean isPassive,
			List<List<String>> nounPhrases)
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

				String govValue = gov.toString("value")/*.toLowerCase(Locale.US)*/;
				String depValue = dep.toString("value")/*.toLowerCase(Locale.US)*/;

				/*if (alternativeUint.equalsIgnoreCase(govValue))
				{
					result = new TreeGraphNode[] { gov, dep };
				}
				else if (alternativeUint.equalsIgnoreCase(depValue))
				{
					result = new TreeGraphNode[] { dep, gov };
				}*/

				for (List<String> nounPhrase : nounPhrases)
				{
					if (containsIgnoreCase(nounPhrase, alternativeUint)
							&& containsIgnoreCase(nounPhrase, govValue))
					{
						result = new TreeGraphNode[] { gov, dep };
						break;
					}
					else if (containsIgnoreCase(nounPhrase, alternativeUint)
							&& containsIgnoreCase(nounPhrase, depValue))
					{
						result = new TreeGraphNode[] { dep, gov };
						break;
					}
				}

				// System.out.println("\t" + shortName + ": " + depValue);

			}
		}

		return result;
	}

	private boolean containsIgnoreCase(Collection<String> collection, String s)
	{
		Iterator<String> it = collection.iterator();
		while (it.hasNext())
		{
			if (it.next().equalsIgnoreCase(s))
				return true;
		}
		return false;
	}

	@Override
	public HashMap<String, String> parseSentenceIntoTermsWithPosTag(
			String sentence, List<String> resultTerms)
	{
		Assert.notNull(resultTerms);

		// sentence = StringUtils.capitalize(sentence);
		Tree parsedTree = lexicalizedParser.apply(sentence);
		List<TaggedWord> taggedWords = parsedTree.taggedYield();

		HashMap<String, String> result = new LinkedHashMap<String, String>();
		for (TaggedWord taggedWord : taggedWords)
		{
			String psoTag = mapPosTagToBasicForm(taggedWord.tag());
			result.put(taggedWord.word(), psoTag);
			resultTerms.add(taggedWord.word());
		}

		return result;
	}

	@Override
	public boolean hasAlternativeUnitDoneSomething(Statement statement)
	{
		String sentence = statement.getAllAlternativeStatements().get(0);
		String alternativeUint = statement.getAlternativeUnits().get(0);

		return hasAlternativeUnitDoneSomething(sentence, alternativeUint);
	}

	@Override
	public boolean hasAlternativeUnitDoneSomething(String sentence,
			String alternativeUint)
	{
		if (alternativeUint.contains(" ")) // if AU contains more than one term, use "AU" instead. 
		{
			sentence = sentence.replace(alternativeUint, AU_SUBSTITUTE);
			alternativeUint = AU_SUBSTITUTE.toLowerCase(Locale.US);
		}
		sentence = StringUtils.capitalize(sentence);

		Tree parsedTree = lexicalizedParser.apply(sentence);
		GrammaticalStructure gs = grammaticalStructureFactory
				.newGrammaticalStructure(parsedTree);
		Collection<TypedDependency> typedDependencyList = gs
				.typedDependenciesCollapsed(); // collapsed!!

		List<TaggedWord> taggedWords = parsedTree.taggedYield();
		for (TypedDependency typedDependency : typedDependencyList)
		{
			GrammaticalRelation grammaticalRelation = typedDependency.reln();
			String shortName = grammaticalRelation.getShortName();

			/**
			 * AU/NNP invented/VBD the/DT electric/JJ guitar/NN
			 * 
			 * nsubj(invented-2, AU-1)	<--- HERE
			 * root(ROOT-0, invented-2)
			 * det(guitar-5, the-3)
			 * amod(guitar-5, electric-4)
			 * dobj(invented-2, guitar-5)
			 * 
			 * 
			 * the/DT electric/JJ guitar/NN is/VBZ invented/VBN by/IN AU/NNP
			 * 
			 * det(guitar-3, the-1)
			 * amod(guitar-3, electric-2)
			 * nsubjpass(invented-5, guitar-3)
			 * auxpass(invented-5, is-4)
			 * root(ROOT-0, invented-5)
			 * agent(invented-5, AU-7)	<--- HERE
			 */
			if (shortName.equals("nsubj") || shortName.equals("agent")) //(did~gov, AU~dep)
			{
				TreeGraphNode gov = typedDependency.gov();
				TreeGraphNode dep = typedDependency.dep();

				// String govValue = gov.toString("value").toLowerCase(Locale.US);
				String depValue = dep.toString("value").toLowerCase(Locale.US);

				if (alternativeUint.equals(depValue))
				{
					int govIndex = gov.index() - 1;
					TaggedWord govTaggedWord = taggedWords.get(govIndex);
					if ("VB".equals(mapPosTagToBasicForm(govTaggedWord.tag())))
					{
						return true;
					}

				}
			}

		}

		return false;
	}

	protected String restoreWordCasesForSentence(String sentence,
			String alternativeUnit)
	{
		List<List<String>> nounPhrases = new ArrayList<List<String>>();
		// ignore nounPhrases
		return restoreWordCasesForSentence(sentence, alternativeUnit,
				nounPhrases);
	}

	protected String restoreWordCasesForSentence(String sentence,
			String alternativeUnit, List<List<String>> nounPhrases)
	{
		String alternativeUnitCapitalized = WordUtils
				.capitalize(alternativeUnit);
		sentence = sentence
				.replace(alternativeUnit, alternativeUnitCapitalized); // capitalize AU
		sentence = capitalizeProperNounTerms(sentence, nounPhrases);
		return sentence;
	}

	@Deprecated
	protected String capitalizeProperNounTerms(String sentence,
			List<List<String>> nounPhrases)
	{
		sentence = StringUtils.capitalize(sentence);
		List<String> terms = new ArrayList<String>();
		HashMap<String, String> posTagByTerm = parseSentenceIntoTermsWithPosTag(
				sentence, terms);
		// System.out.println("\t" + posTagByTerm);

		StringBuilder result = new StringBuilder();
		// String[] terms = sentence.split("(?<=\\W)");
		/*for (String term : terms)
		{
			String posTag = posTagByTerm.get(term.trim());
			if ("NN".equals(posTag))
			{
				term = StringUtils.capitalize(term);
			}

			result.append(term);
		}*/

		int properNounBegin = -1;
		boolean processingNoun = false;
		String term = null;
		int index = 0;
		for (; index < terms.size(); index++)
		{
			term = terms.get(index);
			String posTag = posTagByTerm.get(term);
			if ("NN".equals(posTag))
			{
				if (!processingNoun)
				{
					processingNoun = true; // begin noun
					properNounBegin = index;
				}
				// else leave it along
			}
			else
			// not noun term
			{
				if (processingNoun) // there are noun(s) pending
				{
					if (index - properNounBegin > 1)
					{
						ArrayList<String> nounPhrase = new ArrayList<String>(
								index - properNounBegin);
						// more than one noun together
						for (int innerIndex = properNounBegin; innerIndex < index; innerIndex++)
						{
							// first try WordNet
							String noun = wordNetReader
									.retrieveTermInStandardCase(
											terms.get(innerIndex), POS.NOUN);
							// then no matter what WordNet gives back, capitalize it
							noun = StringUtils.capitalize(noun);
							result.append(noun).append(" ");
							nounPhrase.add(noun);
						}

						nounPhrases.add(nounPhrase);
					}
					else if (index - properNounBegin == 1)
					{
						// just one noun
						String noun = wordNetReader.retrieveTermInStandardCase(
								terms.get(properNounBegin), POS.NOUN);
						result.append(noun).append(" ");

						nounPhrases.add(Collections.singletonList(noun));
					}
					else
					{
						throw new GeneralException("Can not happen! ");
					}

					processingNoun = false; // end noun
				}

				if ("POS".equals(posTag) || isPunctuation(posTag))
				{
					result.deleteCharAt(result.length() - 1);
				}

				result.append(term).append(" ");
			}
		}

		// still some nouns pending
		if (processingNoun)
		{
			if (index - properNounBegin > 1)
			{
				ArrayList<String> nounPhrase = new ArrayList<String>(index
						- properNounBegin);
				// more than one noun together
				for (int innerIndex = properNounBegin; innerIndex < index; innerIndex++)
				{
					String noun = StringUtils.capitalize(terms.get(innerIndex));
					result.append(noun).append(" ");
					nounPhrase.add(noun);
				}

				nounPhrases.add(nounPhrase);
			}
			else if (index - properNounBegin == 1)
			{
				// just one noun
				String noun = wordNetReader.retrieveTermInStandardCase(
						terms.get(properNounBegin), POS.NOUN);
				result.append(noun); //final noun, no need to append(" ")

				nounPhrases.add(Collections.singletonList(noun));
			}
			else
			{
				throw new GeneralException("Can not happen! ");
			}

			// processingNoun = false; // end noun
		}

		return StringUtils.capitalize(result.toString().trim());

	}

	protected boolean isPunctuation(String posTag)
	{
		return PUNCTUATIONS.contains(posTag);
	}

	public static void main(String[] args)
	{
		NLPAnalyzerImpl3 analyzer = new NLPAnalyzerImpl3();
		String sentence = "this is the largest festival in united states";
		System.out.println(analyzer.capitalizeProperNounTerms(sentence,
				new ArrayList<List<String>>()));

		sentence = "belize located in central america";
		System.out.println(analyzer.capitalizeProperNounTerms(sentence,
				new ArrayList<List<String>>()));
	}

	public static void main3(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();

		NLPAnalyzerImpl3 analyzer = new NLPAnalyzerImpl3();
		for (Statement statement : statements)
		{
			String counterpartOfAU = analyzer
					.retrieveTopicTermIfSameTypeAsAU(statement);
			if (counterpartOfAU == null)
			{
				continue;
			}

			System.out.print(statement.getId() + "\t");
			/*System.out.println(analyzer.retrieveSubject(statement));*/
			System.out.println("[" + counterpartOfAU + "]\t"
					+ statement.getAllAlternativeStatements().get(0));
			//			System.out.println(analyzer.restoreWordCasesForSentence(statement
			//					.getAllAlternativeStatements().get(0), statement
			//					.getAlternativeUnits().get(0)));
		}
	}

	public static void main2(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();
		NLPAnalyzerImpl3 analyzer = new NLPAnalyzerImpl3();

		for (Statement statement : statements)
		{
			List<String> allAlternativeUnits = statement.getAlternativeUnits();
			List<String> allAlternativeStatements = statement
					.getAllAlternativeStatements();

			for (int index = 0; index < allAlternativeStatements.size(); index++)
			{
				String alternativeUnit = allAlternativeUnits.get(index);
				String sentence = allAlternativeStatements.get(index);

				int alternativeUnitStartIndex = sentence
						.indexOf(alternativeUnit);
				String restoredSentence = analyzer.restoreWordCasesForSentence(
						sentence, alternativeUnit);
				String restoredAlternativeUnit = restoredSentence.substring(
						alternativeUnitStartIndex, alternativeUnitStartIndex
								+ alternativeUnit.length());

				System.out.println("["
						+ statement.getId()
						+ "] "
						+ restoredSentence.replace(restoredAlternativeUnit, "["
								+ restoredAlternativeUnit + "]"));
			}

			System.out.println();
		}
	}

	//	public static void main2(String[] args)
	//	{
	//		List<Statement> statements = AlternativeUnitsReader
	//				.parseAllStatementsFromInputFiles();
	//
	//		NLPAnalyzerImpl2 analyzer = new NLPAnalyzerImpl2();
	//		//		HashMap<String, String> result = analyzer
	//		//				.parseSentenceIntoTermsWithPosTag("the biggest producer of tungsten is china");
	//		//		System.out.println(result);
	//
	//		//		for (Statement statement : statements)
	//		//		{
	//		//			/*System.out.println(statement.getId() + "\t"
	//		//					+ statement.getAllAlternativeStatements().get(0));
	//		//			
	//		//			analyzer.retrieveTopicTermIfSameTypeAsAU(statement);
	//		//			System.out.println();*/
	//		//
	//		//			System.out.print(statement.getId() + "\t");
	//		//			System.out.println("["
	//		//					+ analyzer.retrieveTopicTermIfSameTypeAsAU(statement)
	//		//					+ "]\t" + statement.getAllAlternativeStatements().get(0));
	//		//		}
	//
	//		//		System.out.println(analyzer.retrieveTopicTermIfSameTypeAsAU(
	//		//				"poseidon is known as the greek god of sea", "poseidon"));
	//		//
	//		//		System.out
	//		//				.println(analyzer
	//		//						.retrieveTopicTermIfSameTypeAsAU(
	//		//								"ronald reagan is known as the first private citizen to fly in space",
	//		//								"ronald reagan"));
	//
	//		System.out.println(analyzer.hasAlternativeUnitDoneSomething(
	//				"les paul invented the electric guitar", "les paul"));
	//		System.out.println(analyzer.hasAlternativeUnitDoneSomething(
	//				"the electric guitar was invented by les paul", "les paul"));
	//	}

}
