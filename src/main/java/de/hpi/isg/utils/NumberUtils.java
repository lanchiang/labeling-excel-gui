package de.hpi.isg.utils;

import java.util.regex.Pattern;

/**
 * @author lan
 * @since 2021/1/18
 */
public class NumberUtils extends org.apache.commons.lang3.math.NumberUtils {

    private final static String EUROPEAN_NUMBER_FORMAT = "-?\\d{1,3}([.]\\d{3})*([,]\\d{1,})?";
    private final static String US_NUMBER_FORMAT = "-?\\d{1,3}([,]\\d{3})*([.]\\d{1,})?";
    private final static String SPACE_THOUSAND_SEPARATOR_FORMAT = "-?\\d{1,3}(\\p{Z}\\d{3})+((,|.)\\d{1,})?";

    public static Double normalizeNumber(String numberStr) {
        Double normalizedNumber = null;
        if (numberStr == null) {
            return normalizedNumber;
        }
        String numStr = TextUtils.advancedTrim(numberStr);
        try {
            normalizedNumber = Double.parseDouble(numStr);
        } catch (NumberFormatException exception) {
            if (Pattern.matches(SPACE_THOUSAND_SEPARATOR_FORMAT, numStr)) {
                numStr = numStr.replaceAll("\\p{Z}", "").replaceAll(",", ".");
            } else if (Pattern.matches(US_NUMBER_FORMAT, numStr)) {
                numStr = numStr.replaceAll(",", "");
            } else if (Pattern.matches(EUROPEAN_NUMBER_FORMAT, numStr)) {
                numStr = numStr.replaceAll("\\.", "").replaceAll(",", ".");
            }
            try {
                normalizedNumber = Double.parseDouble(numStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return normalizedNumber;
    }

    public static class ErrorBoundMethod {
        public final static int ABSOLUTE_BOUND = 0;
        public final static int RATIO_BOUND = 1;
    }

    public static boolean isMinorError(double expected, double real, double errorBoundRatio, int method) {
        if (method == ErrorBoundMethod.ABSOLUTE_BOUND) {
            return Math.abs(expected - real) <= errorBoundRatio;
        } else if (method == ErrorBoundMethod.RATIO_BOUND) {
            return Math.abs((expected - real) / expected) < errorBoundRatio;
        } else {
            throw new RuntimeException("Specified error measurement not supported.");
        }
    }
}
