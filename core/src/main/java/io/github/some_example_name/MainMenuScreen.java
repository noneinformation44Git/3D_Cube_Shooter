package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;

public class MainMenuScreen implements Screen {
    private final Main game;
    private int currentTab = 0; // 0 - Main, 1 - Settings, 2 - About

    public MainMenuScreen(Main game) {
        this.game = game;
        Gdx.input.setCursorCatched(false);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.11f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.getProjectionMatrix().setToOrtho2D(0, 0, 1280, 720);
        game.batch.begin();

        if (currentTab == 0) {
            drawMainMenu();
        } else if (currentTab == 1) {
            drawSettingsMenu();
        } else if (currentTab == 2) {
            drawAboutMenu();
        }

        game.batch.end();
        handleInput();
    }

    private void drawMainMenu() {
        game.font.setColor(Color.GOLD);
        game.font.draw(game.batch, "=== 3D CUBE SHOOTER ===", 1280 / 2f - 170, 550);

        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "[1] PLAY GAME", 1280 / 2f - 100, 420);
        game.font.draw(game.batch, "[2] SETTINGS", 1280 / 2f - 100, 360);
        game.font.draw(game.batch, "[3] ABOUT PROJECT", 1280 / 2f - 100, 300);
        game.font.draw(game.batch, "[ESC] EXIT", 1280 / 2f - 100, 240);

        game.font.setColor(Color.GRAY);
        game.font.draw(game.batch, "Use keyboard numbers to navigate", 1280 / 2f - 190, 120);
    }

    private void drawSettingsMenu() {
        game.font.setColor(Color.CYAN);
        game.font.draw(game.batch, "=== SETTINGS ===", 1280 / 2f - 100, 550);

        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "[F] Toggle Fullscreen (Current: " + (game.isFullscreen ? "ON" : "OFF") + ")", 1280 / 2f - 250, 420);
        game.font.draw(game.batch, "[7] Set Resolution 1280x720", 1280 / 2f - 250, 360);
        game.font.draw(game.batch, "[8] Set Resolution 1600x900", 1280 / 2f - 250, 300);
        game.font.draw(game.batch, "[9] Set Resolution 1920x1080 (Full HD)", 1280 / 2f - 250, 240);

        game.font.setColor(Color.GOLD);
        game.font.draw(game.batch, "[B] Back to Main Menu", 1280 / 2f - 120, 120);
    }

    private void drawAboutMenu() {
        game.font.setColor(Color.MAGENTA);
        game.font.draw(game.batch, "=== ABOUT ===", 1280 / 2f - 80, 550);

        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "A custom 3D sandbox built with Java & libGDX.", 1280 / 2f - 260, 420);
        game.font.setColor(Color.GREEN);
        game.font.draw(game.batch, "[H] Open GitHub Repository", 1280 / 2f - 160, 320);

        game.font.setColor(Color.GRAY);
        game.font.draw(game.batch, "Clicking 'H' will open your browser", 1280 / 2f - 180, 260);

        game.font.setColor(Color.GOLD);
        game.font.draw(game.batch, "[B] Back to Main Menu", 1280 / 2f - 120, 120);
    }

    private void handleInput() {
        if (currentTab == 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) game.setScreen(new GameScreen(game));
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) currentTab = 1;
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) currentTab = 2;
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.B)) currentTab = 0;

            if (currentTab == 1) { // Настройки экрана
                if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                    game.isFullscreen = !game.isFullscreen;
                    if (game.isFullscreen) {
                        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                    } else {
                        Gdx.graphics.setWindowedMode(1280, 720);
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) Gdx.graphics.setWindowedMode(1280, 720);
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)) Gdx.graphics.setWindowedMode(1600, 900);
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)) Gdx.graphics.setWindowedMode(1920, 1080);
            }

            if (currentTab == 2) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
                    Gdx.net.openURI("https://github.com/noneinformation44gb-cell");
                }
            }
        }
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
