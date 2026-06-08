package com.example.bmu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;

import com.example.bmu.fisicas.*;
import com.example.bmu.modelos.*;
import com.example.bmu.ui.ControlesTouch;
import com.example.bmu.mundo.GestorEscenarios;
import com.example.bmu.vista.AnimadorHeroe;
import com.example.bmu.vista.AnimadorEnemigoDebil;

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
    private GestorEscenarios   gestorEscenarios;
    private AnimadorHeroe      animadorHeroe;
    private AnimadorEnemigoDebil animadorEnemigoDebil;

    private float   stateTime           = 0f;
    private boolean mirandoDerecha      = true;
    private String  estadoAnimAnterior  = "idle";
    private boolean agarrarAnteriorPresionado = false;
    private boolean lanzarAnteriorPresionado  = false;
    private boolean isAtacando          = false;
    private float   tiempoAtacando      = 0f;

    // Estados para EnemigoDebil
    private float   stateTimeDebil      = 0f;
    private float   cooldownAtaqueDebil = 0f;
    private float   tiempoAtacandoDebil = 0f;
    private boolean isAtacandoDebil     = false;
    private float   tiempoEnSueloMuerto = 0f;

    // ── Tamaño del viewport en metros (zoom) ─────────────────────────────────
    // Reducir estos valores hace zoom in (personajes más grandes en pantalla).
    // 8 × 4.5 = relación 16:9 con el doble de zoom respecto a 16 × 9.
    private static final float VP_ANCHO = 8f;
    private static final float VP_ALTO  = 4.5f;

    // HUD
    private Texture texturaHUDFondo;
    private Texture texturaHUDBarra;


    @Override
    public void show() {
        // 1. Mundo físico
        mundo   = new MundoFisico();
        fabrica = new FabricaCuerpos(mundo.getWorld());

        // 2. Listener de colisiones
        escucha = new EscuchaColisiones();
        mundo.getWorld().setContactListener(escucha);

        // 3. Modelos
        Jugador      jugador = new Jugador(100, 20);
        EnemigoDebil enD     = new EnemigoDebil();
        EnemigoFuerte enF    = new EnemigoFuerte();

        // 4. Cuerpos Box2D
        Body cJugador = fabrica.crearCuerpoJugador(200, 304, 100, 160, jugador);
        Body cDebil   = fabrica.crearCuerpoEnemigo(500, 304, 100, 160, true,  enD);
        Body cFuerte  = fabrica.crearCuerpoEnemigo(750, 304, 120, 180, false, enF);

        // 5. Entidades físicas
        entJugador       = new EntidadFisica(cJugador, jugador);
        entEnemigoDebil  = new EntidadFisica(cDebil,   enD);
        entEnemigoFuerte = new EntidadFisica(cFuerte,  enF);

        // 6. Arma
        armaFisica = new TuboMetal();
        jugador.equiparArma(armaFisica);
        cuerpoArma = fabrica.crearCuerpoArma(200, 80, 30, 10, armaFisica);

        // 7. Sistema de agarre
        sistemaAgarre = new SistemaAgarre(jugador, cJugador);

        // ── Cámara con zoom ×2 (VP_ANCHO × VP_ALTO metros) ──────────────────
        camara = new OrthographicCamera();
        camara.setToOrtho(false,
                Gdx.graphics.getWidth()  / MundoFisico.PPM,
                Gdx.graphics.getHeight() / MundoFisico.PPM);
        camara.update();

        // 8. Controles
        controles = new ControlesTouch();
        Gdx.input.setInputProcessor(controles);

        // 9. Render
        debugRenderer       = new Box2DDebugRenderer();
        shapeRenderer        = new ShapeRenderer();
        batch                = new SpriteBatch();
        texturaTubo          = new Texture("armas/tubo.png");
        texturaCuchillo      = new Texture("armas/cuchillo.png");
        gestorEscenarios     = new GestorEscenarios();
        animadorHeroe        = new AnimadorHeroe();
        animadorEnemigoDebil = new AnimadorEnemigoDebil(1);

        // HUD
        texturaHUDFondo = new Texture("HUD/salud/salud_fondo.png");
        texturaHUDBarra = new Texture("HUD/salud/salud_barra.png");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stateTime += delta;
        manejarEntradaTactil();
        actualizarEnemigos(delta);
        mundo.actualizar(delta);
        escucha.procesarEventosPendientes();

        // ── Arrastrar enemigo agarrado ───────────────────────────────────────
        if (sistemaAgarre.tienEnemigoAgarrado()) {
            com.badlogic.gdx.math.Vector2 posJ = entJugador.getCuerpo().getPosition();
            float offsetX = mirandoDerecha ? 0.8f : -0.8f;
            Body cAgarrado = sistemaAgarre.getCuerpoAgarrado();
            if (cAgarrado != null) {
                cAgarrado.setTransform(posJ.x + offsetX, posJ.y, 0f);
                cAgarrado.setLinearVelocity(0, 0);
            }
        }

        // ── Cámara: seguir al jugador con clamp para no salir del escenario ──
        float px = entJugador.getCuerpo().getPosition().x;
        float mitadAncho = (Gdx.graphics.getWidth() / MundoFisico.PPM) / 2f;
        float escenarioAnchoM = 20f;
        camara.position.x = MathUtils.clamp(px, mitadAncho, escenarioAnchoM - mitadAncho);
        // Y fija: centrada en el suelo
        camara.position.y = (Gdx.graphics.getHeight() / MundoFisico.PPM) / 2f;
        camara.update();

        // ── Sprites ──────────────────────────────────────────────────────────
        batch.setProjectionMatrix(camara.combined);
        batch.begin();

        // Escenario de fondo
        float anchoPantallaM = Gdx.graphics.getWidth()  / MundoFisico.PPM;
        float altoPantallaM  = Gdx.graphics.getHeight() / MundoFisico.PPM;
        gestorEscenarios.dibujar(batch, anchoPantallaM, altoPantallaM);

        // ── Animación del héroe ──────────────────────────────────────────────
        float velX = entJugador.getCuerpo().getLinearVelocity().x;
        float velY = entJugador.getCuerpo().getLinearVelocity().y;

        final float DURACION_GOLPE = 8 * 0.07f;
        if (isAtacando) {
            tiempoAtacando += delta;
            if (tiempoAtacando >= DURACION_GOLPE) {
                isAtacando     = false;
                tiempoAtacando = 0f;
            }
        }

        String estadoAnim;
        if (isAtacando) {
            estadoAnim = "punch";
        } else if (velY > 0.5f && animadorHeroe.animJump != null) {
            estadoAnim = "jump";
        } else if (velY < -0.5f && animadorHeroe.animFall != null) {
            estadoAnim = "fall";
        } else if (Math.abs(velX) > 3.5f) {
            estadoAnim = "run";
        } else if (Math.abs(velX) > 0.3f) {
            estadoAnim = "walk";
        } else {
            estadoAnim = "idle";
        }

        if (!estadoAnim.equals(estadoAnimAnterior)) {
            stateTime          = 0f;
            estadoAnimAnterior = estadoAnim;
        }

        TextureRegion frameActual;
        switch (estadoAnim) {
            case "punch":    frameActual = animadorHeroe.animPunch.getKeyFrame(stateTime, false);    break;
            case "jump":     frameActual = animadorHeroe.animJump.getKeyFrame(stateTime, false);     break;
            case "fall":     frameActual = animadorHeroe.animFall.getKeyFrame(stateTime, true);      break;
            case "run":      frameActual = animadorHeroe.animRun.getKeyFrame(stateTime, true);       break;
            case "walk":     frameActual = animadorHeroe.animWalk.getKeyFrame(stateTime, true);      break;
            default:         frameActual = animadorHeroe.animIdle.getKeyFrame(stateTime, true);      break;
        }

        if (!isAtacando) {
            if (controles.getDirX() > 0) mirandoDerecha = true;
            if (controles.getDirX() < 0) mirandoDerecha = false;
        }

        // Escala héroe: misma lógica de antes
        float alturaVisualDeseadaM = 4.0f;          // metros en pantalla
        float altoCanvasFijoPx     = 282f;           // del script
        float escalaPixelAMetro    = alturaVisualDeseadaM / altoCanvasFijoPx;
        float altoCanvasM          = 282f * escalaPixelAMetro;  // = alturaVisualDeseadaM siempre
        float anchoCanvasM         = 243f * escalaPixelAMetro;  // proporcional 

        float posX = entJugador.getCuerpo().getPosition().x;
        float posY = entJugador.getCuerpo().getPosition().y;
        float altoCuerpoFisicoM = 160f / MundoFisico.PPM;
        float piesFisicosY = posY - (altoCuerpoFisicoM / 2f);
        float dibY = piesFisicosY;
        float dibX = posX - (anchoCanvasM / 2f);

        TextureRegion drawFrame = new TextureRegion(frameActual);
        if (!mirandoDerecha) drawFrame.flip(true, false);
        batch.draw(drawFrame, dibX, dibY, anchoCanvasM, altoCanvasM);

        // Enemigo débil
        dibujarEnemigoDebil(batch, entEnemigoDebil, animadorEnemigoDebil, stateTimeDebil, isAtacandoDebil);

        // Arma
        float armaX    = cuerpoArma.getPosition().x;
        float armaY    = cuerpoArma.getPosition().y;
        float anguloA  = cuerpoArma.getAngle() * MathUtils.radiansToDegrees;
        float anchoArma = 30f / MundoFisico.PPM;
        float altoArma  = 10f / MundoFisico.PPM;
        batch.draw(texturaTubo,
                armaX - anchoArma / 2f, armaY - altoArma / 2f,
                anchoArma / 2f, altoArma / 2f,
                anchoArma, altoArma,
                1f, 1f, anguloA,
                0, 0, texturaTubo.getWidth(), texturaTubo.getHeight(),
                false, false);

        // HUD salud
        dibujarHUD(batch);

        batch.end();

        // ── HUD táctil (coordenadas de pantalla en píxeles) ──────────────────
        shapeRenderer.setProjectionMatrix(
                new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()));
        controles.dibujar(shapeRenderer);

        // ── Debug Box2D ──────────────────────────────────────────────────────
        camara.update();
        debugRenderer.render(mundo.getWorld(), camara.combined);
    }

    // Dibujar HUD

    private void dibujarHUD(SpriteBatch batch) {
        Jugador jug   = (Jugador) entJugador.getModelo();
        float pct     = (float) jug.getVidaActual() / jug.getVidaMaxima();

        float hudAncho = 4.5f;
        float hudAlto  = hudAncho * ((float) texturaHUDFondo.getHeight() / texturaHUDFondo.getWidth());
        float hudX = camara.position.x - (Gdx.graphics.getWidth() / MundoFisico.PPM) / 2f;
        float hudY = camara.position.y + (Gdx.graphics.getHeight() / MundoFisico.PPM) / 2f - hudAlto;

        // Posición y tamaño de la zona de la barra dentro del HUD
        float barraX     = hudX   + hudAncho * 0.42f;
        float barraY     = hudY   + hudAlto  * 0.45f;
        float barraAncho = hudAncho * 0.47f; // ancho total de la barra al 100%
        float barraAlto  = hudAlto  * 0.16f;

        // 1. Dibujar la barra roja escalada según vida (usando TextureRegion para recortar)
        TextureRegion regionBarra = new TextureRegion(texturaHUDBarra,
                0, 0,
                (int)(texturaHUDBarra.getWidth() * pct), // recortar en píxeles según pct
                texturaHUDBarra.getHeight());
        batch.draw(regionBarra, barraX, barraY, barraAncho * pct, barraAlto);

        // 2. Dibujar el marco encima (tapa los bordes de la barra)
        batch.draw(texturaHUDFondo, hudX, hudY, hudAncho, hudAlto);
    }

    // ── Entrada táctil ───────────────────────────────────────────────────────

    private void manejarEntradaTactil() {
        float dirX = controles.getDirX();

        float xJugadorMetros       = entJugador.getCuerpo().getPosition().x;
        float limiteIzquierdoMetros = 0.8f;
        if (dirX < 0 && xJugadorMetros <= limiteIzquierdoMetros) {
            dirX = 0;
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);
        }

        if (Math.abs(dirX) > 0.001f) {
            float vel    = controles.isCorriendoRapido() ? 6f : 3f;
            float signo  = dirX > 0 ? 1f : -1f;
            entJugador.getCuerpo().setLinearVelocity(signo * vel, entJugador.getCuerpo().getLinearVelocity().y);
        } else {
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);
        }

        if (controles.saltarPresionado) entJugador.saltar();

        if (controles.golpePresionado && !isAtacando) {
            isAtacando     = true;
            tiempoAtacando = 0f;
            stateTime      = 0f;
            float vx = entJugador.getCuerpo().getLinearVelocity().x;

            Jugador jug      = (Jugador) entJugador.getModelo();
            Enemigo objetivo = enemigoMasCercano();
            if (objetivo != null) {
                boolean estabaVivo = objetivo.estaVivo();
                jug.atacar(objetivo);
                if (estabaVivo && !objetivo.estaVivo()) {
                    float dirEmpuje = mirandoDerecha ? 5f : -5f;
                    Body cEnemigo = null;
                    if (entEnemigoDebil  != null && objetivo == entEnemigoDebil.getModelo())  cEnemigo = entEnemigoDebil.getCuerpo();
                    else if (entEnemigoFuerte != null && objetivo == entEnemigoFuerte.getModelo()) cEnemigo = entEnemigoFuerte.getCuerpo();
                    if (cEnemigo != null) cEnemigo.setLinearVelocity(dirEmpuje, 4f);
                }
            }
        }

        boolean agarrarAhora = controles.agarrarPresionado;
        if (agarrarAhora && !agarrarAnteriorPresionado) {
            if (sistemaAgarre.tienEnemigoAgarrado()) {
                sistemaAgarre.soltarAgarre();
            } else {
                float RANGO_AGARRE = 2.0f;
                EntidadFisica masCercano = null;
                float minDist = Float.MAX_VALUE;
                if (entEnemigoDebil != null && entEnemigoDebil.getModelo().estaVivo()) {
                    float d = Math.abs(xJugadorMetros - entEnemigoDebil.getCuerpo().getPosition().x);
                    if (d < minDist) { minDist = d; masCercano = entEnemigoDebil; }
                }
                if (entEnemigoFuerte.getModelo().estaVivo()) {
                    float d = Math.abs(xJugadorMetros - entEnemigoFuerte.getCuerpo().getPosition().x);
                    if (d < minDist) { minDist = d; masCercano = entEnemigoFuerte; }
                }
                if (masCercano != null && minDist <= RANGO_AGARRE)
                    sistemaAgarre.jugadorIntentaAgarrar((Enemigo) masCercano.getModelo(), masCercano.getCuerpo());
            }
        }
        agarrarAnteriorPresionado = agarrarAhora;

        boolean lanzarAhora = controles.lanzarPresionado;
        if (lanzarAhora && !lanzarAnteriorPresionado) {
            int dir = controles.moviendoIzquierda() ? -1 : 1;
            if (sistemaAgarre.tienEnemigoAgarrado()) sistemaAgarre.lanzarEnemigo(dir);
            else sistemaAgarre.lanzarArma(cuerpoArma, dir, 15f);
        }
        lanzarAnteriorPresionado = lanzarAhora;
    }

    private Enemigo enemigoMasCercano() {
        float xJ      = entJugador.getCuerpo().getPosition().x;
        float minDist  = Float.MAX_VALUE;
        Enemigo mejor  = null;
        if (entEnemigoDebil != null && entEnemigoDebil.getModelo().estaVivo()) {
            float d = Math.abs(xJ - entEnemigoDebil.getCuerpo().getPosition().x);
            if (d < minDist) { minDist = d; mejor = (Enemigo) entEnemigoDebil.getModelo(); }
        }
        if (entEnemigoFuerte.getModelo().estaVivo()) {
            float d = Math.abs(xJ - entEnemigoFuerte.getCuerpo().getPosition().x);
            if (d < minDist) { minDist = d; mejor = (Enemigo) entEnemigoFuerte.getModelo(); }
        }
        return mejor;
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @Override public void resize(int w, int h) {
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
        gestorEscenarios.dispose();
        animadorHeroe.dispose();
        texturaHUDFondo.dispose();
        texturaHUDBarra.dispose();
        if (animadorEnemigoDebil != null) animadorEnemigoDebil.dispose();
    }

    // ── IA enemigos ──────────────────────────────────────────────────────────

    private void actualizarEnemigos(float delta) {
        entJugador.getModelo().actualizar(delta);

        if (entEnemigoDebil != null) {
            entEnemigoDebil.getModelo().actualizar(delta);
            actualizarIA(delta, entEnemigoDebil);

            if (!entEnemigoDebil.getModelo().estaVivo()) {
                tiempoEnSueloMuerto += delta;
                if (tiempoEnSueloMuerto >= 3.0f) {
                    if (sistemaAgarre.getEnemigoAgarrado() == entEnemigoDebil.getModelo())
                        sistemaAgarre.soltarAgarre();
                    mundo.getWorld().destroyBody(entEnemigoDebil.getCuerpo());
                    entEnemigoDebil = null;
                }
            }
        }
    }

    private void actualizarIA(float delta, EntidadFisica enemigo) {
        if (!enemigo.getModelo().estaVivo()) {
            if (enemigo.estaEnSuelo()) enemigo.detener();
            return;
        }

        if (sistemaAgarre.tienEnemigoAgarrado() && sistemaAgarre.getEnemigoAgarrado() == enemigo.getModelo()) return;

        stateTimeDebil += delta;
        if (cooldownAtaqueDebil > 0) cooldownAtaqueDebil -= delta;
        if (isAtacandoDebil) {
            tiempoAtacandoDebil += delta;
            if (tiempoAtacandoDebil >= 0.3f) { isAtacandoDebil = false; tiempoAtacandoDebil = 0f; }
        }

        if (enemigo.getModelo().tiempoHurt > 0 || isAtacandoDebil) {
            enemigo.detener();
            return;
        }

        float xJ = entJugador.getCuerpo().getPosition().x;
        float xE = enemigo.getCuerpo().getPosition().x;
        float dx = xJ - xE;

        if (Math.abs(dx) > 1.2f) {
            enemigo.mover(Math.signum(dx) * 0.5f);
        } else {
            enemigo.detener();
            if (entJugador.getModelo().estaVivo() && cooldownAtaqueDebil <= 0) {
                isAtacandoDebil     = true;
                tiempoAtacandoDebil = 0f;
                stateTimeDebil      = 0f;
                cooldownAtaqueDebil = 1.5f;
                enemigo.getModelo().atacar(entJugador.getModelo());
            }
        }
    }

    private void dibujarEnemigoDebil(SpriteBatch batch, EntidadFisica enemigo,
                                     AnimadorEnemigoDebil animador,
                                     float stateTime, boolean isAtacando) {
        if (enemigo == null) return;

        if (!enemigo.getModelo().estaVivo() && tiempoEnSueloMuerto > 1.5f) {
            boolean mostrar = ((int)((tiempoEnSueloMuerto - 1.5f) * 15)) % 2 == 0;
            if (!mostrar) return;
        }

        float enX = enemigo.getCuerpo().getPosition().x;
        float enY = enemigo.getCuerpo().getPosition().y;

        TextureRegion frameActual;
        TextureRegion primerFrame;

        if (!enemigo.getModelo().estaVivo()) {
            if (tiempoEnSueloMuerto < 1.0f && !enemigo.estaEnSuelo()) {
                frameActual = animador.animFall.getKeyFrame(stateTime, false);
                primerFrame  = animador.animFall.getKeyFrame(0f);
            } else {
                frameActual = animador.animDead.getKeyFrame(stateTime, false);
                primerFrame  = animador.animDead.getKeyFrame(0f);
            }
        } else if (enemigo.getModelo().tiempoHurt > 0) {
            frameActual = animador.animHurt.getKeyFrame(enemigo.getModelo().tiempoHurt, false);
            primerFrame  = animador.animHurt.getKeyFrame(0f);
        } else if (isAtacando) {
            frameActual = animador.animPunch.getKeyFrame(stateTime, false);
            primerFrame  = animador.animPunch.getKeyFrame(0f);
        } else if (Math.abs(enemigo.getCuerpo().getLinearVelocity().x) > 0.3f) {
            frameActual = animador.animWalk.getKeyFrame(stateTime, true);
            primerFrame  = animador.animWalk.getKeyFrame(0f);
        } else {
            frameActual = animador.animIdle.getKeyFrame(stateTime, true);
            primerFrame  = animador.animIdle.getKeyFrame(0f);
        }

        float alturaVisualDeseadaM = 4.0f;
        float altoCanvasFijoPx     = 282f;
        float escalaPixelAMetro    = alturaVisualDeseadaM / altoCanvasFijoPx;
        float altoCanvasM          = 282f * escalaPixelAMetro;
        float anchoCanvasM         = 243f * escalaPixelAMetro;

        float altoCuerpoFisicoM = 160f / MundoFisico.PPM;
        float dibX = enX - (anchoCanvasM / 2f);
        float dibY = enY - (altoCuerpoFisicoM / 2f);

        boolean mirandoDerechaEnemigo;
        if (Math.abs(enemigo.getCuerpo().getLinearVelocity().x) > 0.1f)
            mirandoDerechaEnemigo = enemigo.getCuerpo().getLinearVelocity().x > 0;
        else
            mirandoDerechaEnemigo = entJugador.getCuerpo().getPosition().x > enX;

        TextureRegion drawFrame = new TextureRegion(frameActual);
        if (!mirandoDerechaEnemigo) drawFrame.flip(true, false);

        batch.draw(drawFrame, dibX, dibY, anchoCanvasM, altoCanvasM);
    }
}