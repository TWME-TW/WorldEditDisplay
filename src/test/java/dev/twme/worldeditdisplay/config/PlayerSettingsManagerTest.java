package dev.twme.worldeditdisplay.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerSettingsManagerTest {

    @Test
    void saveAllDirtyKeepsDirtyFlagWhenSaveFails() throws Exception {
        PlayerSettingsManager manager = new PlayerSettingsManager(null);
        UUID playerId = UUID.randomUUID();
        TestSettings settings = new TestSettings(false);
        settings.setDirty(true);

        manager.getSettingsCache().put(playerId, settings);

        manager.saveAllDirty();

        assertTrue(settings.isDirty());
    }

    @Test
    void saveAllDirtyClearsDirtyFlagWhenSaveSucceeds() throws Exception {
        PlayerSettingsManager manager = new PlayerSettingsManager(null);
        UUID playerId = UUID.randomUUID();
        TestSettings settings = new TestSettings(true);
        settings.setDirty(true);

        manager.getSettingsCache().put(playerId, settings);

        manager.saveAllDirty();

        assertFalse(settings.isDirty());
    }

    @Test
    void saveAndUnloadKeepsDirtySettingsCachedWhenSaveFails() throws Exception {
        PlayerSettingsManager manager = new PlayerSettingsManager(null);
        UUID playerId = UUID.randomUUID();
        TestSettings settings = new TestSettings(false);
        settings.setDirty(true);

        manager.getSettingsCache().put(playerId, settings);

        manager.saveAndUnload(playerId);

        assertTrue(settings.isDirty());
        assertTrue(manager.getSettingsCache().containsKey(playerId));
    }

    @Test
    void saveAndUnloadRemovesSettingsWhenSaveSucceeds() throws Exception {
        PlayerSettingsManager manager = new PlayerSettingsManager(null);
        UUID playerId = UUID.randomUUID();
        TestSettings settings = new TestSettings(true);
        settings.setDirty(true);

        manager.getSettingsCache().put(playerId, settings);

        manager.saveAndUnload(playerId);

        assertFalse(settings.isDirty());
        assertFalse(manager.getSettingsCache().containsKey(playerId));
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