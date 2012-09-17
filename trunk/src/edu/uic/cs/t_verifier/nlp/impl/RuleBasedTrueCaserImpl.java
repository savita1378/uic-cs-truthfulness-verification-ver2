package edu.uic.cs.t_verifier.nlp.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import opennlp.tools.util.Span;

import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;

import edu.uic.cs.t_verifier.input.AlternativeUnitsReader;
import edu.uic.cs.t_verifier.input.data.Statement;
import edu.uic.cs.t_verifier.misc.LogHelper;
import edu.uic.cs.t_verifier.nlp.impl.OpenNLPChunker.ChunkType;

public class RuleBasedTrueCaserImpl extends AbstractTrueCaser
{
	// private static final String WEB_ADDRESS_REGEX = "^([a-zA-Z0-9\\-]+\\.)+(com|org|net|mil|edu|COM|ORG|NET|MIL|EDU)$";

	private static final Logger LOGGER = LogHelper
			.getLogger(RuleBasedTrueCaserImpl.class);

	public static void main(String[] args)
	{
		List<Statement> statements = AlternativeUnitsReader
				.parseAllStatementsFromInputFiles();
		RuleBasedTrueCaserImpl impl = new RuleBasedTrueCaserImpl();

		try
		{
			for (Statement statement : statements)
			{
				//				if (statement.getId().intValue() != 4)
				//				{
				//					continue;
				//				}
				List<String> allAlternativeUnits = statement
						.getAlternativeUnits();
				List<String> allAlternativeStatements = statement
						.getAllAlternativeStatements();

				//			String alternativeUnit = allAlternativeUnits.get(0);
				//			String sentence = allAlternativeStatements.get(0);
				//
				//			System.out.println("["
				//					+ statement.getId()
				//					+ "] "
				//					+ sentence.replace(alternativeUnit, "[" + alternativeUnit
				//							+ "]"));
				//
				//			String capitalizedSentence = impl.capitalizeProperNounTerms(
				//					sentence, null);

				for (int index = 0; index < allAlternativeStatements.size(); index++)
				{
					String alternativeUnit = allAlternativeUnits.get(index);
					String sentence = allAlternativeStatements.get(index);

					String originalSentence = "["
							+ statement.getId()
							+ "] "
							+ sentence.replace(alternativeUnit, "["
									+ alternativeUnit + "]");
					// System.out.println(originalSentence);
					LOGGER.info(originalSentence);

					String capitalizedSentence = impl.restoreCases(sentence);
					System.out.println(/*"> " + */capitalizedSentence);

					LOGGER.info("");
				}
				System.out.println();

			}
		}
		finally
		{
			impl.commitCache(); // DO NOT forget this!
		}

	}

	@Override
	public String restoreCases(String sentence,
			List<List<String>> possibleNounPhrases)
	{
		LOGGER.info(sentence);

		sentence = sentence.trim();
		sentence = sentence.replaceAll("\\s{2,}", " ");

		// sentence = StringUtils.capitalize(sentence);

		List<Entry<String, String>> posTagsByTerm = parseIntoPosTagByTerms(
				sentence, false); // this one is not in basic form!!
		// split by the punctuation
		List<List<Entry<String, String>>> originalPosTagsByTermOfSubSentences = splitByPunctuations(posTagsByTerm); // original form for chunking

		List<List<Entry<String, String>>> allNounPhrases = new ArrayList<List<Entry<String, String>>>();
		List<List<Entry<String, String>>> posTagsByTermOfSubSentences = new ArrayList<List<Entry<String, String>>>(
				originalPosTagsByTermOfSubSentences.size());

		List<String> matchedAcronyms = new ArrayList<String>();
		List<Entry<List<Entry<String, String>>, String>> matchedFullNames = new ArrayList<Entry<List<Entry<String, String>>, String>>();

		for (List<Entry<String, String>> tagsByTermOriginalForm : originalPosTagsByTermOfSubSentences) // for each sub-sentence
		{
			List<Entry<String, String>> tagsByTermBasicForm = mapPosTagToBasicForm(tagsByTermOriginalForm);
			posTagsByTermOfSubSentences.add(tagsByTermBasicForm); // basic form

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

				//				Entry<String, String> tagsByTermBeforeNP = (span.getStart() == 0) ? null
				//						: tagsByTermOriginalForm.get(span.getStart() - 1);
				//				Entry<String, String> tagsByTermAfterNP = (span.getEnd() == tagsByTermOriginalForm
				//						.size()) ? null : tagsByTermOriginalForm.get(span
				//						.getEnd());
				//				List<Integer> indicesOfNameTerm = personNameIdentifier
				//						.identifyNameTermsWithinNounPhrase(phraseBasicForm,
				//								tagsByTermBeforeNP, tagsByTermAfterNP);
				//				if (indicesOfNameTerm == null || indicesOfNameTerm.isEmpty())
				//				{
				//					continue;
				//				}
				//
				//				List<Entry<String, String>> name = phraseBasicForm
				//						.subList(indicesOfNameTerm.get(0), indicesOfNameTerm
				//								.get(indicesOfNameTerm.size() - 1) + 1);
				//				matchedFullNames
				//						.add(new SimpleEntry<List<Entry<String, String>>, String>(
				//								name, concatenateTerms(name)));
				// System.out.println(name);
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
		findProperNounsInWikipedia(sentence, posTagsByTermOfSubSentences,
				capitalizationsByOriginalCaseFromWiki,
				possibleCapitalizationsBySingleNounTermFromWiki);
		LOGGER.info(">>>>> ProperNoun_wiki_sequence\t\t*"
				+ capitalizationsByOriginalCaseFromWiki);
		LOGGER.info(">>>>> Candidate_ProperNoun_wiki_single\t"
				+ possibleCapitalizationsBySingleNounTermFromWiki);

		// single noun from wikipedia is not reliable, 
		// for example, "Fastest" is considered as a movie name which is capitalized; also "Become", "MCG", "Descendents"
		List<Entry<Entry<String, String>, String>> capitalizationsBySingleNounTermFromWiki = filterOutNonProperSingleNounByWordNet(possibleCapitalizationsBySingleNounTermFromWiki);
		LOGGER.info(">>>>> ProperNoun_wiki_single\t\t*"
				+ capitalizationsBySingleNounTermFromWiki);

		//		// Get all noun sequences
		//		List<List<Entry<String, String>>> nounSequences = findNounSequences(posTagsByTermOfSubSentences);
		//		LOGGER.info(">>>>> Noun_from_parser\t\t\t" + nounSequences);

		// Find those noun(sequence)s which have not been identified by Wikipedia
		List<List<Entry<String, String>>> nounPhrasesNotIdentifiedByWiki = filterOutNounSequencesIdentifiedByWiki(
		/*nounSequences*/allNounPhrases,
				capitalizationsByOriginalCaseFromWiki,
				capitalizationsBySingleNounTermFromWiki);
		LOGGER.info(">>>>> Noun_notInWiki\t\t\t"
				+ nounPhrasesNotIdentifiedByWiki);

		// Find the proper noun in WordNet
		List<Entry<List<Entry<String, String>>, String>> capitalizationsByOriginalCaseFromWordNet = findProperNounsInWordNet(nounPhrasesNotIdentifiedByWiki);
		LOGGER.info(">>>>> ProperNoun_notInWiki_inWN\t\t*"
				+ capitalizationsByOriginalCaseFromWordNet);

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
				capitalizationsByOriginalCaseFromWordNet);

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

		for (Entry<List<Entry<String, String>>, String> entry : capitalizationsByOriginalCaseFromWordNet)
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

		//		// re-parse it /////////////////////////////////////////////////////////
		//		posTagsByTerm = parseIntoPosTagByTerms(sentence);
		//		// split by the punctuation
		//		posTagsByTermOfSubSentences = splitByPunctuations(posTagsByTerm);
		//		nounSequences = findNounSequences(posTagsByTermOfSubSentences);
		//		LOGGER.info(">>>>> Re-parsed_Noun_from_parser\t\t" + nounSequences);

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
		//return StringUtils.capitalize(sentence);
	}

}
