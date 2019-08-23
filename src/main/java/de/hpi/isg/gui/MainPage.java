package de.hpi.isg.gui;

import javax.swing.*;
import java.awt.*;

/**
 * @author Lan Jiang
 * @since 8/23/19
 */
public class MainPage {

    public void run() {
        JFrame frame = new JFrame("Excel File Line Function Annotator");

        createExcelFileDisplayTable(frame);

        createStartAndEndLineTextFields(frame);
        createLineTypeDropList(frame);
        createSubmitButton(frame);

        createLabelInfoTable(frame);

        configFrame(frame);
    }

    private void configFrame(JFrame frame) {
        frame.setLayout(new FlowLayout());
        frame.setSize(400, 300);
        frame.setVisible(true);
    }

    private void createExcelFileDisplayTable(JFrame frame) {

    }

    private void createStartAndEndLineTextFields(JFrame frame) {
        JTextField start, end;
        start = new JTextField();
        start.setPreferredSize(new Dimension(50, 20));
        end = new JTextField();
        end.setPreferredSize(new Dimension(50, 20));
        frame.add(start);
        frame.add(end);
    }

    private void createSubmitButton(JFrame frame) {
        JButton submitButton = new JButton("Submit!");
        frame.add(submitButton);
    }

    private void createLineTypeDropList(JFrame frame) {
        String[] lineType = {"P", "H", "D", "A", "F", "G", "E"};
        JComboBox<String> lineTypeComboBox = new JComboBox<>(lineType);
        frame.add(lineTypeComboBox);
    }

    private void createLabelInfoTable(JFrame frame) {
        String[] column = {"Start Line", "End Line", "Area Type"};
        JTable table = new JTable(new String[][]{}, column);
        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane);
    }

    // main class
    public static void main(String[] args) {
        // create a new frame
        new MainPage().run();
    }
}
