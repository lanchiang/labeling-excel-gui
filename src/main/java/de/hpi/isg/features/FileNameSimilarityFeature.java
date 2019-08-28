package de.hpi.isg.features;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

import java.io.File;

/**
 * @author Lan Jiang
 * @since 8/28/19
 */
public class FileNameSimilarityFeature extends SheetSimilarityFeature {

    private final NormalizedLevenshtein levenshtein = new NormalizedLevenshtein();

    @Override
    double score(File file1, File file2) {
        String fileName1 = file1.getName().split("@")[0];
        String fileName2 = file2.getName().split("@")[0];
        return levenshtein.similarity(fileName1, fileName2);
    }
}
