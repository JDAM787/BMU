package com.example.bmu.vista;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class AnimadorEnemigoDebil {
    public Animation<TextureRegion> animIdle;
    public Animation<TextureRegion> animWalk;
    public Animation<TextureRegion> animRun;
    public Animation<TextureRegion> animPunch;
    public Animation<TextureRegion> animHurt;
    public Animation<TextureRegion> animFall;
    public Animation<TextureRegion> animDead;

    private Array<Texture> texturasCargadas;

    public AnimadorEnemigoDebil(int enemigoIndex) {
        texturasCargadas = new Array<>();

        // Usamos el índice de enemigo (1 o 2). Si el 2 no tiene todas las carpetas,
        // por ahora usamos las de EnemigoDebil1 como fallback para evitar crashes.
        String folder = "personajes/EnemigoDebil/EnemigoDebil" + enemigoIndex + "/";
        String fallbackFolder = "personajes/EnemigoDebil/EnemigoDebil1/";

        // Cargar idle
        animIdle = crearAnimacion(0.15f, folder, "quieto/quieto.png");

        // Cargar caminar (con fallback si no existe)
        animWalk = crearAnimacionConFallback(0.15f, folder, fallbackFolder,
                new String[]{"caminando/Caminando1.png", "caminando/Caminando2.png", "caminando/Caminando3.png"}, Animation.PlayMode.LOOP);

        // Cargar correr
        animRun = crearAnimacionConFallback(0.12f, folder, fallbackFolder,
                new String[]{"corriendo/Correr1.png", "corriendo/Correr2.png"}, Animation.PlayMode.LOOP);

        // Cargar ataque
        animPunch = crearAnimacionConFallback(0.15f, folder, fallbackFolder,
                new String[]{"golpeando/Golpe1.png", "golpeando/Golpe2.png"}, Animation.PlayMode.NORMAL);

        // Cargar daño
        animHurt = crearAnimacionConFallback(0.15f, folder, fallbackFolder,
                new String[]{"RecibeDano/RecibeDano1.png", "RecibeDano/RecibeDano2.png"}, Animation.PlayMode.NORMAL);

        // Cargar caida
        animFall = crearAnimacionConFallback(0.15f, folder, fallbackFolder,
                new String[]{"Caida/Caida.png"}, Animation.PlayMode.NORMAL);

        // Cargar muerto
        animDead = crearAnimacionConFallback(0.15f, folder, fallbackFolder,
                new String[]{"muerto/Muerte.png"}, Animation.PlayMode.NORMAL);
    }

    private Animation<TextureRegion> crearAnimacion(float duracionFrame, String folder, String... rutasRelativas) {
        Array<TextureRegion> frames = new Array<>();
        for (String ruta : rutasRelativas) {
            try {
                Texture tex = new Texture(folder + ruta);
                texturasCargadas.add(tex);
                frames.add(new TextureRegion(tex));
            } catch (Exception e) {
                System.out.println("No se pudo cargar: " + (folder + ruta));
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

    private Animation<TextureRegion> crearAnimacionConFallback(
            float duracionFrame, String folder, String fallbackFolder,
            String[] rutasRelativas, Animation.PlayMode playMode) {
        Array<TextureRegion> frames = new Array<>();
        for (String ruta : rutasRelativas) {
            Texture tex = null;
            try {
                tex = new Texture(folder + ruta);
            } catch (Exception e) {
                System.out.println("No se pudo cargar: " + (folder + ruta) + ". Probando fallback.");
                try {
                    tex = new Texture(fallbackFolder + ruta);
                } catch (Exception ex) {
                    System.out.println("Fallback fallido: " + (fallbackFolder + ruta));
                }
            }
            if (tex != null) {
                texturasCargadas.add(tex);
                frames.add(new TextureRegion(tex));
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
