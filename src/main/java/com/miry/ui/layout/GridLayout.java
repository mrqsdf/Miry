package com.miry.ui.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple fixed grid layout that computes rectangles for registered cells.
 */
public final class GridLayout {
    private final int columns;
    private final int rows;
    private final int gapX;
    private final int gapY;
    private final List<Cell> cells = new ArrayList<>();

    public GridLayout(int columns, int rows, int gapX, int gapY) {
        this.columns = Math.max(1, columns);
        this.rows = Math.max(1, rows);
        this.gapX = Math.max(0, gapX);
        this.gapY = Math.max(0, gapY);
    }

    public void addCell(int col, int row, int colSpan, int rowSpan) {
        cells.add(new Cell(col, row, Math.max(1, colSpan), Math.max(1, rowSpan)));
    }

    public void layout(int width, int height) {
        int totalGapX = gapX * (columns - 1);
        int totalGapY = gapY * (rows - 1);
        int cellWidth = (width - totalGapX) / columns;
        int cellHeight = (height - totalGapY) / rows;

        for (Cell cell : cells) {
            int x = cell.col * (cellWidth + gapX);
            int y = cell.row * (cellHeight + gapY);
            int w = cellWidth * cell.colSpan + gapX * (cell.colSpan - 1);
            int h = cellHeight * cell.rowSpan + gapY * (cell.rowSpan - 1);
            cell.setComputed(x, y, w, h);
        }
    }

    public List<Cell> cells() {
        return cells;
    }

    public void clear() {
        cells.clear();
    }

    public static final class Cell {
        private final int col;
        private final int row;
        private final int colSpan;
        private final int rowSpan;
        private int x, y, width, height;

        /**
         * One grid entry (column/row with spans) with a computed pixel rectangle.
         */
        Cell(int col, int row, int colSpan, int rowSpan) {
            this.col = col;
            this.row = row;
            this.colSpan = colSpan;
            this.rowSpan = rowSpan;
        }

        void setComputed(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        public int col() { return col; }
        public int row() { return row; }
        public int colSpan() { return colSpan; }
        public int rowSpan() { return rowSpan; }
        public int x() { return x; }
        public int y() { return y; }
        public int width() { return width; }
        public int height() { return height; }
    }
}
