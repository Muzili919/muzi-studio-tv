#!/bin/sh
# Stage the family-launcher release onto the existing zhishixingqiu.top/tv channel.
# Does not open a new site. Does not touch the fongmi key in version.json.
#
# Local layout (this repo, same files the nginx alias serves):
#   deploy/www/version.json          ->  /var/www/muzi-tv/version.json
#   deploy/www/apk/muzi-studio-*.apk ->  /var/www/muzi-tv/apk/muzi-studio-*.apk
# Public URLs:
#   https://zhishixingqiu.top/tv/version.json
#   https://zhishixingqiu.top/tv/apk/muzi-studio-<versionName>.apk
# Mac working copy used by the existing host (if that is where you publish from):
#   ~/Projects/muzi-tv/public-tv/www/version.json
#   ~/Projects/muzi-tv/public-tv/www/apk/
#
# Usage:
#   deploy/sync-tv.sh android/app/build/outputs/apk/release/app-release.apk
#   SSH_TARGET=user@zhishixingqiu-host deploy/sync-tv.sh app-release.apk
#
# Remote publish (only when SSH_TARGET is set and you already have a key):
#   scp the apk to     $SSH_TARGET:/var/www/muzi-tv/apk/
#   scp version.json to $SSH_TARGET:/var/www/muzi-tv/version.json
# nginx snippet deploy/server/muzi-tv.locations maps /tv/ onto /var/www/muzi-tv/.
# /tv/ directory listing returns 403; version.json and the concrete apk URL return 200.
set -eu
cd "$(dirname "$0")/.."
APK="${1:-android/app/build/outputs/apk/release/app-release.apk}"
if [ ! -f "$APK" ]; then
  echo "APK not found: $APK" >&2
  exit 1
fi
python3 - "$APK" <<'PY'
import hashlib, json, re, shutil, sys
from pathlib import Path
apk = Path(sys.argv[1]).resolve()
root = Path(".").resolve()
gradle = (root / "android/app/build.gradle").read_text()
code = int(re.search(r"versionCode\s+(\d+)", gradle).group(1))
name = re.search(r"versionName\s+['\"]([^'\"]+)['\"]", gradle).group(1)
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
dest_name = "muzi-studio-%s.apk" % name
dest_dir = root / "deploy/www/apk"
dest_dir.mkdir(parents=True, exist_ok=True)
dest = dest_dir / dest_name
shutil.copy2(apk, dest)
url = "https://zhishixingqiu.top/tv/apk/" + dest_name
manifest_path = root / "deploy/www/version.json"
manifest = json.loads(manifest_path.read_text())
studio = manifest.get("studio") or {}
studio["package"] = "studio.muzi.tv"
studio["versionCode"] = code
studio["versionName"] = name
studio["url"] = url
studio["sha256"] = digest
manifest["studio"] = studio
manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
print("staged", dest)
print("versionCode", code)
print("versionName", name)
print("url", url)
print("sha256", digest)
print("manifest", manifest_path)
PY
echo "Staged under deploy/www/. APK is gitignored; commit deploy/www/version.json."
if [ -n "${SSH_TARGET:-}" ]; then
  NAME=$(python3 -c 'import re;print(re.search(r"versionName\s+[\x27\"]([^\x27\"]+)[\x27\"]",open("android/app/build.gradle").read()).group(1))')
  ssh "$SSH_TARGET" "mkdir -p /var/www/muzi-tv/apk"
  scp "deploy/www/apk/muzi-studio-${NAME}.apk" "$SSH_TARGET:/var/www/muzi-tv/apk/muzi-studio-${NAME}.apk"
  scp deploy/www/version.json "$SSH_TARGET:/var/www/muzi-tv/version.json"
  echo "Uploaded to $SSH_TARGET:/var/www/muzi-tv/"
else
  echo "No SSH_TARGET. This environment has no deploy key."
  echo "Copy deploy/www/version.json and deploy/www/apk/muzi-studio-*.apk"
  echo "into the machine that already publishes public-tv/www, then sync that tree to /var/www/muzi-tv/."
fi
