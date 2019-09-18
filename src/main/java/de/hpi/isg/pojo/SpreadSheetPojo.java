package de.hpi.isg.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * @author Lan Jiang
 * @since 9/18/19
 */
public class SpreadSheetPojo {

    @JsonProperty("spreadsheet_name")
    @Getter
    private String spreadsheetName;


}
