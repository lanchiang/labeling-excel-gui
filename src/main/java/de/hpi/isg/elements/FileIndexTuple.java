package de.hpi.isg.elements;

import lombok.Getter;

import java.util.Objects;

/**
 * @author lan
 * @since 2021/1/18
 */
public class FileIndexTuple {

    @Getter
    private final int rowIndex;

    @Getter
    private final int columnIndex;

    public FileIndexTuple(int rowIndex, int columnIndex) {
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileIndexTuple that = (FileIndexTuple) o;
        return Objects.equals(rowIndex, that.rowIndex) && Objects.equals(columnIndex, that.columnIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowIndex, columnIndex);
    }
}
