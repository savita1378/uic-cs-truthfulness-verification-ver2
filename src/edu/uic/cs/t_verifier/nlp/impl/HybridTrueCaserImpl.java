package edu.uic.cs.t_verifier.nlp.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Properties;

import opennlp.tools.util.Span;

import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;

import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.CoreAnnotations.TrueCaseTextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.uic.cs.t_verifier.misc.LogHelper;
import edu.uic.cs.t_verifier.nlp.impl.OpenNLPChunker.ChunkType;

public class HybridTrueCaserImpl extends AbstractTrueCaser
{
	private static final Logger LOGGER = LogHelper
			.getLogger(HybridTrueCaserImpl.class);

	private static StanfordCoreNLP stanfordTrueCaser;

	public HybridTrueCaserImpl()
	{
		if (stanfordTrueCaser == null)
		{
			synchronized (RuleBasedTrueCaserImpl.class)
			{
				if (stanfordTrueCaser == null)
				{
					Properties props = new Properties();
					props.put("annotators",
							"tokenize, ssplit, pos, lemma, truecase");
					stanfordTrueCaser = new StanfordCoreNLP(props);
				}
			}
		}
	}

	@Override
	protected StanfordCoreNLP getAnnotater()
	{
		return stanfordTrueCaser;
	}

	@Override
	public String restoreCases(String sentence,
			List<List<String>> possibleNounPhrases)
	{
		LOGGER.info(sentence);

		sentence = sentence.trim();
		sentence = sentence.replaceAll("\\s{2,}", " ");

		List<Entry<String, String>> posTagsByOriginalCase = new ArrayList<Entry<String, String>>();
		List<Entry<String, String>> posTagsByTrueCase = new ArrayList<Entry<String, String>>();
		parseIntoPosTagByOriginalAndTrueCase(sentence, posTagsByOriginalCase,
				posTagsByTrueCase); // this one is not in basic form!!

		// split by the punctuation
		List<List<Entry<String, String>>> originalPosTagsByOriginalCaseOfSubSentences = splitByPunctuations(posTagsByOriginalCase); // original form for chunking
		List<List<Entry<String, String>>> originalPosTagsByTrueCaseOfSubSentences = splitByPunctuations(posTagsByTrueCase);

		List<List<Entry<String, String>>> allNounPhrases = new ArrayList<List<Entry<String, String>>>();
		List<List<Entry<String, String>>> posTagsByOriginalCaseOfSubSentences = new ArrayList<List<Entry<String, String>>>(
				originalPosTagsByOriginalCaseOfSubSentences.size());

		List<String> matchedAcronyms = new ArrayList<String>();
		List<Entry<List<Entry<String, String>>, String>> matchedFullNames = new ArrayList<Entry<List<Entry<String, String>>, String>>();

		for (List<Entry<String, String>> tagsByTermOriginalForm : originalPosTagsByOriginalCaseOfSubSentences) // for each sub-sentence
		{
			List<Entry<String, String>> tagsByTermBasicForm = mapPosTagToBasicForm(tagsByTermOriginalForm);
			posTagsByOriginalCaseOfSubSentences.add(tagsByTermBasicForm); // basic form

			String acronym = searchForAcronyms(tagsByTermBasicForm);
			matchedAcronyms.add(acronym);

			List<Span> spans = chunker.getChunkSpans(tagsByTermOriginalForm,
					ChunkType.NP);

			// find names
			for (Span span : spans)
			{
				List<Entry<String, String>> phraseOriginalForm = tagsByTermOriginalForm
						.subList(span.getStart(), span.getEnd());
				List<Entry<String, String>> phraseBasicForm = mapPosTagToBasicForm(phraseOriginalForm); // basic form
				allNounPhrases.add(phraseBasicForm);
			}

			matchFullNames(tagsByTermOriginalForm, tagsByTermBasicForm,
					matchedFullNames);
		}

		LOGGER.info(">>>>> Acronyms\t\t\t" + matchedAcronyms);
		LOGGER.info(">>>>> NounPhrases_from_chucker\t\t\t" + allNounPhrases);

		////////////////////////////////////////////////////////////////////////
		// IF USING THIS, COMMENT matchFullNames(...), OR USE DUMMY PersonNameIdentifier
		// match names within name-list
		//		List<Entry<Entry<String, String>, String>> matchedSingleNames = new ArrayList<Map.Entry<Entry<String, String>, String>>();
		//		for (List<Entry<String, String>> posTagByTerm : posTagsByTermOfSubSentences)
		//		{
		//			recursiveMatchTerms(trigramPersonNameMatcher,
		//					Collections.singletonList(posTagByTerm), matchedFullNames,
		//					matchedSingleNames);
		//		}
		////////////////////////////////////////////////////////////////////////

		LOGGER.info(">>>>> MatchedPersonName_full\t\t\t" + matchedFullNames); // TODO no use now
		// System.out.println(allNounPhrases);

		// Find the proper noun in Wikiepdia
		// sequence
		List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWiki = new ArrayList<Entry<List<Entry<String, String>>, String>>();
		// single term
		List<Entry<Entry<String, String>, String>> possibleCapitalizationsBySingleNounTermFromWiki = new ArrayList<Entry<Entry<String, String>, String>>();
		findProperNounsInWikipedia(sentence,
				posTagsByOriginalCaseOfSubSentences,
				capitalizationsByOriginalCaseFromWiki,
				possibleCapitalizationsBySingleNounTermFromWiki);
		LOGGER.info(">>>>> ProperNoun_wiki_sequence\t\t*"
				+ capitalizationsByOriginalCaseFromWiki);
		LOGGER.info(">>>>> Candidate_ProperNoun_wiki_single\t"
				+ possibleCapitalizationsBySingleNounTermFromWiki);

		// single noun from wikipedia is not reliable, 
		// for example, "Fastest" is considered as a movie name which is capitalized; also "Become", "MCG", "Descendents"
		// if the single term has been capitalized by true-caser, it is very likely to be correct
		List<Entry<Entry<String, String>, String>> capitalizationsBySingleNounTermFromWiki = filterOutNotTrueCasedSingleTerm(
				possibleCapitalizationsBySingleNounTermFromWiki,
				originalPosTagsByTrueCaseOfSubSentences);
		LOGGER.info(">>>>> True-cased_wiki_single\t\t*"
				+ capitalizationsBySingleNounTermFromWiki);

		// Find those noun(sequence)s which have not been identified by Wikipedia
		List<List<Entry<String, String>>> nounPhrasesNotIdentifiedByWiki = filterOutNounSequencesIdentifiedByWiki(
				allNounPhrases, capitalizationsByOriginalCaseFromWiki,
				capitalizationsBySingleNounTermFromWiki);
		LOGGER.info(">>>>> Noun_notInWiki\t\t\t"
				+ nounPhrasesNotIdentifiedByWiki);

		// Find the proper noun in WordNet or true cased
		List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWordNetOrTrueCaser = findProperNounsInWordNetOrTrueCased(
				nounPhrasesNotIdentifiedByWiki,
				originalPosTagsByTrueCaseOfSubSentences);
		LOGGER.info(">>>>> True-cased_notInWiki_inWN\t\t*"
				+ capitalizationsByOriginalCaseFromWordNetOrTrueCaser);

		/*
		// TODO this matched single name is not used now, 
		// since it may match terms like “long”, “longest”, “kings”, “big”, “from”, “games”, “late”, “states”
		// maybe introducing the frequency of each name term may do some help, but we haven't decided it yet.
		LOGGER.info(">>>>> MatchedPersonName_single\t\t\t" + matchedSingleNames);
		*/

		// find those names only identified by name-list
		List<List<Entry<String, String>>> matchedNamesIdentifiedByNameListOnly = filterOutNamesByWikiAndWordNet(
				matchedFullNames, capitalizationsByOriginalCaseFromWiki,
				capitalizationsBySingleNounTermFromWiki,
				capitalizationsByOriginalCaseFromWordNetOrTrueCaser);

		LOGGER.info(">>>>> PersonName_notInWiki_notInWN\t*"
				+ matchedNamesIdentifiedByNameListOnly);

		// REPLACE /////////////////////////////////////////////////////////////
		for (List<Entry<String, String>> entry : matchedNamesIdentifiedByNameListOnly)
		{
			String target = concatenateTerms(entry);
			String replacement = WordUtils.capitalize(target);
			sentence = sentence.replaceAll(createReplaceTarget(target),
					replacement);
		}

		for (Entry<List<Entry<String, String>>, String> entry : capitalizationsByOriginalCaseFromWiki)
		{
			sentence = replaceMatchedWikiPhrase(sentence, entry.getKey(),
					entry.getValue());
		}

		for (Entry<Entry<String, String>, String> entry : capitalizationsBySingleNounTermFromWiki)
		{
			String target = entry.getKey().getKey();
			sentence = sentence.replaceAll(createReplaceTarget(target),
					entry.getValue());
		}

		for (Entry<List<Entry<String, String>>, String> entry : capitalizationsByOriginalCaseFromWordNetOrTrueCaser)
		{
			String target = concatenateTerms(entry.getKey());
			sentence = sentence.replaceAll(createReplaceTarget(target),
					entry.getValue());
		}

		for (String acronym : matchedAcronyms)
		{
			acronym = "(" + acronym + ")";
			sentence = sentence
					.replace(acronym, acronym.toUpperCase(Locale.US));
		}

		////////////////////////////////////////////////////////////////////////

		// fill the parameter nounPhrases with nounSequences
		if (possibleNounPhrases != null)
		{
			// but now it is OK, since the AU matching is using the same parser for nouns
			for (List<Entry<String, String>> sequence : /*nounSequences*/allNounPhrases)
			{
				List<String> nounSequence = new ArrayList<String>(
						sequence.size());
				possibleNounPhrases.add(nounSequence);
				for (Entry<String, String> entry : sequence)
				{
					nounSequence.add(entry.getKey());
				}
			}
		}

		LOGGER.info("========================================================");

		return sentence;
	}

	private List<Entry<Entry<String, String>, String>> filterOutNotTrueCasedSingleTerm(
			List<Entry<Entry<String, String>, String>> possibleCapitalizationsBySingleNounTermFromWiki,
			List<List<Entry<String, String>>> originalPosTagsByTrueCaseOfSubSentences)
	{
		List<Entry<Entry<String, String>, String>> result = new ArrayList<Entry<Entry<String, String>, String>>();

		for (Entry<Entry<String, String>, String> entry : possibleCapitalizationsBySingleNounTermFromWiki)
		{
			String capitalizedTermInWiki = entry.getValue();
			trueCased: for (List<Entry<String, String>> posByTrueCaseInSubSentence : originalPosTagsByTrueCaseOfSubSentences)
			{
				for (Entry<String, String> posByTrueCase : posByTrueCaseInSubSentence)
				{
					String trueCase = posByTrueCase.getKey();
					if (trueCase.equals(capitalizedTermInWiki)) // the single term also capitalized by true-caser
					{
						result.add(entry);
						break trueCased;
					}
				}
			}
		}

		return result;
	}

	private void parseIntoPosTagByOriginalAndTrueCase(String sentence,
			List<Entry<String, String>> posTagsByOriginalCase,
			List<Entry<String, String>> posTagsByTrueCase)
	{
		Annotation document = new Annotation(sentence);
		getAnnotater().annotate(document);

		for (CoreLabel token : document.get(TokensAnnotation.class))
		{
			String pos = token.get(PartOfSpeechAnnotation.class);
			String originalCase = token.get(TextAnnotation.class);
			String trueCase = token.get(TrueCaseTextAnnotation.class);

			posTagsByOriginalCase.add(new SimpleEntry<String, String>(
					originalCase, pos));
			posTagsByTrueCase
					.add(new SimpleEntry<String, String>(trueCase, pos));
		}
	}

	private List<Entry<List<Entry<String, String>>, String>> findProperNounsInWordNetOrTrueCased(
			List<List<Entry<String, String>>> posTagsByTermOfNounPhrases,
			List<List<Entry<String, String>>> originalPosTagsByTrueCaseOfSubSentences)
	{
		List<Entry<List<Entry<String, String>>, String>> result = new ArrayList<Entry<List<Entry<String, String>>, String>>();

		for (List<Entry<String, String>> nounSequence : posTagsByTermOfNounPhrases)
		{
			String trueCasedSequence = null;

			if (nounSequence.size() > 1) // more than one term (i.e. phrase)
			{
				String nounSequenceString = concatenateTerms(nounSequence);
				String nounSequenceInWordNet = wordNetReader
						.retrieveTermInStandardCase(nounSequenceString,
								POS.NOUN);
				if (nounSequenceInWordNet != null
						&& inDifferentCases(nounSequenceString,
								nounSequenceInWordNet)) // matched in WN
				{
					result.add(new SimpleEntry<List<Entry<String, String>>, String>(
							nounSequence, nounSequenceInWordNet));
					continue;
				}
				//				else if ((trueCasedSequence = findCorrespondingTrueCasedSequence(
				//						nounSequenceString,
				//						originalPosTagsByTrueCaseOfSubSentences)) != null) // here we trust the true-caser
				//				{
				//					result.add(new SimpleEntry<List<Entry<String, String>>, String>(
				//							nounSequence, trueCasedSequence));
				//					continue;
				//				}
			}

			// try each individual noun term
			for (Entry<String, String> entry : nounSequence)
			{
				String term = entry.getKey().toLowerCase(Locale.US);
				String termInWordNet = wordNetReader
						.retrieveTermInStandardCase(term, POS.NOUN);
				if (termInWordNet != null
						&& inDifferentCases(term, termInWordNet) // exists capitalized term in WN
						&& (trueCasedSequence = findCorrespondingTrueCasedSequence(
								term, originalPosTagsByTrueCaseOfSubSentences)) != null) // and also be true cased
				{
					result.add(new SimpleEntry<List<Entry<String, String>>, String>(
							Collections.singletonList(entry), trueCasedSequence));
				}
			}

		}

		return result;
	}

	private String findCorrespondingTrueCasedSequence(
			String nounSequenceString,
			List<List<Entry<String, String>>> originalPosTagsByTrueCaseOfSubSentences)
	{
		// nounSequenceString = nounSequenceString.toLowerCase(Locale.US); // already in lower case
		for (List<Entry<String, String>> trueCasedSubSentence : originalPosTagsByTrueCaseOfSubSentences)
		{
			String trueCasedString = concatenateTerms(trueCasedSubSentence,
					false, false); // MUST not in lower case!
			int beginIndex = trueCasedString.toLowerCase(Locale.US).indexOf(
					nounSequenceString);

			if (!trueCasedString.contains(nounSequenceString)
					&& beginIndex > -1) // contains the sequence but not in lower case
			{
				return trueCasedString.substring(beginIndex, beginIndex
						+ nounSequenceString.length());
			}

		}

		return null;
	}
}
