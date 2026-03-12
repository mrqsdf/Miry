package com.miry.ui.component;

public class GridComponent extends Component {

    private int columns;
    private int rows;
    private int cellWidth;
    private int cellHeight;

    private Component[][] cells;

    public GridComponent(String id, int columns, int rows) {
        super(id);
        this.columns = columns;
        this.rows = rows;
        this.cells = new Component[rows][columns];
        this.cellWidth = 100; // Default cell width
        this.cellHeight = 100; // Default cell height
    }

    public GridComponent setCellSize(int width, int height) {
        this.cellWidth = width;
        this.cellHeight = height;
        return this;
    }

    public GridComponent setCell(int column, int raw, Component component) {
        if (column < 0 || column >= columns || raw < 0 || raw >= rows) {
            throw new IndexOutOfBoundsException("Cell position out of bounds");
        }
        cells[raw][column] = component;
        return this;
    }

    public Component getCell(int column, int raw) {
        if (column < 0 || column >= columns || raw < 0 || raw >= rows) {
            throw new IndexOutOfBoundsException("Cell position out of bounds");
        }
        return cells[column][raw];
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    public void setCellWidth(int cellWidth) {
        this.cellWidth = cellWidth;
    }

    public void setCellHeight(int cellHeight) {
        this.cellHeight = cellHeight;
    }

    public Component[][] getGrid() {
        return cells;
    }
}
