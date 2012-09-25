package edu.uic.cs.wiki_verifier.wiki.local.index;

public interface WikipediaIndexConstants
{
	String FIELD_NAME__PAGE_ID = "PAGE_ID";

	/**
	 * This field services as page's identification when we doing retrieval
	 */
	String FIELD_NAME__TITLE_IN_LOWER_CASE = "TITLE_IN_LOWER_CASE";

	String FIELD_NAME__TITLE_IN_NORMAL_CASE = "TITLE_IN_NORMAL_CASE";

	String FIELD_NAME__PAGE_REVISION_ID = "PAGE_REVISION_ID";
	String FIELD_NAME__TIMESTAMP = "TIMESTAMP";
	String FIELD_NAME__RAW_TEXT = "RAW_TEXT";
}
