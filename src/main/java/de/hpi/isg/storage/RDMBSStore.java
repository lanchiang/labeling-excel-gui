package de.hpi.isg.storage;

import de.hpi.isg.dao.QueryHandler;
import de.hpi.isg.elements.AnnotationResults;
import de.hpi.isg.elements.Sheet;

import java.util.List;

/**
 * @author Lan Jiang
 * @since 9/18/19
 */
public class RDMBSStore extends Store {

    private final QueryHandler queryHandler;

    public RDMBSStore(List<Sheet> spreadsheetPool, QueryHandler queryHandler) {
        super(spreadsheetPool);
        this.queryHandler = queryHandler;
    }

    @Override
    public void addAnnotation(AnnotationResults results) {
        this.queryHandler.insertLineFunctionAnnotationResults(results);
        this.queryHandler.updateSpreadsheetAnnotationStatus(results.getSheet().getSheetName(), results.getSheet().getExcelFileName());
        this.queryHandler.insertTimeCost(results, results.getTimeExpense());
    }
}
