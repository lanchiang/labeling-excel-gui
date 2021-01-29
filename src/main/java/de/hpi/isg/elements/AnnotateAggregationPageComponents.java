package de.hpi.isg.elements;

import lombok.Getter;

import javax.swing.*;

/**
 * @author lan
 * @since 2021/1/17
 */
@Getter
public class AnnotateAggregationPageComponents extends PageComponents {

    private final JRadioButton sumRadioButton;
    private final JRadioButton subtractRadioButton;
    private final JRadioButton averageRadioButton;
    private final JRadioButton percentageRadioButton;
    private final JPanel operatingAggrPanel;
    private final JButton aggrAnnotationLoadDatasetButton;
    private final JTable fileReviewTable;
    private final JScrollPane fileDisplayAggrPane;
    private final JTextField errorTextField;
    private final JPanel aggrAnnotationMainPanel;
    private final JLabel numRowsAggr;
    private final JLabel numColumnsAggr;
    private final JLabel topleftIndexAggr;
    private final JLabel bottomRightIndexAggr;
    private final JTable fileDisplayTableAggr;
    private final JRadioButton operandOneRadioButton;
    private final JRadioButton operandTwoRadioButton;
    private final JLabel operandOneCellRange;
    private final JLabel operandTwoCellRange;
    private final JLabel errorMessageLabel;
    private final JCheckBox hopSelectionModeCheckBox;
    private final JButton calculateHopsButton;
    private final JLabel modeHintLabel;

    public AnnotateAggregationPageComponents(JRadioButton sumRadioButton, JRadioButton subtractRadioButton,
                                             JRadioButton averageRadioButton, JRadioButton percentageRadioButton, JPanel operatingAggrPanel,
                                             JButton aggrAnnotationLoadDatasetButton, JTable fileReviewTable,
                                             JScrollPane fileDisplayAggrPane, JTextField errorTextField, JPanel aggrAnnotationMainPanel,
                                             JLabel numRowsAggr, JLabel numColumnsAggr, JLabel topleftIndexAggr,
                                             JLabel bottomRightIndexAggr, JTable fileDisplayTableAggr, JRadioButton operandOneRadioButton,
                                             JRadioButton operandTwoRadioButton, JLabel operandOneCellRange, JLabel operandTwoCellRange,
                                             JLabel errorMessageLabel, JCheckBox hopSelectionModeCheckBox, JButton calculateHopsButton,
                                             JLabel modeHintLabel) {
        this.sumRadioButton = sumRadioButton;
        this.subtractRadioButton = subtractRadioButton;
        this.averageRadioButton = averageRadioButton;
        this.percentageRadioButton = percentageRadioButton;
        this.operatingAggrPanel = operatingAggrPanel;
        this.aggrAnnotationLoadDatasetButton = aggrAnnotationLoadDatasetButton;
        this.fileReviewTable = fileReviewTable;
        this.fileDisplayAggrPane = fileDisplayAggrPane;
        this.errorTextField = errorTextField;
        this.aggrAnnotationMainPanel = aggrAnnotationMainPanel;
        this.numRowsAggr = numRowsAggr;
        this.numColumnsAggr = numColumnsAggr;
        this.topleftIndexAggr = topleftIndexAggr;
        this.bottomRightIndexAggr = bottomRightIndexAggr;
        this.fileDisplayTableAggr = fileDisplayTableAggr;
        this.operandOneRadioButton = operandOneRadioButton;
        this.operandTwoRadioButton = operandTwoRadioButton;
        this.operandOneCellRange = operandOneCellRange;
        this.operandTwoCellRange = operandTwoCellRange;
        this.errorMessageLabel = errorMessageLabel;
        this.hopSelectionModeCheckBox = hopSelectionModeCheckBox;
        this.calculateHopsButton = calculateHopsButton;
        this.modeHintLabel = modeHintLabel;
    }

}
