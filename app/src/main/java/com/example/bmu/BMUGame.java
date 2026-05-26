package com.example.bmu;

import com.badlogic.gdx.Game;

public class BMUGame extends Game {

    @Override
    public void create() {
        // Aquí se lanza la primera pantalla del juego
        setScreen(new PantallaJuego());
    }
}