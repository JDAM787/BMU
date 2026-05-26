package com.example.bmu.ui;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class ControlesTouch extends InputAdapter {
    public boolean saltarPresionado;
    public boolean golpePresionado;
    public boolean agarrarPresionado;
    public boolean lanzarPresionado;

    public void actualizar() {
        // Resetear presiones de un solo frame si fuera necesario
    }

    public void dibujar(ShapeRenderer renderer) {
        // Renderizar controles visuales
    }

    public float getDirX() {
        return 0f;
    }

    public boolean moviendoIzquierda() {
        return getDirX() < 0;
    }
}
