package io.github.some_example_name;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Main extends Game {
    public SpriteBatch batch;
    public BitmapFont font;

    public boolean isFullscreen = false;
    public int virtualWidth = 1920;
    public int virtualHeight = 1080;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2f);

        int monitorWidth = com.badlogic.gdx.Gdx.graphics.getDisplayMode().width;
        int monitorHeight = com.badlogic.gdx.Gdx.graphics.getDisplayMode().height;
        com.badlogic.gdx.Gdx.graphics.setWindowedMode((int)(monitorWidth * 0.8f), (int)(monitorHeight * 0.8f));

        this.setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
