package de.hpi.isg.elements.aggregations;

import de.hpi.isg.elements.CellIndex;
import de.hpi.isg.utils.NumberUtils;
import lombok.Getter;

import javax.swing.table.TableModel;
import java.util.Collection;

/**
 * The abstract class for aggregation functions.
 *
 * @author lan
 * @since 2021/1/30
 */
abstract public class AggregationDetector {

    /**
     * Allowed error level.
     */
    @Getter
    private final double error;

    @Getter
    private final int errorBoundMethod;

    /**
     * The table model that stores the values of all cells.
     */
    @Getter
    private final TableModel tableModel;

    protected AggregationDetector(TableModel tableModel, double error) {
        this(tableModel, error, NumberUtils.ErrorBoundMethod.ABSOLUTE_BOUND);
    }

    protected AggregationDetector(TableModel tableModel, double error, int errorBoundMethod) {
        this.error = error;
        this.tableModel = tableModel;
        this.errorBoundMethod = errorBoundMethod;
    }

    abstract public Collection<CellIndex> getSatisfiedCellIndices(Collection<CellIndex> aggregatorCandidates, Collection<CellIndex>... selectedAggregatees);
}
