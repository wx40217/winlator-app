package com.winlator.core;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class RuntimePathRebaserTest {
    private final RuntimePathRebaser rebaser = new RuntimePathRebaser("com.winlator", "com.wxwinlat");

    private byte[] rebase(byte[] data, int chunkSize) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(data) {
            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                return super.read(buffer, offset, Math.min(length, chunkSize));
            }
        };
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        rebaser.copy(input, output);
        return output.toByteArray();
    }

    @Test
    public void pathsAcrossBufferBoundariesKeepBinaryOffsets() throws IOException {
        byte[] source = new byte[140000];
        Arrays.fill(source, (byte)0xFF);
        String[] paths = {"/data/data/com.winlator/files/rootfs/lib", "/data/user/0/com.winlator/cache"};
        int[] offsets = {65530, source.length-70};
        byte[] expected = source.clone();
        for (int i = 0; i < paths.length; i++) {
            byte[] origin = paths[i].getBytes(StandardCharsets.US_ASCII);
            byte[] target = paths[i].replace("com.winlator", "com.wxwinlat").getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(origin, 0, source, offsets[i], origin.length);
            System.arraycopy(target, 0, expected, offsets[i], target.length);
            source[offsets[i]+origin.length] = expected[offsets[i]+origin.length] = 0;
        }
        assertArrayEquals(expected, rebase(source, 65536));
        assertArrayEquals(expected, rebase(source, 3));
    }

    @Test
    public void unrelatedAndAlreadyRebasedBytesAreUnchanged() throws IOException {
        byte[] source = ("com.winlator\0/data/data/com.winlator.extra/files\0"+
                "/data/data/com.wxwinlat/cache\0C:\\Games\\game.exe").getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(source, rebase(source, 1));
        assertArrayEquals(new byte[0], rebase(new byte[0], 1));
    }

    @Test
    public void absoluteLinksFollowForkDataDirectory() {
        assertEquals("/data/data/com.wxwinlat/files/rootfs/lib/libc.so.6",
                rebaser.rebaseLink("/data/data/com.winlator/files/rootfs/lib/libc.so.6"));
        assertEquals("../lib/libc.so.6", rebaser.rebaseLink("../lib/libc.so.6"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void longerPackageRequiresRebuildingGuestBinaries() {
        new RuntimePathRebaser("com.winlator", "com.winlator.fork");
    }

    @Test
    public void archiveEntriesDoNotLeakPendingBytesIntoTheNextFile() throws IOException {
        byte[][] files = {
                "/data/data/com.winlator/files/rootfs/lib\0".getBytes(StandardCharsets.US_ASCII),
                new byte[]{0, -1, 1, 0}
        };
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(archive)) {
            for (int i = 0; i < files.length; i++) {
                TarArchiveEntry entry = new TarArchiveEntry("file-"+i);
                entry.setSize(files[i].length);
                tar.putArchiveEntry(entry);
                tar.write(files[i]);
                tar.closeArchiveEntry();
            }
        }
        try (TarArchiveInputStream tar = new TarArchiveInputStream(new ByteArrayInputStream(archive.toByteArray()))) {
            assertEquals("file-0", tar.getNextTarEntry().getName());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            rebaser.copy(tar, output);
            assertEquals("/data/data/com.wxwinlat/files/rootfs/lib\0", output.toString("US-ASCII"));
            assertEquals("file-1", tar.getNextTarEntry().getName());
            output.reset();
            rebaser.copy(tar, output);
            assertArrayEquals(files[1], output.toByteArray());
            assertNull(tar.getNextTarEntry());
        }
    }
}
