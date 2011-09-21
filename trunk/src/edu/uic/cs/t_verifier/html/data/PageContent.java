package edu.uic.cs.t_verifier.html.data;

import java.util.List;

import edu.uic.cs.t_verifier.index.data.Segment;

public class PageContent
{
	private List<Segment> segments;
	// private UrlWithDescription pageUrlWithLinkDescription;
	private List<String> categoriesBelongsTo;

	public PageContent(List<Segment> segments, List<String> categoriesBelongsTo)
	{
		this.segments = segments;
		this.categoriesBelongsTo = categoriesBelongsTo;
	}

	public List<Segment> getSegments()
	{
		return segments;
	}

	public List<String> getCategoriesBelongsTo()
	{
		return categoriesBelongsTo;
	}

}
