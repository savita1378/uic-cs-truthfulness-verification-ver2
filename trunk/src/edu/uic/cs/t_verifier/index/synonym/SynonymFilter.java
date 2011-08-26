package edu.uic.cs.t_verifier.index.synonym;

/**
 * Copyright Manning Publications Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific lan      
*/

import java.io.IOException;
import java.util.Stack;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;

public class SynonymFilter extends TokenFilter
{
	public static final String TOKEN_TYPE_SYNONYM = "SYNONYM";

	private Stack<String> synonymStack;
	private SynonymEngine engine;
	private AttributeSource.State current;

	private final CharTermAttribute termAtt;
	private final PositionIncrementAttribute posIncrAtt;

	public SynonymFilter(TokenStream in, SynonymEngine engine)
	{
		super(in);
		synonymStack = new Stack<String>();
		this.engine = engine;

		this.termAtt = addAttribute(CharTermAttribute.class);
		this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	}

	public boolean incrementToken() throws IOException
	{
		if (synonymStack.size() > 0)
		{
			String syn = synonymStack.pop();
			restoreState(current);
			termAtt.copyBuffer(syn.toCharArray(), 0, syn.length());
			posIncrAtt.setPositionIncrement(0);
			return true;
		}

		if (!input.incrementToken())
		{
			return false;
		}

		if (addAliasesToStack())
		{
			current = captureState();
		}

		return true;
	}

	private boolean addAliasesToStack() throws IOException
	{
		String[] synonyms = engine.getSynonyms(termAtt.toString());
		if (synonyms == null)
		{
			return false;
		}

		for (String synonym : synonyms)
		{
			synonymStack.push(synonym);
		}

		return true;
	}
}
