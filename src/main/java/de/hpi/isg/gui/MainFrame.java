package de.hpi.isg.gui;

import com.opencsv.CSVReader;
import de.hpi.isg.dao.QueryHandler;
import de.hpi.isg.elements.AnnotationResults;
import de.hpi.isg.elements.Sheet;
import de.hpi.isg.features.FileNameSimilarityFeature;
import de.hpi.isg.features.SheetAmountFeature;
import de.hpi.isg.features.SheetNameSimilarityFeature;
import de.hpi.isg.features.SheetSimilarityFeature;
import de.hpi.isg.io.SheetSimilarityCalculator;
import de.hpi.isg.swing.SheetDisplayLineTypeRowRenderer;
import de.hpi.isg.swing.RowNumberTable;
import de.hpi.isg.swing.SheetDisplayTableModel;
import de.hpi.isg.utils.ColorSolution;
import de.hpi.isg.utils.LabelCollideDealStrategy;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Lan Jiang
 * @since 8/26/19
 */
public class MainFrame {

    private JPanel mainPagePanel;
    private JTextField startLine;
    private JTextField endLine;
    private JComboBox lineTypeComboBox;
    private JButton submitAndNextFileButton;
    private JButton submitAndFinishButton;
    private JTable sheetDisplayTable;
    private JPanel labelOperatingPanel;
    private JLabel startLineLabel;
    private JLabel endLineLabel;
    private JLabel lineTypeLabel;
    private JLabel titleLabel;
    private JPanel submitPanel;
    private JScrollPane sheetDisplayPane;
    private JTable labeledInfoTable;
    private JButton addButton;
    private JButton loadAllFilesButton;
    private JLabel loadedFileLabel;
    private JLabel loadedFileNumberLabel;
    private JButton deleteButton;
    private JLabel numOfLines;
    private JLabel numOfLinesLabel;
    private JLabel numOfColumns;
    private JPanel numOfColumnsLabel;

    private File[] loadedFiles;

    private File currentFile;

    private Sheet currentSheet;

    private SheetSimilarityCalculator calculator;

    private long startTime;

    private long endTime;

    private final static LabelCollideDealStrategy LABEL_COLLIDE_DEAL_STRATEGY = LabelCollideDealStrategy.OVERWRITE;

    @Getter
    private QueryHandler queryHandler = new QueryHandler();

    public MainFrame() {
        $$$setupUI$$$();
        submitAndFinishButton.addActionListener(e -> {
        });
        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                addToLabelInfoTable();
            }
        });
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                DefaultTableModel tableModel = (DefaultTableModel) labeledInfoTable.getModel();
                Vector row = (Vector) tableModel.getDataVector().elementAt(labeledInfoTable.getSelectedRow());
                int startIndex = Integer.parseInt(String.valueOf(row.elementAt(0)));
                int endIndex = Integer.parseInt(String.valueOf(row.elementAt(1)));

                tableModel.removeRow(labeledInfoTable.getSelectedRow());
                labeledInfoTable.getSelectionModel().clearSelection();

                SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
                sheetDisplayTableModel.setRowsBackgroundColor(startIndex, endIndex, ColorSolution.DEFAULT_BACKGROUND_COLOR);
                sheetDisplayTableModel.setEmptyRowBackground(startIndex, endIndex);
            }
        });
        submitAndNextFileButton.addActionListener(e -> {
            endTime = System.currentTimeMillis();
            long duration = 0L;
            if (startTime != 0L) {
                duration = endTime - startTime;
            }
            startTime = endTime;
//            DefaultTableModel tableModel = (DefaultTableModel) sheetDisplayTable.getModel();
//            if (tableModel.getColumnCount() != 0 || tableModel.getRowCount() != 0) {
//                DefaultTableModel labeledInfoTableModel = (DefaultTableModel) labeledInfoTable.getModel();
//                int columnCount = labeledInfoTableModel.getColumnCount();
//                Validate.isTrue(columnCount == 3);
//
//                // check whether the label info table is empty.
//                if (labeledInfoTableModel.getRowCount() == 0) {
//                    JOptionPane.showMessageDialog(null, "Please enter some labels for this data file.");
//                    return;
//                }
//
//                String[] nameSplits = currentFile.getName().split("@");
//                String fileName = nameSplits[0];
//                String sheetName = nameSplits[1].split(".csv")[0];
//
//                AnnotationResults results = new AnnotationResults(fileName, sheetName, duration);
//                Vector dataVector = labeledInfoTableModel.getDataVector();
//                for (int i = 0; i < labeledInfoTableModel.getRowCount(); i++) {
//                    Vector row = (Vector) dataVector.elementAt(i);
//                    int startLineNumber = Integer.parseInt(row.elementAt(0).toString());
//                    int endLineNumber = Integer.parseInt(row.elementAt(1).toString());
//                    String lineType = String.valueOf(row.elementAt(2));
//                    results.addAnnotation(startLineNumber, endLineNumber, lineType);
//                }
//
//                this.queryHandler.insertLineFunctionAnnotationResults(results);
//                this.queryHandler.updateSpreadsheetAnnotationStatus(sheetName, fileName);
//                this.queryHandler.insertTimeCost(results, duration);
//
//                // get the most similar file
//                List<Sheet> sheets = this.queryHandler.getAllUnannotatedSpreadsheet();
//                Sheet mostSimilarSheet = findMostSimilarSpreadsheet(currentSheet, sheets);
//                currentFile = calculator.getMostSimilarFile(mostSimilarSheet);
//                currentSheet = mostSimilarSheet;
//
//                this.labeledInfoTable.setModel(new DefaultTableModel(new String[]{"Start Line", "End Line", "Line Type"}, 0));
//            } else {
//                // load a random new table
//                Random random = new Random(System.currentTimeMillis());
//                int selectedIndex = random.nextInt(loadedFiles.length);
//
//                currentFile = loadedFiles[selectedIndex];
//
//                String[] nameSplits = currentFile.getName().split("@");
//                String fileName = nameSplits[0];
//                String sheetName = nameSplits[1].split(".csv")[0];
//
//                int amount = this.queryHandler.getSheetAmountByExcelName(fileName);
//                currentSheet = new Sheet(sheetName, fileName, amount);
//            }

            currentFile = new File("/Users/Fuga/Documents/hpi/data/excel-to-csv/data-gov-uk/mappa-annual-report-13-14-tables.xls@Contents.csv");

            System.out.println(currentFile.getName());

            try {
                loadFile(currentFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        loadAllFilesButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("/Users/Fuga/Documents/hpi/data/excel-to-csv"));
            chooser.setDialogTitle("Dialog title");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            int choiceCode = chooser.showOpenDialog(loadAllFilesButton);
            if (choiceCode == JFileChooser.APPROVE_OPTION) {
                File selectedDir = chooser.getSelectedFile();
                loadedFiles = selectedDir.listFiles();
                assert loadedFiles != null;
                loadedFileNumberLabel.setText(String.valueOf(loadedFiles.length));

                calculator = new SheetSimilarityCalculator(loadedFiles);

                this.queryHandler.loadExcelFileStatistics(calculator.getSheetNamesByFileName());

                submitAndNextFileButton.setEnabled(true);
                submitAndFinishButton.setEnabled(true);
            }
        });
        submitAndFinishButton.addActionListener(e -> {
            endTime = System.currentTimeMillis();
            long duration;
            if (startTime != 0L) {
                duration = endTime - startTime;
            }
            // write the results into a json file.
        });
        labeledInfoTable.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                deleteButton.setEnabled(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                deleteButton.setEnabled(false);
            }
        });
        ListSelectionModel sheetDisplayTableSelectionModel = sheetDisplayTable.getSelectionModel();
        sheetDisplayTableSelectionModel.addListSelectionListener(e -> {
            if (!sheetDisplayTableSelectionModel.isSelectionEmpty()) {
                int startIndex = sheetDisplayTableSelectionModel.getMinSelectionIndex() + 1;
                int endIndex = sheetDisplayTableSelectionModel.getMaxSelectionIndex() + 1;
                if (endIndex - startIndex >= 0) {
                    this.endLine.setText(String.valueOf(endIndex));
                    this.startLine.setText(String.valueOf(startIndex));
                }
            }
        });

        JPopupMenu lineTypePopupMenu = getLineTypePopupMenu();

        sheetDisplayTable.setComponentPopupMenu(lineTypePopupMenu);

        sheetDisplayTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int startLineNumber = Integer.parseInt(startLine.getText());
                int endLineNumber = Integer.parseInt(endLine.getText());

                char pressedKeyChar = e.getKeyChar();
                SheetDisplayTableModel tableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
                Color selectedColor;
                String lineType = null;
                switch (pressedKeyChar) {
                    case 'P': case 'p': {
                        selectedColor = ColorSolution.PREAMBLE_BACKGROUND_COLOR;
                        lineType = "Preamble (P)";
                        break;
                    }
                    case 'H': case 'h': {
                        selectedColor = ColorSolution.HEADER_BACKGROUND_COLOR;
                        lineType = "Header (H)";
                        break;
                    }
                    case 'D': case 'd': {
                        selectedColor = ColorSolution.DATA_BACKGROUND_COLOR;
                        lineType = "Data (D)";
                        break;
                    }
                    case 'A': case 'a': {
                        selectedColor = ColorSolution.AGGREGATION_BACKGROUND_COLOR;
                        lineType = "Aggregation (A)";
                        break;
                    }
                    case 'F': case 'f': {
                        selectedColor = ColorSolution.FOOTNOTE_BACKGROUND_COLOR;
                        lineType = "Footnote (F)";
                        break;
                    }
                    case 'G': case 'g': {
                        selectedColor = ColorSolution.GROUND_HEADER_BACKGROUND_COLOR;
                        lineType = "Group header (G)";
                        break;
                    }
                    default: return;
                }
                tableModel.setRowsBackgroundColor(startLineNumber, endLineNumber, selectedColor);
                addToLabelInfo(startLineNumber, endLineNumber, lineType);
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Excel File Line Function Annotator");
        frame.setContentPane(new MainFrame().mainPagePanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPagePanel = new JPanel();
        mainPagePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPagePanel.setBorder(BorderFactory.createTitledBorder(""));
        titleLabel = new JLabel();
        titleLabel.setText("Excel File Line Function Annotator");
        mainPagePanel.add(titleLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        mainPagePanel.add(sheetDisplayPane, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 1, false));
        sheetDisplayPane.setBorder(BorderFactory.createTitledBorder("Spreedsheet"));
        numOfColumnsLabel = new JPanel();
        numOfColumnsLabel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 5, new Insets(0, 0, 0, 0), -1, -1));
        mainPagePanel.add(numOfColumnsLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(909, 130), null, 0, false));
        labelOperatingPanel = new JPanel();
        labelOperatingPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 4, new Insets(0, 0, 0, 0), -1, -1));
        numOfColumnsLabel.add(labelOperatingPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(311, 56), null, 1, false));
        startLineLabel = new JLabel();
        startLineLabel.setText("Start Line");
        labelOperatingPanel.add(startLineLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        endLineLabel = new JLabel();
        endLineLabel.setText("End Line");
        labelOperatingPanel.add(endLineLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        lineTypeLabel = new JLabel();
        lineTypeLabel.setText("Line Function Type");
        lineTypeLabel.setVerticalAlignment(0);
        lineTypeLabel.setVerticalTextPosition(0);
        labelOperatingPanel.add(lineTypeLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(151, 16), null, 1, false));
        endLine = new JTextField();
        labelOperatingPanel.add(endLine, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 2, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, -1), new Dimension(50, -1), new Dimension(50, -1), 2, false));
        lineTypeComboBox = new JComboBox();
        lineTypeComboBox.setEditable(false);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("-");
        defaultComboBoxModel1.addElement("Preamble (P)");
        defaultComboBoxModel1.addElement("Header (H)");
        defaultComboBoxModel1.addElement("Data (D)");
        defaultComboBoxModel1.addElement("Aggregation (A)");
        defaultComboBoxModel1.addElement("Footnote (F)");
        defaultComboBoxModel1.addElement("Group Header (G)");
        lineTypeComboBox.setModel(defaultComboBoxModel1);
        labelOperatingPanel.add(lineTypeComboBox, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 2, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, -1), new Dimension(151, 27), new Dimension(160, -1), 2, false));
        startLine = new JTextField();
        labelOperatingPanel.add(startLine, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 2, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, -1), new Dimension(50, -1), new Dimension(50, -1), 2, false));
        addButton = new JButton();
        addButton.setText("Add");
        labelOperatingPanel.add(addButton, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setEnabled(false);
        deleteButton.setText("Delete");
        labelOperatingPanel.add(deleteButton, new com.intellij.uiDesigner.core.GridConstraints(2, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        numOfColumnsLabel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 3, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setBorder(BorderFactory.createTitledBorder("Labeled Lines"));
        scrollPane1.setViewportView(labeledInfoTable);
        submitPanel = new JPanel();
        submitPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        numOfColumnsLabel.add(submitPanel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(311, 31), null, 2, false));
        submitAndNextFileButton = new JButton();
        submitAndNextFileButton.setEnabled(true);
        submitAndNextFileButton.setText("Submit and Next File");
        submitPanel.add(submitAndNextFileButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        submitAndFinishButton = new JButton();
        submitAndFinishButton.setEnabled(false);
        submitAndFinishButton.setText("Submit and Finish");
        submitPanel.add(submitAndFinishButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        numOfColumnsLabel.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        loadAllFilesButton = new JButton();
        loadAllFilesButton.setText("Load All files");
        panel1.add(loadAllFilesButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loadedFileLabel = new JLabel();
        loadedFileLabel.setText("File Loaded:");
        panel1.add(loadedFileLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loadedFileNumberLabel = new JLabel();
        loadedFileNumberLabel.setText("0");
        panel1.add(loadedFileNumberLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numOfLinesLabel = new JLabel();
        numOfLinesLabel.setText("Number of Lines:");
        numOfColumnsLabel.add(numOfLinesLabel, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numOfLines = new JLabel();
        numOfLines.setText("");
        numOfColumnsLabel.add(numOfLines, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Number of Columns");
        numOfColumnsLabel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numOfColumns = new JLabel();
        numOfColumns.setText("");
        numOfColumnsLabel.add(numOfColumns, new com.intellij.uiDesigner.core.GridConstraints(3, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPagePanel;
    }

    private void createUIComponents() {
        labeledInfoTable = new JTable(new DefaultTableModel(new String[]{"Start Line", "End Line", "Line Type"}, 0));
        labeledInfoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        sheetDisplayTable = new JTable();
        sheetDisplayPane = new JScrollPane(sheetDisplayTable);
        JTable rowTable = new RowNumberTable(sheetDisplayTable);
        sheetDisplayPane.setRowHeaderView(rowTable);
//        sheetDisplayPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
    }

    private void loadFile(final File file) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(file));
        List<String[]> dataEntries = reader.readAll();

        this.numOfLines.setText(String.valueOf(dataEntries.size()));
        this.numOfColumns.setText(String.valueOf(dataEntries.get(0).length));

        SheetDisplayTableModel tableModel = new SheetDisplayTableModel(0, dataEntries.get(0).length);

        tableModel.insertRows(dataEntries);

        sheetDisplayTable.setModel(tableModel);

        sheetDisplayTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        sheetDisplayTable.setDefaultRenderer(Object.class, new SheetDisplayLineTypeRowRenderer());

        sheetDisplayPane.setBorder(BorderFactory.createTitledBorder(file.getName()));

        for (int i = 0; i < dataEntries.get(0).length; i++) {
            TableColumn column = sheetDisplayTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(150);
        }

        System.out.println(tableModel.getColumnCount() + "\t" + tableModel.getRowCount());
    }

    private Sheet findMostSimilarSpreadsheet(Sheet current, List<Sheet> candidates) {
        Set<SheetSimilarityFeature> features = new HashSet<>();
        features.add(new FileNameSimilarityFeature());
        features.add(new SheetNameSimilarityFeature());
        features.add(new SheetAmountFeature());

        features.forEach(feature -> feature.score(current, candidates));

        Map<Sheet, Double> score = new HashMap<>();

        features.stream().map(SheetSimilarityFeature::getScoreMap).forEach(map -> {
            for (Map.Entry<Sheet, Double> entry : map.entrySet()) {
                if (!score.containsKey(entry.getKey())) {
                    score.put(entry.getKey(), entry.getValue());
                } else {
                    score.put(entry.getKey(), score.get(entry.getKey()) + entry.getValue());
                }
            }
        });
        final Map<Sheet, Double> newScore = score.entrySet().stream().sorted(Map.Entry.<Sheet, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        return newScore.entrySet().iterator().next().getKey();
    }

    private JPopupMenu getLineTypePopupMenu() {
        AtomicInteger startIndex = new AtomicInteger();
        AtomicInteger endIndex = new AtomicInteger();
        ListSelectionModel sheetDisplayTableSelectionModel = sheetDisplayTable.getSelectionModel();
        sheetDisplayTableSelectionModel.addListSelectionListener(e -> {
            if (!sheetDisplayTableSelectionModel.isSelectionEmpty()) {
                startIndex.set(sheetDisplayTableSelectionModel.getMinSelectionIndex() + 1);
                endIndex.set(sheetDisplayTableSelectionModel.getMaxSelectionIndex() + 1);
            }
        });

        JPopupMenu lineTypePopupMenu = new JPopupMenu("Line type Popup Menu");
        JMenuItem lineTypeMenuItem = new JMenuItem("Preamble (P)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.PREAMBLE_BACKGROUND_COLOR);
            addToLabelInfo(startIndex.get(), endIndex.get(), e.getActionCommand());
        });
        lineTypePopupMenu.add(lineTypeMenuItem);

        lineTypeMenuItem = new JMenuItem("Header (H)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.HEADER_BACKGROUND_COLOR);
            addToLabelInfo(startIndex.get(), endIndex.get(), e.getActionCommand());
        });
        lineTypePopupMenu.add(lineTypeMenuItem);

        lineTypeMenuItem = new JMenuItem("Data (D)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.DATA_BACKGROUND_COLOR);
            addToLabelInfo(startIndex.get(), endIndex.get(), e.getActionCommand());
        });
        lineTypePopupMenu.add(lineTypeMenuItem);

        lineTypeMenuItem = new JMenuItem("Aggregation (A)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.AGGREGATION_BACKGROUND_COLOR);
            addToLabelInfo(startIndex.get(), endIndex.get(), e.getActionCommand());
        });
        lineTypePopupMenu.add(lineTypeMenuItem);

        lineTypeMenuItem = new JMenuItem("Footnote (F)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.FOOTNOTE_BACKGROUND_COLOR);
            addToLabelInfo(startIndex.get(), endIndex.get(), e.getActionCommand());
        });
        lineTypePopupMenu.add(lineTypeMenuItem);

        lineTypeMenuItem = new JMenuItem("Group header (G)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.GROUND_HEADER_BACKGROUND_COLOR);
            addToLabelInfo(startIndex.get(), endIndex.get(), e.getActionCommand());
        });
        lineTypePopupMenu.add(lineTypeMenuItem);
        return lineTypePopupMenu;
    }

    private void addToLabelInfoTable() {
        if (startLine.getText().equals("") || endLine.getText().equals("")) {
            JOptionPane.showMessageDialog(null, "Start Line or End Line cannot be empty.");
        } else if (!startLine.getText().matches("\\d+") || !endLine.getText().matches("\\d+")) {
            JOptionPane.showMessageDialog(null, "Start line or End Line value is not valid integer.");
        } else if (Integer.parseInt(startLine.getText()) > Integer.parseInt(endLine.getText())) {
            JOptionPane.showMessageDialog(null, "The start line index cannot be larger than the end line index.");
        } else if (lineTypeComboBox.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(null, "Please select a line function type");
        } else {
            addToLabelInfo(startLine.getText(), endLine.getText(), Objects.requireNonNull(lineTypeComboBox.getSelectedItem()).toString());
        }
    }

    private void addToLabelInfo(Object startIndex, Object endIndex, String type) {
        DefaultTableModel tableModel = (DefaultTableModel) this.labeledInfoTable.getModel();
        String[] row = new String[tableModel.getColumnCount()];
        row[0] = startIndex.toString();
        row[1] = endIndex.toString();
        row[2] = type;
        tableModel.addRow(row);

        SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
        sheetDisplayTableModel.setRowsBackgroundColor(Integer.parseInt(startIndex.toString()), Integer.parseInt(endIndex.toString()), ColorSolution.getColor(type));
    }
}
