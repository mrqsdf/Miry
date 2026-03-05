package com.miry.ui.component.graphic;

import com.miry.ui.component.Component;

import java.util.ArrayList;
import java.util.List;

public class GraphicComponent extends Component {

    private GraphicType type;

    private String title;
    private String axisXLabel;
    private String axisYLabel;

    private boolean showLegend;
    private int maxDataPoints;
    private List<GraphicDataSeries> dataSeries;
    private boolean grid;
    private int gridSpacing;
    private int gridLineThickness;
    private int width;
    private int height;

    private int maxValueX;
    private int maxValueY;
    private int minValueX;
    private int minValueY;

    public GraphicComponent(String id, GraphicType type, int width, int height) {
        super(id);
        this.type = type;
        this.width = width;
        this.height = height;
        this.maxValueX = Integer.MAX_VALUE;
        this.maxValueY = Integer.MAX_VALUE;
        this.minValueX = Integer.MIN_VALUE;
        this.minValueY = Integer.MIN_VALUE;
        this.title = "";
        this.axisXLabel = "";
        this.axisYLabel = "";
        this.showLegend = true;
        this.maxDataPoints = 15;
        this.dataSeries = new ArrayList<>();
        this.grid = true;
    }

    public GraphicType getType() {
        return type;
    }

    public void setType(GraphicType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAxisXLabel() {
        return axisXLabel;
    }

    public void setAxisXLabel(String axisXLabel) {
        this.axisXLabel = axisXLabel;
    }

    public String getAxisYLabel() {
        return axisYLabel;
    }

    public void setAxisYLabel(String axisYLabel) {
        this.axisYLabel = axisYLabel;
    }

    public boolean isShowLegend() {
        return showLegend;
    }

    public void setShowLegend(boolean showLegend) {
        this.showLegend = showLegend;
    }

    public int getMaxDataPoints() {
        return maxDataPoints;
    }

    public void setMaxDataPoints(int maxDataPoints) {
        this.maxDataPoints = maxDataPoints;
    }

    public List<GraphicDataSeries> getDataSeries() {
        return dataSeries;
    }

    public void addDataSeries(GraphicDataSeries series) {
        this.dataSeries.add(series);
        if (dataSeries.size() > maxDataPoints) {
            this.dataSeries.remove(0); // Remove the oldest data series
        }
    }

    public void clearDataSeries() {
        this.dataSeries.clear();
    }

    public boolean isGrid() {
        return grid;
    }

    public void setGrid(boolean grid) {
        this.grid = grid;
    }

    public int getGridSpacing() {
        return gridSpacing;
    }

    public void setGridSpacing(int gridSpacing) {
        this.gridSpacing = gridSpacing;
    }

    public int getGridLineThickness() {
        return gridLineThickness;
    }

    public void setGridLineThickness(int gridLineThickness) {
        this.gridLineThickness = gridLineThickness;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getMaxValueX() {
        return maxValueX;
    }

    public void setMaxValueX(int maxValueX) {
        this.maxValueX = maxValueX;
    }

    public int getMaxValueY() {
        return maxValueY;
    }

    public void setMaxValueY(int maxValueY) {
        this.maxValueY = maxValueY;
    }

    public int getMinValueX() {
        return minValueX;
    }

    public void setMinValueX(int minValueX) {
        this.minValueX = minValueX;
    }

    public int getMinValueY() {
        return minValueY;
    }

    public void setMinValueY(int minValueY) {
        this.minValueY = minValueY;
    }


}
