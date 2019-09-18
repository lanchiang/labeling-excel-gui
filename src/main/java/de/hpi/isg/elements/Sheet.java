package de.hpi.isg.elements;

import lombok.Getter;

/**
 * This entity represents a sheet.
 *
 * @author Lan Jiang
 * @since 8/28/19
 */
public class Sheet {

    @Getter
    private final String sheetName;

    @Getter
    private final String excelFileName;

    /**
     * Number of spreadsheets in the excel file that contains this spreadsheet.
     */
    @Getter
    private final int numOfSpreadsheetsOfExcelFile;

    public Sheet(String sheetName, String excelFileName) {
        this(sheetName, excelFileName, 0);
    }

    public Sheet(String sheetName, String excelFileName, int numOfSpreadsheetsOfExcelFile) {
        this.sheetName = sheetName;
        this.excelFileName = excelFileName;
        this.numOfSpreadsheetsOfExcelFile = numOfSpreadsheetsOfExcelFile;
    }
}
