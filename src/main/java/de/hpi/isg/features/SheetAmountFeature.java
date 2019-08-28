package de.hpi.isg.features;

import java.io.File;
import java.util.Map;

/**
 * @author Lan Jiang
 * @since 8/28/19
 */
public class SheetAmountFeature extends SheetSimilarityFeature {

    private final Map<String, Integer> sheetAmountByFileName;

    public SheetAmountFeature(Map<String, Integer> sheetAmountByFileName) {
        this.sheetAmountByFileName = sheetAmountByFileName;
    }

    @Override
    double score(File file1, File file2) {
        String fileName1 = file1.getName().split("@")[0];
        String fileName2 = file2.getName().split("@")[0];
        int sheetAmount1 = sheetAmountByFileName.get(fileName1);
        int sheetAmount2 = sheetAmountByFileName.get(fileName2);

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
