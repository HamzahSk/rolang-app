Please update the Android build configuration and GitHub Actions workflow in this project with the following requirements:

1. **APK Signing:** Add a `signingConfigs` block in `app/build.gradle.kts` that reads from `keystore.properties` (with safe fallback if the file is absent for local builds) and applies the signing configuration to the release build.
2. **ABI Splits:** Configure `splits.abi` in `app/build.gradle.kts` to generate separate APKs for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` (disable universal APK if unneeded).
3. **Update GitHub Actions Workflow:** Update `.github/workflows/android-build.yml`[span_0](start_span)[span_0](end_span) to build the signed release APKs, upload all generated ABI-split APKs as build artifacts, and include the following keystore setup steps before the Gradle build:

      - name: Decode Keystore
        env:
          ENCODED_STRING: ${{ secrets.SIGNING_KEY }}
        run: |
          echo $ENCODED_STRING | base64 -d > keystore.jks

      - name: Create keystore.properties
        env:
          STORE_PASSWORD: ${{ secrets.KEY_STORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          echo "storeFile=keystore.jks" > keystore.properties
          echo "storePassword=$STORE_PASSWORD" >> keystore.properties
          echo "keyAlias=$KEY_ALIAS" >> keystore.properties
          echo "keyPassword=$KEY_PASSWORD" >> keystore.properties