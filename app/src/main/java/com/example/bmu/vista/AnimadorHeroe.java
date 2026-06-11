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
    public Animation<TextureRegion> animKick;
    public Animation<TextureRegion> animJump;
    public Animation<TextureRegion> animHurt;
    public Animation<TextureRegion> animFall;
    public Animation<TextureRegion> animGrab;
    public Animation<TextureRegion> animGrabPunch;
    public Animation<TextureRegion> animThrow;
    public Animation<TextureRegion> animDeathFall;
    public Animation<TextureRegion> animDead;

    // Custom weapon animations
    public Animation<TextureRegion> animWalkKnife;
    public Animation<TextureRegion> animWalkTubo;
    public Animation<TextureRegion> animRunKnife;
    public Animation<TextureRegion> animRunTubo;
    public Animation<TextureRegion> animJumpKnife;
    public Animation<TextureRegion> animJumpTubo;
    public Animation<TextureRegion> animIdleKnife;
    public Animation<TextureRegion> animIdleTubo;
    public Animation<TextureRegion> animPunchKnife;
    public Animation<TextureRegion> animPunchTubo;

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
        
        // ── Golpeando: frames individuales ──────────────────────────────────
        animPunch = crearAnimacion(0.07f, Animation.PlayMode.NORMAL,
                "golpeando/punch1.png",
                "golpeando/punch2.png",
                "golpeando/punch3.png",
                "golpeando/punch4.png",
                "golpeando/punch5.png",
                "golpeando/punch6.png",
                "golpeando/punch7.png"
        );

        animJump = crearAnimacion(0.12f, Animation.PlayMode.NORMAL,
                "saltando/saltando1.png",
                "saltando/saltando2.png"
        );
        
        animFall = crearAnimacion(0.1f, Animation.PlayMode.NORMAL,
                "cayendo/cayendo1.png",
                "cayendo/cayendo2.png"
        );

        // Agregamos la animación de recibir daño con PlayMode.NORMAL
        animHurt = crearAnimacion(0.2f, Animation.PlayMode.NORMAL,
                "recibeDano/dano1.png"
        );

        // Agregamos las animaciones de agarre y lanzamiento
        animGrab = crearAnimacion(0.2f, Animation.PlayMode.NORMAL,
                "agarre/Agarre.png"
        );
        animGrabPunch = crearAnimacion(0.2f, Animation.PlayMode.NORMAL,
                "agarre/AgarreGolpe.png"
        );
        animThrow = crearAnimacion(0.2f, Animation.PlayMode.NORMAL,
                "Lanzar/Lanzar.png"
        );
        animDeathFall = crearAnimacion(0.15f, Animation.PlayMode.NORMAL,
                "Caida/Caida.png"
        );
        animDead = crearAnimacion(0.15f, Animation.PlayMode.NORMAL,
                "muerto/Muerto.png"
        );

        // Load new weapon animations
        animWalkKnife = crearAnimacion(0.15f, "caminando/WalkKnife1.png", "caminando/WalkKnife2.png");
        animWalkTubo  = crearAnimacion(0.15f, "caminando/WalkTubo1.png", "caminando/WalkTubo2.png");
        animRunKnife  = crearAnimacion(0.12f, "corriendo/RunKnife1.png", "corriendo/RunKnife2.png", "corriendo/RunKnife3.png");
        animRunTubo   = crearAnimacion(0.12f, "corriendo/RunTubo1.png", "corriendo/RunTubo2.png", "corriendo/RunTubo3.png");
        animJumpKnife = crearAnimacion(0.15f, Animation.PlayMode.NORMAL, "saltando/SaltandoKnife.png");
        animJumpTubo  = crearAnimacion(0.15f, Animation.PlayMode.NORMAL, "saltando/SaltandoTubo.png");
        animIdleKnife  = crearAnimacion(0.15f, "quieto/QuietoKnife.png");
        animIdleTubo   = crearAnimacion(0.15f, "quieto/QuietoTubo.png");
        animPunchKnife = crearAnimacion(0.07f, Animation.PlayMode.NORMAL, "golpeando/GolpeKnife.png");
        animPunchTubo  = crearAnimacion(0.07f, Animation.PlayMode.NORMAL, "golpeando/GolpeTubo.png");

        // Pendientes por agregar:
        // animKick = ...
    }

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
        return crearAnimacion(duracionFrame, Animation.PlayMode.LOOP, rutasRelativas);
    }

    private Animation<TextureRegion> crearAnimacion(float duracionFrame, Animation.PlayMode playMode, String... rutasRelativas) {
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
        return new Animation<>(duracionFrame, frames, playMode);
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
