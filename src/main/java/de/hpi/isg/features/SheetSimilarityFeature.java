package de.hpi.isg.features;

import java.io.File;

/**
 *
 *
 * @author Lan Jiang
 * @since 8/28/19
 */
abstract class SheetSimilarityFeature {

    // spreadsheet file name similarity
    // sheet count
    // sheet name similarity (pair-wise)

    // these are suspended
    // extend_name ?
    // number of figures
    // content similarity

    /**
     * Calculate the feature score between the two given files.
     * @param file1 the first given file
     * @param file2 the second given file
     * @return the feature score, between zero and one, inclusively
     */
    abstract double score(File file1, File file2);
}
