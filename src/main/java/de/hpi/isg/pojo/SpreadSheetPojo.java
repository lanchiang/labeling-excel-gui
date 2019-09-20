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
    private final String spreadsheetName;

    @JsonProperty("excel_file_name")
    @Getter
    private final String excelFileName;

    @JsonProperty("time_expense")
    @Getter
    private final long timeExpense;

    @JsonProperty("is_multitable_file")
    @Getter
    private final String isMultitableFile;

    @JsonProperty("annotations")
    @JacksonXmlElementWrapper(useWrapping = false)
    @Getter
    private final Collection<AnnotationPojo> annotationPojos;

    public SpreadSheetPojo(String spreadsheetName, String excelFileName, long timeExpense, boolean isMultitableFile, Collection<AnnotationPojo> annotationPojos) {
        this.spreadsheetName = spreadsheetName;
        this.excelFileName = excelFileName;
        this.timeExpense = timeExpense;
        this.isMultitableFile = isMultitableFile ? "true" : "false";
        this.annotationPojos = annotationPojos;
    }
}
