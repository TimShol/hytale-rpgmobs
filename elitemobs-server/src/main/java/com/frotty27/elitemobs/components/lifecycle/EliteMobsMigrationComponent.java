package com.frotty27.elitemobs.components.lifecycle;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EliteMobsMigrationComponent implements Component<EntityStore> {

    public int migrationVersion;

    private static final KeyedCodec<Integer> K_MIGRATION_VERSION =
            new KeyedCodec<>("MigrationVersion", new IntegerCodec());

    public static final BuilderCodec<EliteMobsMigrationComponent> CODEC =
            BuilderCodec.builder(EliteMobsMigrationComponent.class, EliteMobsMigrationComponent::new)
                    .append(K_MIGRATION_VERSION, (c, v) -> c.migrationVersion = v, c -> c.migrationVersion).add()
                    .build();

    public EliteMobsMigrationComponent() {
        this.migrationVersion = 0;
    }

    public EliteMobsMigrationComponent(int version) {
        this.migrationVersion = version;
    }

    @Override
    public Component<EntityStore> clone() {
        EliteMobsMigrationComponent c = new EliteMobsMigrationComponent();
        c.migrationVersion = this.migrationVersion;
        return c;
    }

    public boolean needsMigration() {
        return migrationVersion < 2;
    }
}
