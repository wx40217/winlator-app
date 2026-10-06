package com.winlator.library;

import android.content.SharedPreferences;
import org.junit.Test;
import java.io.File;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class GameLibraryStoreTest {
    private static SharedPreferences memoryPreferences(Map<String, Object> values) {
        return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(), new Class[]{SharedPreferences.class}, (proxy, method, args) -> {
            if (method.getName().equals("edit")) {
                Map<String, Object> updates = new HashMap<>();
                return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(), new Class[]{SharedPreferences.Editor.class}, (editor, editMethod, editArgs) -> {
                    String operation = editMethod.getName();
                    if (operation.startsWith("put")) updates.put((String)editArgs[0], editArgs[1]);
                    else if (operation.equals("remove")) updates.put((String)editArgs[0], null);
                    else if (operation.equals("apply") || operation.equals("commit")) {
                        updates.forEach((key, value) -> { if (value == null) values.remove(key); else values.put(key, value); });
                        return operation.equals("commit") ? true : null;
                    }
                    return editor;
                });
            }
            if (method.getName().equals("getAll")) return new HashMap<>(values);
            if (method.getName().startsWith("get")) return values.getOrDefault((String)args[0], args[1]);
            throw new UnsupportedOperationException(method.getName());
        });
    }

    @Test public void identicalPathsInDifferentContainersDoNotShareState() {
        GameLibraryStore store = new GameLibraryStore(memoryPreferences(new HashMap<>()));
        File game = new File("/desktop/Game.desktop");
        store.toggleFavorite(1, game);
        store.recordLaunch(1, game, 42);
        assertTrue(store.isFavorite(1, game));
        assertFalse(store.isFavorite(2, game));
        assertEquals(0, store.lastLaunch(2, game));
    }

    @Test public void renamePreservesFavoriteAndHistoryButDoesNotAffectOtherItems() {
        GameLibraryStore store = new GameLibraryStore(memoryPreferences(new HashMap<>()));
        File original = new File("/desktop/Game.desktop"), renamed = new File("/desktop/Renamed.desktop");
        File other = new File("/desktop/Other.desktop");
        store.toggleFavorite(1, original);
        store.recordLaunch(1, original, 100);
        store.recordLaunch(1, other, 200);
        store.move(1, original, 1, renamed);
        assertFalse(store.isFavorite(1, original));
        assertTrue(store.isFavorite(1, renamed));
        assertEquals(100, store.lastLaunch(1, renamed));
        assertEquals(0, store.lastLaunch(1, original));
        assertEquals(200, store.lastLaunch(1, other));
    }

    @Test public void movingDirectoryAcrossContainersMigratesDescendantsOnly() {
        GameLibraryStore store = new GameLibraryStore(memoryPreferences(new HashMap<>()));
        File nested = new File("/desktop/Folder/nested/Game.desktop");
        File sibling = new File("/desktop/Folder-more/Game.desktop");
        store.toggleFavorite(1, nested);
        store.recordLaunch(1, nested, 70);
        store.toggleFavorite(1, sibling);
        store.move(1, new File("/desktop/Folder"), 2, new File("/other/Moved"));
        assertTrue(store.isFavorite(2, new File("/other/Moved/nested/Game.desktop")));
        assertEquals(70, store.lastLaunch(2, new File("/other/Moved/nested/Game.desktop")));
        assertFalse(store.isFavorite(1, nested));
        assertTrue(store.isFavorite(1, sibling));
    }

    @Test public void copyHasIndependentStateAndDeletionDoesNotRemoveItsSource() {
        GameLibraryStore store = new GameLibraryStore(memoryPreferences(new HashMap<>()));
        File source = new File("/desktop/Game.desktop"), copy = new File("/desktop/Copy.desktop");
        store.toggleFavorite(1, source);
        store.recordLaunch(1, source, 9);
        assertFalse(store.isFavorite(1, copy));
        assertEquals(0, store.lastLaunch(1, copy));
        store.toggleFavorite(1, copy);
        store.remove(1, copy);
        assertTrue(store.isFavorite(1, source));
        assertEquals(9, store.lastLaunch(1, source));
    }

    @Test public void deletedContainerCannotLeakStateWhenItsIdIsReused() {
        GameLibraryStore store = new GameLibraryStore(memoryPreferences(new HashMap<>()));
        File game = new File("/containers/1/Desktop/Game.desktop");
        store.toggleFavorite(1, game);
        store.recordLaunch(1, game, 10);
        store.remove(1, new File("/containers/1"));
        assertFalse(store.isFavorite(1, game));
        assertEquals(0, store.lastLaunch(1, game));
    }
}
