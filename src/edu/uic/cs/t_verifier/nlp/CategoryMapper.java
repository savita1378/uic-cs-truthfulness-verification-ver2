package edu.uic.cs.t_verifier.nlp;

import java.util.Collection;

import edu.uic.cs.t_verifier.nlp.StatementTypeIdentifier.StatementType;

public interface CategoryMapper
{
	StatementType mapCategoryIntoStatementType(String category);

	StatementType mapCategoriesIntoStatementType(Collection<String> categories);
}
