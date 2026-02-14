#!/bin/bash

echo "📦 Voice Assistant - Package for Testing"
echo "========================================"
echo ""

# Create package directory
PACKAGE_DIR="VoiceAssistant_Package"
rm -rf $PACKAGE_DIR
mkdir -p $PACKAGE_DIR

echo "✅ Creating package directory..."

# Copy all source files
cp -r ~/VoiceAssistant/* $PACKAGE_DIR/

# Create installation guide
cat > $PACKAGE_DIR/INSTALL.md << 'EOF'
# 📱 Installation Guide

## Method 1: Android Studio (Recommended)

1. **Install Android Studio**
   - Download from: https://developer.android.com/studio
   - Install and open

2. **Open Project**
   - File → Open
   - Select VoiceAssistant folder
   - Wait for Gradle sync

3. **Connect Phone**
   - Enable USB Debugging on phone:
     - Settings → About Phone
     - Tap "Build Number" 7 times
     - Settings → Developer Options
     - Enable "USB Debugging"
   - Connect via USB
   - Allow USB debugging prompt

4. **Run App**
   - Click green "Run" button
   - Select your device
   - Wait for installation

## Method 2: Pre-built APK (If Available)

1. Transfer APK to phone
2. Settings → Security → Enable "Unknown Sources"
3. Open APK file
4. Tap "Install"
5. Open app and grant permissions

## Method 3: Command Line

```bash
cd VoiceAssistant
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📋 Required Permissions

The app will request:
- ✅ Microphone (voice commands)
- ✅ Phone (make calls)
- ✅ SMS (send messages)
- ✅ Contacts (call by name)
- ✅ Storage (read/write files)
- ✅ Camera (facial recognition)
- ✅ Location (location-based features)

## 🚀 First Run

1. Open app
2. Grant all permissions
3. Say "Hey Assistant"
4. Try: "What time is it?"

## 🔧 Troubleshooting

**App won't install:**
- Enable "Unknown Sources" in settings
- Check Android version (7.0+ required)

**Voice not working:**
- Grant microphone permission
- Check volume is up
- Try saying "Hey Assistant" clearly

**App crashes:**
- Clear app data
- Reinstall
- Check logs: `adb logcat | grep VoiceAssistant`

## 📞 Support

Issues? Check README.md for full documentation.
EOF

# Create quick start guide
cat > $PACKAGE_DIR/QUICKSTART.md << 'EOF'
# ⚡ Quick Start Guide

## 🎯 Basic Commands

### Phone Control
- "Call [name/number]"
- "Open [app name]"
- "Volume up/down"
- "Flashlight on/off"

### Information
- "What time is it?"
- "What day is it?"
- "Battery status"
- "Weather"
- "News"

### Smart Features
- "Remember [anything]"
- "What do you remember?"
- "I'm feeling [emotion]"
- "What should I do?"

### Multi-Language
- "Switch to Spanish"
- "Switch to French"
- "What languages do you support?"

### Media
- "Apply grayscale filter to [image]"
- "Create meme from [image]"
- "Read text from image"
- "Stabilize video"

### Voice Features
- "Register my voice"
- "Verify my voice"
- "Detect my accent"

## 🎨 Try These First!

1. "Hey Assistant, what time is it?"
2. "Remember my favorite color is blue"
3. "What do you remember about my favorite color?"
4. "I'm feeling happy"
5. "Switch to Spanish"
6. "Technology news"

## 💡 Pro Tips

- Say "Conversation mode on" for continuous listening
- Say "Good morning" for weather + news routine
- Say "What should I do?" for smart suggestions
- Enable federated learning to improve AI

## 🔐 Privacy

- All voice processing is offline
- No data sent to servers (unless you enable federated learning)
- You control everything

Enjoy! 🚀
EOF

# Create testing checklist
cat > $PACKAGE_DIR/TEST_CHECKLIST.md << 'EOF'
# ✅ Testing Checklist

## Basic Features
- [ ] App installs successfully
- [ ] App opens without crashing
- [ ] Permissions granted
- [ ] Wake word "Hey Assistant" works
- [ ] Voice recognition working

## Voice Commands
- [ ] "What time is it?" - responds correctly
- [ ] "Call [contact]" - opens dialer
- [ ] "Open WhatsApp" - opens app
- [ ] "Volume up" - increases volume
- [ ] "Flashlight on" - turns on flashlight

## Smart Features
- [ ] "Remember [something]" - saves memory
- [ ] "What do you remember?" - recalls memory
- [ ] Emotional detection working
- [ ] Predictive suggestions appear

## Multi-Language
- [ ] "Switch to Spanish" - changes language
- [ ] Commands work in Spanish
- [ ] Switch back to English works

## Media Features
- [ ] Image filter application works
- [ ] OCR text extraction works
- [ ] Meme generation works
- [ ] Video editing (if tested)

## Voice Biometrics
- [ ] Voice registration works
- [ ] Voice verification works
- [ ] Accent detection works

## Background Running
- [ ] App runs in background
- [ ] Responds when screen is locked
- [ ] Auto-starts after reboot
- [ ] Notification shows

## Performance
- [ ] Response time < 1 second
- [ ] No lag or stuttering
- [ ] Battery usage acceptable
- [ ] Memory usage reasonable

## Issues Found
```
List any bugs or issues here:
1. 
2. 
3. 
```

## Overall Rating
- Functionality: __ / 10
- Performance: __ / 10
- Usability: __ / 10
- Stability: __ / 10

**Total: __ / 40**
EOF

# Create README for package
cat > $PACKAGE_DIR/PACKAGE_README.md << 'EOF'
# 📦 Voice Assistant - Testing Package

This package contains everything you need to test the Voice Assistant on your Android phone.

## 📁 Contents

- `app/` - Source code
- `INSTALL.md` - Installation instructions
- `QUICKSTART.md` - Quick start guide
- `TEST_CHECKLIST.md` - Testing checklist
- `DEMO_SCRIPT.md` - Demo video script
- `README.md` - Full documentation

## 🚀 Quick Install

### Option 1: Android Studio
1. Install Android Studio
2. Open this folder
3. Connect phone
4. Click Run

### Option 2: Command Line
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📱 System Requirements

- Android 7.0 (Nougat) or higher
- 100 MB free storage
- Microphone
- Internet (for online features)

## 🎯 First Test

1. Install app
2. Open and grant permissions
3. Say "Hey Assistant"
4. Say "What time is it?"

If it responds, it's working! ✅

## 📊 What to Test

- Basic voice commands
- Smart features (memory, emotions)
- Multi-language support
- Media editing
- Voice biometrics
- Background running

See TEST_CHECKLIST.md for complete list.

## 🐛 Found a Bug?

Note it in TEST_CHECKLIST.md

## 💡 Tips

- Speak clearly
- Wait for response
- Check permissions if not working
- Restart app if issues occur

Good luck testing! 🚀
EOF

# Create a simple build script
cat > $PACKAGE_DIR/build_apk.sh << 'EOF'
#!/bin/bash

echo "🔨 Building Voice Assistant APK..."
echo ""

# Check for Android SDK
if ! command -v adb &> /dev/null; then
    echo "❌ Android SDK not found!"
    echo "Please install Android Studio or Android SDK"
    echo "Download: https://developer.android.com/studio"
    exit 1
fi

# Build APK
echo "📦 Building debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "📱 APK location:"
    echo "   app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "📲 To install on connected device:"
    echo "   adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
else
    echo ""
    echo "❌ Build failed!"
    echo "Check errors above"
    exit 1
fi
EOF

chmod +x $PACKAGE_DIR/build_apk.sh

# Create transfer script
cat > $PACKAGE_DIR/transfer_to_phone.sh << 'EOF'
#!/bin/bash

echo "📲 Transfer APK to Phone"
echo "========================"
echo ""

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found! Build it first:"
    echo "   ./build_apk.sh"
    exit 1
fi

# Check if device connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No device connected!"
    echo ""
    echo "Connect your phone via USB and enable USB debugging:"
    echo "1. Settings → About Phone"
    echo "2. Tap 'Build Number' 7 times"
    echo "3. Settings → Developer Options"
    echo "4. Enable 'USB Debugging'"
    exit 1
fi

echo "📱 Installing on device..."
adb install -r "$APK_PATH"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Installation successful!"
    echo ""
    echo "🚀 Open the app and say 'Hey Assistant'"
else
    echo ""
    echo "❌ Installation failed!"
    echo ""
    echo "Try:"
    echo "1. Enable 'Unknown Sources' in phone settings"
    echo "2. Uninstall old version first"
    echo "3. Check USB debugging is enabled"
fi
EOF

chmod +x $PACKAGE_DIR/transfer_to_phone.sh

# Compress package
echo "📦 Compressing package..."
tar -czf VoiceAssistant_Package.tar.gz $PACKAGE_DIR

# Create final summary
echo ""
echo "✅ Package created successfully!"
echo ""
echo "📦 Package contents:"
echo "   - Source code"
echo "   - Installation guide"
echo "   - Quick start guide"
echo "   - Testing checklist"
echo "   - Build scripts"
echo ""
echo "📁 Location:"
echo "   Directory: $PACKAGE_DIR/"
echo "   Archive:   VoiceAssistant_Package.tar.gz"
echo ""
echo "📲 Next steps:"
echo ""
echo "Option 1: Use Android Studio (Easiest)"
echo "   1. Install Android Studio"
echo "   2. Open $PACKAGE_DIR folder"
echo "   3. Connect phone and click Run"
echo ""
echo "Option 2: Transfer to phone"
echo "   1. Copy VoiceAssistant_Package.tar.gz to computer with Android Studio"
echo "   2. Extract and open in Android Studio"
echo "   3. Build and install"
echo ""
echo "Option 3: Build on this machine (requires Android SDK)"
echo "   cd $PACKAGE_DIR"
echo "   ./build_apk.sh"
echo "   ./transfer_to_phone.sh"
echo ""
echo "📖 Read INSTALL.md for detailed instructions"
echo ""
echo "🚀 Ready for testing!"
