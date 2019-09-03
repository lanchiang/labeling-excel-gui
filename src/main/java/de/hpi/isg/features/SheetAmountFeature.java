package de.hpi.isg.features;

import de.hpi.isg.elements.Sheet;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Lan Jiang
 * @since 8/28/19
 */
public class SheetAmountFeature extends SheetSimilarityFeature {

    private final Map<String, List<String>> sheetNamesByFileName;

    public SheetAmountFeature(Map<String, List<String>> sheetAmountByFileName) {
        this.sheetNamesByFileName = sheetAmountByFileName;
    }

    @Override
    public double score(File file1, File file2, Map<String, Sheet> sheets) {
        String fileName1 = sheets.get(file1.getName()).getFileName();
        String fileName2 = sheets.get(file2.getName()).getFileName();
        int sheetAmount1 = sheetNamesByFileName.get(fileName1).size();
        int sheetAmount2 = sheetNamesByFileName.get(fileName2).size();

        if (sheetAmount1 == 0 || sheetAmount2 == 0) {
            System.out.println("Sheet amount equals to zero.");
            return 0;
        }

        double score;
        if (sheetAmount1 > sheetAmount2) {
            score = (double) sheetAmount2 / (double) sheetAmount1;
        } else {
            score = (double) sheetAmount1 / (double) sheetAmount2;
        }
        return score;
    }
}
