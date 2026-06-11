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
    private boolean iniciado = false;

    private GestorAudio() {}

    public static GestorAudio getInstance() {
        if (instancia == null) {
            instancia = new GestorAudio();
        }
        return instancia;
    }

    public void iniciar() {
        if (!iniciado) {
            try {
                musicaPrincipal = Gdx.audio.newMusic(Gdx.files.internal("audio/music/maintheme.mp3"));
                musicaPrincipal.setLooping(true);
                musicaPrincipal.setVolume(0.6f);
                musicaPrincipal.play();
                iniciado = true;
                System.out.println("[Audio] Música principal iniciada.");
            } catch (Exception e) {
                System.out.println("[Audio] Error al cargar música: " + e.getMessage());
            }
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
            iniciado = false;
        }
    }
}
