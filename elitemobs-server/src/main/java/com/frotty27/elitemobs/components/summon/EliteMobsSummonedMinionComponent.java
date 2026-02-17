package com.frotty27.elitemobs.components.summon;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.UUIDBinaryCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public final class EliteMobsSummonedMinionComponent implements Component<EntityStore> {

    public UUID summonerId;
    public int minTierIndex = 0;
    public int maxTierIndex = 1;
    public boolean tierApplied;
    /**
     * Tick at which this minion should die in a chain reaction. 0 = not scheduled.
     */
    public long chainDeathAtTick = 0L;

    private static final KeyedCodec<UUID> K_SUMMONER = new KeyedCodec<>("SummonerId", new UUIDBinaryCodec());
    private static final KeyedCodec<Integer> K_MIN_TIER = new KeyedCodec<>("MinTierIndex", new IntegerCodec());
    private static final KeyedCodec<Integer> K_MAX_TIER = new KeyedCodec<>("MaxTierIndex", new IntegerCodec());
    private static final KeyedCodec<Boolean> K_TIER_APPLIED = new KeyedCodec<>("TierApplied", new BooleanCodec());
    private static final KeyedCodec<Long> K_CHAIN_DEATH_AT = new KeyedCodec<>("ChainDeathAtTick", new LongCodec());

    public static final BuilderCodec<EliteMobsSummonedMinionComponent> CODEC =
            BuilderCodec.builder(EliteMobsSummonedMinionComponent.class, EliteMobsSummonedMinionComponent::new)
                    .append(K_SUMMONER, (c, v) -> c.summonerId = v, c -> c.summonerId).add()
                    .append(K_MIN_TIER, (c, v) -> c.minTierIndex = v, c -> c.minTierIndex).add()
                    .append(K_MAX_TIER, (c, v) -> c.maxTierIndex = v, c -> c.maxTierIndex).add().append(K_TIER_APPLIED,
                                                                                                        (c, v) -> c.tierApplied = v,
                                                                                                        c -> c.tierApplied
                    ).add().append(K_CHAIN_DEATH_AT, (c, v) -> c.chainDeathAtTick = v, c -> c.chainDeathAtTick).add()
                    .build();

    @Override
    public Component<EntityStore> clone() {
        EliteMobsSummonedMinionComponent c = new EliteMobsSummonedMinionComponent();
        c.summonerId = this.summonerId;
        c.minTierIndex = this.minTierIndex;
        c.maxTierIndex = this.maxTierIndex;
        c.tierApplied = this.tierApplied;
        c.chainDeathAtTick = this.chainDeathAtTick;
        return c;
    }
}
