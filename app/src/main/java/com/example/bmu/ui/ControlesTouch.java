package com.example.bmu.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class ControlesTouch extends InputAdapter {
    public boolean saltarPresionado;
    public boolean golpePresionado;
    public boolean agarrarPresionado;
    public boolean lanzarPresionado;

    // Posiciones y tamaños
    private float joystickX = 150, joystickY = 150, joystickRadio = 100;
    private float dirX = 0f; // -1.0 a 1.0 (valor ANALÓGICO, proporcional al desplazamiento del joystick)
    private float fuerza = 0f; // 0.0 a 1.0 (módulo del desplazamiento, sin importar dirección)
    
    // Botones
    private float btnGolpeX = Gdx.graphics.getWidth() - 300, btnGolpeY = 150, btnRadio = 60;
    private float btnSaltoX = Gdx.graphics.getWidth() - 150, btnSaltoY = 150;
    private float btnAgarreX = Gdx.graphics.getWidth() - 225, btnAgarreY = 250;
    private float btnLanzarX = Gdx.graphics.getWidth() - 225, btnLanzarY = 50;
    
    // Identificadores de los dedos
    private int punteroJoystick = -1;

    public void actualizar() {
        // Resetear botones pulsados en el frame anterior si es necesario.
        // Como estamos leyendo estados por punteros activos, lo hacemos en touchUp/Dragged.
    }

    public void dibujar(ShapeRenderer renderer) {
        // Habilitar transparencia
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Joystick base
        renderer.setColor(1, 1, 1, 0.3f);
        renderer.circle(joystickX, joystickY, joystickRadio);
        // Joystick palanca (la bolita se mueve proporcionalmente)
        renderer.setColor(1, 1, 1, 0.6f);
        renderer.circle(joystickX + (dirX * joystickRadio), joystickY, joystickRadio / 2);

        // Botones (Golpe, Salto, Agarre, Lanzar)
        renderer.setColor(1, 0, 0, golpePresionado ? 0.7f : 0.3f); // Rojo: Golpe
        renderer.circle(btnGolpeX, btnGolpeY, btnRadio);
        
        renderer.setColor(0, 1, 0, saltarPresionado ? 0.7f : 0.3f); // Verde: Salto
        renderer.circle(btnSaltoX, btnSaltoY, btnRadio);
        
        renderer.setColor(0, 0, 1, agarrarPresionado ? 0.7f : 0.3f); // Azul: Agarre
        renderer.circle(btnAgarreX, btnAgarreY, btnRadio);
        
        renderer.setColor(1, 1, 0, lanzarPresionado ? 0.7f : 0.3f); // Amarillo: Lanzar
        renderer.circle(btnLanzarX, btnLanzarY, btnRadio);
        
        renderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float touchY = Gdx.graphics.getHeight() - screenY; // Invertir Y

        // Chequear Joystick
        if (Math.hypot(screenX - joystickX, touchY - joystickY) < joystickRadio * 1.5f) {
            punteroJoystick = pointer;
            calcularJoystick(screenX, touchY);
        }
        
        // Chequear botones
        if (Math.hypot(screenX - btnGolpeX, touchY - btnGolpeY) < btnRadio) golpePresionado = true;
        if (Math.hypot(screenX - btnSaltoX, touchY - btnSaltoY) < btnRadio) saltarPresionado = true;
        if (Math.hypot(screenX - btnAgarreX, touchY - btnAgarreY) < btnRadio) agarrarPresionado = true;
        if (Math.hypot(screenX - btnLanzarX, touchY - btnLanzarY) < btnRadio) lanzarPresionado = true;

        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        float touchY = Gdx.graphics.getHeight() - screenY;

        if (pointer == punteroJoystick) {
            calcularJoystick(screenX, touchY);
        }
        return true;
    }

    /** Calcula dirX y fuerza basados en la posición del dedo relativa al centro del joystick. */
    private void calcularJoystick(float touchX, float touchY) {
        float dx = touchX - joystickX;
        // Zona muerta del 15% del radio
        if (Math.abs(dx) < joystickRadio * 0.15f) {
            dirX  = 0f;
            fuerza = 0f;
            return;
        }
        // Valor proporcional al desplazamiento, clampeado a [-1, 1]
        float raw = dx / joystickRadio;
        dirX  = Math.max(-1f, Math.min(1f, raw));  // clamp
        fuerza = Math.abs(dirX);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        float touchY = Gdx.graphics.getHeight() - screenY;

        if (pointer == punteroJoystick) {
            punteroJoystick = -1;
            dirX = 0f;
        }

        // Si se levanta el dedo que estaba sobre un botón, liberarlo.
        // Hacemos el área un poco más grande (x2) para que si el dedo resbala no se quede trabado.
        if (Math.hypot(screenX - btnGolpeX, touchY - btnGolpeY) < btnRadio * 2.5f) golpePresionado = false;
        if (Math.hypot(screenX - btnSaltoX, touchY - btnSaltoY) < btnRadio * 2.5f) saltarPresionado = false;
        if (Math.hypot(screenX - btnAgarreX, touchY - btnAgarreY) < btnRadio * 2.5f) agarrarPresionado = false;
        if (Math.hypot(screenX - btnLanzarX, touchY - btnLanzarY) < btnRadio * 2.5f) lanzarPresionado = false;

        return true;
    }

    public float getDirX() {
        return dirX;
    }

    /** true si el joystick está empujado más del 70% del radio (correr). */
    public boolean isCorriendoRapido() {
        return fuerza >= 0.70f;
    }

    public boolean moviendoIzquierda() {
        return getDirX() < 0;
    }
}
