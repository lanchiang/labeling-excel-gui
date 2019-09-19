package de.hpi.isg.utils;

import de.hpi.isg.elements.AnnotationResults;
import de.hpi.isg.swing.SheetDisplayLineTypeRowRenderer;

import java.awt.*;

/**
 *
 * @author Lan Jiang
 * @since 8/26/19
 */
public class ColorSolution {

    public static Color EMPTY_LINE_BACKGROUND_COLOR = new Color(255, 255, 0, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color DEFAULT_BACKGROUND_COLOR = new Color(255, 255, 255, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color BLACK = new Color(0, 0, 0, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color PREAMBLE_BACKGROUND_COLOR = new Color(0, 0, 255, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color CYAN = new Color(0, 255, 255, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color HEADER_BACKGROUND_COLOR = new Color(255, 0, 255, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color DATA_BACKGROUND_COLOR = new Color(0, 255, 0, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color ORANGE = new Color(255, 200, 0, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color FOOTNOTE_BACKGROUND_COLOR = new Color(255, 175, 175, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color GROUND_HEADER_BACKGROUND_COLOR = new Color(255, 0, 0, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color AGGREGATION_BACKGROUND_COLOR = new Color(64, 64, 64, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color GREY  = new Color(128, 128, 128, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);
    public static Color LIGHTGREY = new Color(192, 192, 192, SheetDisplayLineTypeRowRenderer.OPACITY_PARAMETER);

    public static Color getColor(String string) {
        Color color;
        switch (string) {
            case LineTypeUtils.PREAMBLE: {
                color = PREAMBLE_BACKGROUND_COLOR;
                break;
            }
            case LineTypeUtils.HEADER: {
                color = HEADER_BACKGROUND_COLOR;
                break;
            }
            case LineTypeUtils.DATA: {
                color = DATA_BACKGROUND_COLOR;
                break;
            }
            case LineTypeUtils.AGGREGATION: {
                color = AGGREGATION_BACKGROUND_COLOR;
                break;
            }
            case LineTypeUtils.FOOTNOTE: {
                color = FOOTNOTE_BACKGROUND_COLOR;
                break;
            }
            case LineTypeUtils.GROUP_HEADER: {
                color = GROUND_HEADER_BACKGROUND_COLOR;
                break;
            }
            default: {
                color = EMPTY_LINE_BACKGROUND_COLOR;
            }
        }
        return color;
    }

    public static String getLineType(Color color) {
        String lineType;
        if (color.equals(PREAMBLE_BACKGROUND_COLOR)) {
            lineType = LineTypeUtils.PREAMBLE;
        } else if (color.equals(HEADER_BACKGROUND_COLOR)) {
            lineType = LineTypeUtils.HEADER;
        } else if (color.equals(DATA_BACKGROUND_COLOR)) {
            lineType = LineTypeUtils.DATA;
        } else if (color.equals(AGGREGATION_BACKGROUND_COLOR)) {
            lineType = LineTypeUtils.AGGREGATION;
        } else if (color.equals(FOOTNOTE_BACKGROUND_COLOR)) {
            lineType = LineTypeUtils.FOOTNOTE;
        } else if (color.equals(GROUND_HEADER_BACKGROUND_COLOR)) {
            lineType = LineTypeUtils.GROUP_HEADER;
        } else if (color.equals(EMPTY_LINE_BACKGROUND_COLOR)) {
            lineType = LineTypeUtils.EMPTY;
        } else {
            return null;
        }
        return lineType;
    }
}
