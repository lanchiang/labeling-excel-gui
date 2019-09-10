package de.hpi.isg.dao;

import de.hpi.isg.elements.AnnotationResults;

/**
 * This interface defines a bunch of queries to the database.
 *
 * @author Lan Jiang
 * @since 9/9/19
 */
public interface AbstractQueries {

    void insertLineFunctionAnnotationResults(AnnotationResults results);

    int getDataFileIdByDataFileNameAndSpreadsheetName(String dataFileName, String spreadSheetName);

}
