package com.frotty27.elitemobs.api;

import com.frotty27.elitemobs.api.events.*;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EliteMobsEventBus implements IEliteMobsEventBus {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final List<IEliteMobsEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void registerListener(IEliteMobsEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(IEliteMobsEventListener listener) {
        listeners.remove(listener);
    }

    public void fire(EliteMobSpawnedEvent event) {
        LOGGER.atInfo().log("[EventBus] fire(EliteMobSpawnedEvent) tier=%d listeners=%d", event.getTier(), listeners.size());
        for (var listener : listeners) {
            try { listener.onEliteMobSpawned(event); } catch (Throwable t) { logError("onEliteMobSpawned", t); }
        }
    }

    public void fire(EliteMobDeathEvent event) {
        for (var listener : listeners) {
            try { listener.onEliteMobDeath(event); } catch (Throwable t) { logError("onEliteMobDeath", t); }
        }
    }

    public void fire(EliteMobDropsEvent event) {
        for (var listener : listeners) {
            try { listener.onEliteMobDrops(event); } catch (Throwable t) { logError("onEliteMobDrops", t); }
        }
    }

    public void fire(EliteMobDamageDealtEvent event) {
        for (var listener : listeners) {
            try { listener.onEliteMobDamageDealt(event); } catch (Throwable t) { logError("onEliteMobDamageDealt", t); }
        }
    }

    public void fire(EliteMobDamageReceivedEvent event) {
        for (var listener : listeners) {
            try { listener.onEliteMobDamageReceived(event); } catch (Throwable t) { logError("onEliteMobDamageReceived", t); }
        }
    }

    public void fire(EliteMobReconcileEvent event) {
        for (var listener : listeners) {
            try { listener.onReconcile(event); } catch (Throwable t) { logError("onReconcile", t); }
        }
    }

    public void fire(EliteMobAggroEvent event) {
        for (var listener : listeners) {
            try { listener.onEliteMobAggro(event); } catch (Throwable t) { logError("onEliteMobAggro", t); }
        }
    }

    public void fire(EliteMobDeaggroEvent event) {
        for (var listener : listeners) {
            try { listener.onEliteMobDeaggro(event); } catch (Throwable t) { logError("onEliteMobDeaggro", t); }
        }
    }

    public void fire(EliteMobAbilityStartedEvent event) {
        for (var listener : listeners) {
            try { listener.onEliteMobAbilityStarted(event); } catch (Throwable t) { logError("onEliteMobAbilityStarted", t); }
        }
    }

    public void fire(EliteMobAbilityCompletedEvent event) {
        for (var listener : listeners) {
            try { listener.onEliteMobAbilityCompleted(event); } catch (Throwable t) { logError("onEliteMobAbilityCompleted", t); }
        }
    }

    public void fire(EliteMobAbilityInterruptedEvent event) {
        for (var listener : listeners) {
            try { listener.onEliteMobAbilityInterrupted(event); } catch (Throwable t) { logError("onEliteMobAbilityInterrupted", t); }
        }
    }

    public void fire(EliteMobScalingAppliedEvent event) {
        for (var listener : listeners) {
            try { listener.onScalingApplied(event); } catch (Throwable t) { logError("onScalingApplied", t); }
        }
    }

    private static void logError(String eventName, Throwable t) {
        EliteMobsLogger.debug(
                LOGGER,
                "Listener threw in %s: %s",
                EliteMobsLogLevel.WARNING,
                eventName,
                t.toString()
        );
    }
}
