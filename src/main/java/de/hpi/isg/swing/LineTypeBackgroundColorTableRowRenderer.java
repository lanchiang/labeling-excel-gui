package de.hpi.isg.swing;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.Color;
import java.util.List;

/**
 * @author Lan Jiang
 * @since 9/13/19
 */
public class LineTypeBackgroundColorTableRowRenderer implements TableCellRenderer {

    public static final DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();

    private static final int OPACITY_PARAMETER = 96;

    private static final Color DEFAULT_COLOR = ColorSolution.WHITE;

    private final List<Integer> emptyLineIndices;

    public LineTypeBackgroundColorTableRowRenderer(List<Integer> emptyLineIndices) {
        this.emptyLineIndices = emptyLineIndices;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = DEFAULT_RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (isSelected) {
            component.setBackground(Color.BLUE);
            return component;
        }
        if (emptyLineIndices.contains(row)) {
            component.setBackground(ColorSolution.YELLOW);
        } else {
            component.setBackground(DEFAULT_COLOR);
        }
        return component;
    }

    public static class ColorSolution {
        static Color YELLOW = new Color(255, 255, 0, OPACITY_PARAMETER);
        static Color WHITE = new Color(255, 255, 255, OPACITY_PARAMETER);
        static Color BLACK = new Color(0, 0, 0, OPACITY_PARAMETER);
        static Color BLUE = new Color(0, 0, 255, OPACITY_PARAMETER);
        static Color CYAN = new Color(0, 255, 255, OPACITY_PARAMETER);
        static Color MAGENTA = new Color(255, 0, 255, OPACITY_PARAMETER);
        static Color GREEN = new Color(0, 255, 0, OPACITY_PARAMETER);
        static Color ORANGE = new Color(255, 200, 0, OPACITY_PARAMETER);
        static Color PINK = new Color(255, 175, 175, OPACITY_PARAMETER);
        static Color RED = new Color(255, 0, 0, OPACITY_PARAMETER);
        static Color DARKGREY = new Color(64, 64, 64, OPACITY_PARAMETER);
        static Color GREY  = new Color(128, 128, 128, OPACITY_PARAMETER);
        static Color LIGHTGREY = new Color(192, 192, 192, OPACITY_PARAMETER);
    }
}
