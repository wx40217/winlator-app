#!/usr/bin/env python3
"""Cloud APK integrity and fork invariants; deliberately excludes device/game claims."""
import os
from pathlib import Path
import struct
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

app = Path(sys.argv[1]).resolve()
config = (app / 'app/build.gradle').read_text()
assert "'com.wxwinlat'" in config, 'Fork application ID missing'
assert 'RuntimePathRebaser' in (app / 'app/src/main/java/com/winlator/core/TarCompressorUtils.java').read_text()
assert 'GameLibraryStore' in (app / 'app/src/main/java/com/winlator/ShortcutsFragment.java').read_text()
assert 'WINLATOR_APPLICATION_ID' in (app / 'app/src/main/cpp/CMakeLists.txt').read_text()
subprocess.run(['bash', 'gradlew', '--no-daemon', '--max-workers=4',
                '-Dorg.gradle.jvmargs=-Xmx4g', '--console=plain',
                ':app:assembleDebug', ':app:testDebugUnitTest'], cwd=app, check=True)
results = list((app / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
assert results, 'Unit tests must not be NO-SOURCE'
suites = [ET.parse(p).getroot() for p in results]
assert sum(int(s.attrib['tests']) for s in suites) >= 10, 'Expected at least 10 tests'
assert all(all(int(s.attrib.get(k, 0)) == 0 for k in ('failures', 'errors', 'skipped')) for s in suites)
assert {'com.winlator.core.RuntimePathRebaserTest', 'com.winlator.library.GameLibraryStoreTest'} <= {s.attrib['name'] for s in suites}
apk = app / 'app/build/outputs/apk/debug/app-debug.apk'
tools = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT']) / 'build-tools/34.0.0'
subprocess.run([str(tools / 'apksigner'), 'verify', '--verbose', str(apk)], check=True)
badging = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(apk)], text=True)
assert "package: name='com.wxwinlat'" in badging
assert "native-code: 'arm64-v8a'" in badging
assert "application-label:'Winlator Fork'" in badging
with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None
    names = archive.namelist()
    assert {'classes.dex', 'assets/rootfs.tzst', 'assets/rootfs_patches.tzst',
            'res/layout/game_library_fragment.xml'} <= set(names)
    libs = [n for n in names if n.startswith('lib/') and n.endswith('.so')]
    assert len(libs) >= 35
    for name in libs:
        header = archive.read(name)[:20]
        assert name.startswith('lib/arm64-v8a/')
        assert header[:4] == b'\x7fELF' and header[4] == 2
        assert struct.unpack('<H', header[18:20])[0] == 183
print('PASS: fork invariants, debug APK, unit tests, signature, ZIP, DEX, RootFS, ARM64 ELF')
