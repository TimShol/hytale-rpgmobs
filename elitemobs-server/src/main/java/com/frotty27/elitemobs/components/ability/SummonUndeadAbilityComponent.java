package com.frotty27.elitemobs.components.ability;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class SummonUndeadAbilityComponent implements Component<EntityStore> {

    public boolean abilityEnabled;

    public long cooldownTicksRemaining;

    public long pendingSummonTicksRemaining;
    public @Nullable String pendingSummonRole;

    public transient boolean swapActive;
    public transient byte swapSlot = -1;
    public transient @Nullable ItemStack swapPreviousItem;

    private static final KeyedCodec<Boolean> K_ABILITY_ENABLED =
            new KeyedCodec<>("AbilityEnabled", new BooleanCodec());
    private static final KeyedCodec<Long> K_COOLDOWN_TICKS_REMAINING =
            new KeyedCodec<>("CooldownTicksRemaining", new LongCodec());
    private static final KeyedCodec<Long> K_PENDING_SUMMON_TICKS_REMAINING =
            new KeyedCodec<>("PendingSummonTicksRemaining", new LongCodec());
    private static final KeyedCodec<String> K_PENDING_SUMMON_ROLE =
            new KeyedCodec<>("PendingSummonRole", new StringCodec());

    public static final BuilderCodec<SummonUndeadAbilityComponent> CODEC =
            BuilderCodec.builder(SummonUndeadAbilityComponent.class, SummonUndeadAbilityComponent::new)
                    .append(K_ABILITY_ENABLED, (c, v) -> c.abilityEnabled = v, c -> c.abilityEnabled).add()
                    .append(K_COOLDOWN_TICKS_REMAINING, (c, v) -> c.cooldownTicksRemaining = v, c -> c.cooldownTicksRemaining).add()
                    .append(K_PENDING_SUMMON_TICKS_REMAINING, (c, v) -> c.pendingSummonTicksRemaining = v, c -> c.pendingSummonTicksRemaining).add()
                    .append(K_PENDING_SUMMON_ROLE, (c, v) -> c.pendingSummonRole = v, c -> c.pendingSummonRole).add()
                    .build();

    public SummonUndeadAbilityComponent() {
        this.abilityEnabled = false;
        this.cooldownTicksRemaining = 0L;
        this.pendingSummonTicksRemaining = 0L;
        this.pendingSummonRole = null;
    }

    @Override
    public Component<EntityStore> clone() {
        SummonUndeadAbilityComponent c = new SummonUndeadAbilityComponent();
        c.abilityEnabled = this.abilityEnabled;
        c.cooldownTicksRemaining = this.cooldownTicksRemaining;
        c.pendingSummonTicksRemaining = this.pendingSummonTicksRemaining;
        c.pendingSummonRole = this.pendingSummonRole;
        return c;
    }
}
