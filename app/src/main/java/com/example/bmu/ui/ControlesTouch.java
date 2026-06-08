package com.example.bmu.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;

/**
 * Controles táctiles que funcionan correctamente con FitViewport.
 *
 * CLAVE: todas las posiciones de botones y joystick están en coordenadas
 * de PANTALLA EN PÍXELES (igual que antes). La única diferencia es que
 * ahora convertimos las coordenadas táctiles a través del viewport antes
 * de compararlas, para que las barras negras del letterbox no las desvíen.
 *
 * Uso desde PantallaJuego:
 *   controles = new ControlesTouch(viewport);
 */
public class ControlesTouch extends InputAdapter {

    public boolean saltarPresionado;
    public boolean golpePresionado;
    public boolean agarrarPresionado;
    public boolean lanzarPresionado;

    // ── Joystick ─────────────────────────────────────────────────────────────
    private final float joystickX     = 150f;
    private final float joystickY     = 150f;
    private final float joystickRadio = 100f;
    private float dirX  = 0f;   // -1.0 a 1.0
    private float fuerza = 0f;  // 0.0 a 1.0
    private int   punteroJoystick = -1;

    // ── Botones ───────────────────────────────────────────────────────────────
    // Se calculan en recalcularBotones() con el ancho real de pantalla
    private float btnGolpeX, btnGolpeY;
    private float btnSaltoX, btnSaltoY;
    private float btnAgarreX, btnAgarreY;
    private float btnLanzarX, btnLanzarY;
    private final float btnRadio = 60f;

    public ControlesTouch() {
        recalcularBotones();
    }

    /**
     * Recalcula las posiciones de los botones usando el ancho/alto real de pantalla.
     * Llamar también desde resize() si el tamaño cambia.
     */
    public void recalcularBotones() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        btnGolpeX  = w - 300f;  btnGolpeY  = h * 0.33f;
        btnSaltoX  = w - 225f;  btnSaltoY  = h * 0.13f;
        btnAgarreX = w - 225f;  btnAgarreY = h * 0.55f;
        btnLanzarX = w - 150f;  btnLanzarY = h * 0.33f;
    }

    public void actualizar() { /* estados manejados en touchDown/Up */ }

    // ── Dibujo del HUD (coordenadas de pantalla en píxeles) ──────────────────

    public void dibujar(ShapeRenderer renderer) {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        renderer.begin(ShapeRenderer.ShapeType.Filled);

        // Base del joystick
        renderer.setColor(1, 1, 1, 0.3f);
        renderer.circle(joystickX, joystickY, joystickRadio);
        // Palanca
        renderer.setColor(1, 1, 1, 0.6f);
        renderer.circle(joystickX + (dirX * joystickRadio), joystickY, joystickRadio / 2f);

        // Botones
        renderer.setColor(1, 0, 0, golpePresionado   ? 0.8f : 0.3f);
        renderer.circle(btnGolpeX,  btnGolpeY,  btnRadio);

        renderer.setColor(0, 1, 0, saltarPresionado  ? 0.8f : 0.3f);
        renderer.circle(btnSaltoX,  btnSaltoY,  btnRadio);

        renderer.setColor(0, 0, 1, agarrarPresionado ? 0.8f : 0.3f);
        renderer.circle(btnAgarreX, btnAgarreY, btnRadio);

        renderer.setColor(1, 1, 0, lanzarPresionado  ? 0.8f : 0.3f);
        renderer.circle(btnLanzarX, btnLanzarY, btnRadio);

        renderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }

    // ── Eventos táctiles ──────────────────────────────────────────────────────

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float tx = screenX;
        float ty = Gdx.graphics.getHeight() - screenY; // LibGDX: Y invertida

        if (Math.hypot(tx - joystickX, ty - joystickY) < joystickRadio * 1.5f) {
            punteroJoystick = pointer;
            calcularJoystick(tx, ty);
        }
        chequearBotonesBajo(tx, ty);
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        float tx = screenX;
        float ty = Gdx.graphics.getHeight() - screenY;
        if (pointer == punteroJoystick) calcularJoystick(tx, ty);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        float tx = screenX;
        float ty = Gdx.graphics.getHeight() - screenY;

        if (pointer == punteroJoystick) {
            punteroJoystick = -1;
            dirX   = 0f;
            fuerza = 0f;
        }
        chequearBotonesAlzado(tx, ty);
        return true;
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private void calcularJoystick(float tx, float ty) {
        float dx = tx - joystickX;
        if (Math.abs(dx) < joystickRadio * 0.15f) {
            dirX = 0f; fuerza = 0f;
            return;
        }
        dirX   = Math.max(-1f, Math.min(1f, dx / joystickRadio));
        fuerza = Math.abs(dirX);
    }

    private void chequearBotonesBajo(float tx, float ty) {
        if (Math.hypot(tx - btnGolpeX,  ty - btnGolpeY)  < btnRadio) golpePresionado   = true;
        if (Math.hypot(tx - btnSaltoX,  ty - btnSaltoY)  < btnRadio) saltarPresionado  = true;
        if (Math.hypot(tx - btnAgarreX, ty - btnAgarreY) < btnRadio) agarrarPresionado = true;
        if (Math.hypot(tx - btnLanzarX, ty - btnLanzarY) < btnRadio) lanzarPresionado  = true;
    }

    private void chequearBotonesAlzado(float tx, float ty) {
        // Radio x2.5 para que si el dedo resbala un poco igual se suelte el botón
        float r = btnRadio * 2.5f;
        if (Math.hypot(tx - btnGolpeX,  ty - btnGolpeY)  < r) golpePresionado   = false;
        if (Math.hypot(tx - btnSaltoX,  ty - btnSaltoY)  < r) saltarPresionado  = false;
        if (Math.hypot(tx - btnAgarreX, ty - btnAgarreY) < r) agarrarPresionado = false;
        if (Math.hypot(tx - btnLanzarX, ty - btnLanzarY) < r) lanzarPresionado  = false;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public float getDirX()           { return dirX; }
    public boolean isCorriendoRapido() { return fuerza >= 0.70f; }
    public boolean moviendoIzquierda() { return dirX < 0; }
}