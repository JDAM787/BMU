package com.example.bmu.controladores;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.example.bmu.fisicas.EntidadFisica;
import com.example.bmu.fisicas.FabricaCuerpos;
import com.example.bmu.fisicas.MundoFisico;
import com.example.bmu.fisicas.SistemaAgarre;
import com.example.bmu.modelos.EnemigoDebil;
import com.example.bmu.modelos.EnemigoFuerte;
import com.example.bmu.modelos.Jefe1;
import com.example.bmu.modelos.Jefe2;
import com.example.bmu.modelos.Jefe3;
import com.example.bmu.vista.AnimadorEnemigoDebil;
import com.example.bmu.vista.AnimadorEnemigoFuerte;
import com.example.bmu.vista.AnimadorJefe1;
import com.example.bmu.vista.AnimadorJefe2;
import com.example.bmu.vista.AnimadorJefe3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Gestiona todos los enemigos del juego.
 * Los enemigos normales (débiles y fuertes) son listas que se
 * configuran por escenario. Los jefes son entidades únicas.
 */
public class GestorEnemigos {

    // ── Estado por enemigo individual ────────────────────────────────────────
    private static class EstadoEnemigo {
        EntidadFisica entidad;
        float stateTime         = 0f;
        float cooldownAtaque    = 0f;
        float tiempoAtacando    = 0f;
        boolean isAtacando      = false;
        float tiempoEnSuelo     = 0f; // tiempo muerto en suelo (para desvanecimiento)
        boolean yaSoltoItem     = false;

        EstadoEnemigo(EntidadFisica entidad) {
            this.entidad = entidad;
        }
    }

    // ── Enemigos normales (listas) ────────────────────────────────────────────
    private final List<EstadoEnemigo> enemigosDebiles  = new ArrayList<>();
    private final List<EstadoEnemigo> enemigosFuertes  = new ArrayList<>();

    // ── Jefes (entidades únicas) ─────────────────────────────────────────────
    private EntidadFisica entJefe1;
    private EntidadFisica entJefe2;
    private EntidadFisica entJefe3;

    private float   stateTimeJefe1 = 0f, stateTimeJefe2 = 0f, stateTimeJefe3 = 0f;
    private float   cooldownJefe1  = 0f, cooldownJefe2  = 0f, cooldownJefe3  = 0f;
    private float   tiempoAtJefe1  = 0f, tiempoAtJefe2  = 0f, tiempoAtJefe3  = 0f;
    private boolean atacandoJefe1  = false, atacandoJefe2  = false, atacandoJefe3  = false;
    private float   sueloMuertoJ1  = 0f, sueloMuertoJ2  = 0f, sueloMuertoJ3  = 0f;
    private boolean jefe1Activado  = false, jefe2Activado = false, jefe3Activado = false;
    private boolean yaSoltoItemJefe1 = false, yaSoltoItemJefe2 = false, yaSoltoItemJefe3 = false;
 
    public interface CallbackDropArma {
        void onDropArma(String tipoArma, float xMetros, float yMetros);
    }
    private CallbackDropArma callbackDrop;
    public void setCallbackDropArma(CallbackDropArma cb) {
        this.callbackDrop = cb;
    }

    // ── Animadores ────────────────────────────────────────────────────────────
    private final AnimadorEnemigoDebil  animadorDebil;
    private final AnimadorEnemigoFuerte animadorFuerte;
    private final AnimadorJefe1         animadorJefe1;
    private final AnimadorJefe2         animadorJefe2;
    private final AnimadorJefe3         animadorJefe3;

    private final MundoFisico    mundo;
    private final FabricaCuerpos fabrica;

    // ── Constructor ───────────────────────────────────────────────────────────
    public GestorEnemigos(MundoFisico mundo, FabricaCuerpos fabrica) {
        this.mundo   = mundo;
        this.fabrica = fabrica;
        animadorDebil  = new AnimadorEnemigoDebil(1);
        animadorFuerte = new AnimadorEnemigoFuerte();
        animadorJefe1  = new AnimadorJefe1();
        animadorJefe2  = new AnimadorJefe2();
        animadorJefe3  = new AnimadorJefe3();
        reiniciarEnemigos();
    }

    // ── Getters públicos ──────────────────────────────────────────────────────

    /** Compatibilidad retroactiva – devuelve la primera entidad débil (o null). */
    public EntidadFisica getEntEnemigoDebil() {
        return enemigosDebiles.isEmpty() ? null : enemigosDebiles.get(0).entidad;
    }
    /** Compatibilidad retroactiva – devuelve la primera entidad fuerte (o null). */
    public EntidadFisica getEntEnemigoFuerte() {
        return enemigosFuertes.isEmpty() ? null : enemigosFuertes.get(0).entidad;
    }

    public List<EntidadFisica> getEntidadesDebiles() {
        List<EntidadFisica> r = new ArrayList<>();
        for (EstadoEnemigo e : enemigosDebiles) if (e.entidad != null) r.add(e.entidad);
        return r;
    }
    public List<EntidadFisica> getEntidadesFuertes() {
        List<EntidadFisica> r = new ArrayList<>();
        for (EstadoEnemigo e : enemigosFuertes) if (e.entidad != null) r.add(e.entidad);
        return r;
    }

    public EntidadFisica getEntJefe1() { return entJefe1; }
    public EntidadFisica getEntJefe2() { return entJefe2; }
    public EntidadFisica getEntJefe3() { return entJefe3; }

    public boolean isJefe1Activado() { return jefe1Activado; }
    public boolean isJefe2Activado() { return jefe2Activado; }
    public boolean isJefe3Activado() { return jefe3Activado; }
    public void setJefe1Activado(boolean v) { jefe1Activado = v; }
    public void setJefe2Activado(boolean v) { jefe2Activado = v; }
    public void setJefe3Activado(boolean v) { jefe3Activado = v; }

    // ── Configuración por escenario ───────────────────────────────────────────

    /**
     * Destruye los enemigos normales actuales y crea los adecuados
     * para el escenario indicado, posicionados sobre el suelo dado.
     * Llamar FUERA de world.step().
     */
    public void configurarEnemigosParaEscenario(int escenario, float ySuelo) {
        limpiarEnemigosNormales();

        // Centro vertical del cuerpo físico sobre el suelo
        int yPx = (int)((ySuelo + 0.8f) * MundoFisico.PPM);

        switch (escenario) {
            case 0: // Azotea – 2 débiles
                spawnDebil(800,  yPx);
                spawnDebil(1400, yPx);
                break;
            case 1: // Calle – 3 débiles + 1 fuerte + Jefe1
                spawnDebil(600,  yPx);
                spawnDebil(1000, yPx);
                spawnDebil(1500, yPx);
                spawnFuerte(1200, yPx);
                break;
            case 2: // Muelle – 2 débiles + 2 fuertes + Jefe2
                spawnDebil(600,  yPx);
                spawnDebil(1400, yPx);
                spawnFuerte(900,  yPx);
                spawnFuerte(1300, yPx);
                break;
            case 3: // Industria – 1 débil + 2 fuertes + Jefe3
                spawnDebil(1000, yPx);
                spawnFuerte(600,  yPx);
                spawnFuerte(1400, yPx);
                break;
        }
    }

    private void limpiarEnemigosNormales() {
        for (EstadoEnemigo e : enemigosDebiles) {
            if (e.entidad != null) mundo.getWorld().destroyBody(e.entidad.getCuerpo());
        }
        enemigosDebiles.clear();
        for (EstadoEnemigo e : enemigosFuertes) {
            if (e.entidad != null) mundo.getWorld().destroyBody(e.entidad.getCuerpo());
        }
        enemigosFuertes.clear();
    }

    private void spawnDebil(int xPx, int yPx) {
        EnemigoDebil en = new EnemigoDebil();
        Body c = fabrica.crearCuerpoEnemigo(xPx, yPx, 100, 160, true, en);
        enemigosDebiles.add(new EstadoEnemigo(new EntidadFisica(c, en)));
    }

    private void spawnFuerte(int xPx, int yPx) {
        EnemigoFuerte en = new EnemigoFuerte();
        Body c = fabrica.crearCuerpoEnemigo(xPx, yPx, 120, 180, false, en);
        enemigosFuertes.add(new EstadoEnemigo(new EntidadFisica(c, en)));
    }

    // ── Reinicio completo (jefes + limpieza de normales) ─────────────────────

    public void reiniciarEnemigos() {
        limpiarEnemigosNormales();

        // Jefe 1
        if (entJefe1 != null) { mundo.getWorld().destroyBody(entJefe1.getCuerpo()); }
        Jefe1 j1 = new Jefe1();
        Body cJ1 = fabrica.crearCuerpoEnemigo(10000, 304, 150, 200, false, j1);
        cJ1.setActive(false);
        entJefe1 = new EntidadFisica(cJ1, j1);
        jefe1Activado = false; sueloMuertoJ1 = 0; stateTimeJefe1 = 0; cooldownJefe1 = 0; tiempoAtJefe1 = 0; atacandoJefe1 = false;
        yaSoltoItemJefe1 = false;

        // Jefe 2
        if (entJefe2 != null) { mundo.getWorld().destroyBody(entJefe2.getCuerpo()); }
        Jefe2 j2 = new Jefe2();
        Body cJ2 = fabrica.crearCuerpoEnemigo(10000, 304, 150, 200, false, j2);
        cJ2.setActive(false);
        entJefe2 = new EntidadFisica(cJ2, j2);
        jefe2Activado = false; sueloMuertoJ2 = 0; stateTimeJefe2 = 0; cooldownJefe2 = 0; tiempoAtJefe2 = 0; atacandoJefe2 = false;
        yaSoltoItemJefe2 = false;

        // Jefe 3
        if (entJefe3 != null) { mundo.getWorld().destroyBody(entJefe3.getCuerpo()); }
        Jefe3 j3 = new Jefe3();
        Body cJ3 = fabrica.crearCuerpoEnemigo(10000, 304, 150, 200, false, j3);
        cJ3.setActive(false);
        entJefe3 = new EntidadFisica(cJ3, j3);
        jefe3Activado = false; sueloMuertoJ3 = 0; stateTimeJefe3 = 0; cooldownJefe3 = 0; tiempoAtJefe3 = 0; atacandoJefe3 = false;
        yaSoltoItemJefe3 = false;
    }

    // ── Posición segura de spawn para el jugador ──────────────────────────────

    public float calcularPosicionSeguraSpawn(int escenario) {
        float xMin = 3.0f, xMax = 16.0f;
        List<Float> xs = new ArrayList<>();

        for (EstadoEnemigo e : enemigosDebiles)
            if (e.entidad != null && e.entidad.getModelo().estaVivo())
                xs.add(e.entidad.getCuerpo().getPosition().x);
        for (EstadoEnemigo e : enemigosFuertes)
            if (e.entidad != null && e.entidad.getModelo().estaVivo())
                xs.add(e.entidad.getCuerpo().getPosition().x);
        if (entJefe1 != null && entJefe1.getModelo().estaVivo() && jefe1Activado) xs.add(entJefe1.getCuerpo().getPosition().x);
        if (entJefe2 != null && entJefe2.getModelo().estaVivo() && jefe2Activado) xs.add(entJefe2.getCuerpo().getPosition().x);
        if (entJefe3 != null && entJefe3.getModelo().estaVivo() && jefe3Activado) xs.add(entJefe3.getCuerpo().getPosition().x);

        if (xs.isEmpty()) return (escenario == 0) ? 16f : 3f;

        float mejorX = xMin, maxDMin = -1f;
        for (float x = xMin; x <= xMax; x += 0.5f) {
            float dMin = Float.MAX_VALUE;
            for (float ex : xs) { float d = Math.abs(x - ex); if (d < dMin) dMin = d; }
            if (dMin > maxDMin) { maxDMin = dMin; mejorX = x; }
        }
        if (maxDMin < 3.0f) return xMin + (float) Math.random() * (xMax - xMin);
        return mejorX;
    }

    // ── Actualización (lógica + IA) ───────────────────────────────────────────

    public void actualizar(float delta, EntidadFisica entJugador, SistemaAgarre sistemaAgarre) {

        // Débiles
        Iterator<EstadoEnemigo> itD = enemigosDebiles.iterator();
        while (itD.hasNext()) {
            EstadoEnemigo e = itD.next();
            if (e.entidad == null) { itD.remove(); continue; }
            e.entidad.getModelo().actualizar(delta);
            if (e.entidad.getModelo().isLanzado() && e.entidad.estaEnSuelo())
                e.entidad.getModelo().setLanzado(false);
            actualizarIADebil(delta, e, entJugador, sistemaAgarre);
            if (!e.entidad.getModelo().estaVivo()) {
                if (!e.yaSoltoItem) {
                    e.yaSoltoItem = true;
                    if (callbackDrop != null) {
                        double rand = Math.random();
                        if (rand < 0.25) {
                            callbackDrop.onDropArma("cuchillo", e.entidad.getCuerpo().getPosition().x, e.entidad.getCuerpo().getPosition().y);
                        }
                    }
                }
                e.tiempoEnSuelo += delta;
                if (e.tiempoEnSuelo >= 3.0f) {
                    if (sistemaAgarre.getEnemigoAgarrado() == e.entidad.getModelo())
                        sistemaAgarre.soltarAgarre();
                    mundo.getWorld().destroyBody(e.entidad.getCuerpo());
                    e.entidad = null;
                    itD.remove();
                }
            }
        }

        // Fuertes
        Iterator<EstadoEnemigo> itF = enemigosFuertes.iterator();
        while (itF.hasNext()) {
            EstadoEnemigo e = itF.next();
            if (e.entidad == null) { itF.remove(); continue; }
            e.entidad.getModelo().actualizar(delta);
            if (e.entidad.getModelo().isLanzado() && e.entidad.estaEnSuelo())
                e.entidad.getModelo().setLanzado(false);
            actualizarIAFuerte(delta, e, entJugador, sistemaAgarre);
            if (!e.entidad.getModelo().estaVivo()) {
                if (!e.yaSoltoItem) {
                    e.yaSoltoItem = true;
                    if (callbackDrop != null) {
                        double rand = Math.random();
                        if (rand < 0.35) {
                            callbackDrop.onDropArma("tubo", e.entidad.getCuerpo().getPosition().x, e.entidad.getCuerpo().getPosition().y);
                        }
                    }
                }
                e.tiempoEnSuelo += delta;
                if (e.tiempoEnSuelo >= 3.0f) {
                    if (sistemaAgarre.getEnemigoAgarrado() == e.entidad.getModelo())
                        sistemaAgarre.soltarAgarre();
                    mundo.getWorld().destroyBody(e.entidad.getCuerpo());
                    e.entidad = null;
                    itF.remove();
                }
            }
        }

        // Jefe 1
        if (entJefe1 != null && jefe1Activado) {
            entJefe1.getModelo().actualizar(delta);
            actualizarIAJefe(delta, entJefe1, entJugador, 1);
            if (!entJefe1.getModelo().estaVivo()) {
                if (!yaSoltoItemJefe1) {
                    yaSoltoItemJefe1 = true;
                    if (callbackDrop != null) {
                        callbackDrop.onDropArma("tubo", entJefe1.getCuerpo().getPosition().x, entJefe1.getCuerpo().getPosition().y);
                    }
                }
                sueloMuertoJ1 += delta;
                if (sueloMuertoJ1 >= 3.0f) { mundo.getWorld().destroyBody(entJefe1.getCuerpo()); entJefe1 = null; }
            }
        }

        // Jefe 2
        if (entJefe2 != null && jefe2Activado) {
            entJefe2.getModelo().actualizar(delta);
            actualizarIAJefe(delta, entJefe2, entJugador, 2);
            if (!entJefe2.getModelo().estaVivo()) {
                if (!yaSoltoItemJefe2) {
                    yaSoltoItemJefe2 = true;
                    if (callbackDrop != null) {
                        callbackDrop.onDropArma("tubo", entJefe2.getCuerpo().getPosition().x, entJefe2.getCuerpo().getPosition().y);
                    }
                }
                sueloMuertoJ2 += delta;
                if (sueloMuertoJ2 >= 3.0f) { mundo.getWorld().destroyBody(entJefe2.getCuerpo()); entJefe2 = null; }
            }
        }

        // Jefe 3
        if (entJefe3 != null && jefe3Activado) {
            entJefe3.getModelo().actualizar(delta);
            actualizarIAJefe(delta, entJefe3, entJugador, 3);
            if (!entJefe3.getModelo().estaVivo()) {
                if (!yaSoltoItemJefe3) {
                    yaSoltoItemJefe3 = true;
                    if (callbackDrop != null) {
                        callbackDrop.onDropArma("tubo", entJefe3.getCuerpo().getPosition().x, entJefe3.getCuerpo().getPosition().y);
                    }
                }
                sueloMuertoJ3 += delta;
                if (sueloMuertoJ3 >= 3.0f) { mundo.getWorld().destroyBody(entJefe3.getCuerpo()); entJefe3 = null; }
            }
        }
    }

    // ── IA enemigo débil ──────────────────────────────────────────────────────

    private void actualizarIADebil(float delta, EstadoEnemigo est, EntidadFisica entJugador, SistemaAgarre sistemaAgarre) {
        EntidadFisica en = est.entidad;
        if (!en.getModelo().estaVivo()) { if (en.estaEnSuelo()) en.detener(); return; }
        if (sistemaAgarre.tienEnemigoAgarrado() && sistemaAgarre.getEnemigoAgarrado() == en.getModelo()) return;

        est.stateTime += delta;
        if (est.cooldownAtaque > 0) est.cooldownAtaque -= delta;
        if (est.isAtacando) {
            est.tiempoAtacando += delta;
            if (est.tiempoAtacando >= 0.3f) { est.isAtacando = false; est.tiempoAtacando = 0f; }
        }
        if (en.getModelo().tiempoHurt > 0 || est.isAtacando) { en.detener(); return; }

        float dx = entJugador.getCuerpo().getPosition().x - en.getCuerpo().getPosition().x;
        if (Math.abs(dx) > 1.2f) {
            en.mover(Math.signum(dx) * 0.5f);
        } else {
            en.detener();
            float dy = entJugador.getCuerpo().getPosition().y - en.getCuerpo().getPosition().y;
            if (entJugador.getModelo().estaVivo() && Math.abs(dy) <= 1.0f && est.cooldownAtaque <= 0) {
                est.isAtacando = true; est.tiempoAtacando = 0f; est.stateTime = 0f;
                est.cooldownAtaque = 1.5f;
                en.getModelo().atacar(entJugador.getModelo());
            }
        }
    }

    // ── IA enemigo fuerte ─────────────────────────────────────────────────────

    private void actualizarIAFuerte(float delta, EstadoEnemigo est, EntidadFisica entJugador, SistemaAgarre sistemaAgarre) {
        EntidadFisica en = est.entidad;
        if (!en.getModelo().estaVivo()) { if (en.estaEnSuelo()) en.detener(); return; }
        if (sistemaAgarre.tienEnemigoAgarrado() && sistemaAgarre.getEnemigoAgarrado() == en.getModelo()) return;

        est.stateTime += delta;
        if (est.cooldownAtaque > 0) est.cooldownAtaque -= delta;
        if (est.isAtacando) {
            est.tiempoAtacando += delta;
            if (est.tiempoAtacando >= 0.4f) { est.isAtacando = false; est.tiempoAtacando = 0f; }
        }
        if (en.getModelo().tiempoHurt > 0 || est.isAtacando) { en.detener(); return; }

        float dx = entJugador.getCuerpo().getPosition().x - en.getCuerpo().getPosition().x;
        if (Math.abs(dx) > 1.4f) {
            en.mover(Math.signum(dx) * 0.35f);
        } else {
            en.detener();
            float dy = entJugador.getCuerpo().getPosition().y - en.getCuerpo().getPosition().y;
            if (entJugador.getModelo().estaVivo() && Math.abs(dy) <= 1.0f && est.cooldownAtaque <= 0) {
                est.isAtacando = true; est.tiempoAtacando = 0f; est.stateTime = 0f;
                est.cooldownAtaque = 2.0f;
                en.getModelo().atacar(entJugador.getModelo());
            }
        }
    }

    // ── IA jefes (unificada por índice) ──────────────────────────────────────

    private void actualizarIAJefe(float delta, EntidadFisica enemigo, EntidadFisica entJugador, int idx) {
        if (!enemigo.getModelo().estaVivo()) { if (enemigo.estaEnSuelo()) enemigo.detener(); return; }

        float spd, rangue, cooldownMax, durAtaque;
        switch (idx) {
            case 1:  spd = 0.50f; rangue = 1.6f; cooldownMax = 2.0f; durAtaque = 0.5f; break;
            case 2:  spd = 0.40f; rangue = 1.6f; cooldownMax = 2.5f; durAtaque = 0.5f; break;
            default: spd = 0.45f; rangue = 1.6f; cooldownMax = 2.2f; durAtaque = 0.5f; break;
        }

        // Proxy refs to per-boss state
        if (idx == 1) {
            stateTimeJefe1 += delta;
            if (cooldownJefe1 > 0) cooldownJefe1 -= delta;
            if (atacandoJefe1) { tiempoAtJefe1 += delta; if (tiempoAtJefe1 >= durAtaque) { atacandoJefe1 = false; tiempoAtJefe1 = 0; } }
            if (enemigo.getModelo().tiempoHurt > 0 || atacandoJefe1) { enemigo.detener(); return; }
            float dx = entJugador.getCuerpo().getPosition().x - enemigo.getCuerpo().getPosition().x;
            if (Math.abs(dx) > rangue) { enemigo.mover(Math.signum(dx) * spd); }
            else { enemigo.detener();
                float dy = entJugador.getCuerpo().getPosition().y - enemigo.getCuerpo().getPosition().y;
                if (entJugador.getModelo().estaVivo() && Math.abs(dy) <= 1.0f && cooldownJefe1 <= 0) {
                    atacandoJefe1 = true; tiempoAtJefe1 = 0; stateTimeJefe1 = 0; cooldownJefe1 = cooldownMax;
                    enemigo.getModelo().atacar(entJugador.getModelo());
                }
            }
        } else if (idx == 2) {
            stateTimeJefe2 += delta;
            if (cooldownJefe2 > 0) cooldownJefe2 -= delta;
            if (atacandoJefe2) { tiempoAtJefe2 += delta; if (tiempoAtJefe2 >= durAtaque) { atacandoJefe2 = false; tiempoAtJefe2 = 0; } }
            if (enemigo.getModelo().tiempoHurt > 0 || atacandoJefe2) { enemigo.detener(); return; }
            float dx = entJugador.getCuerpo().getPosition().x - enemigo.getCuerpo().getPosition().x;
            if (Math.abs(dx) > rangue) { enemigo.mover(Math.signum(dx) * spd); }
            else { enemigo.detener();
                float dy = entJugador.getCuerpo().getPosition().y - enemigo.getCuerpo().getPosition().y;
                if (entJugador.getModelo().estaVivo() && Math.abs(dy) <= 1.0f && cooldownJefe2 <= 0) {
                    atacandoJefe2 = true; tiempoAtJefe2 = 0; stateTimeJefe2 = 0; cooldownJefe2 = cooldownMax;
                    enemigo.getModelo().atacar(entJugador.getModelo());
                }
            }
        } else {
            stateTimeJefe3 += delta;
            if (cooldownJefe3 > 0) cooldownJefe3 -= delta;
            if (atacandoJefe3) { tiempoAtJefe3 += delta; if (tiempoAtJefe3 >= durAtaque) { atacandoJefe3 = false; tiempoAtJefe3 = 0; } }
            if (enemigo.getModelo().tiempoHurt > 0 || atacandoJefe3) { enemigo.detener(); return; }
            float dx = entJugador.getCuerpo().getPosition().x - enemigo.getCuerpo().getPosition().x;
            if (Math.abs(dx) > rangue) { enemigo.mover(Math.signum(dx) * spd); }
            else { enemigo.detener();
                float dy = entJugador.getCuerpo().getPosition().y - enemigo.getCuerpo().getPosition().y;
                if (entJugador.getModelo().estaVivo() && Math.abs(dy) <= 1.0f && cooldownJefe3 <= 0) {
                    atacandoJefe3 = true; tiempoAtJefe3 = 0; stateTimeJefe3 = 0; cooldownJefe3 = cooldownMax;
                    enemigo.getModelo().atacar(entJugador.getModelo());
                }
            }
        }
    }

    // ── Dibujo ────────────────────────────────────────────────────────────────

    public void dibujar(SpriteBatch batch, EntidadFisica entJugador, SistemaAgarre sistemaAgarre, float tiempoRecibeDanoGrab) {
        for (EstadoEnemigo e : enemigosDebiles)  dibujarDebil(batch, e, entJugador, sistemaAgarre, tiempoRecibeDanoGrab);
        for (EstadoEnemigo e : enemigosFuertes)  dibujarFuerte(batch, e, entJugador, sistemaAgarre, tiempoRecibeDanoGrab);
        if (entJefe1 != null && jefe1Activado)   dibujarJefe(batch, entJefe1, entJugador, 1);
        if (entJefe2 != null && jefe2Activado)   dibujarJefe(batch, entJefe2, entJugador, 2);
        if (entJefe3 != null && jefe3Activado)   dibujarJefe(batch, entJefe3, entJugador, 3);
    }

    private void dibujarDebil(SpriteBatch batch, EstadoEnemigo est, EntidadFisica entJugador,
                              SistemaAgarre sistemaAgarre, float tiempoRecibeDanoGrab) {
        if (est.entidad == null) return;
        if (!est.entidad.getModelo().estaVivo() && est.tiempoEnSuelo > 1.5f) {
            if (((int)((est.tiempoEnSuelo - 1.5f) * 15)) % 2 != 0) return;
        }

        float enX = est.entidad.getCuerpo().getPosition().x;
        float enY = est.entidad.getCuerpo().getPosition().y;
        float altoC = 250f / MundoFisico.PPM;
        float anchoC = 193f / MundoFisico.PPM;

        TextureRegion frame;
        if (!est.entidad.getModelo().estaVivo()) {
            if (est.tiempoEnSuelo < 1.0f && !est.entidad.estaEnSuelo())
                frame = animadorDebil.animFall.getKeyFrame(est.stateTime, false);
            else { frame = animadorDebil.animDead.getKeyFrame(est.stateTime, false); anchoC = 225f / MundoFisico.PPM; }
        } else if (sistemaAgarre.tienEnemigoAgarrado() && sistemaAgarre.getEnemigoAgarrado() == est.entidad.getModelo()) {
            frame = animadorDebil.animHurt.getKeyFrame(tiempoRecibeDanoGrab > 0 ? 0.15f : 0f, false);
        } else if (est.entidad.getModelo().isLanzado()) {
            frame = animadorDebil.animFall.getKeyFrame(est.stateTime, false);
        } else if (est.entidad.getModelo().tiempoHurt > 0) {
            frame = animadorDebil.animHurt.getKeyFrame(est.entidad.getModelo().tiempoHurt, false);
        } else if (est.isAtacando) {
            frame = animadorDebil.animPunch.getKeyFrame(est.stateTime, false);
        } else if (Math.abs(est.entidad.getCuerpo().getLinearVelocity().x) > 0.3f) {
            frame = animadorDebil.animWalk.getKeyFrame(est.stateTime, true);
        } else {
            frame = animadorDebil.animIdle.getKeyFrame(est.stateTime, true);
        }

        boolean miraDerecha = Math.abs(est.entidad.getCuerpo().getLinearVelocity().x) > 0.1f
                ? est.entidad.getCuerpo().getLinearVelocity().x > 0
                : entJugador.getCuerpo().getPosition().x > enX;

        TextureRegion drawFrame = new TextureRegion(frame);
        if (!miraDerecha) drawFrame.flip(true, false);
        float altoCuerpo = 160f / MundoFisico.PPM;
        batch.draw(drawFrame, enX - anchoC / 2f, enY - altoCuerpo / 2f, anchoC, altoC);
    }

    private void dibujarFuerte(SpriteBatch batch, EstadoEnemigo est, EntidadFisica entJugador,
                               SistemaAgarre sistemaAgarre, float tiempoRecibeDanoGrab) {
        if (est.entidad == null) return;
        if (!est.entidad.getModelo().estaVivo() && est.tiempoEnSuelo > 1.5f) {
            if (((int)((est.tiempoEnSuelo - 1.5f) * 15)) % 2 != 0) return;
        }

        float enX = est.entidad.getCuerpo().getPosition().x;
        float enY = est.entidad.getCuerpo().getPosition().y;
        float altoC = 240f / MundoFisico.PPM;
        float anchoC = 240f / MundoFisico.PPM;

        TextureRegion frame;
        boolean muertoEnSuelo = false;
        if (!est.entidad.getModelo().estaVivo()) {
            if (est.tiempoEnSuelo < 1.0f && !est.entidad.estaEnSuelo())
                frame = animadorFuerte.animFall.getKeyFrame(est.stateTime, false);
            else {
                frame = animadorFuerte.animDead.getKeyFrame(est.stateTime, false);
                anchoC = 283f / MundoFisico.PPM;
                altoC = 116f / MundoFisico.PPM;
                muertoEnSuelo = true;
            }
        } else if (sistemaAgarre.tienEnemigoAgarrado() && sistemaAgarre.getEnemigoAgarrado() == est.entidad.getModelo()) {
            frame = animadorFuerte.animHurt.getKeyFrame(tiempoRecibeDanoGrab > 0 ? 0.15f : 0f, false);
        } else if (est.entidad.getModelo().isLanzado()) {
            frame = animadorFuerte.animFall.getKeyFrame(est.stateTime, false);
        } else if (est.entidad.getModelo().tiempoHurt > 0) {
            frame = animadorFuerte.animHurt.getKeyFrame(est.entidad.getModelo().tiempoHurt, false);
        } else if (est.isAtacando) {
            frame = animadorFuerte.animPunch.getKeyFrame(est.stateTime, false);
        } else if (Math.abs(est.entidad.getCuerpo().getLinearVelocity().x) > 0.3f) {
            frame = animadorFuerte.animWalk.getKeyFrame(est.stateTime, true);
        } else {
            frame = animadorFuerte.animIdle.getKeyFrame(est.stateTime, true);
        }

        boolean miraDerecha = Math.abs(est.entidad.getCuerpo().getLinearVelocity().x) > 0.1f
                ? est.entidad.getCuerpo().getLinearVelocity().x > 0
                : entJugador.getCuerpo().getPosition().x > enX;

        TextureRegion drawFrame = new TextureRegion(frame);
        if (!miraDerecha) drawFrame.flip(true, false);
        float altoCuerpo = 180f / MundoFisico.PPM;
        float dibY = enY - altoCuerpo / 2f;
        if (muertoEnSuelo) {
            dibY = enY - 100f / MundoFisico.PPM; // Alinea los pies/cuerpo con el suelo físico al estar acostado
        }
        batch.draw(drawFrame, enX - anchoC / 2f, dibY, anchoC, altoC);
    }

    private void dibujarJefe(SpriteBatch batch, EntidadFisica entidad, EntidadFisica entJugador, int idx) {
        float suelo = idx == 1 ? sueloMuertoJ1 : idx == 2 ? sueloMuertoJ2 : sueloMuertoJ3;
        if (!entidad.getModelo().estaVivo() && suelo > 1.5f) {
            if (((int)((suelo - 1.5f) * 15)) % 2 != 0) return;
        }

        float enX   = entidad.getCuerpo().getPosition().x;
        float enY   = entidad.getCuerpo().getPosition().y;
        float altoC = 240f / MundoFisico.PPM;
        float anchoC = 204f / MundoFisico.PPM;

        float stateT  = idx == 1 ? stateTimeJefe1  : idx == 2 ? stateTimeJefe2  : stateTimeJefe3;
        boolean atac  = idx == 1 ? atacandoJefe1    : idx == 2 ? atacandoJefe2   : atacandoJefe3;

        TextureRegion frame;
        AnimadorJefe1 a1 = idx == 1 ? animadorJefe1 : null;
        AnimadorJefe2 a2 = idx == 2 ? animadorJefe2 : null;
        AnimadorJefe3 a3 = idx == 3 ? animadorJefe3 : null;

        boolean muertoEnSuelo = false;
        if (!entidad.getModelo().estaVivo()) {
            boolean enSuelo = entidad.estaEnSuelo();
            if (suelo < 1.0f && !enSuelo) {
                frame = (a1 != null) ? a1.animFall.getKeyFrame(stateT, false)
                      : (a2 != null) ? a2.animFall.getKeyFrame(stateT, false)
                      :                a3.animFall.getKeyFrame(stateT, false);
            } else {
                frame = (a1 != null) ? a1.animDead.getKeyFrame(stateT, false)
                      : (a2 != null) ? a2.animDead.getKeyFrame(stateT, false)
                      :                a3.animDead.getKeyFrame(stateT, false);
                muertoEnSuelo = true;
                if (idx == 1) {
                    anchoC = 278f / MundoFisico.PPM;
                    altoC = 240f / MundoFisico.PPM;
                } else if (idx == 2) {
                    anchoC = 305f / MundoFisico.PPM;
                    altoC = 240f / MundoFisico.PPM;
                } else {
                    anchoC = 314f / MundoFisico.PPM;
                    altoC = 240f / MundoFisico.PPM;
                }
            }
        } else if (entidad.getModelo().tiempoHurt > 0) {
            frame = (a1 != null) ? a1.animHurt.getKeyFrame(entidad.getModelo().tiempoHurt, false)
                  : (a2 != null) ? a2.animHurt.getKeyFrame(entidad.getModelo().tiempoHurt, false)
                  :                a3.animHurt.getKeyFrame(entidad.getModelo().tiempoHurt, false);
        } else if (atac) {
            frame = (a1 != null) ? a1.animPunch.getKeyFrame(stateT, false)
                  : (a2 != null) ? a2.animPunch.getKeyFrame(stateT, false)
                  :                a3.animPunch.getKeyFrame(stateT, false);
        } else if (Math.abs(entidad.getCuerpo().getLinearVelocity().x) > 0.3f) {
            frame = (a1 != null) ? a1.animWalk.getKeyFrame(stateT, true)
                  : (a2 != null) ? a2.animWalk.getKeyFrame(stateT, true)
                  :                a3.animWalk.getKeyFrame(stateT, true);
        } else {
            frame = (a1 != null) ? a1.animIdle.getKeyFrame(stateT, true)
                  : (a2 != null) ? a2.animWalk.getKeyFrame(0f, true) // jefe2 sin idle propio
                  :                a3.animIdle.getKeyFrame(stateT, true);
        }

        boolean miraDerecha = Math.abs(entidad.getCuerpo().getLinearVelocity().x) > 0.1f
                ? entidad.getCuerpo().getLinearVelocity().x > 0
                : entJugador.getCuerpo().getPosition().x > enX;

        TextureRegion drawFrame = new TextureRegion(frame);
        if (!miraDerecha) drawFrame.flip(true, false);
        float altoCuerpo = 200f / MundoFisico.PPM;
        float dibY = enY - altoCuerpo / 2f;
        if (muertoEnSuelo) {
            dibY = enY - altoC / 2f;
        }
        batch.draw(drawFrame, enX - anchoC / 2f, dibY, anchoC, altoC);
    }

    public boolean todosEnemigosNormalesMuertos() {
        for (EstadoEnemigo e : enemigosDebiles)
            if (e.entidad != null && e.entidad.getModelo().estaVivo()) return false;
        for (EstadoEnemigo e : enemigosFuertes)
            if (e.entidad != null && e.entidad.getModelo().estaVivo()) return false;
        return true;
    }

    // ── Dispose ───────────────────────────────────────────────────────────────

    public void dispose() {
        animadorDebil.dispose();
        animadorFuerte.dispose();
        animadorJefe1.dispose();
        animadorJefe2.dispose();
        animadorJefe3.dispose();
    }
}
