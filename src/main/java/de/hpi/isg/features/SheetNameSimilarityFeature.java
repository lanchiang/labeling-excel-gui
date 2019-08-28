package de.hpi.isg.features;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

import java.io.File;

/**
 * @author Lan Jiang
 * @since 8/28/19
 */
public class SheetNameSimilarityFeature extends SheetSimilarityFeature {

    private final NormalizedLevenshtein levenshtein = new NormalizedLevenshtein();

    @Override
    double score(File file1, File file2) {
        String sheetName1 = file1.getName().split("@")[1].split(".csv")[0];
        String sheetName2 = file2.getName().split("@")[1].split(".csv")[0];
        return levenshtein.similarity(sheetName1, sheetName2);
    }
}
