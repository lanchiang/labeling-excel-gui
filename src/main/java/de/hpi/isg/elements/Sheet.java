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
    private final String fileName;

    public Sheet(String sheetName, String fileName) {
        this.sheetName = sheetName;
        this.fileName = fileName;
    }
}
