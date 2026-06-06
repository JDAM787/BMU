package com.example.bmu.vista;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class AnimadorHeroe {
    public Animation<TextureRegion> animIdle;
    public Animation<TextureRegion> animWalk;
    public Animation<TextureRegion> animRun;
    public Animation<TextureRegion> animPunch;     // Golpe estando parado o caminando (fila 0)
    public Animation<TextureRegion> animPunchRun;  // Golpe estando corriendo (fila 1)
    public Animation<TextureRegion> animKick;
    public Animation<TextureRegion> animHurt;
    public Animation<TextureRegion> animFall;

    // Guardaremos las texturas para poder hacer dispose al final
    private Array<Texture> texturasCargadas;

    public AnimadorHeroe() {
        texturasCargadas = new Array<>();

        // Cargar las animaciones desde las nuevas subcarpetas (Deben ser PNG, no JPEG)
        animIdle = crearAnimacion(0.15f, "quieto/stand1.png", "quieto/stand2.png", "quieto/stand3.png");
        animWalk = crearAnimacion(0.1f, "caminando/walk1.png", "caminando/walk2.png", "caminando/walk3.png", "caminando/walk4.png");
        animRun = crearAnimacion(0.08f, 
                "corriendo/run1.png", 
                "corriendo/run2.png", 
                "corriendo/run3.png", 
                "corriendo/run4.png",
                "corriendo/run5.png",
                "corriendo/run6.png"
        );
        
        // ── Golpeando: sprite sheet 8x2 (200x249 px por frame) ──────────────
        // Fila 0: golpe estando parado/caminando  → PlayMode.NORMAL (se reproduce una sola vez)
        // Fila 1: golpe estando corriendo          → PlayMode.NORMAL
        animPunch    = crearAnimacionFila("golpeando/golpeando.png", 200, 249, 8, 0, 0.07f);
        animPunchRun = crearAnimacionFila("golpeando/golpeando.png", 200, 249, 8, 1, 0.07f);

        // Pendientes por agregar:
        // animKick = ...
        // animHurt = ...
        // animFall = ...
    }

    /**
     * Carga UNA FILA del sprite sheet como animación independiente.
     * PlayMode.NORMAL: se reproduce una sola vez y se queda en el último frame.
     *
     * @param ruta       ruta relativa dentro de assets/personajes/heroe/sprites-personaje/
     * @param frameAncho ancho de cada frame en píxeles
     * @param frameAlto  alto de cada frame en píxeles
     * @param cols       número de columnas en el sheet
     * @param fila       índice de la fila a leer (0 = primera)
     * @param duracion   segundos por frame
     */
    private Animation<TextureRegion> crearAnimacionFila(
            String ruta, int frameAncho, int frameAlto,
            int cols, int fila, float duracion) {
        TextureRegion[][] grilla = null;
        try {
            Texture sheet = new Texture("personajes/heroe/sprites-personaje/" + ruta);
            texturasCargadas.add(sheet);
            grilla = TextureRegion.split(sheet, frameAncho, frameAlto);
        } catch (Exception e) {
            System.out.println("No se pudo cargar sprite sheet: personajes/heroe/sprites-personaje/" + ruta + ". Usando fallback.");
        }
        
        Array<TextureRegion> frames = new Array<>();
        if (grilla != null && fila < grilla.length && cols <= grilla[fila].length) {
            for (int c = 0; c < cols; c++) {
                frames.add(grilla[fila][c]);
            }
        }
        
        if (frames.size == 0) {
            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(com.badlogic.gdx.graphics.Color.CLEAR);
            pixmap.fill();
            Texture tex = new Texture(pixmap);
            texturasCargadas.add(tex);
            frames.add(new TextureRegion(tex));
            pixmap.dispose();
        }
        return new Animation<>(duracion, frames, Animation.PlayMode.NORMAL);
    }

    private Animation<TextureRegion> crearAnimacion(float duracionFrame, String... rutasRelativas) {
        Array<TextureRegion> frames = new Array<>();
        for (String ruta : rutasRelativas) {
            try {
                Texture tex = new Texture("personajes/heroe/sprites-personaje/" + ruta);
                texturasCargadas.add(tex);
                frames.add(new TextureRegion(tex));
            } catch (Exception e) {
                System.out.println("No se pudo cargar: personajes/heroe/sprites-personaje/" + ruta + ". Saltando o usando fallback.");
            }
        }
        if (frames.size == 0) {
            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(com.badlogic.gdx.graphics.Color.CLEAR);
            pixmap.fill();
            Texture tex = new Texture(pixmap);
            texturasCargadas.add(tex);
            frames.add(new TextureRegion(tex));
            pixmap.dispose();
        }
        return new Animation<>(duracionFrame, frames, Animation.PlayMode.LOOP);
    }

    /** Crea una animación en modo ping-pong (ida y vuelta): 1→2→3→4→3→2→1...) */
    private Animation<TextureRegion> crearAnimacionPingPong(float duracionFrame, String... rutasRelativas) {
        Array<TextureRegion> frames = new Array<>();
        for (String ruta : rutasRelativas) {
            try {
                Texture tex = new Texture("personajes/heroe/sprites-personaje/" + ruta);
                texturasCargadas.add(tex);
                frames.add(new TextureRegion(tex));
            } catch (Exception e) {
                System.out.println("No se pudo cargar: personajes/heroe/sprites-personaje/" + ruta);
            }
        }
        if (frames.size == 0) {
            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(com.badlogic.gdx.graphics.Color.CLEAR);
            pixmap.fill();
            Texture tex = new Texture(pixmap);
            texturasCargadas.add(tex);
            frames.add(new TextureRegion(tex));
            pixmap.dispose();
        }
        return new Animation<>(duracionFrame, frames, Animation.PlayMode.LOOP_PINGPONG);
    }

    public void dispose() {
        for (Texture tex : texturasCargadas) {
            tex.dispose();
        }
    }
}
