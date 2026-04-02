package com.shiroha.mmdskin.stage.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageDescriptorTest {

    @Test
    void shouldCopyDescriptor() {
        StageDescriptor descriptor = new StageDescriptor(
                "demo_pack",
                List.of("dance_a.vmd", "dance_b.vmd"),
                "camera.vmd",
                "music.ogg"
        );

        StageDescriptor copy = descriptor.copy();

        assertTrue(copy.isValid());
        assertEquals("demo_pack", copy.getPackName());
        assertEquals(List.of("dance_a.vmd", "dance_b.vmd"), copy.getMotionFiles());
        assertEquals("camera.vmd", copy.getCameraFile());
        assertEquals("music.ogg", copy.getAudioFile());
    }

    @Test
    void shouldResolveAudioPath(@TempDir Path tempDir) throws IOException {
        StageDescriptor descriptor = new StageDescriptor(
                "demo_pack",
                List.of("dance_a.vmd"),
                "camera.vmd",
                "music.ogg"
        );

        assertEquals(tempDir.resolve("music.ogg").toFile().getAbsolutePath(), descriptor.resolveAudioPath(tempDir.toFile()));
    }

    @Test
    void shouldRejectUnsafeDescriptor() {
        StageDescriptor descriptor = new StageDescriptor("../pack", List.of("dance.vmd"), null, null);

        assertFalse(descriptor.isValid());
    }

    @Test
    void shouldRejectDescriptorWithoutMotion() {
        StageDescriptor descriptor = new StageDescriptor("demo_pack", List.of(), "camera.vmd", null);
        assertFalse(descriptor.isValid());
    }
}
