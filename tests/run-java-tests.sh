#!/usr/bin/env bash
set -euo pipefail
rumo_test_dir="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/rumo-java-tests"
mkdir -p "$rumo_test_dir"
curl --fail --silent --show-error --location --retry 2 \
  https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar \
  --output "$rumo_test_dir/json.jar"
printf '%s  %s\n' 3ea61b2a06e31edf1c91134fe9106b0ebb16628be169f3db75bc7a2b06b45796 "$rumo_test_dir/json.jar" | sha256sum --check --status
javac -cp "$rumo_test_dir/json.jar" -d "$rumo_test_dir" \
  app/src/main/java/br/com/rumo/diario/LicenseVerifier.java tests/LicenseVerifierTest.java \
  app/src/main/java/br/com/rumo/diario/VaultCrypto.java tests/VaultCryptoTest.java
java -cp "$rumo_test_dir:$rumo_test_dir/json.jar" br.com.rumo.diario.LicenseVerifierTest
java -cp "$rumo_test_dir" br.com.rumo.diario.VaultCryptoTest
