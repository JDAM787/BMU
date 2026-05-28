package com.example.bmu.mundo;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class GestorEscenarios {

    private Texture texturaAzotea;
    private Texture texturaAzoteaTransicion;
    private Texture texturaAbajo;
    private Texture texturaAbajoTransicion;

    private Texture texturaActual;
    private int escenarioActivo; // 0: azotea, 1: abajo

    public GestorEscenarios() {
        texturaAzotea = new Texture("escenarios/azotea.png");
        texturaAzoteaTransicion = new Texture("escenarios/azotea-transicion.png");
        texturaAbajo = new Texture("escenarios/abajo-de-azotea.png");
        texturaAbajoTransicion = new Texture("escenarios/abajo-de-azotea-transicion.png");

        escenarioActivo = 0;
        texturaActual = texturaAzotea;
    }

    public void cambiarEscenario(int nuevoEscenario) {
        if (nuevoEscenario == 0) {
            texturaActual = texturaAzotea;
            escenarioActivo = 0;
        } else if (nuevoEscenario == 1) {
            texturaActual = texturaAbajo;
            escenarioActivo = 1;
        }
    }
    
    public void mostrarTransicion(int haciaEscenario) {
        if (haciaEscenario == 0) {
            texturaActual = texturaAzoteaTransicion;
        } else if (haciaEscenario == 1) {
            texturaActual = texturaAbajoTransicion;
        }
    }

    public void dibujar(SpriteBatch batch, float anchoM, float altoM) {
        batch.draw(texturaActual, 0, 0, anchoM, altoM);
    }

    public int getEscenarioActivo() {
        return escenarioActivo;
    }

    public void dispose() {
        texturaAzotea.dispose();
        texturaAzoteaTransicion.dispose();
        texturaAbajo.dispose();
        texturaAbajoTransicion.dispose();
    }
}
