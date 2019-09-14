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
import de.hpi.isg.swing.LineTypeBackgroundColorTableRowRenderer;
import de.hpi.isg.swing.RowNumberTable;
import org.apache.commons.lang3.Validate;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;
import java.util.List;
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

    private QueryHandler queryHandler = new QueryHandler();

    public MainFrame() {
        $$$setupUI$$$();
        submitAndFinishButton.addActionListener(e -> {
        });
        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (startLine.getText().equals("") || endLine.getText().equals("")) {
                    JOptionPane.showMessageDialog(null, "Start Line or End Line cannot be empty.");
                } else if (!startLine.getText().matches("\\d+") || !endLine.getText().matches("\\d+")) {
                    JOptionPane.showMessageDialog(null, "Start line or End Line value is not valid integer.");
                } else if (Integer.parseInt(startLine.getText()) > Integer.parseInt(endLine.getText())) {
                    JOptionPane.showMessageDialog(null, "The start line index cannot be larger than the end line index.");
                } else if (lineTypeComboBox.getSelectedIndex() == 0) {
                    JOptionPane.showMessageDialog(null, "Please select a line function type");
                } else {
                    DefaultTableModel tableModel = (DefaultTableModel) labeledInfoTable.getModel();
                    String[] row = new String[tableModel.getColumnCount()];
                    row[0] = startLine.getText();
                    row[1] = endLine.getText();
                    row[2] = Objects.requireNonNull(lineTypeComboBox.getSelectedItem()).toString();
                    tableModel.addRow(row);
                }
            }
        });
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DefaultTableModel tableModel = (DefaultTableModel) labeledInfoTable.getModel();
                tableModel.removeRow(labeledInfoTable.getSelectedRow());

                labeledInfoTable.getSelectionModel().clearSelection();
            }
        });
        submitAndNextFileButton.addActionListener(e -> {
            DefaultTableModel tableModel = (DefaultTableModel) sheetDisplayTable.getModel();
            if (tableModel.getColumnCount() != 0 || tableModel.getRowCount() != 0) {
                DefaultTableModel labeledInfoTableModel = (DefaultTableModel) labeledInfoTable.getModel();
                int columnCount = labeledInfoTableModel.getColumnCount();
                Validate.isTrue(columnCount == 3);

                // check whether the label info table is empty.
                if (labeledInfoTableModel.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(null, "Please enter some labels for this data file.");
                    return;
                }

                String[] nameSplits = currentFile.getName().split("@");
                String fileName = nameSplits[0];
                String sheetName = nameSplits[1].split(".csv")[0];

                AnnotationResults results = new AnnotationResults(fileName, sheetName);
                Vector dataVector = labeledInfoTableModel.getDataVector();
                for (int i = 0; i < labeledInfoTableModel.getRowCount(); i++) {
                    Vector row = (Vector) dataVector.elementAt(i);
                    int startLineNumber = Integer.parseInt(row.elementAt(0).toString());
                    int endLineNumber = Integer.parseInt(row.elementAt(1).toString());
                    String lineType = String.valueOf(row.elementAt(2));
                    results.addAnnotation(startLineNumber, endLineNumber, lineType);
                }

                this.queryHandler.insertLineFunctionAnnotationResults(results);

                this.queryHandler.updateSpreadsheetAnnotationStatus(sheetName, fileName);

                // get the most similar file
                List<Sheet> sheets = this.queryHandler.getAllUnannotatedSpreadsheet();
                Sheet mostSimilarSheet = findMostSimilarSpreadsheet(currentSheet, sheets);
                currentFile = calculator.getMostSimilarFile(mostSimilarSheet);
                currentSheet = mostSimilarSheet;

                this.labeledInfoTable.setModel(new DefaultTableModel(new String[]{"Start Line", "End Line", "Line Type"}, 0));
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
                if (endIndex - startIndex > 0) {
                    this.endLine.setText(String.valueOf(endIndex));
                    this.startLine.setText(String.valueOf(startIndex));
                }
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
        sheetDisplayPane.setCorner(JScrollPane.UPPER_LEFT_CORNER,
                rowTable.getTableHeader());
    }

    private void loadFile(final File file) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(file));
        List<String[]> dataEntries = reader.readAll();

        this.numOfLines.setText(String.valueOf(dataEntries.size()));
        this.numOfColumns.setText(String.valueOf(dataEntries.get(0).length));

        DefaultTableModel tableModel = new DefaultTableModel(0, dataEntries.get(0).length) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        List<Integer> emptyRowIndices = new LinkedList<>();
        for (int i = 0; i < dataEntries.size(); i++) {
            tableModel.addRow(dataEntries.get(i));
            if (Arrays.stream(dataEntries.get(i)).allMatch(""::equals)) {
                emptyRowIndices.add(i);
            }
        }
        sheetDisplayTable.setModel(tableModel);

        sheetDisplayTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        sheetDisplayTable.setDefaultRenderer(Object.class, new LineTypeBackgroundColorTableRowRenderer(emptyRowIndices));

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
}
