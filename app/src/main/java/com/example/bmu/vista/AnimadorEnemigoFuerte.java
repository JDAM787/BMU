package com.example.bmu.vista;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

/**
 * Animador para el EnemigoFuerte.
 * Carga los sprites individuales desde assets/personajes/EnemigoFuerte/.
 *
 * Carpetas disponibles:
 *   quieto/quieto1.png
 *   caminando/caminando1.png, caminando2.png
 *   corriendo/corre1.png, corre2.png
 *   golpeando/golpea1.png, golpea2.png
 *   recibeDano/dano1.png
 *   muerto/muerto1.png
 *   caida/ (vacía, usamos recibeDano como fallback visual)
 */
public class AnimadorEnemigoFuerte {
    public Animation<TextureRegion> animIdle;
    public Animation<TextureRegion> animWalk;
    public Animation<TextureRegion> animRun;
    public Animation<TextureRegion> animPunch;
    public Animation<TextureRegion> animHurt;
    public Animation<TextureRegion> animFall;
    public Animation<TextureRegion> animDead;

    private Array<Texture> texturasCargadas;

    private static final String FOLDER = "personajes/EnemigoFuerte/";

    public AnimadorEnemigoFuerte() {
        texturasCargadas = new Array<>();

        // Quieto (1 frame)
        animIdle = crearAnimacion(0.15f, Animation.PlayMode.LOOP,
                "quieto/quieto1.png");

        // Caminando (2 frames)
        animWalk = crearAnimacion(0.2f, Animation.PlayMode.LOOP,
                "caminando/caminando1.png",
                "caminando/caminando2.png");

        // Corriendo (2 frames)
        animRun = crearAnimacion(0.12f, Animation.PlayMode.LOOP,
                "corriendo/corre1.png",
                "corriendo/corre2.png");

        // Golpeando (2 frames)
        animPunch = crearAnimacion(0.15f, Animation.PlayMode.NORMAL,
                "golpeando/golpea1.png",
                "golpeando/golpea2.png");

        // Recibir daño (1 frame)
        animHurt = crearAnimacion(0.15f, Animation.PlayMode.NORMAL,
                "recibeDano/dano1.png");

        // Caída — la carpeta está vacía, reutilizamos el sprite de daño
        animFall = crearAnimacion(0.15f, Animation.PlayMode.NORMAL,
                "recibeDano/dano1.png");

        // Muerto (1 frame)
        animDead = crearAnimacion(0.15f, Animation.PlayMode.NORMAL,
                "muerto/muerto1.png");
    }

    private Animation<TextureRegion> crearAnimacion(float duracionFrame,
                                                     Animation.PlayMode playMode,
                                                     String... rutasRelativas) {
        Array<TextureRegion> frames = new Array<>();
        for (String ruta : rutasRelativas) {
            try {
                Texture tex = new Texture(FOLDER + ruta);
                texturasCargadas.add(tex);
                frames.add(new TextureRegion(tex));
            } catch (Exception e) {
                System.out.println("AnimadorEnemigoFuerte: No se pudo cargar: " + FOLDER + ruta);
            }
        }
        // Fallback: 1×1 transparente para evitar crashes
        if (frames.size == 0) {
            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(
                    1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(com.badlogic.gdx.graphics.Color.CLEAR);
            pixmap.fill();
            Texture tex = new Texture(pixmap);
            texturasCargadas.add(tex);
            frames.add(new TextureRegion(tex));
            pixmap.dispose();
        }
        return new Animation<>(duracionFrame, frames, playMode);
    }

    public void dispose() {
        for (Texture tex : texturasCargadas) {
            tex.dispose();
        }
    }
}
