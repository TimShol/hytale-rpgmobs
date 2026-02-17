package com.frotty27.elitemobs.components.summon;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EliteMobsSummonMinionTrackingComponent implements Component<EntityStore> {

    public int summonedAliveCount;
    public boolean disableDrops;

    private static final KeyedCodec<Integer> K_SUMMONED_ALIVE_COUNT =
            new KeyedCodec<>("SummonedAliveCount", new IntegerCodec());
    private static final KeyedCodec<Boolean> K_DISABLE_DROPS =
            new KeyedCodec<>("DisableDrops", new BooleanCodec());

    public static final BuilderCodec<EliteMobsSummonMinionTrackingComponent> CODEC =
            BuilderCodec.builder(EliteMobsSummonMinionTrackingComponent.class, EliteMobsSummonMinionTrackingComponent::new)
                    .append(K_SUMMONED_ALIVE_COUNT, (c, v) -> c.summonedAliveCount = v, c -> c.summonedAliveCount).add()
                    .append(K_DISABLE_DROPS, (c, v) -> c.disableDrops = v, c -> c.disableDrops).add()
                    .build();

    public EliteMobsSummonMinionTrackingComponent() {}

    public static EliteMobsSummonMinionTrackingComponent forParent() {
        EliteMobsSummonMinionTrackingComponent c = new EliteMobsSummonMinionTrackingComponent();
        c.summonedAliveCount = 0;
        c.disableDrops = false;
        return c;
    }

    public static EliteMobsSummonMinionTrackingComponent forMinion() {
        EliteMobsSummonMinionTrackingComponent c = new EliteMobsSummonMinionTrackingComponent();
        c.summonedAliveCount = 0;
        c.disableDrops = true;
        return c;
    }

    @Override
    public Component<EntityStore> clone() {
        EliteMobsSummonMinionTrackingComponent c = new EliteMobsSummonMinionTrackingComponent();
        c.summonedAliveCount = this.summonedAliveCount;
        c.disableDrops = this.disableDrops;
        return c;
    }

    public void decrementCount() {
        this.summonedAliveCount = Math.max(0, this.summonedAliveCount - 1);
    }

    public boolean canSummonMore(int maxSummons) {
        return summonedAliveCount < maxSummons;
    }
}
