package com.winlator.library;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.Map;

/** Library-only metadata; never modifies Wine or shortcut configuration. */
public final class GameLibraryStore {
    private final SharedPreferences preferences;

    public GameLibraryStore(Context context) {
        this(context.getSharedPreferences("game_library", Context.MODE_PRIVATE));
    }

    public GameLibraryStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public static String identity(int containerId, File file) {
        return containerId + ":" + file.getAbsolutePath();
    }

    public boolean isFavorite(int containerId, File file) {
        return preferences.getBoolean("favorite:" + identity(containerId, file), false);
    }

    public void toggleFavorite(int containerId, File file) {
        String key = "favorite:" + identity(containerId, file);
        preferences.edit().putBoolean(key, !preferences.getBoolean(key, false)).apply();
    }

    public long lastLaunch(int containerId, File file) {
        return preferences.getLong("launch:" + identity(containerId, file), 0);
    }

    public boolean recordLaunch(int containerId, File file, long timestamp) {
        // The runtime exits the application process, so this must reach disk before launch.
        return preferences.edit().putLong("launch:" + identity(containerId, file), timestamp).commit();
    }

    public void remove(int containerId, File file) {
        updateLocation(identity(containerId, file), null);
    }

    public void move(int oldContainerId, File origin, int newContainerId, File target) {
        updateLocation(identity(oldContainerId, origin), identity(newContainerId, target));
    }

    private void updateLocation(String oldIdentity, String newIdentity) {
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            String key = entry.getKey();
            String prefix = key.startsWith("favorite:") ? "favorite:" : key.startsWith("launch:") ? "launch:" : null;
            if (prefix == null) continue;
            String identity = key.substring(prefix.length());
            if (!identity.equals(oldIdentity) && !identity.startsWith(oldIdentity + File.separator)) continue;
            editor.remove(key);
            if (newIdentity != null) {
                String newKey = prefix + newIdentity + identity.substring(oldIdentity.length());
                if (entry.getValue() instanceof Boolean) editor.putBoolean(newKey, (Boolean)entry.getValue());
                else if (entry.getValue() instanceof Long) editor.putLong(newKey, (Long)entry.getValue());
            }
        }
        editor.apply();
    }
}
