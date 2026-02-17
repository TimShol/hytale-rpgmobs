package com.frotty27.elitemobs.components.effects;

import com.frotty27.elitemobs.components.data.EffectState;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.Map;

public final class EliteMobsActiveEffectsComponent implements Component<EntityStore> {

    public Map<String, EffectState> activeEffects;

    public static final BuilderCodec<EliteMobsActiveEffectsComponent> CODEC =
            BuilderCodec.builder(EliteMobsActiveEffectsComponent.class, EliteMobsActiveEffectsComponent::new)
                    .build();

    public EliteMobsActiveEffectsComponent() {
        this.activeEffects = new HashMap<>();
    }

    @Override
    public Component<EntityStore> clone() {
        EliteMobsActiveEffectsComponent c = new EliteMobsActiveEffectsComponent();
        c.activeEffects = new HashMap<>(this.activeEffects);
        return c;
    }

    public void addEffect(String effectId, EffectState state) {
        activeEffects.put(effectId, state);
    }

    public void removeEffect(String effectId) {
        activeEffects.remove(effectId);
    }
}
