package com.miry.ui.component.graphic;

import com.miry.ui.component.Color;

public record GraphicDataSeries(
        float xValue,
        float yValue,
        String label,
        Color color) {
}
