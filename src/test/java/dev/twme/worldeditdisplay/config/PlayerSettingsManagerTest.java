package dev.twme.worldeditdisplay.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerSettingsManagerTest {

    @Test
    void saveAllDirtyKeepsDirtyFlagWhenSaveFails() throws Exception {
        PlayerSettingsManager manager = new PlayerSettingsManager(null);
        UUID playerId = UUID.randomUUID();
        TestSettings settings = new TestSettings(false);
        settings.setDirty(true);

        settingsCache(manager).put(playerId, settings);

        manager.saveAllDirty();

        assertTrue(settings.isDirty());
    }

    @Test
    void saveAllDirtyClearsDirtyFlagWhenSaveSucceeds() throws Exception {
        PlayerSettingsManager manager = new PlayerSettingsManager(null);
        UUID playerId = UUID.randomUUID();
        TestSettings settings = new TestSettings(true);
        settings.setDirty(true);

        settingsCache(manager).put(playerId, settings);

        manager.saveAllDirty();

        assertFalse(settings.isDirty());
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, PlayerRenderSettings> settingsCache(PlayerSettingsManager manager) throws Exception {
        Field field = PlayerSettingsManager.class.getDeclaredField("settingsCache");
        field.setAccessible(true);
        return (Map<UUID, PlayerRenderSettings>) field.get(manager);
    }

    private static final class TestSettings extends PlayerRenderSettings {
        private final boolean saveResult;
        private boolean dirty;

        private TestSettings(boolean saveResult) {
            super(null);
            this.saveResult = saveResult;
        }

        @Override
        public boolean save() {
            return saveResult;
        }

        @Override
        public synchronized boolean isDirty() {
            return dirty;
        }

        @Override
        public synchronized void markClean() {
            dirty = false;
        }

        private void setDirty(boolean dirty) {
            this.dirty = dirty;
        }
    }
}