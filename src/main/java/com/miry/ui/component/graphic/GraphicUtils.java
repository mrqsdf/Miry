package com.miry.ui.component.graphic;

import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

import java.util.List;

import static com.miry.ui.util.MathUtils.clamp01;

public class GraphicUtils {

        /* =========================
   Rendering helpers
   ========================= */

    private final Theme theme;

    public GraphicUtils(Theme theme) {
        this.theme = theme;
    }


    public void drawAxesAndGrid(UiRenderer r,
                                 GraphicComponent component,
                                 int px0, int py0, int px1, int py1,
                                 Scale scale) {
        final int axis = theme.textMuted.getArgb();
        final int grid = theme.widgetOutline.getArgb();

        // Draw plot border
        r.drawRectOutline(px0, py0, px1 - px0, py1 - py0, 1, grid);

        // Grid spacing/thickness defaults
        int spacing = component.getGridSpacing() > 0 ? component.getGridSpacing() : 28;
        int thick = component.getGridLineThickness() > 0 ? component.getGridLineThickness() : 1;

        // Grid
        if (component.isGrid()) {
            // Vertical lines
            for (int x = px0 + spacing; x < px1; x += spacing) {
                r.drawLine(x, py0, x, py1, thick, grid);
            }
            // Horizontal lines
            for (int y = py0 + spacing; y < py1; y += spacing) {
                r.drawLine(px0, y, px1, y, thick, grid);
            }
        }

        // Axes (always)
        // X axis at y=0 if inside range, otherwise bottom of plot
        int xAxisY;
        if (scale.minY <= 0 && scale.maxY >= 0) {
            xAxisY = (int) Math.round(scale.toScreenY(0));
        } else {
            xAxisY = py1;
        }
        r.drawLine(px0, xAxisY, px1, xAxisY, 2, axis);

        // Y axis at x=0 if inside range, otherwise left of plot
        int yAxisX;
        if (scale.minX <= 0 && scale.maxX >= 0) {
            yAxisX = (int) Math.round(scale.toScreenX(0));
        } else {
            yAxisX = px0;
        }
        r.drawLine(yAxisX, py0, yAxisX, py1, 2, axis);

        // Draw tick labels (simple 5 ticks)
        drawTicks(r, px0, py0, px1, py1, scale);
    }

    public void drawTicks(UiRenderer r, int px0, int py0, int px1, int py1, Scale scale) {
        final int tickColor = theme.textMuted.getArgb();
        final int ticks = 5;

        // Y ticks (left)
        for (int i = 0; i <= ticks; i++) {
            float t = i / (float) ticks;
            float y = py1 - t * (py1 - py0);
            float v = scale.minY + t * (scale.maxY - scale.minY);

            String s = formatTick(v);
            float tw = r.measureText(s);

            r.drawLine(px0 - 4, (int) y, px0, (int) y, 1, tickColor);
            r.drawText(s, px0 - 8 - tw, r.baselineForBox((int) y - 8, 16), tickColor);
        }

        // X ticks (bottom)
        for (int i = 0; i <= ticks; i++) {
            float t = i / (float) ticks;
            float x = px0 + t * (px1 - px0);
            float v = scale.minX + t * (scale.maxX - scale.minX);

            String s = formatTick(v);
            float tw = r.measureText(s);

            r.drawLine((int) x, py1, (int) x, py1 + 4, 1, tickColor);
            r.drawText(s, x - tw / 2.0f, r.baselineForBox(py1 + 4, 16), tickColor);
        }
    }

    public void drawAxisLabels(UiRenderer r,
                                GraphicComponent component,
                                int x0, int y0, int x1, int y1,
                                int px0, int py0, int px1, int py1) {
        final int text = theme.text.getArgb();
        final int muted = theme.textMuted.getArgb();

        // X label (bottom centered)
        if (!isNullOrBlank(component.getAxisXLabel())) {
            String s = component.getAxisXLabel();
            float tw = r.measureText(s);
            float tx = px0 + (px1 - px0 - tw) / 2.0f;
            float ty = r.baselineForBox(y1 - (int) Math.ceil(r.lineHeight() + 4), (int) Math.ceil(r.lineHeight() + 4));
            r.drawText(s, tx, ty, muted);
        }

        // Y label (left top) - no rotation support, so we draw horizontally near top-left
        if (!isNullOrBlank(component.getAxisYLabel())) {
            String s = component.getAxisYLabel();
            float ty = r.baselineForBox(y0, (int) Math.ceil(r.lineHeight() + 4));
            r.drawText(s, x0, ty, muted);
        }
    }

    public void drawLegendRight(UiRenderer r, GraphicComponent component, List<GraphicDataSeries> points,
                                 int x, int y, int w, int h) {
        final int bg = theme.panelBg.getArgb();
        final int outline = theme.widgetOutline.getArgb();
        final int text = theme.text.getArgb();

        r.drawRoundedRect(x, y, w, h, theme.design.radius_sm, bg, theme.design.border_thin, outline);

        int pad = 8;
        int cy = y + pad;
        int cx = x + pad;

        int maxItems = Math.max(1, (int) ((h - pad * 2) / (r.lineHeight() + 6)));
        int count = Math.min(points.size(), maxItems);

        for (int i = 0; i < count; i++) {
            GraphicDataSeries p = points.get(i);
            int c = (p.color() != null) ? p.color().getArgb() : theme.widgetActive.getArgb();

            // Color box
            int box = 10;
            r.drawRect(cx, cy + 3, box, box, c);
            // Label
            String name = !isNullOrBlank(p.label()) ? p.label() : ("#" + i);
            String clipped = r.clipText(name, w - pad * 2 - box - 8);
            r.drawText(clipped, cx + box + 8, r.baselineForBox(cy, (int) Math.ceil(r.lineHeight() + 6)), text);

            cy += (int) Math.ceil(r.lineHeight() + 6);
        }
    }

    public void drawLegendBottom(UiRenderer r, GraphicComponent component, List<GraphicDataSeries> points,
                                  int x, int y, int w, int h) {
        final int bg = theme.panelBg.getArgb();
        final int outline = theme.widgetOutline.getArgb();
        final int text = theme.text.getArgb();

        r.drawRoundedRect(x, y, w, h, theme.design.radius_sm, bg, theme.design.border_thin, outline);

        int pad = 8;
        int cx = x + pad;
        int cy = y + pad;

        int box = 10;
        int lineH = (int) Math.ceil(r.lineHeight() + 6);

        // Simple flow layout
        for (int i = 0; i < points.size(); i++) {
            GraphicDataSeries p = points.get(i);
            int c = (p.color() != null) ? p.color().getArgb() : theme.widgetActive.getArgb();
            String name = !isNullOrBlank(p.label()) ? p.label() : ("#" + i);

            float tw = r.measureText(name);
            int itemW = (int) (box + 6 + tw + 12);

            if (cx + itemW > x + w - pad) {
                cx = x + pad;
                cy += lineH;
                if (cy + lineH > y + h - pad) break;
            }

            r.drawRect(cx, cy + 3, box, box, c);
            r.drawText(name, cx + box + 6, r.baselineForBox(cy, lineH), text);
            cx += itemW;
        }
    }

/* =========================
   Chart types
   ========================= */

    public void drawLineChart(UiRenderer r, GraphicComponent component,
                               int px0, int py0, int px1, int py1,
                               Scale scale, boolean filledArea) {
        List<GraphicDataSeries> pts = component.getDataSeries();
        if (pts == null || pts.size() < 2) return;

        // Sort by x is not supported without allocations; assume user inserts in order.
        // If needed, you can sort externally before calling.

        int stroke = theme.widgetActive.getArgb();
        int fill = Theme.lightenArgb(stroke, 0.12f);

        // Area fill: draw triangles to baseline (x-axis if inside range, otherwise bottom)
        float baseY = (scale.minY <= 0 && scale.maxY >= 0) ? scale.toScreenY(0) : py1;

        float prevX = scale.toScreenX(pts.get(0).xValue());
        float prevY = scale.toScreenY(pts.get(0).yValue());

        for (int i = 1; i < pts.size(); i++) {
            GraphicDataSeries p = pts.get(i);
            int c = (p.color() != null) ? p.color().getArgb() : stroke;

            float x = scale.toScreenX(p.xValue());
            float y = scale.toScreenY(p.yValue());

            if (filledArea) {
                // Triangle fan per segment to baseline
                r.drawTriangle(prevX, prevY, prevX, baseY, x, baseY, fill);
                r.drawTriangle(prevX, prevY, x, baseY, x, y, fill);
            }

            r.drawLine((int) prevX, (int) prevY, (int) x, (int) y, 2, c);

            prevX = x;
            prevY = y;
        }

        // Draw points
        for (int i = 0; i < pts.size(); i++) {
            GraphicDataSeries p = pts.get(i);
            int c = (p.color() != null) ? p.color().getArgb() : stroke;
            float x = scale.toScreenX(p.xValue());
            float y = scale.toScreenY(p.yValue());
            r.drawCircle(x, y, 3.5f, c);
        }
    }

    public void drawColumnChart(UiRenderer r, GraphicComponent component,
                                 int px0, int py0, int px1, int py1,
                                 Scale scale) {
        List<GraphicDataSeries> pts = component.getDataSeries();
        if (pts == null || pts.isEmpty()) return;

        int n = pts.size();
        float plotW = (px1 - px0);
        float slot = plotW / Math.max(1, n);
        float barW = Math.max(2, slot * 0.7f);

        float baseY = (scale.minY <= 0 && scale.maxY >= 0) ? scale.toScreenY(0) : py1;

        for (int i = 0; i < n; i++) {
            GraphicDataSeries p = pts.get(i);
            int c = (p.color() != null) ? p.color().getArgb() : theme.widgetActive.getArgb();

            float cx = px0 + (i + 0.5f) * slot;
            float x0 = cx - barW / 2.0f;
            float y = scale.toScreenY(p.yValue());

            float top = Math.min(y, baseY);
            float h = Math.abs(baseY - y);

            r.drawRect(x0, top, barW, h, c);
        }
    }

    public void drawBarChart(UiRenderer r, GraphicComponent component,
                              int px0, int py0, int px1, int py1,
                              Scale scale) {
        List<GraphicDataSeries> pts = component.getDataSeries();
        if (pts == null || pts.isEmpty()) return;

        int n = pts.size();
        float plotH = (py1 - py0);
        float slot = plotH / Math.max(1, n);
        float barH = Math.max(2, slot * 0.7f);

        float baseX = (scale.minX <= 0 && scale.maxX >= 0) ? scale.toScreenX(0) : px0;

        for (int i = 0; i < n; i++) {
            GraphicDataSeries p = pts.get(i);
            int c = (p.color() != null) ? p.color().getArgb() : theme.widgetActive.getArgb();

            float cy = py0 + (i + 0.5f) * slot;
            float y0 = cy - barH / 2.0f;
            float x = scale.toScreenX(p.xValue()); // In BAR mode, xValue is used as the bar length.

            float left = Math.min(x, baseX);
            float w = Math.abs(baseX - x);

            r.drawRect(left, y0, w, barH, c);
        }
    }

    public void drawPieChart(UiRenderer r, GraphicComponent component,
                              int px0, int py0, int px1, int py1) {
        List<GraphicDataSeries> pts = component.getDataSeries();
        if (pts == null || pts.isEmpty()) return;

        // Use yValue as the slice weight
        double sum = 0.0;
        for (GraphicDataSeries p : pts) sum += Math.max(0.0, p.yValue());
        if (sum <= 0.0) return;

        float cx = (px0 + px1) * 0.5f;
        float cy = (py0 + py1) * 0.5f;
        float radius = Math.max(10.0f, Math.min(px1 - px0, py1 - py0) * 0.35f);

        // Triangle-fan approximation
        int steps = 64;
        double startA = -Math.PI / 2.0;

        double acc = 0.0;
        for (int i = 0; i < pts.size(); i++) {
            GraphicDataSeries p = pts.get(i);
            double w = Math.max(0.0, p.yValue()) / sum;
            if (w <= 0.0) continue;

            int col = (p.color() != null) ? p.color().getArgb() : theme.widgetActive.getArgb();

            double endAcc = acc + w;
            double a0 = startA + acc * Math.PI * 2.0;
            double a1 = startA + endAcc * Math.PI * 2.0;

            // Slice fan
            double da = (a1 - a0) / steps;
            double prevX = cx + Math.cos(a0) * radius;
            double prevY = cy + Math.sin(a0) * radius;

            for (int s = 1; s <= steps; s++) {
                double a = a0 + da * s;
                double x = cx + Math.cos(a) * radius;
                double y = cy + Math.sin(a) * radius;
                r.drawTriangle(cx, cy, (float) prevX, (float) prevY, (float) x, (float) y, col);
                prevX = x;
                prevY = y;
            }

            acc = endAcc;
        }

        // Outline circle (approx by rounded rect outline)
        int outline = theme.widgetOutline.getArgb();
        r.drawCircle(cx, cy, radius, 0x00000000, 1.0f, outline);
    }

    public void drawRadarChart(UiRenderer r, GraphicComponent component,
                                int px0, int py0, int px1, int py1) {
        List<GraphicDataSeries> pts = component.getDataSeries();
        if (pts == null || pts.isEmpty()) return;

        // Radar chart: number of axes is maxValueX; values are rounded to integer.
        int axes = component.getMaxValueX();
        if (axes <= 2 || axes == Integer.MAX_VALUE) {
            // Fallback: use number of points as axes
            axes = Math.max(3, pts.size());
        }

        float cx = (px0 + px1) * 0.5f;
        float cy = (py0 + py1) * 0.5f;
        float radius = Math.max(12.0f, Math.min(px1 - px0, py1 - py0) * 0.40f);

        // Determine value range for scaling
        float minV, maxV;
        if (component.getMinValueY() != Integer.MIN_VALUE && component.getMaxValueY() != Integer.MAX_VALUE) {
            minV = component.getMinValueY();
            maxV = component.getMaxValueY();
        } else {
            float mn = Float.POSITIVE_INFINITY;
            float mx = Float.NEGATIVE_INFINITY;
            for (GraphicDataSeries p : pts) {
                float v = (float) Math.rint(p.yValue());
                mn = Math.min(mn, v);
                mx = Math.max(mx, v);
            }
            if (!Float.isFinite(mn) || !Float.isFinite(mx) || mn == mx) {
                mn = 0;
                mx = mn + 1;
            }
            float pad = (mx - mn) * 0.10f;
            minV = mn - pad;
            maxV = mx + pad;
        }

        // Draw web (grid rings)
        int grid = theme.widgetOutline.getArgb();
        int rings = 4;
        for (int k = 1; k <= rings; k++) {
            float t = k / (float) rings;
            float rr = radius * t;

            float prevX = 0, prevY = 0;
            for (int i = 0; i <= axes; i++) {
                float a = (float) (-Math.PI / 2.0 + (Math.PI * 2.0) * (i % axes) / axes);
                float x = cx + (float) Math.cos(a) * rr;
                float y = cy + (float) Math.sin(a) * rr;
                if (i > 0) r.drawLine((int) prevX, (int) prevY, (int) x, (int) y, 1, grid);
                prevX = x;
                prevY = y;
            }
        }

        // Draw axes lines
        for (int i = 0; i < axes; i++) {
            float a = (float) (-Math.PI / 2.0 + (Math.PI * 2.0) * i / axes);
            float x = cx + (float) Math.cos(a) * radius;
            float y = cy + (float) Math.sin(a) * radius;
            r.drawLine((int) cx, (int) cy, (int) x, (int) y, 1, grid);
        }

        // Plot polygon using points by axis index (use xValue as axis index)
        int stroke = theme.widgetActive.getArgb();
        int fill = Theme.lightenArgb(stroke, 0.12f);

        float[] vx = new float[axes];
        float[] vy = new float[axes];

        for (int i = 0; i < axes; i++) {
            vx[i] = cx;
            vy[i] = cy;
        }

        for (GraphicDataSeries p : pts) {
            int idx = (int) Math.floor(p.xValue());
            if (idx < 0 || idx >= axes) continue;

            float v = (float) Math.rint(p.yValue());
            float t = (v - minV) / (maxV - minV);
            t = clamp01(t);

            float a = (float) (-Math.PI / 2.0 + (Math.PI * 2.0) * idx / axes);
            float rr = radius * t;

            vx[idx] = cx + (float) Math.cos(a) * rr;
            vy[idx] = cy + (float) Math.sin(a) * rr;
        }

        // Fill polygon by triangle fan
        for (int i = 1; i < axes - 1; i++) {
            r.drawTriangle(cx, cy, vx[i], vy[i], vx[i + 1], vy[i + 1], fill);
        }

        // Stroke polygon
        for (int i = 0; i < axes; i++) {
            int j = (i + 1) % axes;
            r.drawLine((int) vx[i], (int) vy[i], (int) vx[j], (int) vy[j], 2, stroke);
        }

        // Draw points
        for (int i = 0; i < axes; i++) {
            r.drawCircle(vx[i], vy[i], 3.0f, stroke);
        }
    }

    public void drawCloudChart(UiRenderer r, GraphicComponent component,
                                int px0, int py0, int px1, int py1,
                                Scale scale) {
        List<GraphicDataSeries> pts = component.getDataSeries();
        if (pts == null || pts.isEmpty()) return;

        int stroke = theme.widgetActive.getArgb();

        for (GraphicDataSeries p : pts) {
            int c = (p.color() != null) ? p.color().getArgb() : stroke;

            float x = scale.toScreenX(p.xValue());
            float y = scale.toScreenY(p.yValue());

            // Draw a small dot
            r.drawCircle(x, y, 2.8f, c);
        }
    }

/* =========================
   Scaling helpers
   ========================= */

    public Scale computeScale(GraphicComponent component, List<GraphicDataSeries> pts,
                               int px0, int py0, int px1, int py1) {
        // If min/max are set to extreme bounds, auto-scale from points.
        float minX, maxX, minY, maxY;

        boolean hasUserX = component.getMinValueX() != Integer.MIN_VALUE || component.getMaxValueX() != Integer.MAX_VALUE;
        boolean hasUserY = component.getMinValueY() != Integer.MIN_VALUE || component.getMaxValueY() != Integer.MAX_VALUE;

        if (hasUserX) {
            minX = component.getMinValueX();
            maxX = component.getMaxValueX();
            if (minX == Integer.MIN_VALUE) minX = Float.NaN;
            if (maxX == Integer.MAX_VALUE) maxX = Float.NaN;
        } else {
            minX = Float.NaN;
            maxX = Float.NaN;
        }

        if (hasUserY) {
            minY = component.getMinValueY();
            maxY = component.getMaxValueY();
            if (minY == Integer.MIN_VALUE) minY = Float.NaN;
            if (maxY == Integer.MAX_VALUE) maxY = Float.NaN;
        } else {
            minY = Float.NaN;
            maxY = Float.NaN;
        }

        // Auto compute from points when needed
        if (pts == null || pts.isEmpty()) {
            if (!Float.isFinite(minX)) minX = 0;
            if (!Float.isFinite(maxX)) maxX = 1;
            if (!Float.isFinite(minY)) minY = 0;
            if (!Float.isFinite(maxY)) maxY = 1;
            return new Scale(minX, maxX, minY, maxY, px0, py0, px1, py1);
        }

        float axMinX = Float.POSITIVE_INFINITY;
        float axMaxX = Float.NEGATIVE_INFINITY;
        float axMinY = Float.POSITIVE_INFINITY;
        float axMaxY = Float.NEGATIVE_INFINITY;

        for (GraphicDataSeries p : pts) {
            float x = p.xValue();
            float y = p.yValue();
            if (Float.isFinite(x)) {
                axMinX = Math.min(axMinX, x);
                axMaxX = Math.max(axMaxX, x);
            }
            if (Float.isFinite(y)) {
                axMinY = Math.min(axMinY, y);
                axMaxY = Math.max(axMaxY, y);
            }
        }

        if (!Float.isFinite(minX)) minX = axMinX;
        if (!Float.isFinite(maxX)) maxX = axMaxX;
        if (!Float.isFinite(minY)) minY = axMinY;
        if (!Float.isFinite(maxY)) maxY = axMaxY;

        // Avoid degenerate ranges
        if (!Float.isFinite(minX) || !Float.isFinite(maxX) || minX == maxX) {
            minX = 0;
            maxX = minX + 1;
        }
        if (!Float.isFinite(minY) || !Float.isFinite(maxY) || minY == maxY) {
            minY = 0;
            maxY = minY + 1;
        }

        // Add padding for nicer visuals (requested +/- for clean graph)
        float padX = (maxX - minX) * 0.08f;
        float padY = (maxY - minY) * 0.10f;

        // If user explicitly provided both bounds, do not expand.
        boolean userBothX = component.getMinValueX() != Integer.MIN_VALUE && component.getMaxValueX() != Integer.MAX_VALUE;
        boolean userBothY = component.getMinValueY() != Integer.MIN_VALUE && component.getMaxValueY() != Integer.MAX_VALUE;

        if (!userBothX) {
            minX -= padX;
            maxX += padX;
        }
        if (!userBothY) {
            minY -= padY;
            maxY += padY;
        }

        // Still avoid zero range after padding
        if (minX == maxX) maxX = minX + 1;
        if (minY == maxY) maxY = minY + 1;

        return new Scale(minX, maxX, minY, maxY, px0, py0, px1, py1);
    }

    public static final class Scale {
        final float minX, maxX, minY, maxY;
        final int px0, py0, px1, py1;

        Scale(float minX, float maxX, float minY, float maxY, int px0, int py0, int px1, int py1) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.px0 = px0;
            this.py0 = py0;
            this.px1 = px1;
            this.py1 = py1;
        }

        float toScreenX(float x) {
            float t = (x - minX) / (maxX - minX);
            t = clamp01(t);
            return px0 + t * (px1 - px0);
        }

        float toScreenY(float y) {
            float t = (y - minY) / (maxY - minY);
            t = clamp01(t);
            return py1 - t * (py1 - py0);
        }
    }

/* =========================
   Small utils
   ========================= */

    public static boolean isNullOrBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static String formatTick(float v) {
        // Simple formatting: integer if close, else 1 decimal
        float rv = Math.round(v);
        if (Math.abs(rv - v) < 0.001f) return Integer.toString((int) rv);
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

}
