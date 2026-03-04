package com.miry.ui.component;

public class SpacerComponent extends Component {

    private int spacer;

    public SpacerComponent(int spacer) {
        super(null);
        this.spacer = spacer;
    }

    public int getSpacer() {
        return spacer;
    }

    public void setSpacer(int spacer) {
        this.spacer = spacer;
    }

}
