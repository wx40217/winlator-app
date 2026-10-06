package com.winlator.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Rebase upstream private paths while preserving precompiled ELF byte offsets. */
final class RuntimePathRebaser {
    private final byte[][] origins;
    private final byte[][] targets;
    private final String[] originPaths;
    private final String[] targetPaths;
    private final int overlap;

    RuntimePathRebaser(String originPackage, String targetPackage) {
        if (originPackage.getBytes(StandardCharsets.US_ASCII).length !=
                targetPackage.getBytes(StandardCharsets.US_ASCII).length) {
            throw new IllegalArgumentException("Runtime package paths must have equal byte lengths");
        }
        originPaths = new String[]{"/data/data/"+originPackage+"/", "/data/user/0/"+originPackage+"/"};
        targetPaths = new String[]{"/data/data/"+targetPackage+"/", "/data/user/0/"+targetPackage+"/"};
        origins = new byte[originPaths.length][];
        targets = new byte[targetPaths.length][];
        int longest = 0;
        for (int i = 0; i < origins.length; i++) {
            origins[i] = originPaths[i].getBytes(StandardCharsets.US_ASCII);
            targets[i] = targetPaths[i].getBytes(StandardCharsets.US_ASCII);
            longest = Math.max(longest, origins[i].length);
        }
        overlap = longest-1;
    }

    String rebaseLink(String path) {
        for (int i = 0; i < originPaths.length; i++) path = path.replace(originPaths[i], targetPaths[i]);
        return path;
    }

    void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[65536+overlap];
        int pending = 0;
        int read;
        while ((read = input.read(buffer, pending, 65536)) != -1) {
            if (read == 0) continue;
            int total = pending+read;
            replacePaths(buffer, total);
            int written = Math.max(0, total-overlap);
            output.write(buffer, 0, written);
            pending = total-written;
            System.arraycopy(buffer, written, buffer, 0, pending);
        }
        replacePaths(buffer, pending);
        output.write(buffer, 0, pending);
    }

    private void replacePaths(byte[] buffer, int length) {
        for (int offset = 0; offset < length; offset++) {
            if (buffer[offset] != '/') continue;
            for (int path = 0; path < origins.length; path++) {
                byte[] origin = origins[path];
                if (offset+origin.length > length) continue;
                int matched = 0;
                while (matched < origin.length && buffer[offset+matched] == origin[matched]) matched++;
                if (matched == origin.length) {
                    System.arraycopy(targets[path], 0, buffer, offset, origin.length);
                    offset += origin.length-1;
                    break;
                }
            }
        }
    }
}
