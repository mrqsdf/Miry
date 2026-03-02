package com.miry.ui.component;

import com.miry.graphics.Texture;

public class TextureComponent extends Component{

    private final Texture texture;
    private final int width;
    private final int height;
    private int tintArgb;

    public TextureComponent(String id, Texture texture, int width, int height) {
        super(id);
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    public TextureComponent(String id, Texture texture) {
        this(id, texture, texture.width(), texture.height());
    }

    public Texture getTexture() {
        return texture;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTintArgb() {
        return tintArgb;
    }

    public TextureComponent setTintArgb(int tintArgb) {
        this.tintArgb = tintArgb;
        return this;
    }


}
