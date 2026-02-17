package com.frotty27.elitemobs.config;

import com.frotty27.elitemobs.config.schema.YamlSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YamlSerializerTest {

    @Test
    void fixedArraySizePadsAndTruncatesAndClampsNumbers(@TempDir Path tempDir) throws IOException {
        Path mainYaml = tempDir.resolve("core.yml");
        Path visualsYaml = tempDir.resolve("visuals.yml");

        String mainText = """
                Spawning:
                  spawnChancePerTier: [0.9, 0.1]
                Gear:
                  spawnGearDurabilityMin: 2.0
                  spawnGearDurabilityMax: -1.0
                """;

        String visualsText = """
                Model:
                  modelScaleMultiplierPerTier: [0.5, 0.6, 0.7, 0.8, 0.9, 1.0]
                """;

        Files.writeString(mainYaml, mainText, StandardCharsets.UTF_8);
        Files.writeString(visualsYaml, visualsText, StandardCharsets.UTF_8);

        EliteMobsConfig cfg = YamlSerializer.loadOrCreate(tempDir, new EliteMobsConfig());

        assertNotNull(cfg.spawning.spawnChancePerTier);
        assertTrue(cfg.spawning.spawnChancePerTier.length == 5);
        for (double chance : cfg.spawning.spawnChancePerTier) {
            assertTrue(chance >= 0.0);
        }

        assertNotNull(cfg.modelConfig.mobModelScaleMultiplierPerTier);
        assertTrue(cfg.modelConfig.mobModelScaleMultiplierPerTier.length == 5);
        for (float scale : cfg.modelConfig.mobModelScaleMultiplierPerTier) {
            assertTrue(scale >= 0.0f);
        }

        assertTrue(cfg.gearConfig.spawnGearDurabilityMin >= 0.0);
        assertTrue(cfg.gearConfig.spawnGearDurabilityMax >= 0.0);
    }
}
