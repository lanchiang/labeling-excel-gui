package de.hpi.isg.elements;

import de.hpi.isg.swing.SheetDisplayTableModel;
import de.hpi.isg.utils.ColorSolution;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

/**
 * This class represent an object to store the annotation information of a data file.
 *
 * @author Lan Jiang
 * @since 9/3/19
 */
public class AnnotationResults {

    @Getter
    private final Sheet sheet;

    @Getter
    private final List<AnnotationResult> annotationResults = new LinkedList<>();

    @Getter
    private final long timeExpense;

    @Getter
    private final boolean isMultitableFile;

    public AnnotationResults(final String excelFileName, final String spreadsheetName, final long timeExpense) {
        this.sheet = new Sheet(spreadsheetName, excelFileName);
        this.timeExpense = timeExpense;
        this.isMultitableFile = false;
    }

    public AnnotationResults(final String excelFileName, final String spreadsheetName, final long timeExpense, boolean isMultitableFile) {
        this.sheet = new Sheet(spreadsheetName, excelFileName);
        this.timeExpense = timeExpense;
        this.isMultitableFile = isMultitableFile;
    }

    public void annotate(SheetDisplayTableModel tableModel) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getRowColor(i).equals(ColorSolution.DEFAULT_BACKGROUND_COLOR)) {
                annotationResults.add(new AnnotationResult(i + 1, ColorSolution.getLineType(ColorSolution.EMPTY_LINE_BACKGROUND_COLOR)));
            } else {
                annotationResults.add(new AnnotationResult(i + 1, ColorSolution.getLineType(tableModel.getRowColor(i))));
            }
        }
    }

    public static class AnnotationResult {
        @Getter
        private int lineNumber;

        @Getter
        private String type;

        public AnnotationResult(int lineNumber, String type) {
            this.lineNumber = lineNumber;
            this.type = type;
        }
    }
}
