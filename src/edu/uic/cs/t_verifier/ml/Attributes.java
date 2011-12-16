package edu.uic.cs.t_verifier.ml;

public interface Attributes
{
	String ATTR_VALUE_TRUE = "true";
	String ATTR_VALUE_FALSE = "false";
	String[] ATTR_DOMAIN_TRUE_FALSE = new String[] { ATTR_VALUE_TRUE,
			ATTR_VALUE_FALSE };

	/*interface ATTR_CATEGORY
	{
		String name = ATTR_CATEGORY.class.getSimpleName();
		String[] domain = Category.toStringList();
	}*/

	interface ATTR_MATCHED_BY_AU
	{
		String name = ATTR_MATCHED_BY_AU.class.getSimpleName();
		String[] domain = ATTR_DOMAIN_TRUE_FALSE;
	}

	interface ATTR_MATCHED_BY_TU
	{
		String name = ATTR_MATCHED_BY_TU.class.getSimpleName();
		String[] domain = ATTR_DOMAIN_TRUE_FALSE;
	}

	interface ATTR_AU_TOTAL_NUM_TO_MATCH
	{
		String name = ATTR_AU_TOTAL_NUM_TO_MATCH.class.getSimpleName();
	}

	interface ATTR_AU_MAX_NUM_MATCHED
	{
		String name = ATTR_AU_MAX_NUM_MATCHED.class.getSimpleName();
	}

	/**
	 * The minimum window size for the match with maximum terms
	 * 
	 * @author Hong Wang
	 *
	 */
	interface ATTR_AU_MIN_WIN_SIZE //_FOR_MAX_MATCH
	{
		String name = ATTR_AU_MIN_WIN_SIZE.class.getSimpleName();
	}

	interface ATTR_TU_TOTAL_NUM_TO_MATCH
	{
		String name = ATTR_TU_TOTAL_NUM_TO_MATCH.class.getSimpleName();
	}

	interface ATTR_TU_MAX_NUM_MATCHED
	{
		String name = ATTR_TU_MAX_NUM_MATCHED.class.getSimpleName();
	}

	/**
	 * The minimum window size for the match with maximum terms
	 * 
	 * @author Hong Wang
	 *
	 */
	interface ATTR_TU_MIN_WIN_SIZE //_FOR_MAX_MATCH
	{
		String name = ATTR_TU_MIN_WIN_SIZE.class.getSimpleName();
	}
}
