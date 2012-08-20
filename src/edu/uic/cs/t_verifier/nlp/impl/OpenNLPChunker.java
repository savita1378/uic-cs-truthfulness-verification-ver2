package edu.uic.cs.t_verifier.nlp.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.util.Span;
import edu.uic.cs.t_verifier.misc.GeneralException;

public class OpenNLPChunker
{
	public enum ChunkType
	{
		NP, VP, PP, ADJP, ADVP, O;
	}

	private static final String CHUNKER_MODEL_NAME = "en-chunker.bin";
	private static final String CHUNKER_MODEL_PATH = OpenNLPChunker.class
			.getClassLoader().getResource(".").getFile()
			+ File.separator + CHUNKER_MODEL_NAME;

	private ChunkerModel model = null;

	private synchronized ChunkerModel getChunkerModel()
	{
		if (model != null)
		{
			return model;
		}

		try
		{
			InputStream modelIn = new FileInputStream(CHUNKER_MODEL_PATH);
			model = new ChunkerModel(modelIn);
		}
		catch (IOException ioe)
		{
			throw new GeneralException(ioe);
		}

		return model;
	}

	public List<Span> getChunkSpans(List<Entry<String, String>> tagsByTerm,
			ChunkType chunkType)
	{
		String[] terms = new String[tagsByTerm.size()];
		String[] tags = new String[tagsByTerm.size()];
		int index = 0;
		for (Entry<String, String> tagByTerm : tagsByTerm)
		{
			terms[index] = tagByTerm.getKey();
			tags[index] = tagByTerm.getValue();
			index++;
		}

		ChunkerME chunker = new ChunkerME(getChunkerModel());
		Span[] spans = chunker.chunkAsSpans(terms, tags);

		List<Span> result = new ArrayList<Span>();
		for (Span span : spans)
		{
			if (chunkType.name().equals(span.getType()))
			{
				result.add(span);
			}
		}

		return result;
	}

	public List<List<Entry<String, String>>> getChunks(
			List<Entry<String, String>> tagsByTerm, ChunkType chunkType)
	{
		List<Span> spans = getChunkSpans(tagsByTerm, chunkType);

		List<List<Entry<String, String>>> result = new ArrayList<List<Entry<String, String>>>();

		for (Span span : spans)
		{
			List<Entry<String, String>> phrase = tagsByTerm.subList(
					span.getStart(), span.getEnd());
			result.add(phrase);
		}

		return result;
	}

	/*public static void main(String[] args) throws Exception
	{
		InputStream modelIn = new FileInputStream(CHUNKER_MODEL_PATH);
		ChunkerModel model = new ChunkerModel(modelIn);

		ChunkerME chunker = new ChunkerME(model);

		String[] sent = new String[] { "Rockwell", "International", "Corp.",
				"'s", "Tulsa", "unit", "said", "it", "signed", "a",
				"tentative", "agreement", "extending", "its", "contract",
				"with", "Boeing", "Co.", "to", "provide", "structural",
				"parts", "for", "Boeing", "'s", "747", "jetliners", "." };

		String[] pos = new String[] { "NNP", "NNP", "NNP", "POS", "NNP", "NN",
				"VBD", "PRP", "VBD", "DT", "JJ", "NN", "VBG", "PRP$", "NN",
				"IN", "NNP", "NNP", "TO", "VB", "JJ", "NNS", "IN", "NNP",
				"POS", "CD", "NNS", "." };

		String[] tag = chunker.chunk(sent, pos);
		System.out.println(Arrays.toString(tag));

		Span[] spans = chunker.chunkAsSpans(sent, pos);
		System.out.println(Arrays.toString(spans));

	}*/
}
