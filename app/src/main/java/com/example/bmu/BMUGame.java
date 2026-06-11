package com.example.bmu;

import com.badlogic.gdx.Game;

public class BMUGame extends Game {

    public final ScreenshotHandler screenshotHandler;

    public BMUGame(ScreenshotHandler screenshotHandler) {
        this.screenshotHandler = screenshotHandler;
    }

    @Override
    public void create() {
        setScreen(new PantallaMenu(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        GestorAudio.getInstance().dispose();
    }
}