package de.hpi.isg.features;

import java.io.File;

/**
 * @author Lan Jiang
 * @since 8/28/19
 */
public class SheetNameSimilarityFeature extends SheetSimilarityFeature {
    @Override
    double score(File file1, File file2) {
        return 0;
    }
}
