package de.hpi.isg.io;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import de.hpi.isg.elements.AnnotationResults;
import de.hpi.isg.pojo.AnnotationPojo;
import de.hpi.isg.pojo.SpreadSheetPojo;
import lombok.Getter;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @author Lan Jiang
 * @since 9/18/19
 */
public class ResultCache<T> {

    @JsonProperty("result")
    @JacksonXmlElementWrapper(useWrapping = false)
    @Getter
    private Collection<T> resultCache = new LinkedList<>();

    public void addResultToCache(T result) {
        resultCache.add(result);
    }

    public SpreadSheetPojo convertToResultCacheFormat(AnnotationResults annotationResults) {
        String excelFileName = annotationResults.getSheet().getExcelFileName();
        String spreadsheetName = annotationResults.getSheet().getSheetName();
        long timeExpense = annotationResults.getTimeExpense();

        Collection<AnnotationPojo> results = new LinkedList<>();

        AnnotationResults.LineType lineType = annotationResults.getAnnotationResults().get(0).getType();
        int startLineNumber = 0;
        int endLineNumber = 0;
        for (int i = 1; i < annotationResults.getAnnotationResults().size(); i++) {
            AnnotationResults.AnnotationResult annotationResult = annotationResults.getAnnotationResults().get(i);
            if (annotationResult.getType().equals(lineType)) {
                endLineNumber = i;
            } else {
                results.add(new AnnotationPojo(startLineNumber + 1, endLineNumber + 1, lineType.toString()));
                startLineNumber = i;
                endLineNumber = i;
                lineType = annotationResult.getType();
            }
        }
        results.add(new AnnotationPojo(startLineNumber + 1, endLineNumber + 1, lineType.toString()));

        return new SpreadSheetPojo(spreadsheetName, excelFileName, timeExpense, results);
    }
}
