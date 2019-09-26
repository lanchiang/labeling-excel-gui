package de.hpi.isg.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.opencsv.CSVReader;
import de.hpi.isg.dao.DatabaseQueryHandler;
import de.hpi.isg.elements.AnnotationResults;
import de.hpi.isg.elements.Sheet;
import de.hpi.isg.features.FileNameSimilarityFeature;
import de.hpi.isg.features.SheetAmountFeature;
import de.hpi.isg.features.SheetNameSimilarityFeature;
import de.hpi.isg.features.SheetSimilarityFeature;
import de.hpi.isg.json.JsonWriter;
import de.hpi.isg.pojo.SpreadSheetPojo;
import de.hpi.isg.storage.Store;
import de.hpi.isg.storage.JsonStore;
import de.hpi.isg.swing.SheetDisplayLineTypeRowRenderer;
import de.hpi.isg.swing.RowNumberTable;
import de.hpi.isg.swing.SheetDisplayTableModel;
import de.hpi.isg.utils.ColorSolution;
import lombok.Getter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
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
    private JLabel startLine;
    private JLabel endLine;
    private JLabel lineTypeDisplay;
    private JButton finishThisAnnotationButton;
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

    private int annotatedFileAmount = 0;

    private File[] loadedFiles;

    private File currentFile;

    private Sheet currentSheet;

    private long startTime;

    private long endTime;

    private Store store;

    private Color[] colorPattern;

    private String inputFileFolder;

    @Getter
    private DatabaseQueryHandler queryHandler = new DatabaseQueryHandler();

    public MainFrame() {
        $$$setupUI$$$();
        submitAllResultButton.addActionListener(e -> {
            // write the results into a json file.
            DefaultTableModel tableModel = (DefaultTableModel) sheetDisplayTable.getModel();
            if (tableModel.getColumnCount() != 0 || tableModel.getRowCount() != 0) {
                SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) tableModel;
                if (sheetDisplayTableModel.hasUnannotatedLines()) {
                    int selectCode = JOptionPane.showConfirmDialog(null,
                            "Seems this file has not been annotated. Do you want to anyway finish?");
                    if (selectCode != JOptionPane.OK_OPTION) {
                        return;
                    }
                }

                JsonWriter<SpreadSheetPojo> writer = new JsonWriter<>();
                writer.write(((JsonStore) store).getResultCache());

                this.queryHandler.close();
            }
        });
        finishThisAnnotationButton.addActionListener(e -> {
            endTime = System.currentTimeMillis();
            DefaultTableModel tableModel = (DefaultTableModel) sheetDisplayTable.getModel();
            if (tableModel.getColumnCount() != 0 || tableModel.getRowCount() != 0) {
                if (!submitResult()) {
                    return;
                }
//                loadNextFile();
            } else {
                throw new RuntimeException("There is no sheet being displayed.");
            }

//            currentFile = new File("/Users/Fuga/Documents/hpi/data/excel-to-csv/data-gov-uk/mappa-annual-report-13-14-tables.xls@Contents.csv");

            this.submitAsMultitableFileButton.setEnabled(false);
            this.finishThisAnnotationButton.setEnabled(false);

            this.nextFileButton.setEnabled(true);

            startTime = System.currentTimeMillis();
        });
        loadAllFilesButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
//            chooser.setCurrentDirectory(new File("/Users/Fuga/Documents/hpi/data/excel-to-csv"));
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle("Dialog title");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            int choiceCode = chooser.showOpenDialog(loadAllFilesButton);
            if (choiceCode == JFileChooser.APPROVE_OPTION) {
                File selectedDir = chooser.getSelectedFile();
                this.inputFileFolder = chooser.getSelectedFile().getPath();
//                System.out.println(this.inputFileFolder);
                loadedFiles = selectedDir.listFiles();
                assert loadedFiles != null;

                loadedFileNumberLabel.setText(annotatedFileAmount + "/" + loadedFiles.length);

                List<File> fileList = Arrays.stream(loadedFiles).filter(file -> !file.getName().equals(".DS_Store")).collect(Collectors.toList());

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

                submitAsMultitableFileButton.setEnabled(true);
                finishThisAnnotationButton.setEnabled(true);
                submitAllResultButton.setEnabled(true);
                nextFileButton.setEnabled(true);

//                store = new RDMBSStore(null, this.queryHandler);
                store = new JsonStore(sheets);

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

            this.submitAsMultitableFileButton.setEnabled(false);
            this.finishThisAnnotationButton.setEnabled(false);

            this.nextFileButton.setEnabled(true);

            startTime = System.currentTimeMillis();
        });
        nextFileButton.addActionListener(e -> {
            loadNextFile();
            this.submitAsMultitableFileButton.setEnabled(true);
            this.finishThisAnnotationButton.setEnabled(true);

            this.nextFileButton.setEnabled(false);
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
        lineTypeDescriptionPanel.setLayout(new GridLayoutManager(6, 2, new Insets(5, 10, 5, 10), -1, -1));
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
        groupheaderDesc.setText("Group header");
        lineTypeDescriptionPanel.add(groupheaderDesc, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(141, 16), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("A preamble line describes the characteristics of a table following it.");
        lineTypeDescriptionPanel.add(label1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("A header line describes the header of a table, including single column header line or cross column header line. Therefore, a table may have multiple header lines.");
        lineTypeDescriptionPanel.add(label2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("A data line describes the value of a table.");
        lineTypeDescriptionPanel.add(label3, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("An aggregation line displays the aggregation results (e.g., sum, average) of several other lines in the table.");
        lineTypeDescriptionPanel.add(label4, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("A footnote line gives explanations of part of or the whole table and appears after the table.");
        lineTypeDescriptionPanel.add(label5, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("A group header is a header of a group of lines in a table. In the example below, the line May (1st line) and June (11st line) are both group header lines.");
        lineTypeDescriptionPanel.add(label6, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        howToUsePanel = new JPanel();
        howToUsePanel.setLayout(new GridLayoutManager(9, 1, new Insets(5, 10, 5, 10), -1, -1));
        panel1.add(howToUsePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        howToUsePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "How to Use"));
        final JLabel label7 = new JLabel();
        label7.setText("Click on the \"Line Type Annotation\" tab.");
        howToUsePanel.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Click \"Start Annotation\" to select the input file folder.");
        howToUsePanel.add(label8, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("In the displayed spreadsheet, you can select a line block and mark it as one of the following six line types plus empty line.");
        howToUsePanel.add(label9, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("The color helps you to determine the line types. The spreadsheet statistics indicate the width and length of this file.");
        howToUsePanel.add(label10, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("If you think the line type pattern of some consecutive lines can be reused for some other lines.");
        howToUsePanel.add(label11, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("After finish annotation of this file, click \"Finish annotation\", and \"Next File\" for the next file.");
        howToUsePanel.add(label12, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("If you think this file is a multi-table file, you don't need to annotate any line. Instead, you just click \"Mark as Multitable File\", and prceed to the next file.");
        howToUsePanel.add(label13, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("To mark it, you can either select the line type in the right click menu, or use shortcut key.");
        howToUsePanel.add(label14, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        label15.setText("You can select them, click \"Copy pattern\", and chose the first line where you want to reuse the pattern, and click \"Paste pattern\".");
        howToUsePanel.add(label15, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        exampleFigurePanel = new JPanel();
        exampleFigurePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(exampleFigurePanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        groupheaderExample = new JLabel();
        groupheaderExample.setIcon(new ImageIcon(getClass().getResource("/groupheader-example.png")));
        groupheaderExample.setText("");
        exampleFigurePanel.add(groupheaderExample, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        menuTab.addTab("Line Type Annotation", panel2);
        panel2.add(sheetDisplayPane, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 1, false));
        sheetDisplayPane.setBorder(BorderFactory.createTitledBorder("Spreedsheet"));
        operatingPanel = new JPanel();
        operatingPanel.setLayout(new GridBagLayout());
        panel2.add(operatingPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        loadFilePanel = new JPanel();
        loadFilePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.VERTICAL;
        operatingPanel.add(loadFilePanel, gbc);
        submitPanel = new JPanel();
        submitPanel.setLayout(new GridLayoutManager(2, 2, new Insets(5, 5, 5, 5), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        loadFilePanel.add(submitPanel, gbc);
        submitPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16777216)), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, submitPanel.getFont())));
        submitAsMultitableFileButton = new JButton();
        submitAsMultitableFileButton.setEnabled(false);
        submitAsMultitableFileButton.setText("Mark as Multitable File");
        submitPanel.add(submitAsMultitableFileButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        finishThisAnnotationButton = new JButton();
        finishThisAnnotationButton.setEnabled(false);
        finishThisAnnotationButton.setText("Finish annotation");
        submitPanel.add(finishThisAnnotationButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        nextFileButton = new JButton();
        nextFileButton.setEnabled(false);
        nextFileButton.setText("Next File");
        submitPanel.add(nextFileButton, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startEndJPanel = new JPanel();
        startEndJPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
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
        startEndJPanel.add(loadAllFilesButton, gbc);
        submitAllResultButton = new JButton();
        submitAllResultButton.setEnabled(false);
        submitAllResultButton.setText("Submit all Results");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        startEndJPanel.add(submitAllResultButton, gbc);
        loadedFileNumberLabel = new JLabel();
        loadedFileNumberLabel.setText("0");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        startEndJPanel.add(loadedFileNumberLabel, gbc);
        loadedFileLabel = new JLabel();
        loadedFileLabel.setText("File Annotated / Loaded:");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        startEndJPanel.add(loadedFileLabel, gbc);
        annotationPanel = new JPanel();
        annotationPanel.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.VERTICAL;
        operatingPanel.add(annotationPanel, gbc);
        labelOperatingPanel = new JPanel();
        labelOperatingPanel.setLayout(new GridBagLayout());
        annotationPanel.add(labelOperatingPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(311, 56), null, 1, false));
        labelOperatingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16777216)), "Block selection", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, labelOperatingPanel.getFont()), new Color(-16777216)));
        startLineLabel = new JLabel();
        startLineLabel.setText("Start Line");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        labelOperatingPanel.add(startLineLabel, gbc);
        endLineLabel = new JLabel();
        endLineLabel.setText("End Line");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        labelOperatingPanel.add(endLineLabel, gbc);
        startLine = new JLabel();
        startLine.setText("n/a");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        labelOperatingPanel.add(startLine, gbc);
        endLine = new JLabel();
        endLine.setText("n/a");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        labelOperatingPanel.add(endLine, gbc);
        lineTypeLabel = new JLabel();
        lineTypeLabel.setText("Line Function Type");
        lineTypeLabel.setVerticalAlignment(0);
        lineTypeLabel.setVerticalTextPosition(0);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        labelOperatingPanel.add(lineTypeLabel, gbc);
        lineTypeDisplay = new JLabel();
        lineTypeDisplay.setText("n/a");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        labelOperatingPanel.add(lineTypeDisplay, gbc);
        sheetStatPanel = new JPanel();
        sheetStatPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        annotationPanel.add(sheetStatPanel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        sheetStatPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Spreadsheet statistics"));
        numOfLinesLabel = new JLabel();
        numOfLinesLabel.setText("Number of Lines:");
        sheetStatPanel.add(numOfLinesLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numOfColumns = new JLabel();
        numOfColumns.setText("");
        sheetStatPanel.add(numOfColumns, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        label16.setText("Number of Columns");
        sheetStatPanel.add(label16, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numOfLines = new JLabel();
        numOfLines.setText("");
        sheetStatPanel.add(numOfLines, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        colorInstructionPanel = new JPanel();
        colorInstructionPanel.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
        annotationPanel.add(colorInstructionPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        patternOperationPanel = new JPanel();
        patternOperationPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        annotationPanel.add(patternOperationPanel, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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

        resizeColumnWidth(sheetDisplayTable);

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

    private boolean submitResult() {
        SheetDisplayTableModel sheetDisplayTableModel = (SheetDisplayTableModel) sheetDisplayTable.getModel();
        if (sheetDisplayTableModel.hasUnannotatedLines()) {
            int selectCode = JOptionPane.showConfirmDialog(null,
                    "Some lines are not annotated yet. Do you want to proceed? Click on \"Yes\" will automatically annotate this lines as empty lines");
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

        return true;
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
            currentFile = new File(this.inputFileFolder + "/" + mostSimilarSheet.getExcelFileName() + "@" + mostSimilarSheet.getSheetName() + ".csv");
            System.out.println(currentFile.getPath());

            currentSheet = mostSimilarSheet;
        }

        annotatedFileAmount++;
        this.loadedFileNumberLabel.setText(annotatedFileAmount + "/" + loadedFiles.length);

        System.out.println(currentFile.getName());

        try {
            loadFile(currentFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 20; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 1, width);
            }
            if (width > 300)
                width = 300;
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }
}