package de.hpi.isg.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Getter;

import java.util.Collection;

/**
 * @author Lan Jiang
 * @since 9/18/19
 */
public class SpreadSheetPojo {

    @JsonProperty("spreadsheet_name")
    @Getter
    private String spreadsheetName;

    @JsonProperty("excel_file_name")
    @Getter
    private String excelFileName;

    @JsonProperty("time_expense")
    @Getter
    private long timeExpense;

    @JsonProperty("annotations")
    @JacksonXmlElementWrapper(useWrapping = false)
    @Getter
    private Collection<AnnotationPojo> annotationPojos;

    public SpreadSheetPojo(String spreadsheetName, String excelFileName, long timeExpense, Collection<AnnotationPojo> annotationPojos) {
        this.spreadsheetName = spreadsheetName;
        this.excelFileName = excelFileName;
        this.timeExpense = timeExpense;
        this.annotationPojos = annotationPojos;
    }
}
