package com.example.bmu.vista;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class AnimadorJefe3 {
    public Animation<TextureRegion> animIdle;
    public Animation<TextureRegion> animWalk;
    public Animation<TextureRegion> animPunch;
    public Animation<TextureRegion> animHurt;
    public Animation<TextureRegion> animFall;
    public Animation<TextureRegion> animDead;

    private Array<Texture> texturasCargadas;
    private static final String FOLDER = "personajes/jefes/jefe3/";

    public AnimadorJefe3() {
        texturasCargadas = new Array<>();

        animWalk = crearAnimacion(0.2f, Animation.PlayMode.LOOP, "caminando/camina1.png", "caminando/camina2.png");
        animIdle = animWalk; // No hay animación de quieto
        
        animPunch = crearAnimacion(0.15f, Animation.PlayMode.NORMAL, "golpeando/golpea1.png");
        animHurt = crearAnimacion(0.15f, Animation.PlayMode.NORMAL, "recibeDano/dano1.png");
        animFall = crearAnimacion(0.15f, Animation.PlayMode.NORMAL, "recibeDano/dano1.png"); // fallback
        animDead = crearAnimacion(0.15f, Animation.PlayMode.NORMAL, "muerto/muerto1.png");
    }

    private Animation<TextureRegion> crearAnimacion(float duracionFrame, Animation.PlayMode playMode, String... rutasRelativas) {
        Array<TextureRegion> frames = new Array<>();
        for (String ruta : rutasRelativas) {
            try {
                Texture tex = new Texture(FOLDER + ruta);
                texturasCargadas.add(tex);
                frames.add(new TextureRegion(tex));
            } catch (Exception e) {
                System.out.println("AnimadorJefe3: No se pudo cargar: " + FOLDER + ruta);
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
        return new Animation<>(duracionFrame, frames, playMode);
    }

    public void dispose() {
        for (Texture tex : texturasCargadas) {
            tex.dispose();
        }
    }
}
