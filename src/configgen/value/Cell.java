package configgen.value;

import configgen.util.ListParser;
import configgen.util.NestListParser;

import java.util.List;
import java.util.stream.Collectors;

public final class Cell {
    final int row;
    final int col;
    final String data;

    public Cell(int row, int col, String data) {
        this.row = row;
        this.col = col;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "row=" + (row + 1) + ",col=" + toAZ(col) + ",data=" + data;
    }

    private static final int N = 'Z' - 'A' + 1;

    private static String toAZ(int v) {
        int q = v / N;
        String r = String.valueOf((char) ('A' + (v % N)));
        if (q > 0)
            return toAZ(q) + r;
        else
            return r;
    }

}
