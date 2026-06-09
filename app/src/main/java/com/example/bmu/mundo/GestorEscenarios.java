package com.example.bmu.mundo;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class GestorEscenarios {

    private Texture texturaAzotea;
    private Texture texturaMuelle;
    private Texture texturaAbajo;
    private Texture texturaIndustria;


    private Texture texturaActual;
    private int escenarioActivo; 
    
    // Variables para la transición
    private boolean enTransicion;
    private float tiempoTransicion;
    private float duracionTransicion;
    private int escenarioDestino;

    public GestorEscenarios() {
        texturaAzotea = new Texture("escenarios/azotea.png");
        texturaMuelle = new Texture("escenarios/muelle.png");
        texturaAbajo = new Texture("escenarios/abajo-de-azotea.png");
        texturaIndustria = new Texture("escenarios/e1_industrias.png");



        escenarioActivo = 0;
        texturaActual = texturaAzotea;
        
        // Inicializar sistema de transición
        enTransicion = false;
        duracionTransicion = 1.5f; // Duración de la transición en segundos
        tiempoTransicion = 0;
        escenarioDestino = 0;
    }
    
    // Actualizar el gestor (llamar en el render de la pantalla principal)
    public void update(float deltaTime) {
        if (enTransicion) {
            tiempoTransicion += deltaTime;
            if (tiempoTransicion >= duracionTransicion) {
                // Termina la transición
                enTransicion = false;
                // Cambiar al escenario destino
                cambiarEscenario(escenarioDestino);
            }
        }
    }

    private Runnable callbackCambioEscenario;

    public void setCallbackCambioEscenario(Runnable callback) {
        this.callbackCambioEscenario = callback;
    }

    public void cambiarEscenario(int nuevoEscenario) {
        if (nuevoEscenario == 0) {
            texturaActual = texturaAzotea;
            escenarioActivo = 0;
        } else if (nuevoEscenario == 1) {
            texturaActual = texturaAbajo;
            escenarioActivo = 1;
        } else if (nuevoEscenario == 2) {
            texturaActual = texturaMuelle;
            escenarioActivo = 2;
        } else if (nuevoEscenario == 3) {
            texturaActual = texturaIndustria;
            escenarioActivo = 3;
        }

        if (callbackCambioEscenario != null) {
            callbackCambioEscenario.run();
        }
    }
    
    // Iniciar transición (llamar cuando el personaje toca el punto de cambio)
    public void iniciarTransicion(int haciaEscenario) {
        if (enTransicion) return; // Ya hay una transición en curso
        
        enTransicion = true;
        tiempoTransicion = 0;
        escenarioDestino = haciaEscenario;

        // Mostrar la textura correspondiente
        if (haciaEscenario == 0) {
                texturaActual = texturaAzotea;
            } else if (haciaEscenario == 1) {
                texturaActual = texturaAbajo;
            } else if (haciaEscenario == 2) {
                texturaActual = texturaMuelle;
            } else if (haciaEscenario == 3) {
                texturaActual = texturaIndustria; 
            }
    }
    
    public void mostrarTransicion(int haciaEscenario) {
        iniciarTransicion(haciaEscenario);
    }

    public void dibujar(SpriteBatch batch, float camaraIzqX, float camaraAbajoY, 
                        float anchoPantallaM, float altoPantallaM) {
        batch.draw(texturaActual, camaraIzqX, camaraAbajoY, anchoPantallaM, altoPantallaM);
    }

    public int getEscenarioActivo() {
        return escenarioActivo;
    }
    
    public boolean estaEnTransicion() {
        return enTransicion;
    }

    public int getEscenarioDestino() {
        return escenarioDestino;
    }

    public void setEscenarioDestino(int destino) {
        this.escenarioDestino = destino;
    }

    public void dispose() {
        texturaAzotea.dispose();
        texturaAbajo.dispose();
        texturaMuelle.dispose();
        texturaIndustria.dispose();
    }
}