package de.hpi.isg.elements;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author lan
 * @since 2021/1/19
 */
@Getter
public class BlockIndexTuples {

    private final FileIndexTuple topLeftIndexTuple;

    private final FileIndexTuple bottomRightIndexTuple;

    public BlockIndexTuples(FileIndexTuple topLeftIndexTuple, FileIndexTuple bottomRightIndexTuple) {
        this.topLeftIndexTuple = topLeftIndexTuple;
        this.bottomRightIndexTuple = bottomRightIndexTuple;
    }

    public static BlockIndexTuples createBlockIndexTuples(String topLeftIndexStr, String bottomRightIndexStr) {
        Pattern pattern = Pattern.compile("<(\\d+),(\\d+)>");
        Matcher matcher = pattern.matcher(topLeftIndexStr);
        int topIndex = 0, leftIndex = 0;
        if (matcher.find()) {
            topIndex = Integer.parseInt(matcher.group(1)) - 1;
            leftIndex = Integer.parseInt(matcher.group(2)) - 1;
        }
        matcher = pattern.matcher(bottomRightIndexStr);
        int bottomIndex = 0, rightIndex = 0;
        if (matcher.find()) {
            bottomIndex = Integer.parseInt(matcher.group(1)) - 1;
            rightIndex = Integer.parseInt(matcher.group(2)) - 1;
        }
        return new BlockIndexTuples(new FileIndexTuple(topIndex, leftIndex), new FileIndexTuple(bottomIndex, rightIndex));
    }

    public List<FileIndexTuple> flatten() {
        List<FileIndexTuple> fileIndexTuples = new ArrayList<>();
        for (int i = this.topLeftIndexTuple.getRowIndex(); i <= this.bottomRightIndexTuple.getRowIndex(); i++) {
            for (int j = this.topLeftIndexTuple.getColumnIndex(); j <= this.bottomRightIndexTuple.getColumnIndex(); j++) {
                fileIndexTuples.add(new FileIndexTuple(i, j));
            }
        }
        return fileIndexTuples;
    }

    public boolean containsIndex(FileIndexTuple tuple) {
        return this.flatten().contains(tuple);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockIndexTuples that = (BlockIndexTuples) o;
        return Objects.equals(topLeftIndexTuple, that.topLeftIndexTuple) && Objects.equals(bottomRightIndexTuple, that.bottomRightIndexTuple);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topLeftIndexTuple, bottomRightIndexTuple);
    }
}
