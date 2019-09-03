package de.hpi.isg.features;

import de.hpi.isg.elements.Sheet;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

import java.io.File;
import java.util.Map;

/**
 * @author Lan Jiang
 * @since 8/28/19
 */
public class SheetNameSimilarityFeature extends SheetSimilarityFeature {

    private final NormalizedLevenshtein levenshtein = new NormalizedLevenshtein();

    @Override
    public double score(File file1, File file2, Map<String, Sheet> sheets) {
        String sheetName1 = sheets.get(file1.getName()).getSheetName();
        String sheetName2 = sheets.get(file2.getName()).getSheetName();
        return levenshtein.similarity(sheetName1, sheetName2);
    }
}
