package de.hpi.isg.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.opencsv.CSVReader;
import de.hpi.isg.dao.DatabaseQueryHandler;
import de.hpi.isg.elements.AnnotationResults;
import de.hpi.isg.elements.Sheet;
import de.hpi.isg.json.JsonReader;
import de.hpi.isg.json.JsonWriter;
import de.hpi.isg.pojo.AnnotationPojo;
import de.hpi.isg.pojo.ResultPojo;
import de.hpi.isg.pojo.SpreadSheetPojo;
import de.hpi.isg.storage.JsonStore;
import de.hpi.isg.storage.Store;
import de.hpi.isg.swing.RowNumberTable;
import de.hpi.isg.swing.SheetDisplayLineTypeRowRenderer;
import de.hpi.isg.swing.SheetDisplayTableModel;
import de.hpi.isg.utils.ColorSolution;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Lan Jiang
 * @since 8/26/19
 */
public class MainFrame {

    private JPanel mainPagePanel;
    private JLabel startLine;
    private JLabel endLine;
    private JLabel lineTypeDisplay;
    private JButton submitAllResultButton;
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
    private JPanel sheetStatPanel;
    private JPanel annotationPanel;
    private JTabbedPane menuTab;
    private JButton copyPatternButton;
    private JButton pastePatternButton;
    private JPanel loadFilePanel;
    private JLabel preambleColorLabel;
    private JLabel headerColorLabel;
    private JLabel dataColorLabel;
    private JLabel aggregationColorLabel;
    private JLabel footnoteColorLabel;
    private JLabel groupHeaderColorLabel;
    private JLabel emptyColorLabel;
    private JButton submitAsMultitableFileButton;
    private JButton nextFileButton;
    private JPanel startEndJPanel;
    private JLabel preambleDesc;
    private JLabel headerDesc;
    private JLabel dataDesc;
    private JLabel aggregationDesc;
    private JLabel footnoteDesc;
    private JLabel groupheaderDesc;
    private JPanel lineTypeDescriptionPanel;
    private JLabel groupheaderExample;
    private JPanel howToUsePanel;
    private JPanel colorInstructionPanel;
    private JPanel patternOperationPanel;
    private JPanel operatingPanel;
    private JPanel exampleFigurePanel;
    private JLabel emptyDesc;
    private JTable annotationReviewTable;
    private JScrollPane annotationReviewScrollPane;
    private JButton returnToCurrentButton;

    private int annotatedFileAmount = 0;

    private File[] loadedFiles;

    private File currentFile;

    private SheetDisplayTableModel currentFileTableModel;

    private Sheet currentSheet;

    private long startTime;

    private long endTime;

    private Store store;

    private Color[] colorPattern;

    private String inputFileFolder;

    private String annotationReviewTableSelection;

    @Getter
    private DatabaseQueryHandler queryHandler = new DatabaseQueryHandler();

    public MainFrame() {
        $$$setupUI$$$();
        submitAllResultButton.addActionListener(e -> {
            // write the results into a json file.
            DefaultTableModel tableModel = (DefaultTableModel) sheetDisplayTable.getModel();
            if (tableModel.getColumnCount() != 0 || tableModel.getRowCount() != 0) {
                SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) tableModel;
//                if (sheetDisplayTableModel.hasUnannotatedLines()) {
//                    int selectCode = JOptionPane.showConfirmDialog(null,
//                            "Seems this file has not been annotated. Do you want to anyway finish?");
//                    if (selectCode != JOptionPane.OK_OPTION) {
//                        return;
//                    }
//                }

                submitResult();

                saveResults();

                this.queryHandler.close();

                this.submitAllResultButton.setEnabled(false);
                this.submitAsMultitableFileButton.setEnabled(false);
                this.nextFileButton.setEnabled(false);
                this.returnToCurrentButton.setEnabled(false);
                this.copyPatternButton.setEnabled(false);
                this.pastePatternButton.setEnabled(false);
                this.loadAllFilesButton.setEnabled(false);
            }
        });
        loadAllFilesButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("/Users/Fuga/Documents/hpi/code/sidescript"));
//            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle("Dialog title");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            int choiceCode = chooser.showOpenDialog(loadAllFilesButton);
            if (choiceCode == JFileChooser.APPROVE_OPTION) {
                File selectedDir = chooser.getSelectedFile();
                this.inputFileFolder = chooser.getSelectedFile().getPath();
                loadedFiles = selectedDir.listFiles();
                assert loadedFiles != null;

                List<File> fileList = Arrays.stream(loadedFiles).filter(file -> !file.getName().equals(".DS_Store")).collect(Collectors.toList());
                loadedFiles = fileList.toArray(new File[0]);

                final Map<String, List<String>> sheetNamesByFileName = new HashMap<>();
                fileList.forEach(file -> {
                    String[] nameSplits = file.getName().split("@");
                    String fileName = nameSplits[0];
                    String sheetName = nameSplits[1].split(".csv")[0];

                    sheetNamesByFileName.putIfAbsent(fileName, new LinkedList<>());
                    sheetNamesByFileName.get(fileName).add(sheetName);
                });

                List<Sheet> sheets = new ArrayList<>();
                sheetNamesByFileName.forEach((key, value) -> value.forEach(sheetList -> {
                    sheets.add(new Sheet(sheetList, key, value.size()));
                }));
                store = new JsonStore(sheets);

                JsonReader jsonReader = new JsonReader();
                ResultPojo resultPojo = null;
                try {
                    resultPojo = jsonReader.read();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                if (resultPojo != null) {
                    annotatedFileAmount = resultPojo.getSpreadSheetPojos().size();
                    if (annotatedFileAmount == loadedFiles.length) {
                        JOptionPane.showMessageDialog(null, "You have annotated all the data");
                        return;
                    }

                    resultPojo.getSpreadSheetPojos().stream()
                            .filter(spreadSheetPojo -> spreadSheetPojo.getIsMultitableFile().equals("false"))
                            .forEach(spreadSheetPojo -> {
                                String fullName = spreadSheetPojo.getExcelFileName() + "@" + spreadSheetPojo.getSpreadsheetName() + ".csv";
                                addToAnnotationReviewTable(fullName);

                                Optional<AnnotationPojo> optional = spreadSheetPojo.getAnnotationPojos().stream()
                                        .max(Comparator.comparingInt(AnnotationPojo::getEndLineNumber));
                                if (!optional.isPresent()) {
                                    throw new RuntimeException("The row count can not be obtained.");
                                }
                                int rowCount = optional.get().getEndLineNumber();
                                Color[] colors = new Color[rowCount];
                                spreadSheetPojo.getAnnotationPojos().forEach(annotationPojo -> {
                                    int start = annotationPojo.getStartLineNumber();
                                    int end = annotationPojo.getEndLineNumber();
                                    String lineType = annotationPojo.getLineType();
                                    for (int i = start; i <= end; i++) {
                                        colors[i - 1] = ColorSolution.getColor(lineType);
                                    }
                                });

                                AnnotationResults annotationResults = new AnnotationResults(
                                        spreadSheetPojo.getExcelFileName(),
                                        spreadSheetPojo.getSpreadsheetName(),
                                        spreadSheetPojo.getTimeExpense());
                                annotationResults.annotate(colors);
                                this.store.getSpreadsheet(spreadSheetPojo.getExcelFileName(), spreadSheetPojo.getSpreadsheetName()).setAnnotated(true);
                                this.store.addAnnotation(annotationResults);
                                this.currentFile = new File(this.inputFileFolder + "/" + fullName);
                                this.currentSheet = this.store.getSpreadsheet(spreadSheetPojo.getExcelFileName(), spreadSheetPojo.getSpreadsheetName());
                            });
                }
                loadedFileNumberLabel.setText(annotatedFileAmount + "/" + loadedFiles.length);

                submitAsMultitableFileButton.setEnabled(true);
                submitAllResultButton.setEnabled(true);
                nextFileButton.setEnabled(true);

//                store = new RDMBSStore(null, this.queryHandler);

                loadNextFile();
                startTime = System.currentTimeMillis();
            }
        });
        ListSelectionModel sheetDisplayTableSelectionModel = sheetDisplayTable.getSelectionModel();
        sheetDisplayTableSelectionModel.addListSelectionListener(e -> {
            if (!sheetDisplayTableSelectionModel.isSelectionEmpty()) {
                int startIndex = sheetDisplayTableSelectionModel.getMinSelectionIndex() + 1;
                int endIndex = sheetDisplayTableSelectionModel.getMaxSelectionIndex() + 1;
                this.endLine.setText(String.valueOf(endIndex));
                this.startLine.setText(String.valueOf(startIndex));

                SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
                final Color currentColor = sheetDisplayTableModel.getRowColor(sheetDisplayTableSelectionModel.getMinSelectionIndex());
                String lineType = ColorSolution.getLineType(currentColor);
                if (lineType != null) {
                    lineTypeDisplay.setText(lineType);
                } else {
                    lineTypeDisplay.setText("n/a");
                }
            }
        });

        ListSelectionModel annotationReviewTableSelectionModel = annotationReviewTable.getSelectionModel();
        annotationReviewTableSelectionModel.addListSelectionListener(e -> {
            if (!annotationReviewTableSelectionModel.isSelectionEmpty()) {
                this.nextFileButton.setEnabled(false);
                this.submitAsMultitableFileButton.setEnabled(false);
                if (this.currentFileTableModel == null) {
                    this.currentFileTableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
                }

                int selectedIndex = annotationReviewTableSelectionModel.getMinSelectionIndex();

                DefaultTableModel defaultTableModel = (DefaultTableModel) annotationReviewTable.getModel();
                String fileName = (String) defaultTableModel.getValueAt(selectedIndex, 0);
                annotationReviewTableSelection = fileName;

                try {
                    loadFile(new File(this.inputFileFolder + "/" + fileName));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) this.sheetDisplayTable.getModel();
                AnnotationResults annotationResults = this.store.getAnnotation(fileName);
                annotationResults.getAnnotationResults().forEach(result -> {
                    int lineNumber = result.getLineNumber();
                    String lineType = result.getType();
                    sheetDisplayTableModel.setRowsBackgroundColor(lineNumber, lineNumber, ColorSolution.getColor(lineType));
                });

                returnToCurrentButton.setEnabled(true);
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
                    endLine.setText(String.valueOf(endIndex));
                    startLine.setText(String.valueOf(startIndex));

                    SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
                    final Color currentColor = sheetDisplayTableModel.getRowColor(sheetDisplayTableSelectionModel.getMinSelectionIndex());
                    String lineType = ColorSolution.getLineType(currentColor);
                    if (lineType != null) {
                        lineTypeDisplay.setText(lineType);
                    } else {
                        lineTypeDisplay.setText("n/a");
                    }
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
        submitAsMultitableFileButton.addActionListener(e -> {
            endTime = System.currentTimeMillis();
            int selectCode = JOptionPane.showConfirmDialog(null,
                    "Are you sure to mark this spreadsheet as multi-table sheet?");
            if (selectCode != JOptionPane.OK_OPTION) {
                return;
            }

            String[] nameSplits = currentFile.getName().split("@");
            String fileName = nameSplits[0];
            String sheetName = nameSplits[1].split(".csv")[0];

            AnnotationResults results = new AnnotationResults(fileName, sheetName, endTime - startTime, true);

            this.store.addAnnotation(results);

            // add a piece to the annotation review table.
//            addToAnnotationReviewTable(currentFile.getName());

            loadNextFile();

            this.currentFileTableModel = null;

//            loadNextFile();

            startTime = System.currentTimeMillis();
        });
        nextFileButton.addActionListener(e -> {
            // when this button is clicked, first do submit, after that load the next file.
            endTime = System.currentTimeMillis();
            DefaultTableModel tableModel = (DefaultTableModel) sheetDisplayTable.getModel();
            if (tableModel.getColumnCount() != 0 || tableModel.getRowCount() != 0) {
                if (!submitResult()) {
                    return;
                }
            } else {
                throw new RuntimeException("There is no sheet being displayed.");
            }

            this.currentFileTableModel = null;

            loadNextFile();

            startTime = System.currentTimeMillis();
        });
        returnToCurrentButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                returnToCurrent();
                returnToCurrentButton.setEnabled(false);
                submitAsMultitableFileButton.setEnabled(true);
                nextFileButton.setEnabled(true);
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Excel File Line Function Annotator");
        frame.setContentPane(new MainFrame().mainPagePanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.pack();
        frame.setSize(screenSize);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
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
        mainPagePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1, true, true));
        mainPagePanel.setBorder(BorderFactory.createTitledBorder(""));
        menuTab = new JTabbedPane();
        menuTab.setTabLayoutPolicy(1);
        menuTab.setTabPlacement(1);
        mainPagePanel.add(menuTab, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        menuTab.addTab("Instruction", panel1);
        lineTypeDescriptionPanel = new JPanel();
        lineTypeDescriptionPanel.setLayout(new GridLayoutManager(7, 2, new Insets(5, 10, 5, 10), -1, -1));
        panel1.add(lineTypeDescriptionPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        lineTypeDescriptionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Line Type Description"));
        preambleDesc.setText("Preamble");
        lineTypeDescriptionPanel.add(preambleDesc, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, 16), null, 0, false));
        headerDesc.setText("Header");
        lineTypeDescriptionPanel.add(headerDesc, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, 16), null, 0, false));
        dataDesc.setText("Data");
        lineTypeDescriptionPanel.add(dataDesc, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, 16), null, 0, false));
        aggregationDesc.setText("Aggregation");
        lineTypeDescriptionPanel.add(aggregationDesc, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, 16), null, 0, false));
        footnoteDesc.setText("Footnote");
        lineTypeDescriptionPanel.add(footnoteDesc, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, 16), null, 0, false));
        groupheaderDesc.setText("Group title");
        lineTypeDescriptionPanel.add(groupheaderDesc, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, 16), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("A preamble line describes the characteristics of a table following it.");
        lineTypeDescriptionPanel.add(label1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("A header line describes the header of a table, including single column header line or cross column header line. A table may have multiple header lines.");
        lineTypeDescriptionPanel.add(label2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("A data line describes the value of a table.");
        lineTypeDescriptionPanel.add(label3, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("An aggregation line displays the aggregation results (e.g., sum, average) of one or more other data lines in the table.");
        lineTypeDescriptionPanel.add(label4, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("A footnote line gives explanations of parts of or the whole table and appears after the table.");
        lineTypeDescriptionPanel.add(label5, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("A group header is a header of a group of lines in a table. In the example below, the line 9, 22, 30 are all group title lines for the following parts of the table..");
        lineTypeDescriptionPanel.add(label6, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        emptyDesc.setText("Empty");
        lineTypeDescriptionPanel.add(emptyDesc, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, 16), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("A empty line contains no values.");
        lineTypeDescriptionPanel.add(label7, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        howToUsePanel = new JPanel();
        howToUsePanel.setLayout(new GridLayoutManager(1, 1, new Insets(5, 10, 5, 10), -1, -1));
        panel1.add(howToUsePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        howToUsePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "How to Use"));
        final JLabel label8 = new JLabel();
        label8.setText("Please read the instruction document.");
        howToUsePanel.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        exampleFigurePanel = new JPanel();
        exampleFigurePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(exampleFigurePanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        groupheaderExample = new JLabel();
        groupheaderExample.setIcon(new ImageIcon(getClass().getResource("/grouptitle-example.png")));
        groupheaderExample.setText("");
        exampleFigurePanel.add(groupheaderExample, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        menuTab.addTab("Line Type Annotation", panel2);
        operatingPanel = new JPanel();
        operatingPanel.setLayout(new GridBagLayout());
        panel2.add(operatingPanel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        loadFilePanel = new JPanel();
        loadFilePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.VERTICAL;
        operatingPanel.add(loadFilePanel, gbc);
        startEndJPanel = new JPanel();
        startEndJPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        loadFilePanel.add(startEndJPanel, gbc);
        startEndJPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16777216)), null));
        loadAllFilesButton = new JButton();
        loadAllFilesButton.setText("Start Annotation");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        startEndJPanel.add(loadAllFilesButton, gbc);
        submitAllResultButton = new JButton();
        submitAllResultButton.setEnabled(false);
        submitAllResultButton.setText("Submit all Results");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        startEndJPanel.add(submitAllResultButton, gbc);
        loadedFileNumberLabel = new JLabel();
        loadedFileNumberLabel.setText("0");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        startEndJPanel.add(loadedFileNumberLabel, gbc);
        loadedFileLabel = new JLabel();
        loadedFileLabel.setText("Annotated / Loaded:");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        startEndJPanel.add(loadedFileLabel, gbc);
        submitPanel = new JPanel();
        submitPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        loadFilePanel.add(submitPanel, gbc);
        submitPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16777216)), null));
        submitAsMultitableFileButton = new JButton();
        submitAsMultitableFileButton.setEnabled(false);
        submitAsMultitableFileButton.setText("Mark as Multitable File");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        submitPanel.add(submitAsMultitableFileButton, gbc);
        nextFileButton = new JButton();
        nextFileButton.setEnabled(false);
        nextFileButton.setText("Next File");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        submitPanel.add(nextFileButton, gbc);
        returnToCurrentButton = new JButton();
        returnToCurrentButton.setEnabled(false);
        returnToCurrentButton.setText("Return to Current");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        submitPanel.add(returnToCurrentButton, gbc);
        annotationPanel = new JPanel();
        annotationPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        operatingPanel.add(annotationPanel, gbc);
        labelOperatingPanel = new JPanel();
        labelOperatingPanel.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        annotationPanel.add(labelOperatingPanel, gbc);
        labelOperatingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16777216)), "Block selection", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, labelOperatingPanel.getFont()), new Color(-16777216)));
        startLineLabel = new JLabel();
        startLineLabel.setText("Start Line");
        labelOperatingPanel.add(startLineLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        startLine = new JLabel();
        startLine.setText("n/a");
        labelOperatingPanel.add(startLine, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        lineTypeLabel = new JLabel();
        lineTypeLabel.setText("Line Function Type");
        lineTypeLabel.setVerticalAlignment(0);
        lineTypeLabel.setVerticalTextPosition(0);
        labelOperatingPanel.add(lineTypeLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(151, 16), null, 1, false));
        endLineLabel = new JLabel();
        endLineLabel.setText("End Line");
        labelOperatingPanel.add(endLineLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        endLine = new JLabel();
        endLine.setText("n/a");
        labelOperatingPanel.add(endLine, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        lineTypeDisplay = new JLabel();
        lineTypeDisplay.setText("n/a");
        labelOperatingPanel.add(lineTypeDisplay, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        colorInstructionPanel = new JPanel();
        colorInstructionPanel.setLayout(new GridLayoutManager(7, 1, new Insets(5, 5, 5, 5), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        annotationPanel.add(colorInstructionPanel, gbc);
        preambleColorLabel.setText("Preamble (P)");
        colorInstructionPanel.add(preambleColorLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, -1), null, 0, false));
        headerColorLabel.setText("Header (H)");
        colorInstructionPanel.add(headerColorLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, -1), null, 0, false));
        dataColorLabel.setText("Data (D)");
        colorInstructionPanel.add(dataColorLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, -1), null, 0, false));
        aggregationColorLabel.setText("Aggregation (A)");
        colorInstructionPanel.add(aggregationColorLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, -1), null, 0, false));
        footnoteColorLabel.setText("Footnote (F)");
        colorInstructionPanel.add(footnoteColorLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, -1), null, 0, false));
        groupHeaderColorLabel.setText("Group header (G)");
        colorInstructionPanel.add(groupHeaderColorLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, -1), null, 0, false));
        emptyColorLabel.setText("Empty (E)");
        colorInstructionPanel.add(emptyColorLabel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, -1), null, 0, false));
        sheetStatPanel = new JPanel();
        sheetStatPanel.setLayout(new GridLayoutManager(2, 2, new Insets(5, 5, 5, 5), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        annotationPanel.add(sheetStatPanel, gbc);
        sheetStatPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Spreadsheet statistics"));
        numOfLinesLabel = new JLabel();
        numOfLinesLabel.setText("Number of Lines:");
        sheetStatPanel.add(numOfLinesLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numOfColumns = new JLabel();
        numOfColumns.setText("");
        sheetStatPanel.add(numOfColumns, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Number of Columns");
        sheetStatPanel.add(label9, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numOfLines = new JLabel();
        numOfLines.setText("");
        sheetStatPanel.add(numOfLines, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        annotationReviewScrollPane = new JScrollPane();
        annotationReviewScrollPane.setAutoscrolls(false);
        annotationReviewScrollPane.setMaximumSize(new Dimension(-1, 500));
        annotationReviewScrollPane.setPreferredSize(new Dimension(-1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        annotationPanel.add(annotationReviewScrollPane, gbc);
        annotationReviewScrollPane.setBorder(BorderFactory.createTitledBorder("Spreadsheet Annotation Review"));
        annotationReviewTable.setAutoCreateRowSorter(false);
        annotationReviewScrollPane.setViewportView(annotationReviewTable);
        patternOperationPanel = new JPanel();
        patternOperationPanel.setLayout(new GridLayoutManager(2, 1, new Insets(5, 5, 5, 5), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        annotationPanel.add(patternOperationPanel, gbc);
        patternOperationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16777216)), "Pattern operation"));
        copyPatternButton = new JButton();
        copyPatternButton.setEnabled(false);
        copyPatternButton.setText("Copy pattern");
        patternOperationPanel.add(copyPatternButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, new Dimension(200, -1), 0, false));
        pastePatternButton = new JButton();
        pastePatternButton.setEnabled(false);
        pastePatternButton.setText("Paste pattern");
        patternOperationPanel.add(pastePatternButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, new Dimension(200, -1), 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        panel3.add(sheetDisplayPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 1, false));
        sheetDisplayPane.setBorder(BorderFactory.createTitledBorder("Spreedsheet"));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        menuTab.addTab("Multitable Annotation", panel4);
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
        resetReviewTable();

        sheetDisplayTable = new JTable();
        sheetDisplayPane = new JScrollPane(sheetDisplayTable);
        JTable rowTable = new RowNumberTable(sheetDisplayTable);
        sheetDisplayPane.setRowHeaderView(rowTable);

        preambleColorLabel = new JLabel("Preamble (P)");
        preambleColorLabel.setOpaque(true);
        preambleColorLabel.setBackground(ColorSolution.PREAMBLE_BACKGROUND_COLOR);
        headerColorLabel = new JLabel("Header (H)");
        headerColorLabel.setOpaque(true);
        headerColorLabel.setBackground(ColorSolution.HEADER_BACKGROUND_COLOR);
        dataColorLabel = new JLabel("Data (D)");
        dataColorLabel.setOpaque(true);
        dataColorLabel.setBackground(ColorSolution.DATA_BACKGROUND_COLOR);
        aggregationColorLabel = new JLabel("Aggregation (A)");
        aggregationColorLabel.setOpaque(true);
        aggregationColorLabel.setBackground(ColorSolution.AGGREGATION_BACKGROUND_COLOR);
        footnoteColorLabel = new JLabel("Footnote (F)");
        footnoteColorLabel.setOpaque(true);
        footnoteColorLabel.setBackground(ColorSolution.FOOTNOTE_BACKGROUND_COLOR);
        groupHeaderColorLabel = new JLabel("Group header (G)");
        groupHeaderColorLabel.setOpaque(true);
        groupHeaderColorLabel.setBackground(ColorSolution.GROUND_HEADER_BACKGROUND_COLOR);
        emptyColorLabel = new JLabel("Empty (E)");
        emptyColorLabel.setOpaque(true);
        emptyColorLabel.setBackground(ColorSolution.EMPTY_LINE_BACKGROUND_COLOR);

        preambleDesc = new JLabel("Preamble (P)");
        preambleDesc.setOpaque(true);
        preambleDesc.setBackground(ColorSolution.PREAMBLE_BACKGROUND_COLOR);
        headerDesc = new JLabel("Header (H)");
        headerDesc.setOpaque(true);
        headerDesc.setBackground(ColorSolution.HEADER_BACKGROUND_COLOR);
        dataDesc = new JLabel("Data (D)");
        dataDesc.setOpaque(true);
        dataDesc.setBackground(ColorSolution.DATA_BACKGROUND_COLOR);
        aggregationDesc = new JLabel("Aggregation (A)");
        aggregationDesc.setOpaque(true);
        aggregationDesc.setBackground(ColorSolution.AGGREGATION_BACKGROUND_COLOR);
        footnoteDesc = new JLabel("Footnote (F)");
        footnoteDesc.setOpaque(true);
        footnoteDesc.setBackground(ColorSolution.FOOTNOTE_BACKGROUND_COLOR);
        groupheaderDesc = new JLabel("Group header (G)");
        groupheaderDesc.setOpaque(true);
        groupheaderDesc.setBackground(ColorSolution.GROUND_HEADER_BACKGROUND_COLOR);
        emptyDesc = new JLabel("Empty (E)");
        emptyDesc.setOpaque(true);
        emptyDesc.setBackground(ColorSolution.EMPTY_LINE_BACKGROUND_COLOR);
    }

    private void loadFile(final File file) throws IOException {
        System.out.println(file.getName());

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

        resizeColumnWidth(sheetDisplayTable);

        System.out.println(tableModel.getColumnCount() + "\t" + tableModel.getRowCount());
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

    /**
     * Store the annotation result of this file in the memory.
     *
     * @return
     */
    private boolean submitResult() {
        SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
        if (sheetDisplayTableModel.hasUnannotatedLines()) {
            int selectCode = JOptionPane.showConfirmDialog(null,
                    "Some lines of this file are not annotated yet. Do you still want to finish it? Click on \"Yes\" will automatically annotate this lines as empty lines");
            if (selectCode != JOptionPane.OK_OPTION) {
                return false;
            }
        }

        String[] nameSplits = currentFile.getName().split("@");
        String fileName = nameSplits[0];
        String sheetName = nameSplits[1].split(".csv")[0];

        AnnotationResults results = new AnnotationResults(fileName, sheetName, endTime - startTime);

        results.annotate(sheetDisplayTableModel);

        this.store.addAnnotation(results);

        // add a piece to the annotation review table.
        addToAnnotationReviewTable(currentFile.getName());

        return true;
    }

    private void addToAnnotationReviewTable(String fileName) {
        DefaultTableModel tableModel = (DefaultTableModel) annotationReviewTable.getModel();
        tableModel.addRow(new String[]{fileName});
    }

    private void loadNextFile() {
        if (currentFile == null) {
            // load a random new table
            Random random = new Random(System.currentTimeMillis());
            int selectedIndex = random.nextInt(loadedFiles.length);

            currentFile = loadedFiles[selectedIndex];

            String[] nameSplits = currentFile.getName().split("@");
            String fileName = nameSplits[0];
            String sheetName = nameSplits[1].split(".csv")[0];

            currentSheet = this.store.getSpreadsheet(fileName, sheetName);
        } else {
            // get the most similar file
            Sheet mostSimilarSheet = store.findMostSimilarSheet(currentSheet);
            if (mostSimilarSheet == null) {
                int selectionCode = JOptionPane.showConfirmDialog(null, "No next file. All files have been annotated. Do you want to save all the annotation results now?");
                if (selectionCode == JOptionPane.OK_OPTION) {
                    saveResults();
                }
                return;
            }
            currentFile = new File(this.inputFileFolder + "/" + mostSimilarSheet.getExcelFileName() + "@" + mostSimilarSheet.getSheetName() + ".csv");
            System.out.println(currentFile.getPath());

            currentSheet = mostSimilarSheet;
        }

        annotatedFileAmount++;
        this.loadedFileNumberLabel.setText(annotatedFileAmount + "/" + loadedFiles.length);

        try {
            loadFile(currentFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 30; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 1, width);
            }
            if (width > 350)
                width = 350;
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    private void saveResults() {
        ((JsonStore) store).generateResultCache();
        JsonWriter<SpreadSheetPojo> writer = new JsonWriter<>();
        writer.write(((JsonStore) store).getResultCache());

        JOptionPane.showMessageDialog(null, "Annotation results have been saved to the file \"annotation_result.json\"");
    }

    private void returnToCurrent() {
        // first store the change to the reviewed sheet.
        SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
        if (sheetDisplayTableModel.hasUnannotatedLines()) {
            int selectCode = JOptionPane.showConfirmDialog(null,
                    "Some lines are not annotated yet. Do you still want to finish it? Click on \"Yes\" will automatically annotate this lines as empty lines");
            if (selectCode != JOptionPane.OK_OPTION) {
                return;
            }
        }

        String[] nameSplits = annotationReviewTableSelection.split("@");
        String fileName = nameSplits[0];
        String sheetName = nameSplits[1].split(".csv")[0];

        AnnotationResults results = new AnnotationResults(fileName, sheetName, endTime - startTime);

        results.annotate(sheetDisplayTableModel);

        store.addAnnotation(results);

        annotationReviewTable.clearSelection();
        try {
            loadFile(currentFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        sheetDisplayTable.setModel(currentFileTableModel);

        resizeColumnWidth(sheetDisplayTable);

        nextFileButton.setEnabled(true);
    }

    private void resetReviewTable() {
        DefaultTableModel tableModel = new DefaultTableModel(0, 1) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        annotationReviewTable = new JTable(tableModel);
        annotationReviewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        annotationReviewTable.setTableHeader(null);
    }
}