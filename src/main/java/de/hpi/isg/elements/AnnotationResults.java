package de.hpi.isg.elements;

import de.hpi.isg.swing.SheetDisplayTableModel;
import de.hpi.isg.utils.ColorSolution;
import lombok.Getter;

import java.security.InvalidParameterException;
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

    public AnnotationResults(final String excelFileName, final String spreadsheetName, final long timeExpense) {
        this.sheet = new Sheet(spreadsheetName, excelFileName);
        this.timeExpense = timeExpense;
    }

    public void annotate(SheetDisplayTableModel tableModel) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getRowColor(i).equals(ColorSolution.DEFAULT_BACKGROUND_COLOR)) {
                annotationResults.add(new AnnotationResult(i + 1, ColorSolution.getLineType(ColorSolution.EMPTY_LINE_BACKGROUND_COLOR).toString()));
            } else {
                annotationResults.add(new AnnotationResult(i + 1, ColorSolution.getLineType(tableModel.getRowColor(i)).toString()));
            }
        }
    }

    public static class AnnotationResult {

        @Getter
        private int lineNumber;

        @Getter
        private LineType type;

        public AnnotationResult(int lineNumber, String type) {
            this.lineNumber = lineNumber;
            this.type = getLineType(type);
        }

        private LineType getLineType(String type) {
            LineType innerType;
            switch (type) {
                case "PREAMBLE": {
                    innerType = LineType.PREAMBLE;
                    break;
                }
                case "HEADER": {
                    innerType = LineType.HEADER;
                    break;
                }
                case "DATA": {
                    innerType = LineType.DATA;
                    break;
                }
                case "AGGREGATION": {
                    innerType = LineType.AGGREGATION;
                    break;
                }
                case "FOOTNOTE": {
                    innerType = LineType.FOOTNOTE;
                    break;
                }
                case "GROUP_HEADER": {
                    innerType = LineType.GROUP_HEADER;
                    break;
                }
                case "EMPTY": {
                    innerType = LineType.EMPTY;
                    break;
                }
                default: {
                    throw new InvalidParameterException("The input line type is not parsable");
                }
            }
            return innerType;
        }
    }


    public enum LineType {
        PREAMBLE,
        HEADER,
        DATA,
        AGGREGATION,
        FOOTNOTE,
        GROUP_HEADER,
        EMPTY
    }
}
