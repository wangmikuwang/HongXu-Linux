#!/usr/bin/env python3
"""List every Maven file a build downloaded as Flatpak sources, so Flathub can build offline.

  GRADLE_USER_HOME=$(mktemp -d) ./gradlew --no-daemon packageUberJarForCurrentOS
  python3 packaging/flatpak/gen-maven-sources.py "$GRADLE_USER_HOME" > packaging/flatpak/maven-sources.json

Files are placed as a Maven repository under maven/ (settings.gradle.kts reads OFFLINE_MAVEN_REPO).
"""
import concurrent.futures
import hashlib
import json
import os
import sys
import urllib.error
import urllib.request

REPOS = ("https://repo.maven.apache.org/maven2", "https://dl.google.com/dl/android/maven2", "https://plugins.gradle.org/m2")


def exists(url):
    try:
        with urllib.request.urlopen(urllib.request.Request(url, method="HEAD"), timeout=30) as r:
            return r.status == 200
    except (urllib.error.URLError, TimeoutError):
        return False


def entry(path):
    parts = path.split(os.sep)
    group, module, version, name = parts[-5], parts[-4], parts[-3], parts[-1]
    rel = f"{group.replace('.', '/')}/{module}/{version}"
    url = next((f"{repo}/{rel}/{name}" for repo in REPOS if exists(f"{repo}/{rel}/{name}")), None)
    if url is None:
        raise SystemExit(f"no public repository has {rel}/{name}")
    # Hash the public file itself: a mirror may have served Gradle a different copy (e.g. a plugin-portal pom).
    with urllib.request.urlopen(url, timeout=120) as r:
        sha = hashlib.sha256(r.read()).hexdigest()
    return {"type": "file", "url": url, "sha256": sha, "dest": f"maven/{rel}", "dest-filename": name}


def main():
    root = os.path.join(sys.argv[1], "caches", "modules-2", "files-2.1")
    files = sorted(os.path.join(d, f) for d, _, fs in os.walk(root) for f in fs)
    with concurrent.futures.ThreadPoolExecutor(16) as pool:
        sources = list(pool.map(entry, files))
    json.dump(sources, sys.stdout, indent=2)
    print()


if __name__ == "__main__":
    main()
