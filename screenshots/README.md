# 📸 Screenshots

Add your app screenshots here to display in the main README.

## Required Screenshots

Please add the following screenshots (recommended size: 1080x2400):

### 1. `splash.png`
- Splash screen with app logo

### 2. `auth.png`
- Authentication screen (Login/Register)

### 3. `home.png`
- Home screen showing user cards and explore section

### 4. `create.png`
- Create card screen with customization options

### 5. `detail.png`
- Card detail view

### 6. `profile.png`
- User profile screen

## How to Add Screenshots

### Option 1: From Android Studio

1. Run the app on emulator/device
2. Navigate to the screen you want to capture
3. Click the camera icon in Android Studio
4. Save screenshot to this directory

### Option 2: Using ADB

```bash
# Take screenshot
adb shell screencap -p /sdcard/screenshot.png

# Pull to computer
adb pull /sdcard/screenshot.png screenshots/splash.png
```

### Option 3: From Device

1. Take screenshot on device (Power + Volume Down)
2. Transfer to computer
3. Copy to this directory

## Image Guidelines

- **Format:** PNG (preferred) or JPEG
- **Size:** 1080x2400 (or similar aspect ratio)
- **Quality:** High resolution, no compression artifacts
- **Content:** 
  - Use real/demo data, not lorem ipsum
  - Show app in light mode (or provide both light/dark)
  - Ensure no sensitive information is visible

## Naming Convention

Use lowercase with descriptive names:
- `splash.png` - Splash screen
- `auth.png` - Authentication
- `home.png` - Home screen
- `create.png` - Create card
- `detail.png` - Card detail
- `profile.png` - Profile
- `explore.png` - Explore cards
- `settings.png` - Settings

## Optional: Create a Showcase

You can create a combined showcase image using tools like:
- [Figma](https://www.figma.com/)
- [MockuPhone](https://mockuphone.com/)
- [Shotbot](https://shotbot.io/)
- [App Mockup](https://app-mockup.com/)

Example:
```
screenshots/
├── splash.png
├── auth.png
├── home.png
├── create.png
├── detail.png
├── profile.png
└── showcase.png  (Combined image)
```

Then update main README.md to use `showcase.png` instead of individual screenshots.

