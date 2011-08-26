package edu.uic.cs.t_verifier.score;

import edu.uic.cs.t_verifier.index.IndexConstants;

public enum IndexBy
{
	SEGMENT
	{
		@Override
		public String toString()
		{
			return IndexConstants.FIELD_NAME__CONTENT_INDEXED_BY_SEGMENT;
		}
	},

	PARAGRAPH
	{
		@Override
		public String toString()
		{
			return IndexConstants.FIELD_NAME__CONTENT_INDEXED_BY_PARAGRAPH;
		}
	},

	SENTENCE
	{
		@Override
		public String toString()
		{
			return IndexConstants.FIELD_NAME__CONTENT_INDEXED_BY_SENTENCE;
		}
	};
}
