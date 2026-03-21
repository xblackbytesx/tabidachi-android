#!/usr/bin/env bash
set -euo pipefail
source .env
ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
BUILD_TOOLS="${ANDROID_HOME}/build-tools/$(ls "${ANDROID_HOME}/build-tools/" | sort -V | tail -1)"
INPUT="output/tabidachi-release-unsigned.apk"
ALIGNED="output/tabidachi-release-aligned.apk"
OUTPUT="output/tabidachi-signed.apk"

"${BUILD_TOOLS}/zipalign" -f 4 "${INPUT}" "${ALIGNED}"
"${BUILD_TOOLS}/apksigner" sign \
  --ks "keystore/tabidachi.jks" \
  --ks-key-alias "${KEY_ALIAS}" \
  --ks-pass "pass:${KEYSTORE_PASSWORD}" \
  --key-pass "pass:${KEY_PASSWORD}" \
  --out "${OUTPUT}" \
  "${ALIGNED}"
rm -f "${ALIGNED}"
echo "Signed APK: ${OUTPUT}"
