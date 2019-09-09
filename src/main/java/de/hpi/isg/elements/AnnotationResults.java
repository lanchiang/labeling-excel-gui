package de.hpi.isg.elements;

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

    private final List<AnnotationResult> annotationResults = new LinkedList<>();

    public AnnotationResults(final String fileName, final String sheetName) {
        this.sheet = new Sheet(sheetName, fileName);
    }

    public void addAnnotation(final int startLineNumber,
                              final int endLineNumber,
                              String type) {
        annotationResults.add(new AnnotationResult(startLineNumber, endLineNumber, type));
    }

    public static class AnnotationResult {

        @Getter
        private int startLineNumber;

        @Getter
        private int endLineNumber;

        @Getter
        private LineType type;

        public AnnotationResult(int startLineNumber, int endLineNumber, String type) {
            this.startLineNumber = startLineNumber;
            this.endLineNumber = endLineNumber;
            this.type = getLineType(type);
        }

        private LineType getLineType(String type) {
            LineType innerType;
            switch (type) {
                case "P": {
                    innerType = LineType.PREAMBLE;
                    break;
                }
                case "H": {
                    innerType = LineType.HEADER;
                    break;
                }
                case "D": {
                    innerType = LineType.DATA;
                    break;
                }
                case "A": {
                    innerType = LineType.AGGREGATION;
                    break;
                }
                case "F": {
                    innerType = LineType.FOOTNOTE;
                    break;
                }
                case "G": {
                    innerType = LineType.GROUP_HEADER;
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
        GROUP_HEADER
    }
}
