package com.shiroha.mmdskin.stage.client.playback;

import com.shiroha.mmdskin.stage.domain.model.StageDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageLocalPlaybackPreferencesTest {

    private final StageLocalPlaybackPreferences preferences = StageLocalPlaybackPreferences.getInstance();

    @AfterEach
    void tearDown() {
        preferences.reset();
    }

    @Test
    void shouldKeepHostDescriptorWhenCustomMotionDisabled(@TempDir Path tempDir) throws IOException {
        new File(tempDir.toFile(), "host_dance.vmd").createNewFile();
        StageDescriptor descriptor = new StageDescriptor("demo_pack", List.of("host_dance.vmd"), "camera.vmd", "music.ogg");

        StageDescriptor resolved = preferences.resolveDescriptor(descriptor, tempDir.toFile());

        assertEquals(List.of("host_dance.vmd"), resolved.getMotionFiles());
    }

    @Test
    void shouldApplyLocalCustomMotionSelection(@TempDir Path tempDir) throws IOException {
        new File(tempDir.toFile(), "self_a.vmd").createNewFile();
        new File(tempDir.toFile(), "self_b.vmd").createNewFile();
        StageDescriptor descriptor = new StageDescriptor("demo_pack", List.of("host_dance.vmd"), "camera.vmd", "music.ogg");

        preferences.setCustomMotionEnabled(true);
        preferences.toggleMotionFile("self_a.vmd");
        preferences.toggleMotionFile("self_b.vmd");

        StageDescriptor resolved = preferences.resolveDescriptor(descriptor, tempDir.toFile());

        assertEquals(List.of("self_a.vmd", "self_b.vmd"), resolved.getMotionFiles());
        assertTrue(preferences.isSelected("self_a.vmd"));
    }
}
