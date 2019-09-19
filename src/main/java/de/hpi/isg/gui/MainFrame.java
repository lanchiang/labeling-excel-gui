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
import de.hpi.isg.json.JsonWriter;
import de.hpi.isg.pojo.SpreadSheetPojo;
import de.hpi.isg.storage.RDMBSStore;
import de.hpi.isg.storage.Store;
import de.hpi.isg.storage.JsonStore;
import de.hpi.isg.swing.SheetDisplayLineTypeRowRenderer;
import de.hpi.isg.swing.RowNumberTable;
import de.hpi.isg.swing.SheetDisplayTableModel;
import de.hpi.isg.utils.ColorSolution;
import de.hpi.isg.utils.LineTypeUtils;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
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
    private JComboBox<String> lineTypeComboBox;
    private JButton submitAndNextFileButton;
    private JButton submitAndFinishButton;
    private JTable sheetDisplayTable;
    private JPanel labelOperatingPanel;
    private JLabel startLineLabel;
    private JLabel endLineLabel;
    private JLabel lineTypeLabel;
    private JPanel submitPanel;
    private JScrollPane sheetDisplayPane;
    private JButton loadAllFilesButton;
    private JLabel loadedFileLabel;
    private JLabel loadedFileNumberLabel;
    private JLabel numOfLines;
    private JLabel numOfLinesLabel;
    private JLabel numOfColumns;
    private JPanel numOfColumnsLabel;
    private JPanel sheetStatPanel;
    private JPanel annotationPanel;
    private JTabbedPane menuTab;
    private JButton copyPatternButton;
    private JButton pastePatternButton;
    private JPanel loadFilePanel;
    private JProgressBar annotationProgress;

    private int annotatedFileAmount = 0;

    private File[] loadedFiles;

    private File currentFile;

    private Sheet currentSheet;

    private SheetSimilarityCalculator calculator;

    private long startTime;

    private long endTime;

    private Store store;

    private Color[] colorPattern;

    @Getter
    private QueryHandler queryHandler = new QueryHandler();

    public MainFrame() {
        $$$setupUI$$$();
        submitAndFinishButton.addActionListener(e -> {
            endTime = System.currentTimeMillis();
            long duration = 0L;
            if (startTime != 0L) {
                duration = endTime - startTime;
            }

            // write the results into a json file.
            DefaultTableModel tableModel = (DefaultTableModel) sheetDisplayTable.getModel();
            if (tableModel.getColumnCount() != 0 || tableModel.getRowCount() != 0) {
                SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) tableModel;
                if (sheetDisplayTableModel.hasUnannotatedLines()) {
                    int selectCode = JOptionPane.showConfirmDialog(null,
                            "Some lines are not annotated yet. Do you want to proceed? Click on \"Yes\" will automatically annotate this lines as empty lines");
                    if (selectCode != JOptionPane.OK_OPTION) {
                        return;
                    }
                }

                String[] nameSplits = currentFile.getName().split("@");
                String fileName = nameSplits[0];
                String sheetName = nameSplits[1].split(".csv")[0];

                AnnotationResults results = new AnnotationResults(fileName, sheetName, duration);

                results.annotate(sheetDisplayTableModel);

                this.store.addAnnotation(results);

                JsonWriter<SpreadSheetPojo> writer = new JsonWriter<>();
                writer.write(((JsonStore) store).getResultCache());

                this.queryHandler.close();
            }
        });
        submitAndNextFileButton.addActionListener(e -> {
            endTime = System.currentTimeMillis();
            long duration = 0L;
            if (startTime != 0L) {
                duration = endTime - startTime;
            }
            startTime = endTime;
            DefaultTableModel tableModel = (DefaultTableModel) sheetDisplayTable.getModel();
            if (tableModel.getColumnCount() != 0 || tableModel.getRowCount() != 0) {
                SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) tableModel;
                if (sheetDisplayTableModel.hasUnannotatedLines()) {
                    int selectCode = JOptionPane.showConfirmDialog(null,
                            "Some lines are not annotated yet. Do you want to proceed? Click on \"Yes\" will automatically annotate this lines as empty lines");
                    if (selectCode != JOptionPane.OK_OPTION) {
                        return;
                    }
                }

                String[] nameSplits = currentFile.getName().split("@");
                String fileName = nameSplits[0];
                String sheetName = nameSplits[1].split(".csv")[0];

                AnnotationResults results = new AnnotationResults(fileName, sheetName, duration);

                results.annotate(sheetDisplayTableModel);

                this.store.addAnnotation(results);

//                resultCache.addResultToCache(resultCache.convertToResultCacheFormat(results));

                // get the most similar file
//                Sheet mostSimilarSheet = store.findMostSimilarSheet(currentSheet);
                List<Sheet> sheets = this.queryHandler.getAllUnannotatedSpreadsheet();
                Sheet mostSimilarSheet = findMostSimilarSpreadsheet(currentSheet, sheets);
                currentFile = calculator.getMostSimilarFile(mostSimilarSheet);
                currentSheet = mostSimilarSheet;

                annotatedFileAmount++;
                this.loadedFileNumberLabel.setText(annotatedFileAmount + "/" + loadedFiles.length);
            } else {
                // load a random new table
                Random random = new Random(System.currentTimeMillis());
                int selectedIndex = random.nextInt(loadedFiles.length);

                currentFile = loadedFiles[selectedIndex];

                String[] nameSplits = currentFile.getName().split("@");
                String fileName = nameSplits[0];
                String sheetName = nameSplits[1].split(".csv")[0];

                int amount = this.queryHandler.getSheetAmountByExcelName(fileName);
                currentSheet = new Sheet(sheetName, fileName, amount);
            }

//            currentFile = new File("/Users/Fuga/Documents/hpi/data/excel-to-csv/data-gov-uk/mappa-annual-report-13-14-tables.xls@Contents.csv");

            this.annotationProgress.setValue(annotatedFileAmount);

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

                loadedFileNumberLabel.setText(annotatedFileAmount + "/" + loadedFiles.length);

                calculator = new SheetSimilarityCalculator(loadedFiles);

                this.queryHandler.loadExcelFileStatistics(calculator.getSheetNamesByFileName());

                this.annotationProgress.setMinimum(0);
                this.annotationProgress.setMaximum(loadedFiles.length);

                submitAndNextFileButton.setEnabled(true);
                submitAndFinishButton.setEnabled(true);

                store = new RDMBSStore(null, this.queryHandler);
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

//                SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
//                final Color currentColor = sheetDisplayTableModel.getRowColor(sheetDisplayTableSelectionModel.getMinSelectionIndex());
//                int index = 0;
//                String lineType = ColorSolution.getLineType(currentColor);
//                if (lineType == null) {
//                    lineTypeComboBox.setSelectedIndex(0);
//                    return;
//                }
//                switch (lineType) {
//                    case LineTypeUtils.PREAMBLE:
//                        index = 1;
//                        break;
//                    case LineTypeUtils.HEADER:
//                        index = 2;
//                        break;
//                    case LineTypeUtils.DATA:
//                        index = 3;
//                        break;
//                    case LineTypeUtils.AGGREGATION:
//                        index = 4;
//                        break;
//                    case LineTypeUtils.FOOTNOTE:
//                        index = 5;
//                        break;
//                    case LineTypeUtils.GROUP_HEADER:
//                        index = 6;
//                        break;
//                }
//                lineTypeComboBox.setSelectedIndex(index);
            }
        });

        JPopupMenu lineTypePopupMenu = getLineTypePopupMenu();

        sheetDisplayTable.setComponentPopupMenu(lineTypePopupMenu);

        sheetDisplayTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                ListSelectionModel sheetDisplayTableSelectionModel = sheetDisplayTable.getSelectionModel();
                if (!sheetDisplayTableSelectionModel.isSelectionEmpty()) {
                    int startIndex = sheetDisplayTableSelectionModel.getMinSelectionIndex() + 1;
                    int endIndex = sheetDisplayTableSelectionModel.getMaxSelectionIndex() + 1;
                    if (endIndex - startIndex >= 0) {
                        endLine.setText(String.valueOf(endIndex));
                        startLine.setText(String.valueOf(startIndex));
                    }

//                    SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
//                    final Color currentColor = sheetDisplayTableModel.getRowColor(sheetDisplayTableSelectionModel.getMinSelectionIndex());
//                    int index = 0;
//                    String lineType = ColorSolution.getLineType(currentColor);
//                    if (lineType == null) {
//                        lineTypeComboBox.setSelectedIndex(0);
//                        return;
//                    }
//                    switch (lineType) {
//                        case LineTypeUtils.PREAMBLE:
//                            index = 1;
//                            break;
//                        case LineTypeUtils.HEADER:
//                            index = 2;
//                            break;
//                        case LineTypeUtils.DATA:
//                            index = 3;
//                            break;
//                        case LineTypeUtils.AGGREGATION:
//                            index = 4;
//                            break;
//                        case LineTypeUtils.FOOTNOTE:
//                            index = 5;
//                            break;
//                        case LineTypeUtils.GROUP_HEADER:
//                            index = 6;
//                            break;
//                    }
//                    lineTypeComboBox.setSelectedIndex(index);
//                    sheetDisplayTable.requestFocus();
                }
            }
        });

        sheetDisplayTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.isControlDown() || e.isMetaDown()) {
                    return;
                }
                int startLineNumber = Integer.parseInt(startLine.getText());
                int endLineNumber = Integer.parseInt(endLine.getText());

                char pressedKeyChar = e.getKeyChar();
                SheetDisplayTableModel tableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
                Color selectedColor;
                switch (pressedKeyChar) {
                    case 'P':
                    case 'p': {
                        selectedColor = ColorSolution.PREAMBLE_BACKGROUND_COLOR;
                        break;
                    }
                    case 'H':
                    case 'h': {
                        selectedColor = ColorSolution.HEADER_BACKGROUND_COLOR;
                        break;
                    }
                    case 'D':
                    case 'd': {
                        selectedColor = ColorSolution.DATA_BACKGROUND_COLOR;
                        break;
                    }
                    case 'A':
                    case 'a': {
                        selectedColor = ColorSolution.AGGREGATION_BACKGROUND_COLOR;
                        break;
                    }
                    case 'F':
                    case 'f': {
                        selectedColor = ColorSolution.FOOTNOTE_BACKGROUND_COLOR;
                        break;
                    }
                    case 'G':
                    case 'g': {
                        selectedColor = ColorSolution.GROUND_HEADER_BACKGROUND_COLOR;
                        break;
                    }
                    case 'E':
                    case 'e': {
                        selectedColor = ColorSolution.EMPTY_LINE_BACKGROUND_COLOR;
                        break;
                    }
                    default:
                        return;
                }
                tableModel.setRowsBackgroundColor(startLineNumber, endLineNumber, selectedColor);
                if (endLineNumber < tableModel.getRowCount()) {
                    sheetDisplayTable.requestFocus();
                    sheetDisplayTable.changeSelection(endLineNumber, 0, false, false);
                }
            }
        });

        lineTypeComboBox.addActionListener(e -> {
            int index = lineTypeComboBox.getSelectedIndex();
            if (index == 0)
                return;
            if (startLine.getText() == null || endLine.getText() == null)
                return;
            int startLineIndex = Integer.parseInt(startLine.getText());
            int endLineIndex = Integer.parseInt(endLine.getText());

            String selectedLineType = Objects.requireNonNull(lineTypeComboBox.getSelectedItem()).toString();

            SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            sheetDisplayTableModel.setRowsBackgroundColor(startLineIndex, endLineIndex, ColorSolution.getColor(selectedLineType));
            lineTypeComboBox.setSelectedIndex(0);
        });
        copyPatternButton.addActionListener(e -> {
            int startLineIndex = sheetDisplayTableSelectionModel.getMinSelectionIndex();
            int endLineIndex = sheetDisplayTableSelectionModel.getMaxSelectionIndex();

            colorPattern = new Color[endLineIndex - startLineIndex + 1];
            SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            for (int i = startLineIndex, j = 0; i <= endLineIndex; i++, j++) {
                colorPattern[j] = sheetDisplayTableModel.getRowColor(i);
            }
            System.out.println("Color pattern is copied");
            this.pastePatternButton.setEnabled(true);
        });
        pastePatternButton.addActionListener(e -> {
            if (colorPattern == null) {
                return;
            }
            int startLineIndex = sheetDisplayTableSelectionModel.getMinSelectionIndex() + 1;

            SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();

            for (int i = 0; i < colorPattern.length; i++) {
                if (startLineIndex + i <= sheetDisplayTableModel.getRowCount()) {
                    sheetDisplayTableModel.setRowsBackgroundColor(startLineIndex + i, startLineIndex + i, colorPattern[i]);
                }
            }
            sheetDisplayTableModel.fireTableDataChanged();

            int endLineIndex = (startLineIndex - 1) + colorPattern.length;
            if (endLineIndex < sheetDisplayTableModel.getRowCount()) {
                sheetDisplayTable.requestFocus();
                sheetDisplayTable.changeSelection(endLineIndex, 0, false, false);
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
        mainPagePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPagePanel.setBorder(BorderFactory.createTitledBorder(""));
        menuTab = new JTabbedPane();
        menuTab.setTabLayoutPolicy(1);
        menuTab.setTabPlacement(1);
        mainPagePanel.add(menuTab, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        menuTab.addTab("Instruction", panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        menuTab.addTab("Line Type Annotation", panel2);
        numOfColumnsLabel = new JPanel();
        numOfColumnsLabel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(numOfColumnsLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(909, 130), null, 0, false));
        annotationPanel = new JPanel();
        annotationPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        numOfColumnsLabel.add(annotationPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 2, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelOperatingPanel = new JPanel();
        labelOperatingPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        annotationPanel.add(labelOperatingPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(311, 56), null, 1, false));
        labelOperatingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16777216)), "Block selection", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, labelOperatingPanel.getFont()), new Color(-16777216)));
        startLineLabel = new JLabel();
        startLineLabel.setText("Start Line");
        labelOperatingPanel.add(startLineLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        endLineLabel = new JLabel();
        endLineLabel.setText("End Line");
        labelOperatingPanel.add(endLineLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        startLine = new JTextField();
        labelOperatingPanel.add(startLine, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 3, false));
        endLine = new JTextField();
        labelOperatingPanel.add(endLine, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 2, false));
        lineTypeLabel = new JLabel();
        lineTypeLabel.setText("Line Function Type");
        lineTypeLabel.setVerticalAlignment(0);
        lineTypeLabel.setVerticalTextPosition(0);
        labelOperatingPanel.add(lineTypeLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(151, 16), null, 1, false));
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
        labelOperatingPanel.add(lineTypeComboBox, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, -1), new Dimension(151, 27), new Dimension(160, -1), 2, false));
        sheetStatPanel = new JPanel();
        sheetStatPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        annotationPanel.add(sheetStatPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        sheetStatPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Spreadsheet statistics"));
        numOfLinesLabel = new JLabel();
        numOfLinesLabel.setText("Number of Lines:");
        sheetStatPanel.add(numOfLinesLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numOfColumns = new JLabel();
        numOfColumns.setText("");
        sheetStatPanel.add(numOfColumns, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Number of Columns");
        sheetStatPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numOfLines = new JLabel();
        numOfLines.setText("");
        sheetStatPanel.add(numOfLines, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        submitPanel = new JPanel();
        submitPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        numOfColumnsLabel.add(submitPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 2, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(311, 31), null, 2, false));
        submitAndNextFileButton = new JButton();
        submitAndNextFileButton.setEnabled(true);
        submitAndNextFileButton.setText("Submit and Next File");
        submitPanel.add(submitAndNextFileButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        submitAndFinishButton = new JButton();
        submitAndFinishButton.setEnabled(false);
        submitAndFinishButton.setText("Submit and Finish");
        submitPanel.add(submitAndFinishButton, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        copyPatternButton = new JButton();
        copyPatternButton.setEnabled(false);
        copyPatternButton.setText("Copy pattern");
        numOfColumnsLabel.add(copyPatternButton, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pastePatternButton = new JButton();
        pastePatternButton.setEnabled(false);
        pastePatternButton.setText("Paste pattern");
        numOfColumnsLabel.add(pastePatternButton, new com.intellij.uiDesigner.core.GridConstraints(1, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel2.add(sheetDisplayPane, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 1, false));
        sheetDisplayPane.setBorder(BorderFactory.createTitledBorder("Spreedsheet"));
        loadFilePanel = new JPanel();
        loadFilePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(loadFilePanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        loadAllFilesButton = new JButton();
        loadAllFilesButton.setText("Load All files");
        loadFilePanel.add(loadAllFilesButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loadedFileNumberLabel = new JLabel();
        loadedFileNumberLabel.setText("0");
        loadFilePanel.add(loadedFileNumberLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loadedFileLabel = new JLabel();
        loadedFileLabel.setText("File Loaded:");
        loadFilePanel.add(loadedFileLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        annotationProgress.setIndeterminate(false);
        annotationProgress.setOrientation(0);
        annotationProgress.setString("");
        annotationProgress.setStringPainted(true);
        annotationProgress.setValue(0);
        loadFilePanel.add(annotationProgress, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        menuTab.addTab("Multitable Annotation", panel3);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPagePanel;
    }

    private void createUIComponents() {
        sheetDisplayTable = new JTable();
        sheetDisplayPane = new JScrollPane(sheetDisplayTable);
        JTable rowTable = new RowNumberTable(sheetDisplayTable);
        sheetDisplayPane.setRowHeaderView(rowTable);
//        sheetDisplayPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());

        lineTypeComboBox = new JComboBox<>();
        lineTypeComboBox.setEditable(false);
        final DefaultComboBoxModel<String> comboItems = new DefaultComboBoxModel<>();
        comboItems.addElement("-");
        comboItems.addElement(LineTypeUtils.PREAMBLE);
        comboItems.addElement(LineTypeUtils.HEADER);
        comboItems.addElement(LineTypeUtils.DATA);
        comboItems.addElement(LineTypeUtils.AGGREGATION);
        comboItems.addElement(LineTypeUtils.FOOTNOTE);
        comboItems.addElement(LineTypeUtils.GROUP_HEADER);
        lineTypeComboBox.setModel(comboItems);

        annotationProgress = new JProgressBar();
        annotationProgress.setIndeterminate(false);
        annotationProgress.setOrientation(0);
        annotationProgress.setStringPainted(true);
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

        sheetDisplayTable.setDefaultRenderer(Object.class, new SheetDisplayLineTypeRowRenderer(dataEntries.size(), dataEntries.get(0).length));

        sheetDisplayTable.setColumnSelectionAllowed(false);
        sheetDisplayTable.setRowSelectionAllowed(true);

        sheetDisplayPane.setBorder(BorderFactory.createTitledBorder(file.getName()));

        this.copyPatternButton.setEnabled(true);

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
        });
        lineTypePopupMenu.add(lineTypeMenuItem);

        lineTypeMenuItem = new JMenuItem("Header (H)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.HEADER_BACKGROUND_COLOR);
        });
        lineTypePopupMenu.add(lineTypeMenuItem);

        lineTypeMenuItem = new JMenuItem("Data (D)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.DATA_BACKGROUND_COLOR);
        });
        lineTypePopupMenu.add(lineTypeMenuItem);

        lineTypeMenuItem = new JMenuItem("Aggregation (A)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.AGGREGATION_BACKGROUND_COLOR);
        });
        lineTypePopupMenu.add(lineTypeMenuItem);

        lineTypeMenuItem = new JMenuItem("Footnote (F)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.FOOTNOTE_BACKGROUND_COLOR);
        });
        lineTypePopupMenu.add(lineTypeMenuItem);

        lineTypeMenuItem = new JMenuItem("Group header (G)");
        lineTypeMenuItem.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            SheetDisplayTableModel tableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
            tableModel.setRowsBackgroundColor(startIndex.get(), endIndex.get(), ColorSolution.GROUND_HEADER_BACKGROUND_COLOR);
        });
        lineTypePopupMenu.add(lineTypeMenuItem);
        return lineTypePopupMenu;
    }
}