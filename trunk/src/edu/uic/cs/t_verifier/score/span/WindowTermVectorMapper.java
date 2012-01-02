package edu.uic.cs.t_verifier.score.span;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.lucene.index.TermVectorMapper;
import org.apache.lucene.index.TermVectorOffsetInfo;

public class WindowTermVectorMapper extends TermVectorMapper
{
	public static class WindowEntry
	{
		private String term;
		//a term could appear more than once w/in a position
		private List<Integer> positions = new ArrayList<Integer>();

		private WindowEntry(String term)
		{
			this.term = term;
		}

		@Override
		public String toString()
		{
			/*return "WindowEntry{" + "term='" + term + '\'' + ", positions="
					+ positions + '}';*/
			return term + "(" + positions + ")";
		}

		public String getTerm()
		{
			return term;
		}

		public List<Integer> getPositions()
		{
			return positions;
		}

	}

	private int start;
	private int end;

	public WindowTermVectorMapper(int start, int end)
	{
		this.start = start;
		this.end = end;
	}

	private LinkedHashMap<String, WindowEntry> entries = new LinkedHashMap<String, WindowEntry>();

	public void map(String term, int frequency, TermVectorOffsetInfo[] offsets,
			int[] positions)
	{
		for (int i = 0; i < positions.length; i++)
		{
			//unfortunately, we still have to loop over the positions
			//we'll make this inclusive of the boundaries
			if (positions[i] >= start && positions[i] < end)
			{
				WindowEntry entry = entries.get(term);
				if (entry == null)
				{
					entry = new WindowEntry(term);
					entries.put(term, entry);
				}
				entry.positions.add(positions[i]);
			}
		}
	}

	public void setExpectations(String field, int numTerms,
			boolean storeOffsets, boolean storePositions)
	{
		// Do nothing for this example
		// See also the PositionBasedTermVectorMapper.
	}

	public Collection<WindowEntry> getAllEntriesInWindow()
	{
		return entries.values();
	}

	public void clean()
	{
		entries.clear();
	}

}
