package de.hpi.isg.swing;

import de.hpi.isg.utils.ColorSolution;

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
public class SheetDisplayLineTypeRowRenderer implements TableCellRenderer {

    private static final DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();

    public static final int OPACITY_PARAMETER = 64;

    private static final Color DEFAULT_COLOR = Color.WHITE;

//    private final List<Integer> emptyLineIndices;

//    public SheetDisplayLineTypeRowRenderer(List<Integer> emptyLineIndices) {
//        this.emptyLineIndices = emptyLineIndices;
//    }

    public SheetDisplayLineTypeRowRenderer() {
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = DEFAULT_RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (isSelected) {
            component.setBackground(Color.BLUE);
            return component;
        }
//        if (emptyLineIndices.contains(row)) {
//            component.setBackground(ColorSolution.EMPTY_LINE_BACKGROUND_COLOR);
//            return component;
//        } else {
//            component.setBackground(DEFAULT_COLOR);
//        }
        SheetDisplayTableModel tableModel = (SheetDisplayTableModel) table.getModel();
        component.setBackground(tableModel.getRowColor(row));
        return component;
    }
}