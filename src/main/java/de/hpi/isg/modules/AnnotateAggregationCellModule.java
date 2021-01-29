package de.hpi.isg.modules;

import de.hpi.isg.elements.*;
import de.hpi.isg.gui.MainFrame;
import de.hpi.isg.io.FileWriter;
import de.hpi.isg.json.JsonSheetEntry;
import de.hpi.isg.swing.CsvDisplayTableModel;
import de.hpi.isg.swing.CsvDisplayTableRender;
import de.hpi.isg.swing.SheetDisplayCellTableModel;
import de.hpi.isg.utils.ColorSolution;
import de.hpi.isg.utils.NumberUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author lan
 * @since 1/15/21
 */
public class AnnotateAggregationCellModule extends Module {

    private static AnnotateAggregationCellModule instance;

    private AnnotateAggregationPageComponents pageComponents;

    private double errorParameter;
    private final int errorBoundMethod = NumberUtils.ErrorBoundMethod.ABSOLUTE_BOUND;

    private List<BlockIndexTuples> selectedAggregateeBlocks;

    private List<BlockIndexTuples> selectedAggregatorBlocks;

    private AnnotateAggregationCellModule() {}

    public static Module getInstance(PageComponents pageComponents) {
        if (instance == null) {
            instance = new AnnotateAggregationCellModule();
            instance.initializePageComponents(pageComponents);
        }
        return instance;
    }


    @Override
    public void initializePageComponents(PageComponents pageComponents) {
        if (pageComponents instanceof AnnotateAggregationPageComponents) {
            this.pageComponents = (AnnotateAggregationPageComponents) pageComponents;
        } else {
            throw new RuntimeException("Internal error: page components do not fit to this page.");
        }
    }

    @Override
    public void renderFile(ListSelectionModel selectionModel) {
        int selectedIndex = selectionModel.getMinSelectionIndex();

        DefaultTableModel defaultTableModel = (DefaultTableModel) this.pageComponents.getFileReviewTable().getModel();
        String fileSheetName = (String) defaultTableModel.getValueAt(selectedIndex, 0); // get the selectedCellValue from the first and the only column.

        String[] splits = fileSheetName.split("@");
        String fileName = splits[0];
        String sheetName = splits[1].split(".csv")[0];

        Optional<JsonSheetEntry> selectedJsonSheetEntryOpt = this.loadedJsonSheetEntries.stream().filter(entry ->
                fileName.equals(entry.getFileName()) && sheetName.equals(entry.getTableId())).findFirst();
        if (!selectedJsonSheetEntryOpt.isPresent()) {
            throw new RuntimeException("Json object of this index cannot be found.");
        }
        selectedJsonSheetEntry = selectedJsonSheetEntryOpt.get();
        int numOfRows = selectedJsonSheetEntry.getNumOfRows();
        int numOfColumns = selectedJsonSheetEntry.getNumOfColumns();

        this.pageComponents.getNumRowsAggr().setText(String.valueOf(numOfRows));
        this.pageComponents.getNumColumnsAggr().setText(String.valueOf(numOfColumns));

        this.errorParameter = Double.parseDouble(this.pageComponents.getErrorTextField().getText());

        this.pageComponents.getSumRadioButton().doClick();

        JSONArray tableValueArray = selectedJsonSheetEntry.getTable_array();

        List<String[]> tableValues = new ArrayList<>();
        for (Object o : tableValueArray) {
            JSONArray lineJsonArray = (JSONArray) o;
            String[] lineArray = new String[lineJsonArray.size()];
            for (int j = 0; j < lineArray.length; j++) {
                lineArray[j] = lineJsonArray.get(j).toString();
            }
            tableValues.add(lineArray);
        }

        JSONArray annotations = selectedJsonSheetEntry.getAggregation_annotations();
        Set<AggregationRelation> relations = new HashSet<>();
        if (annotations != null) {
            for (Object relationObj : annotations) {
                JSONObject jsonObj = (JSONObject) relationObj;
                JSONArray array = (JSONArray) jsonObj.get("aggregator_index");
                FileIndexTuple aggregator_index = new FileIndexTuple(Integer.parseInt(array.get(0).toString()), Integer.parseInt(array.get(1).toString()));
                String operator = jsonObj.get("operator").toString();
                array = (JSONArray) jsonObj.get("aggregatee_indices");
                List<FileIndexTuple> aggregatee_indices = new ArrayList<>();
                for (Object obj : array) {
                    JSONArray aggregatee = (JSONArray) obj;
                    FileIndexTuple aggregatee_index = new FileIndexTuple(Integer.parseInt(aggregatee.get(0).toString()), Integer.parseInt(aggregatee.get(1).toString()));
                    aggregatee_indices.add(aggregatee_index);
                }
                double error = Double.parseDouble(jsonObj.get("error_bound").toString());
                AggregationRelation relation = new AggregationRelation(aggregator_index, aggregatee_indices, operator, error);
                relations.add(relation);
            }
        }

        CsvDisplayTableModel tableModel = new CsvDisplayTableModel(0, numOfColumns, fileName, sheetName);
        tableModel.insertRows(tableValues);
        tableModel.loadAnnotations(relations);

        this.pageComponents.getFileDisplayTableAggr().setModel(tableModel);

        this.pageComponents.getFileDisplayTableAggr().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        this.pageComponents.getFileDisplayTableAggr().setColumnSelectionAllowed(true);
        this.pageComponents.getFileDisplayTableAggr().setRowSelectionAllowed(true);

        this.pageComponents.getFileDisplayTableAggr().setDefaultRenderer(Object.class, new CsvDisplayTableRender());

        MainFrame.resizeColumnWidth(this.pageComponents.getFileDisplayTableAggr());

        this.pageComponents.getFileDisplayAggrPane().setBorder(BorderFactory.createTitledBorder(fileSheetName));

        this.selectedAggregateeBlocks = new ArrayList<>();
        this.selectedAggregatorBlocks = new ArrayList<>();
    }

    private Set<AggregationRelation> confirmAggregatorAnnotations() {
        Set<AggregationRelation> relations = new HashSet<>();
        CsvDisplayTableModel tableModel = (CsvDisplayTableModel) pageComponents.getFileDisplayTableAggr().getModel();
        if (pageComponents.getSumRadioButton().isSelected()) {
            List<BlockIndexTuples> blocks = tableModel.getSelectedAggregatorBlocks();
            Set<FileIndexTuple> aggregatorIndices = blocks.stream().flatMap(b -> b.flatten().stream()).collect(Collectors.toSet());
            Set<FileIndexTuple> aggregateeIndices = tableModel.getSelectedAggregateeBlocks().stream().flatMap(b -> b.flatten().stream()).collect(Collectors.toSet());
            for (FileIndexTuple aggregatorIndex : aggregatorIndices) {
                List<FileIndexTuple> aggregateesSameRow = aggregateeIndices.stream()
                        .filter(tuple -> tuple.getRowIndex() == aggregatorIndex.getRowIndex()).collect(Collectors.toList());
                if (aggregateesSameRow.size() > 0) {
                    relations.add(new AggregationRelation(aggregatorIndex, aggregateesSameRow,
                            pageComponents.getSumRadioButton().getText(), Double.parseDouble(pageComponents.getErrorTextField().getText())));
                }
                List<FileIndexTuple> aggregateesSameColumn = aggregateeIndices.stream()
                        .filter(tuple -> tuple.getColumnIndex() == aggregatorIndex.getColumnIndex()).collect(Collectors.toList());
                if (aggregateesSameColumn.size() > 0) {
                    relations.add(new AggregationRelation(aggregatorIndex, aggregateesSameColumn,
                            pageComponents.getSumRadioButton().getText(), Double.parseDouble(pageComponents.getErrorTextField().getText())));
                }
            }
        } else if (pageComponents.getAverageRadioButton().isSelected()) {
            List<BlockIndexTuples> blocks = tableModel.getSelectedAggregatorBlocks();
            Set<FileIndexTuple> aggregatorIndices = blocks.stream().flatMap(b -> b.flatten().stream()).collect(Collectors.toSet());
            Set<FileIndexTuple> aggregateeIndices = tableModel.getSelectedAggregateeBlocks().stream().flatMap(b -> b.flatten().stream()).collect(Collectors.toSet());
            for (FileIndexTuple aggregatorIndex : aggregatorIndices) {
                List<FileIndexTuple> aggregateesSameRow = aggregateeIndices.stream()
                        .filter(tuple -> tuple.getRowIndex() == aggregatorIndex.getRowIndex()).collect(Collectors.toList());
                if (aggregateesSameRow.size() > 0) {
                    relations.add(new AggregationRelation(aggregatorIndex, aggregateesSameRow,
                            pageComponents.getAverageRadioButton().getText(), Double.parseDouble(pageComponents.getErrorTextField().getText())));
                }
                List<FileIndexTuple> aggregateesSameColumn = aggregateeIndices.stream()
                        .filter(tuple -> tuple.getColumnIndex() == aggregatorIndex.getColumnIndex()).collect(Collectors.toList());
                if (aggregateesSameColumn.size() > 0) {
                    relations.add(new AggregationRelation(aggregatorIndex, aggregateesSameColumn,
                            pageComponents.getAverageRadioButton().getText(), Double.parseDouble(pageComponents.getErrorTextField().getText())));
                }
            }
        } else if (pageComponents.getSubtractRadioButton().isSelected()) {
            Set<FileIndexTuple> aggregatorIndices = tableModel.getSelectedAggregatorBlocks().stream().flatMap(b -> b.flatten().stream()).collect(Collectors.toSet());
            Set<FileIndexTuple> aggregateeIndices = tableModel.getSelectedAggregateeBlocks().stream().flatMap(b -> b.flatten().stream()).collect(Collectors.toSet());
            for (FileIndexTuple aggregatorIndex : aggregatorIndices) {
                List<FileIndexTuple> aggregateesSameRow = aggregateeIndices.stream()
                        .filter(tuple -> tuple.getRowIndex() == aggregatorIndex.getRowIndex()).collect(Collectors.toList());
                if (aggregateesSameRow.size() == 2) {
                    relations.add(new AggregationRelation(aggregatorIndex, aggregateesSameRow,
                            pageComponents.getSubtractRadioButton().getText(), Double.parseDouble(pageComponents.getErrorTextField().getText())));
                }
                List<FileIndexTuple> aggregateesSameColumn = aggregateeIndices.stream()
                        .filter(tuple -> tuple.getColumnIndex() == aggregatorIndex.getColumnIndex()).collect(Collectors.toList());
                if (aggregateesSameColumn.size() == 2) {
                    relations.add(new AggregationRelation(aggregatorIndex, aggregateesSameColumn,
                            pageComponents.getSubtractRadioButton().getText(), Double.parseDouble(pageComponents.getErrorTextField().getText())));
                }
            }
        } else if (pageComponents.getPercentageRadioButton().isSelected()) {
            Set<FileIndexTuple> aggregatorIndices = tableModel.getSelectedAggregatorBlocks().stream().flatMap(b -> b.flatten().stream()).collect(Collectors.toSet());
            Set<FileIndexTuple> aggregateeIndices = tableModel.getSelectedAggregateeBlocks().stream().flatMap(b -> b.flatten().stream()).collect(Collectors.toSet());
            for (FileIndexTuple aggregatorIndex : aggregatorIndices) {
                List<FileIndexTuple> aggregateesSameRow = aggregateeIndices.stream()
                        .filter(tuple -> tuple.getRowIndex() == aggregatorIndex.getRowIndex()).collect(Collectors.toList());
                if (aggregateesSameRow.size() == 2) {
                    relations.add(new AggregationRelation(aggregatorIndex, aggregateesSameRow,
                            pageComponents.getPercentageRadioButton().getText(), Double.parseDouble(pageComponents.getErrorTextField().getText())));
                }
                List<FileIndexTuple> aggregateesSameColumn = aggregateeIndices.stream()
                        .filter(tuple -> tuple.getColumnIndex() == aggregatorIndex.getColumnIndex()).collect(Collectors.toList());
                if (aggregateesSameColumn.size() == 2) {
                    relations.add(new AggregationRelation(aggregatorIndex, aggregateesSameColumn,
                            pageComponents.getPercentageRadioButton().getText(), Double.parseDouble(pageComponents.getErrorTextField().getText())));
                }
            }
        }
        return relations;
    }

    public List<FileIndexTuple> detectPotentialAggregators() {
        // calculate the aggregation on the selected cells with the selected function.
        List<FileIndexTuple> satisfiedIndices = new ArrayList<>();
        if (pageComponents.getSumRadioButton().isSelected()) {
            if (this.selectedAggregateeBlocks.size() > 0) {
                long isLeftAligned = this.selectedAggregateeBlocks.stream().map(block -> block.getTopLeftIndexTuple().getColumnIndex()).distinct().count();
                long isRightAligned = this.selectedAggregateeBlocks.stream().map(block -> block.getBottomRightIndexTuple().getColumnIndex()).distinct().count();

                if (isLeftAligned == 1 && isRightAligned == 1) {
                    List<Integer> selectedRowsList = new ArrayList<>();
                    for (BlockIndexTuples block : this.selectedAggregateeBlocks) {
                        for (int i = block.getTopLeftIndexTuple().getRowIndex(); i <= block.getBottomRightIndexTuple().getRowIndex(); i++) {
                            selectedRowsList.add(i);
                        }
                    }
                    int[] selectedRows = selectedRowsList.stream().mapToInt(i -> i).toArray();
                    List<Integer> selectedColumnsList = new ArrayList<>();
                    for (int i = this.selectedAggregateeBlocks.get(0).getTopLeftIndexTuple().getColumnIndex();
                         i <= this.selectedAggregateeBlocks.get(0).getBottomRightIndexTuple().getColumnIndex(); i++) {
                        selectedColumnsList.add(i);
                    }
                    int[] selectedColumns = selectedColumnsList.stream().mapToInt(i -> i).toArray();
                    satisfiedIndices = getSumSatisfiedIndices(selectedRows, selectedColumns);
                }

                long isTopAligned = this.selectedAggregateeBlocks.stream().map(block -> block.getTopLeftIndexTuple().getRowIndex()).distinct().count();
                long isBottomAligned = this.selectedAggregateeBlocks.stream().map(block -> block.getBottomRightIndexTuple().getRowIndex()).distinct().count();

                if (isTopAligned == 1 && isBottomAligned == 1) {
                    List<Integer> selectedColumnsList = new ArrayList<>();
                    for (BlockIndexTuples block : this.selectedAggregateeBlocks) {
                        for (int i = block.getTopLeftIndexTuple().getColumnIndex(); i <= block.getBottomRightIndexTuple().getColumnIndex(); i++) {
                            selectedColumnsList.add(i);
                        }
                    }
                    int[] selectedColumns = selectedColumnsList.stream().mapToInt(i -> i).toArray();
                    List<Integer> selectedRowsList = new ArrayList<>();
                    for (int i = this.selectedAggregateeBlocks.get(0).getTopLeftIndexTuple().getRowIndex();
                         i <= this.selectedAggregateeBlocks.get(0).getBottomRightIndexTuple().getRowIndex(); i++) {
                        selectedRowsList.add(i);
                    }
                    int[] selectedRows = selectedRowsList.stream().mapToInt(i -> i).toArray();
                    satisfiedIndices = getSumSatisfiedIndices(selectedRows, selectedColumns);
                }
            }
        } else if (pageComponents.getSubtractRadioButton().isSelected()) {
            satisfiedIndices = getSubtractSatisfiedIndices();

        } else if (pageComponents.getAverageRadioButton().isSelected()) {
            if (this.selectedAggregateeBlocks.size() > 0) {
                long isLeftAligned = this.selectedAggregateeBlocks.stream().map(block -> block.getTopLeftIndexTuple().getColumnIndex()).distinct().count();
                long isRightAligned = this.selectedAggregateeBlocks.stream().map(block -> block.getBottomRightIndexTuple().getColumnIndex()).distinct().count();

                if (isLeftAligned == 1 && isRightAligned == 1) {
                    List<Integer> selectedRowsList = new ArrayList<>();
                    for (BlockIndexTuples block : this.selectedAggregateeBlocks) {
                        for (int i = block.getTopLeftIndexTuple().getRowIndex(); i <= block.getBottomRightIndexTuple().getRowIndex(); i++) {
                            selectedRowsList.add(i);
                        }
                    }
                    int[] selectedRows = selectedRowsList.stream().mapToInt(i -> i).toArray();
                    List<Integer> selectedColumnsList = new ArrayList<>();
                    for (int i = this.selectedAggregateeBlocks.get(0).getTopLeftIndexTuple().getColumnIndex();
                         i <= this.selectedAggregateeBlocks.get(0).getBottomRightIndexTuple().getColumnIndex(); i++) {
                        selectedColumnsList.add(i);
                    }
                    int[] selectedColumns = selectedColumnsList.stream().mapToInt(i -> i).toArray();
                    satisfiedIndices = getAverageSatisfiedIndices(selectedRows, selectedColumns);
                }

                long isTopAligned = this.selectedAggregateeBlocks.stream().map(block -> block.getTopLeftIndexTuple().getRowIndex()).distinct().count();
                long isBottomAligned = this.selectedAggregateeBlocks.stream().map(block -> block.getBottomRightIndexTuple().getRowIndex()).distinct().count();

                if (isTopAligned == 1 && isBottomAligned == 1) {
                    List<Integer> selectedColumnsList = new ArrayList<>();
                    for (BlockIndexTuples block : this.selectedAggregateeBlocks) {
                        for (int i = block.getTopLeftIndexTuple().getColumnIndex(); i <= block.getBottomRightIndexTuple().getColumnIndex(); i++) {
                            selectedColumnsList.add(i);
                        }
                    }
                    int[] selectedColumns = selectedColumnsList.stream().mapToInt(i -> i).toArray();
                    List<Integer> selectedRowsList = new ArrayList<>();
                    for (int i = this.selectedAggregateeBlocks.get(0).getTopLeftIndexTuple().getRowIndex();
                         i <= this.selectedAggregateeBlocks.get(0).getBottomRightIndexTuple().getRowIndex(); i++) {
                        selectedRowsList.add(i);
                    }
                    int[] selectedRows = selectedRowsList.stream().mapToInt(i -> i).toArray();
                    satisfiedIndices = getAverageSatisfiedIndices(selectedRows, selectedColumns);
                }
            }
        } else if (pageComponents.getPercentageRadioButton().isSelected()) {
            satisfiedIndices = getPercentageSatisfiedIndices();
        }

        return satisfiedIndices;
    }

    public void enableHopSelection(boolean isEnable) {
        this.selectedAggregateeBlocks = new ArrayList<>();
        this.resetFileDisplayTableRendering(isEnable?SheetDisplayCellTableModel.TableMode.SELECT: SheetDisplayCellTableModel.TableMode.CALCULATE);
    }

    /**
     * Switch table mode, and clear all colored borders.
     * @param tableMode target table mode
     */
    public void resetFileDisplayTableRendering(int tableMode) {
        CsvDisplayTableModel tableModel = (CsvDisplayTableModel) this.pageComponents.getFileDisplayTableAggr().getModel();
        tableModel.setTableMode(tableMode);
        tableModel.dehighlightAggregatorCells();
        tableModel.removeAggregateeSelection();
    }

    public void switchMode(int tableMode) {
        CsvDisplayTableModel tableModel = (CsvDisplayTableModel) pageComponents.getFileDisplayTableAggr().getModel();
        tableModel.setTableMode(tableMode);
    }

    @Override
    public void mouseOperationOnFileDisplayTable(MouseEvent e) {
        ListSelectionModel fileDisplaySelectionModel = this.pageComponents.getFileDisplayTableAggr().getSelectionModel();
        if (!fileDisplaySelectionModel.isSelectionEmpty()) {
            int[] selectedRows = this.pageComponents.getFileDisplayTableAggr().getSelectedRows();
            int topIndex = selectedRows[0] + 1;
            int bottomIndex = selectedRows[selectedRows.length - 1] + 1;
            int[] selectedColumns = this.pageComponents.getFileDisplayTableAggr().getSelectedColumns();
            int leftIndex = selectedColumns[0] + 1;
            int rightIndex = selectedColumns[selectedColumns.length - 1] + 1;

            pageComponents.getTopleftIndexAggr().setText("<" + topIndex + "," + leftIndex + ">");
            pageComponents.getBottomRightIndexAggr().setText("<" + bottomIndex + "," + rightIndex + ">");

            CsvDisplayTableModel tableModel = (CsvDisplayTableModel) pageComponents.getFileDisplayTableAggr().getModel();

            BlockIndexTuples block = new BlockIndexTuples(new FileIndexTuple(selectedRows[0], selectedColumns[0]),
                    new FileIndexTuple(selectedRows[selectedRows.length - 1], selectedColumns[selectedColumns.length - 1]));

            if (tableModel.getTableMode() == CsvDisplayTableModel.TableMode.VIEW) {
                // in the view mode, if the selected cells have been annotated as aggregators, highlight their respective aggregatee cells.
                tableModel.dehighlightAnnotatedAggregateeCells();
                for (FileIndexTuple cellIndex : block.flatten()) {
                    Optional<AggregationRelation> optRelation = tableModel.getAggregationMapping().stream()
                            .filter(relation -> relation.getAggregator().equals(cellIndex)).findFirst();
                    if (!optRelation.isPresent()) {
                        continue;
                    }
                    AggregationRelation relation = optRelation.get();
                    tableModel.highlightAnnotatedAggregateeCells(relation.getAggregatees());
                }
                return;
            }

            if (tableModel.getTableMode() == CsvDisplayTableModel.TableMode.SELECT_AGGREGATEES) {
                // Todo: in the selecting mode, when selecting a cell that is already in a block, remove that block
                selectedAggregateeBlocks.add(block);
                tableModel.dehighlightAggregatorCells();
                tableModel.selectAggregateeBlock(block);

                if (pageComponents.getOperandOneRadioButton().isSelected()) {
                    pageComponents.getOperandOneCellRange().setText(pageComponents.getTopleftIndexAggr().getText() + " - "
                            + pageComponents.getBottomRightIndexAggr().getText());
                } else if (pageComponents.getOperandTwoRadioButton().isSelected()) {
                    pageComponents.getOperandTwoCellRange().setText(pageComponents.getTopleftIndexAggr().getText() + " - "
                            + pageComponents.getBottomRightIndexAggr().getText());
                }
            } else if (tableModel.getTableMode() == CsvDisplayTableModel.TableMode.SELECT_AGGREGATORS) {
                selectedAggregatorBlocks.add(block);
                tableModel.selectAggregatorBlock(block);
            } else {
                throw new IllegalStateException("Unknown table mode.");
            }

//            if (this.pageComponents.getHopSelectionModeCheckBox().isSelected()) {
//                switchMode(SheetDisplayCellTableModel.TableMode.SELECT);
//                BlockIndexTuples<Integer, Integer> block = new BlockIndexTuples<>(new FileIndexTuple(selectedRows[0], selectedColumns[0]),
//                        new FileIndexTuple(selectedRows[selectedRows.length - 1], selectedColumns[selectedColumns.length - 1]));
//                selectedAggregateeBlocks.add(block);
//                SheetDisplayCellTableModel tableModel = (SheetDisplayCellTableModel) pageComponents.getFileDisplayTableAggr().getModel();
//                tableModel.updateAccSelectedCells(block);
//                tableModel.removeAggregatorEmphasis();
//                return;
//            } else {
//                switchMode(SheetDisplayCellTableModel.TableMode.CALCULATE);
//            }

//            this.resetFileDisplayTableRendering(SheetDisplayCellTableModel.TableMode.CALCULATE);

            // calculate the aggregation on the selected cells with the selected function.
//            List<FileIndexTuple<Integer, Integer>> satisfiedIndices;
//            if (pageComponents.getSumRadioButton().isSelected()) {
//                satisfiedIndices = getSumSatisfiedIndices(selectedRows, selectedColumns);
//            } else if (pageComponents.getSubtractRadioButton().isSelected()) {
//                satisfiedIndices = getSubtractSatisfiedIndices();
//            } else if (pageComponents.getAverageRadioButton().isSelected()) {
//                satisfiedIndices = getAverageSatisfiedIndices(selectedRows, selectedColumns);
//            } else if (pageComponents.getPercentageRadioButton().isSelected()) {
//                satisfiedIndices = getPercentageSatisfiedIndices();
//            } else {
//                satisfiedIndices = new ArrayList<>();
//            }

//            SheetDisplayCellTableModel tableModel = (SheetDisplayCellTableModel) pageComponents.getFileDisplayTableAggr().getModel();
//            tableModel.highlightAggregatorCells(satisfiedIndices);
        }
    }

    private List<FileIndexTuple> getSumSatisfiedIndices(int[] selectedRows, int[] selectedColumns) {
        DefaultTableModel tableModel = (DefaultTableModel) this.pageComponents.getFileDisplayTableAggr().getModel();
        List<FileIndexTuple> satisfiedIndices = new LinkedList<>();

        // slice horizontally
        int columnCount = tableModel.getColumnCount();
        for (int rowIndex : selectedRows) {
            List<Double> numbersInSelection = new LinkedList<>();
            Map<Integer, Double> numbersOutSelection = new HashMap<>();
            for (int j = 0; j < columnCount; j++) {
                String valueInRow = Objects.toString(tableModel.getValueAt(rowIndex, j), "");
                Double normalizedNumber = NumberUtils.normalizeNumber(valueInRow);
                if (normalizedNumber != null) {
                    int finalJ = j;
                    if (Arrays.stream(selectedColumns).anyMatch(value -> value == finalJ)) {
                        numbersInSelection.add(normalizedNumber);
                    } else {
                        numbersOutSelection.putIfAbsent(j, normalizedNumber);
                    }
                }
            }
            double expectedSum = numbersInSelection.stream().mapToDouble(Double::doubleValue).sum();
            for (Map.Entry<Integer, Double> entry : numbersOutSelection.entrySet()) {
                if (NumberUtils.isMinorError(expectedSum, entry.getValue(),
                        Double.parseDouble(pageComponents.getErrorTextField().getText()), this.errorBoundMethod)) {
                    satisfiedIndices.add(new FileIndexTuple(rowIndex, entry.getKey()));
                }
            }
        }

        // slice vertically
        int rowCount = tableModel.getRowCount();
        for (int columnIndex : selectedColumns) {
            List<Double> numbersInSelection = new LinkedList<>();
            Map<Integer, Double> numbersOutSelection = new HashMap<>();
            for (int j = 0; j < rowCount; j++) {
                String valueInColumn = Objects.toString(tableModel.getValueAt(j, columnIndex), "");
                Double normalizedNumber = NumberUtils.normalizeNumber(valueInColumn);
                if (normalizedNumber != null) {
                    int finalJ = j;
                    if (Arrays.stream(selectedRows).anyMatch(value -> value == finalJ)) {
                        numbersInSelection.add(normalizedNumber);
                    } else {
                        numbersOutSelection.putIfAbsent(j, normalizedNumber);
                    }
                }
            }
            double expectedSum = numbersInSelection.stream().mapToDouble(Double::doubleValue).sum();
            for (Map.Entry<Integer, Double> entry : numbersOutSelection.entrySet()) {
                if (NumberUtils.isMinorError(expectedSum, entry.getValue(),
                        Double.parseDouble(pageComponents.getErrorTextField().getText()), this.errorBoundMethod)) {
                    satisfiedIndices.add(new FileIndexTuple(entry.getKey(), columnIndex));
                }
            }
        }
        return satisfiedIndices;
    }

    private List<FileIndexTuple> getAverageSatisfiedIndices(int[] selectedRows, int[] selectedColumns) {
        DefaultTableModel tableModel = (DefaultTableModel) this.pageComponents.getFileDisplayTableAggr().getModel();
        List<FileIndexTuple> satisfiedIndices = new LinkedList<>();

        // slice horizontally
        int columnCount = tableModel.getColumnCount();
        for (int rowIndex : selectedRows) {
            List<Double> numbersInSelection = new LinkedList<>();
            Map<Integer, Double> numbersOutSelection = new HashMap<>();
            for (int j = 0; j < columnCount; j++) {
                String valueInRow = Objects.toString(tableModel.getValueAt(rowIndex, j), "");
                Double normalizedNumber = NumberUtils.normalizeNumber(valueInRow);
                if (normalizedNumber != null) {
                    int finalJ = j;
                    if (Arrays.stream(selectedColumns).anyMatch(value -> value == finalJ)) {
                        numbersInSelection.add(normalizedNumber);
                    } else {
                        numbersOutSelection.putIfAbsent(j, normalizedNumber);
                    }
                }
            }
            OptionalDouble optExpectedAverage = numbersInSelection.stream().mapToDouble(Double::doubleValue).average();
            if (!optExpectedAverage.isPresent()) {
                throw new RuntimeException("Average of the given numbers cannot be computed.");
            }
            double expectedAverage = optExpectedAverage.getAsDouble();
            for (Map.Entry<Integer, Double> entry : numbersOutSelection.entrySet()) {
                if (NumberUtils.isMinorError(expectedAverage, entry.getValue(),
                        Double.parseDouble(pageComponents.getErrorTextField().getText()), this.errorBoundMethod)) {
                    satisfiedIndices.add(new FileIndexTuple(rowIndex, entry.getKey()));
                }
            }
        }

        // slice vertically
        int rowCount = tableModel.getRowCount();
        for (int columnIndex : selectedColumns) {
            List<Double> numbersInSelection = new LinkedList<>();
            Map<Integer, Double> numbersOutSelection = new HashMap<>();
            for (int j = 0; j < rowCount; j++) {
                String valueInColumn = Objects.toString(tableModel.getValueAt(j, columnIndex), "");
                Double normalizedNumber = NumberUtils.normalizeNumber(valueInColumn);
                if (normalizedNumber != null) {
                    int finalJ = j;
                    if (Arrays.stream(selectedRows).anyMatch(value -> value == finalJ)) {
                        numbersInSelection.add(normalizedNumber);
                    } else {
                        numbersOutSelection.putIfAbsent(j, normalizedNumber);
                    }
                }
            }
            OptionalDouble optExpectedAverage = numbersInSelection.stream().mapToDouble(Double::doubleValue).average();
            if (!optExpectedAverage.isPresent()) {
                throw new RuntimeException("Average of the given numbers cannot be computed.");
            }
            double expectedAverage = optExpectedAverage.getAsDouble();
            for (Map.Entry<Integer, Double> entry : numbersOutSelection.entrySet()) {
                if (NumberUtils.isMinorError(expectedAverage, entry.getValue(),
                        Double.parseDouble(pageComponents.getErrorTextField().getText()), this.errorBoundMethod)) {
                    satisfiedIndices.add(new FileIndexTuple(entry.getKey(), columnIndex));
                }
            }
        }
        return satisfiedIndices;
    }

    private List<FileIndexTuple> getSubtractSatisfiedIndices() {
        List<FileIndexTuple> satisfiedIndices = new LinkedList<>();

        DefaultTableModel tableModel = (DefaultTableModel) this.pageComponents.getFileDisplayTableAggr().getModel();

        if (pageComponents.getOperandOneCellRange().getText().equals("N/A") ||
                pageComponents.getOperandTwoCellRange().getText().equals("N/A")) {
            return satisfiedIndices;
        }

        Pattern pattern = Pattern.compile("<(\\d+),(\\d+)> - <(\\d+),(\\d+)>");
        Matcher matcher = pattern.matcher(pageComponents.getOperandOneCellRange().getText());

        int opOneTopIndex = 0, opOneLeftIndex = 0, opOneBottomIndex = 0, opOneRightIndex = 0;
        if (matcher.find()) {
            opOneTopIndex = Integer.parseInt(matcher.group(1)) - 1;
            opOneLeftIndex = Integer.parseInt(matcher.group(2)) - 1;
            opOneBottomIndex = Integer.parseInt(matcher.group(3)) - 1;
            opOneRightIndex = Integer.parseInt(matcher.group(4)) - 1;
        }
        matcher = pattern.matcher(pageComponents.getOperandTwoCellRange().getText());
        int opTwoTopIndex = 0, opTwoLeftIndex = 0, opTwoBottomIndex = 0, opTwoRightIndex = 0;
        if (matcher.find()) {
            opTwoTopIndex = Integer.parseInt(matcher.group(1)) - 1;
            opTwoLeftIndex = Integer.parseInt(matcher.group(2)) - 1;
            opTwoBottomIndex = Integer.parseInt(matcher.group(3)) - 1;
            opTwoRightIndex = Integer.parseInt(matcher.group(4)) - 1;
        }

        // horizontal slice
        if ((opOneTopIndex == opOneBottomIndex) && (opTwoTopIndex == opTwoBottomIndex)) {
            if ((opOneLeftIndex == opTwoLeftIndex) && (opOneRightIndex == opTwoRightIndex)) { // if the two blocks are horizontal slices
                int rowCount = tableModel.getRowCount();
                for (int i = 0; i < rowCount; i++) {
                    if (opOneTopIndex == i || opTwoTopIndex == i) {
                        continue;
                    }
                    for (int j = opOneLeftIndex; j <= opOneRightIndex; j++) {
                        String valueTarget = Objects.toString(tableModel.getValueAt(i, j), "");
                        Double numberTarget = NumberUtils.normalizeNumber(valueTarget);
                        if (numberTarget == null) {
                            continue;
                        }
                        numberTarget = Math.abs(numberTarget);
                        String valueOpOne = Objects.toString(tableModel.getValueAt(opOneTopIndex, j), "");
                        Double numberOpOne = NumberUtils.normalizeNumber(valueOpOne);
                        String valueOpTwo = Objects.toString(tableModel.getValueAt(opTwoTopIndex, j), "");
                        Double numberOpTwo = NumberUtils.normalizeNumber(valueOpTwo);
                        if (numberOpOne != null && numberOpTwo != null) {
                            double expectedSubtraction = Math.abs(numberOpOne - numberOpTwo);
                            if (NumberUtils.isMinorError(expectedSubtraction, numberTarget,
                                    Double.parseDouble(pageComponents.getErrorTextField().getText()), this.errorBoundMethod)) {
                                satisfiedIndices.add(new FileIndexTuple(i, j));
                            }
                        }
                    }
                }
            }

        }

        // vertical slice
        if ((opOneLeftIndex == opOneRightIndex) && (opTwoLeftIndex == opTwoRightIndex)) {
            if ((opOneTopIndex == opTwoTopIndex) && (opOneBottomIndex == opTwoBottomIndex)) { // if the two blocks are horizontal slices
                int columnCount = tableModel.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    if (opOneLeftIndex == i || opTwoLeftIndex == i) {
                        continue;
                    }
                    for (int j = opOneTopIndex; j <= opOneBottomIndex; j++) {
                        String valueTarget = Objects.toString(tableModel.getValueAt(j, i), "");
                        Double numberTarget = NumberUtils.normalizeNumber(valueTarget);
                        if (numberTarget == null) {
                            continue;
                        }
                        numberTarget = Math.abs(numberTarget);
                        String valueOpOne = Objects.toString(tableModel.getValueAt(j, opOneLeftIndex), "");
                        Double numberOpOne = NumberUtils.normalizeNumber(valueOpOne);
                        String valueOpTwo = Objects.toString(tableModel.getValueAt(j, opTwoLeftIndex), "");
                        Double numberOpTwo = NumberUtils.normalizeNumber(valueOpTwo);
                        if (numberOpOne != null && numberOpTwo != null) {
                            double expectedSubtraction = Math.abs(numberOpOne - numberOpTwo);
                            if (NumberUtils.isMinorError(expectedSubtraction, numberTarget,
                                    Double.parseDouble(pageComponents.getErrorTextField().getText()), this.errorBoundMethod)) {
                                satisfiedIndices.add(new FileIndexTuple(j, i));
                            }
                        }
                    }
                }
            }

        }
        return satisfiedIndices;
    }

    private List<FileIndexTuple> getPercentageSatisfiedIndices() {
        List<FileIndexTuple> satisfiedIndices = new LinkedList<>();

        DefaultTableModel tableModel = (DefaultTableModel) this.pageComponents.getFileDisplayTableAggr().getModel();

        if (pageComponents.getOperandOneCellRange().getText().equals("N/A") ||
                pageComponents.getOperandTwoCellRange().getText().equals("N/A")) {
            return satisfiedIndices;
        }

        Pattern pattern = Pattern.compile("<(\\d+),(\\d+)> - <(\\d+),(\\d+)>");
        Matcher matcher = pattern.matcher(pageComponents.getOperandOneCellRange().getText());

        int opOneTopIndex = 0, opOneLeftIndex = 0, opOneBottomIndex = 0, opOneRightIndex = 0;
        if (matcher.find()) {
            opOneTopIndex = Integer.parseInt(matcher.group(1)) - 1;
            opOneLeftIndex = Integer.parseInt(matcher.group(2)) - 1;
            opOneBottomIndex = Integer.parseInt(matcher.group(3)) - 1;
            opOneRightIndex = Integer.parseInt(matcher.group(4)) - 1;
        }
        matcher = pattern.matcher(pageComponents.getOperandTwoCellRange().getText());
        int opTwoTopIndex = 0, opTwoLeftIndex = 0, opTwoBottomIndex = 0, opTwoRightIndex = 0;
        if (matcher.find()) {
            opTwoTopIndex = Integer.parseInt(matcher.group(1)) - 1;
            opTwoLeftIndex = Integer.parseInt(matcher.group(2)) - 1;
            opTwoBottomIndex = Integer.parseInt(matcher.group(3)) - 1;
            opTwoRightIndex = Integer.parseInt(matcher.group(4)) - 1;
        }

        // horizontal slice
        if ((opOneTopIndex == opOneBottomIndex) && (opTwoTopIndex == opTwoBottomIndex)) {
            if ((opOneLeftIndex == opTwoLeftIndex) && (opOneRightIndex == opTwoRightIndex)) { // if the two blocks are horizontal slices
                int rowCount = tableModel.getRowCount();
                for (int i = 0; i < rowCount; i++) {
                    if (opOneTopIndex == i || opTwoTopIndex == i) {
                        continue;
                    }
                    for (int j = opOneLeftIndex; j <= opOneRightIndex; j++) {
                        String valueTarget = Objects.toString(tableModel.getValueAt(i, j), "");
                        Double numberTarget = NumberUtils.normalizeNumber(valueTarget);
                        if (numberTarget == null) {
                            continue;
                        }
                        numberTarget = Math.abs(numberTarget);
                        String valueOpOne = Objects.toString(tableModel.getValueAt(opOneTopIndex, j), "");
                        Double numberOpOne = NumberUtils.normalizeNumber(valueOpOne);
                        String valueOpTwo = Objects.toString(tableModel.getValueAt(opTwoTopIndex, j), "");
                        Double numberOpTwo = NumberUtils.normalizeNumber(valueOpTwo);
                        if (numberOpOne != null && numberOpTwo != null) {
                            double expectedPercentage = numberOpOne / numberOpTwo;
                            double expectedChangedRatio = (numberOpTwo - numberOpOne) / numberOpOne;
                            double param = Double.parseDouble(pageComponents.getErrorTextField().getText());
                            boolean isPercentage = NumberUtils.isMinorError(expectedPercentage, numberTarget, param, errorBoundMethod);
                            boolean isChangedRatio = NumberUtils.isMinorError(expectedChangedRatio, numberTarget, param, errorBoundMethod);
                            boolean isChangedPercentage = NumberUtils.isMinorError(expectedChangedRatio * 100, numberTarget, param, errorBoundMethod);
                            if (isPercentage
                                    || isChangedRatio
                                    || isChangedPercentage) {
                                satisfiedIndices.add(new FileIndexTuple(i, j));
                            }
                        }
                    }
                }
            }

        }

        // vertical slice
        if ((opOneLeftIndex == opOneRightIndex) && (opTwoLeftIndex == opTwoRightIndex)) {
            if ((opOneTopIndex == opTwoTopIndex) && (opOneBottomIndex == opTwoBottomIndex)) { // if the two blocks are horizontal slices
                int columnCount = tableModel.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    if (opOneLeftIndex == i || opTwoLeftIndex == i) {
                        continue;
                    }
                    for (int j = opOneTopIndex; j <= opOneBottomIndex; j++) {
                        String valueTarget = Objects.toString(tableModel.getValueAt(j, i), "");
                        Double numberTarget = NumberUtils.normalizeNumber(valueTarget);
                        if (numberTarget == null) {
                            continue;
                        }
                        numberTarget = Math.abs(numberTarget);
                        String valueOpOne = Objects.toString(tableModel.getValueAt(j, opOneLeftIndex), "");
                        Double numberOpOne = NumberUtils.normalizeNumber(valueOpOne);
                        String valueOpTwo = Objects.toString(tableModel.getValueAt(j, opTwoLeftIndex), "");
                        Double numberOpTwo = NumberUtils.normalizeNumber(valueOpTwo);
                        if (numberOpOne != null && numberOpTwo != null) {
                            double expectedPercentage = numberOpOne / numberOpTwo;
                            double expectedChangedRatio = (numberOpTwo - numberOpOne) / numberOpOne;
                            double param = Double.parseDouble(pageComponents.getErrorTextField().getText());
                            boolean isPercentage = NumberUtils.isMinorError(expectedPercentage, numberTarget, param, errorBoundMethod);
                            boolean isChangedRatio = NumberUtils.isMinorError(expectedChangedRatio, numberTarget, param, errorBoundMethod);
                            boolean isChangedPercentage = NumberUtils.isMinorError(expectedChangedRatio * 100, numberTarget, param, errorBoundMethod);
                            if (isPercentage
                                    || isChangedRatio
                                    || isChangedPercentage) {
                                satisfiedIndices.add(new FileIndexTuple(j, i));
                            }
                        }
                    }
                }
            }

        }
        return satisfiedIndices;
    }

    public void prepareAggregationFunctionSetting(String functionName) {
        if ("Sum".equals(functionName)) {
            this.pageComponents.getOperandOneRadioButton().doClick();
            this.pageComponents.getOperandTwoRadioButton().setEnabled(false);
        } else if ("Subtract".equals(functionName)) {
            this.pageComponents.getOperandOneRadioButton().doClick();
            this.pageComponents.getOperandTwoRadioButton().setEnabled(true);
        } else if ("Average".equals(functionName)) {
            this.pageComponents.getOperandOneRadioButton().doClick();
            this.pageComponents.getOperandTwoRadioButton().setEnabled(false);
        } else if ("Percentage".equals(functionName)) {
            this.pageComponents.getOperandOneRadioButton().doClick();
            this.pageComponents.getOperandTwoRadioButton().setEnabled(true);
        } else {
            throw new RuntimeException("Internal error, the given function name is not recognizable.");
        }
    }

    @Override
    public void keyOperationOnFileDisplayTable(KeyEvent e) {
        // select operand
        switch (e.getKeyCode()) {
            case KeyEvent.VK_1:
            case KeyEvent.VK_2:
                switchOperandSelection(e);
                break;
            case KeyEvent.VK_F:
                // Todo
//                switchHopSelection(e);
                break;
            case KeyEvent.VK_E:
                setAggregatees();
                break;
            case KeyEvent.VK_A:
                annotate(e);
                break;
            case KeyEvent.VK_C:
                cancelCurrentAnnotation();
                break;
            case KeyEvent.VK_SPACE:
                toggleViewMode();
                break;
            case KeyEvent.VK_ENTER:
                break;
            default:
                break;
        }
    }

    private void toggleViewMode() {
        CsvDisplayTableModel tableModel = (CsvDisplayTableModel) pageComponents.getFileDisplayTableAggr().getModel();
        if (tableModel.getTableMode() != CsvDisplayTableModel.TableMode.VIEW) {
            tableModel.setLastTableMode(tableModel.getTableMode());
            tableModel.setTableMode(CsvDisplayTableModel.TableMode.VIEW);
        } else {
            tableModel.setTableMode(tableModel.getLastTableMode());
            tableModel.setLastTableMode(CsvDisplayTableModel.TableMode.VIEW);
        }
        pageComponents.getModeHintLabel().setText(CsvDisplayTableModel.TableMode.getModeString(tableModel.getTableMode()));
    }

    /**
     * Render the cells of the potential aggregators with red cell border by computing the aggregatees.
     */
    private void setAggregatees() {
        CsvDisplayTableModel tableModel = (CsvDisplayTableModel) pageComponents.getFileDisplayTableAggr().getModel();
        if (tableModel.getTableMode() == CsvDisplayTableModel.TableMode.SELECT_AGGREGATEES
                || tableModel.getTableMode() == CsvDisplayTableModel.TableMode.VIEW) {
            tableModel.setTableMode(CsvDisplayTableModel.TableMode.VIEW);
            tableModel.setLastTableMode(CsvDisplayTableModel.TableMode.SELECT_AGGREGATORS);
            pageComponents.getModeHintLabel().setText(CsvDisplayTableModel.TableMode.getModeString(tableModel.getTableMode()));

            List<FileIndexTuple> satisfiedCellIndices = detectPotentialAggregators();
            tableModel.highlightAggregatorCells(satisfiedCellIndices);
        }
    }

    private void cancelCurrentAnnotation() {
        CsvDisplayTableModel tableModel = (CsvDisplayTableModel) pageComponents.getFileDisplayTableAggr().getModel();
        tableModel.resetCurrentAnnotationTask();
        tableModel.setTableMode(CsvDisplayTableModel.TableMode.DEFAULT);
        tableModel.setLastTableMode(CsvDisplayTableModel.TableMode.DEFAULT_LAST);
        this.selectedAggregateeBlocks.clear();
        this.selectedAggregatorBlocks.clear();
    }

    private void switchOperandSelection(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_1) {
            this.pageComponents.getOperandOneRadioButton().setSelected(true);
        } else if (e.getKeyCode() == KeyEvent.VK_2) {
            this.pageComponents.getOperandTwoRadioButton().setSelected(true);
        } else {
            throw new RuntimeException("Internal error: illegal selected key.");
        }
    }

    private void annotate(KeyEvent e) {
        // if shift is pressed down, treat all cells in the block as the indicated cell type.
        boolean isPaintEmptyCells = e.isShiftDown();

        CsvDisplayTableModel tableModel = (CsvDisplayTableModel) pageComponents.getFileDisplayTableAggr().getModel();
        if (tableModel.getTableMode() == CsvDisplayTableModel.TableMode.SELECT_AGGREGATORS
                || tableModel.getTableMode() == CsvDisplayTableModel.TableMode.VIEW) {

            String operatorTypeStr = getOperatorTypeStr();
            Color annotationColor = getAnnotationColor(operatorTypeStr);

            for (BlockIndexTuples aggregatorBlock : this.selectedAggregatorBlocks) {
                int topIndex = aggregatorBlock.getTopLeftIndexTuple().getRowIndex() + 1;
                int leftIndex = aggregatorBlock.getTopLeftIndexTuple().getColumnIndex() + 1;
                int bottomIndex = aggregatorBlock.getBottomRightIndexTuple().getRowIndex() + 1;
                int rightIndex = aggregatorBlock.getBottomRightIndexTuple().getColumnIndex() + 1;
                tableModel.setBlockBackgroundColor(topIndex, bottomIndex, leftIndex, rightIndex, annotationColor, isPaintEmptyCells);
            }

            Set<AggregationRelation> aggregationMapping = confirmAggregatorAnnotations();
            tableModel.updateAggregationMapping(aggregationMapping);
        }

        tableModel.setTableMode(CsvDisplayTableModel.TableMode.VIEW);
        tableModel.setLastTableMode(CsvDisplayTableModel.TableMode.SELECT_AGGREGATEES);
        pageComponents.getModeHintLabel().setText(CsvDisplayTableModel.TableMode.getModeString(tableModel.getTableMode()));

        cancelCurrentAnnotation();

        Optional<JsonSheetEntry> thisFileJsonEntry = loadedJsonSheetEntries.stream()
                .filter(entry -> tableModel.getSelectedFileName().equals(entry.getFileName())
                        && tableModel.getSelectedSheetName().equals(entry.getTableId())).findFirst();
        if (!thisFileJsonEntry.isPresent()) {
            throw new RuntimeException("Given file does not exist in the json set.");
        }
        JsonSheetEntry fileJsonEntry = thisFileJsonEntry.get();
//        JSONObject aggregationAnnotations = fileJsonEntry.getAggregation_annotations();
        List<JSONObject> aggregationAnnotations = tableModel.getAggregationMapping().stream()
                .map(aggregationRelation -> {
                    JSONObject jsonObj = new JSONObject();
                    JSONArray aggregator_index = new JSONArray();
                    aggregator_index.add(aggregationRelation.getAggregator().getRowIndex());
                    aggregator_index.add(aggregationRelation.getAggregator().getColumnIndex());
                    jsonObj.put("aggregator_index", aggregator_index);
                    jsonObj.put("operator", aggregationRelation.getOperator());
                    JSONArray aggregatee_indices = new JSONArray();
                    for (FileIndexTuple tuple : aggregationRelation.getAggregatees()) {
                        JSONArray aggregatee_index = new JSONArray();
                        aggregatee_index.add(tuple.getRowIndex());
                        aggregatee_index.add(tuple.getColumnIndex());
                        aggregatee_indices.add(aggregatee_index);
                    }
                    jsonObj.put("aggregatee_indices", aggregatee_indices);
                    jsonObj.put("error_bound", aggregationRelation.getError());
                    return jsonObj;
                }).collect(Collectors.toList());
        JSONArray annotations = new JSONArray();
        annotations.addAll(aggregationAnnotations);
        fileJsonEntry.setAggregation_annotations(annotations);
    }

    private String getOperatorTypeStr() {
        JRadioButton[] operatorRadioButtons = new JRadioButton[] {pageComponents.getSumRadioButton(),
                pageComponents.getSubtractRadioButton(),
                pageComponents.getAverageRadioButton(),
                pageComponents.getPercentageRadioButton()};
        String selectedOperatorString = null;
        for (JRadioButton opRadioButton : operatorRadioButtons) {
            if (opRadioButton.isSelected()) {
                selectedOperatorString = opRadioButton.getText();
                break;
            }
        }
        return selectedOperatorString;
    }

    private Color getAnnotationColor(String operatorTypeStr) {
        Color color;
        if (pageComponents.getSumRadioButton().getText().equals(operatorTypeStr)) {
            color = ColorSolution.SUM_AGGREGATOR_COLOR;
        } else if (pageComponents.getSubtractRadioButton().getText().equals(operatorTypeStr)) {
            color = ColorSolution.SUBTRACT_AGGREGATOR_COLOR;
        } else if (pageComponents.getAverageRadioButton().getText().equals(operatorTypeStr)) {
            color = ColorSolution.AVERAGE_AGGREGATOR_COLOR;
        } else if (pageComponents.getPercentageRadioButton().getText().equals(operatorTypeStr)) {
            color = ColorSolution.PERCENTAGE_AGGREGATOR_COLOR;
        } else {
            throw new IllegalArgumentException("Operator string unknown.");
        }
        return color;
    }

    private void switchHopSelection(KeyEvent e) {
        this.pageComponents.getHopSelectionModeCheckBox().doClick();
    }

    public void onErrorParameterChanged() {
        // update the error parameter value
        String errorParameterText = this.pageComponents.getErrorTextField().getText();
        if ("".equals(this.pageComponents.getErrorTextField().getText()) || !NumberUtils.isParsable(errorParameterText)) {
            this.errorParameter = 0;
            this.pageComponents.getErrorTextField().setText("0");
        } else {
            this.errorParameter = Double.parseDouble(this.pageComponents.getErrorTextField().getText());
        }

        this.mouseOperationOnFileDisplayTable(null);
    }

    @Override
    public void storeAnnotationResults() {
        List<JSONObject> cellAnnotationResults =
                loadedJsonSheetEntries.stream()
                        .map(entry -> {
                            JSONObject obj = new JSONObject();
                            obj.put("file_name", entry.getFileName());
                            obj.put("table_id", entry.getTableId());
                            obj.put("num_rows", entry.getNumOfRows());
                            obj.put("num_cols", entry.getNumOfColumns());
                            obj.put("table_array", entry.getTable_array());
                            obj.put("annotations", entry.getAnnotations());
                            obj.put("aggregation_annotations", entry.getAggregation_annotations());
                            obj.put("tok_tarr", entry.getToken_tarr());
                            obj.put("tok_tarr_reg", entry.getToken_tarr_reg());
                            obj.put("url", entry.getUrl());
                            obj.put("feature_array", entry.getFeature_array());
                            obj.put("feature_names", entry.getFeature_names());
                            obj.put("dictionary", entry.getDictionary());
                            return obj;
                        })
                        .collect(Collectors.toList());

        //Write JSON file
        FileWriter.writeJSONObjectToDisc(cellAnnotationResults, super.outputPath);
    }
}
