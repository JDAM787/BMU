package com.example.bmu;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.Gdx;

/**
 * Gestiona la música de fondo del juego.
 * Singleton para mantener la música activa entre pantallas.
 */
public class GestorAudio {

    private static GestorAudio instancia;

    private Music musicaPrincipal;
    private String rutaMusicaActual = "";
    private boolean iniciado = false;

    private GestorAudio() {}

    public static GestorAudio getInstance() {
        if (instancia == null) {
            instancia = new GestorAudio();
        }
        return instancia;
    }

    public void iniciar() {
        reproducir("audio/music/menu.mp3", true);
    }

    public void reproducir(String ruta, boolean looping) {
        if (musicaPrincipal != null && ruta.equals(rutaMusicaActual)) {
            if (!musicaPrincipal.isPlaying()) {
                musicaPrincipal.play();
            }
            return;
        }

        if (musicaPrincipal != null) {
            musicaPrincipal.stop();
            musicaPrincipal.dispose();
            musicaPrincipal = null;
        }

        try {
            musicaPrincipal = Gdx.audio.newMusic(Gdx.files.internal(ruta));
            musicaPrincipal.setLooping(looping);
            musicaPrincipal.setVolume(0.6f);
            musicaPrincipal.play();
            rutaMusicaActual = ruta;
            iniciado = true;
            System.out.println("[Audio] Reproduciendo musica: " + ruta);
        } catch (Exception e) {
            System.out.println("[Audio] Error al cargar musica: " + e.getMessage());
        }
    }

    public void pausar() {
        if (musicaPrincipal != null && musicaPrincipal.isPlaying()) {
            musicaPrincipal.pause();
        }
    }

    public void reanudar() {
        if (musicaPrincipal != null && !musicaPrincipal.isPlaying()) {
            musicaPrincipal.play();
        }
    }

    public void setVolumen(float volumen) {
        if (musicaPrincipal != null) {
            musicaPrincipal.setVolume(Math.max(0f, Math.min(1f, volumen)));
        }
    }

    public void dispose() {
        if (musicaPrincipal != null) {
            musicaPrincipal.dispose();
            musicaPrincipal = null;
            rutaMusicaActual = "";
            iniciado = false;
        }
    }
}
