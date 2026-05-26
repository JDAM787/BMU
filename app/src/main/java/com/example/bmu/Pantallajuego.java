package com.example.bmu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;

import com.example.bmu.fisicas.*;
import com.example.bmu.modelos.*;
import com.example.bmu.ui.ControlesTouch;

/**
 * Cómo añadir esta pantalla desde tu Game principal:
 *   game.setScreen(new PantallaJuego());
 */
public class PantallaJuego implements Screen {

    // ── Físicas ──────────────────────────────────────────────────────────────
    private MundoFisico       mundo;
    private FabricaCuerpos    fabrica;
    private EscuchaColisiones escucha;
    private SistemaAgarre     sistemaAgarre;

    // ── Entidades ────────────────────────────────────────────────────────────
    private EntidadFisica entJugador;
    private EntidadFisica entEnemigoDebil;
    private EntidadFisica entEnemigoFuerte;

    private Body      cuerpoArma;
    private TuboMetal armaFisica;

    // ── UI táctil ────────────────────────────────────────────────────────────
    private ControlesTouch controles;

    // ── Render ───────────────────────────────────────────────────────────────
    private Box2DDebugRenderer debugRenderer;
    private ShapeRenderer      shapeRenderer;
    private OrthographicCamera camara;
    private SpriteBatch        batch;
    private Texture            texturaTubo;
    private Texture            texturaCuchillo;
    private Texture            texturaFondo;

    @Override
    public void show() {
        // 1. Mundo físico
        mundo   = new MundoFisico();
        fabrica = new FabricaCuerpos(mundo.getWorld());

        // 2. Listener de colisiones
        escucha = new EscuchaColisiones();
        mundo.getWorld().setContactListener(escucha);

        // 3. Modelos del juego
        Jugador      jugador  = new Jugador(100, 20);
        EnemigoDebil  enD     = new EnemigoDebil();
        EnemigoFuerte enF     = new EnemigoFuerte();

        // 4. Cuerpos Box2D (posiciones en píxeles, la fábrica convierte a metros)
        Body cJugador = fabrica.crearCuerpoJugador( 200, 80, 40, 60, jugador);
        Body cDebil   = fabrica.crearCuerpoEnemigo( 500, 80, 40, 60, true,  enD);
        Body cFuerte  = fabrica.crearCuerpoEnemigo( 750, 80, 50, 70, false, enF);

        // 5. Entidades físicas
        entJugador       = new EntidadFisica(cJugador, jugador);
        entEnemigoDebil  = new EntidadFisica(cDebil,   enD);
        entEnemigoFuerte = new EntidadFisica(cFuerte,  enF);

        // 6. Arma lanzable
        armaFisica = new TuboMetal();
        jugador.equiparArma(armaFisica);
        cuerpoArma = fabrica.crearCuerpoArma(200, 80, 30, 10, armaFisica);

        // 7. Sistema de agarre
        sistemaAgarre = new SistemaAgarre(jugador, cJugador);

        // 8. Controles táctiles → registrar como InputProcessor de LibGDX
        controles = new ControlesTouch();
        Gdx.input.setInputProcessor(controles);

        // 9. Render
        debugRenderer = new Box2DDebugRenderer();
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        texturaTubo = new Texture("tubo.png");
        texturaCuchillo = new Texture("cuchillo.png");
        texturaFondo = new Texture("escenario.png");
        camara = new OrthographicCamera();
        // Vista en metros para el debug renderer de Box2D
        camara.setToOrtho(false,
                Gdx.graphics.getWidth()  / MundoFisico.PPM,
                Gdx.graphics.getHeight() / MundoFisico.PPM);
    }

    @Override
    public void render(float delta) {
        // ── Limpiar pantalla ────────────────────────────────────────────────
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ── Leer controles táctiles ─────────────────────────────────────────
        controles.actualizar();
        manejarEntradaTactil();

        // ── Paso de física ──────────────────────────────────────────────────
        mundo.actualizar(delta);
        escucha.procesarEventosPendientes(); // siempre DESPUÉS de world.step

        // ── Arrastrar enemigo agarrado con el jugador ───────────────────────
        if (sistemaAgarre.tienEnemigoAgarrado()) {
            entEnemigoDebil.getCuerpo().setTransform(
                    entJugador.getCuerpo().getPosition().x + 0.5f,
                    entJugador.getCuerpo().getPosition().y,
                    0f
            );
        }

        // ── Debug render de Box2D ───────────────────────────────────────────
        camara.update();
        debugRenderer.render(mundo.getWorld(), camara.combined);

        // ── Renderizado de Sprites ──────────────────────────────────────────
        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        
        // Dibujar el escenario de fondo
        float anchoPantallaM = Gdx.graphics.getWidth() / MundoFisico.PPM;
        float altoPantallaM = Gdx.graphics.getHeight() / MundoFisico.PPM;
        batch.draw(texturaFondo, 0, 0, anchoPantallaM, altoPantallaM);
        
        // Dibujar el tubo de metal atado a la física
        // El cuerpo del arma mide 30x10 px (según FabricaCuerpos).
        float armaX = cuerpoArma.getPosition().x;
        float armaY = cuerpoArma.getPosition().y;
        float anguloArma = cuerpoArma.getAngle() * MathUtils.radiansToDegrees;
        
        float anchoArma = 30f / MundoFisico.PPM;
        float altoArma = 10f / MundoFisico.PPM;
        
        // Se dibuja centrando la textura en la posición física
        batch.draw(texturaTubo, 
                armaX - anchoArma / 2f, 
                armaY - altoArma / 2f, 
                anchoArma / 2f, altoArma / 2f, 
                anchoArma, altoArma, 
                1f, 1f, 
                anguloArma, 
                0, 0, texturaTubo.getWidth(), texturaTubo.getHeight(), 
                false, false);
                
        batch.end();

        // ── Dibujar HUD táctil ──────────────────────────────────────────────
        // ShapeRenderer usa coordenadas de pantalla (píxeles), no metros
        shapeRenderer.setProjectionMatrix(
                new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()
                )
        );
        controles.dibujar(shapeRenderer);
    }

    // ── Lógica de entrada táctil ─────────────────────────────────────────────

    private void manejarEntradaTactil() {
        // Movimiento horizontal con joystick
        entJugador.mover(controles.getDirX());

        // Salto
        if (controles.saltarPresionado) {
            entJugador.saltar();
        }

        // Golpe normal
        if (controles.golpePresionado) {
            // Atacar al enemigo más cercano (simplificado: siempre al débil primero)
            Jugador jug = (Jugador) entJugador.getModelo();
            Enemigo objetivo = enemigoMasCercano();
            if (objetivo != null) {
                jug.atacar(objetivo);
            }
        }

        // Agarrar
        if (controles.agarrarPresionado) {
            sistemaAgarre.jugadorIntentaAgarrar(
                    (Enemigo) entEnemigoDebil.getModelo(),
                    entEnemigoDebil.getCuerpo()
            );
        }

        // Lanzar: dirección = la que lleva el joystick, o derecha por defecto
        if (controles.lanzarPresionado) {
            int dir = controles.moviendoIzquierda() ? -1 : 1;

            if (sistemaAgarre.tienEnemigoAgarrado()) {
                sistemaAgarre.lanzarEnemigo(dir);
            } else {
                // Sin enemigo agarrado → lanza el arma
                sistemaAgarre.lanzarArma(cuerpoArma, dir, 15f);
            }
        }
    }

    /** Devuelve el enemigo vivo más cercano al jugador (para recibir golpes). */
    private Enemigo enemigoMasCercano() {
        float xJ  = entJugador.getCuerpo().getPosition().x;
        float xD  = entEnemigoDebil.getCuerpo().getPosition().x;
        float xF  = entEnemigoFuerte.getCuerpo().getPosition().x;

        boolean debilVivo  = entEnemigoDebil.getModelo().estaVivo();
        boolean fuerteVivo = entEnemigoFuerte.getModelo().estaVivo();

        if (!debilVivo && !fuerteVivo) return null;
        if (!debilVivo)  return (Enemigo) entEnemigoFuerte.getModelo();
        if (!fuerteVivo) return (Enemigo) entEnemigoDebil.getModelo();

        float distD = Math.abs(xJ - xD);
        float distF = Math.abs(xJ - xF);
        return distD <= distF
                ? (Enemigo) entEnemigoDebil.getModelo()
                : (Enemigo) entEnemigoFuerte.getModelo();
    }

    // ── Ciclo de vida de Screen ───────────────────────────────────────────────

    @Override
    public void resize(int w, int h) {
        camara.setToOrtho(false, w / MundoFisico.PPM, h / MundoFisico.PPM);
    }

    @Override public void pause()  { }
    @Override public void resume() { }
    @Override public void hide()   { }

    @Override
    public void dispose() {
        mundo.dispose();
        debugRenderer.dispose();
        shapeRenderer.dispose();
        batch.dispose();
        texturaTubo.dispose();
        texturaCuchillo.dispose();
        texturaFondo.dispose();
    }
}