#!/bin/bash

echo "=== Voice Assistant Build Script ==="

cd ~/VoiceAssistant

# Download Vosk model
echo "Downloading offline speech model..."
mkdir -p app/src/main/assets
cd app/src/main/assets
wget -q https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
unzip -q vosk-model-small-en-us-0.15.zip
rm vosk-model-small-en-us-0.15.zip
cd ~/VoiceAssistant

# Create build files
echo "Creating build configuration..."

cat > settings.gradle << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "VoiceAssistant"
include ':app'
EOF

cat > build.gradle << 'EOF'
plugins {
    id 'com.android.application' version '8.1.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.0' apply false
}
EOF

# Build APK
echo "Building APK..."
./gradlew assembleDebug

if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
    echo ""
    echo "=== Build Complete ==="
    echo "APK location: ~/VoiceAssistant/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "To install on phone:"
    echo "1. Enable USB debugging on phone"
    echo "2. Connect phone via USB"
    echo "3. Run: adb install app/build/outputs/apk/debug/app-debug.apk"
else
    echo "Build failed. Check errors above."
fi
