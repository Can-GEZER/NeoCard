# CI/CD Setup Guide for NeoCard

## 🚀 Quick Start

The CI/CD workflows are now set up! Here's what you need to do:

### 1. Verify Gradle Wrapper Files ✅

Make sure these files exist in your repo:
```
gradle/
  wrapper/
    gradle-wrapper.jar      ⚠️ Required for CI
    gradle-wrapper.properties ✅ Present
gradlew                     ✅ Should be executable
gradlew.bat                 ✅ For Windows
```

If `gradle-wrapper.jar` is missing, regenerate it:
```bash
gradle wrapper --gradle-version 8.2
```

### 2. Make gradlew Executable (Linux/macOS)

```bash
git update-index --chmod=+x gradlew
git commit -m "Make gradlew executable"
git push
```

### 3. First Push to Trigger CI

```bash
git add .
git commit -m "Add GitHub Actions workflows"
git push origin master
```

The CI workflow will automatically run on push!

---

## 📋 Workflows Overview

### ✅ **android-ci.yml** - Main CI
**Status:** Active on push/PR  
**What it does:**
- Builds debug APK
- Runs unit tests
- Runs lint checks
- Uploads artifacts

**First run:** Will trigger on next push

---

### 🚀 **release.yml** - Release Builds
**Status:** Ready (requires secrets)  
**What it does:**
- Builds signed release APK/AAB
- Creates GitHub releases
- Uploads to GitHub Releases

**To activate:**
1. Generate or use existing keystore
2. Add GitHub Secrets (see below)
3. Push a tag: `git tag v1.0.0 && git push --tags`

---

### 🔍 **code-quality.yml** - Quality Checks
**Status:** Active (optional tools)  
**What it does:**
- Detekt static analysis
- Dependency updates check
- Test coverage reports

**Optional setup:** Add Codecov token for coverage

---

## 🔐 Setting Up Release Workflow (Optional)

### Step 1: Encode Your Keystore

```bash
# On Linux/macOS
base64 -i keystore.jks | tr -d '\n' > keystore_base64.txt

# On Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.jks")) | Out-File keystore_base64.txt
```

### Step 2: Add GitHub Secrets

1. Go to your repo → Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Add these secrets:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `KEYSTORE_BASE64` | Content of keystore_base64.txt | Base64 encoded keystore |
| `KEYSTORE_PASSWORD` | Your keystore password | Password for the keystore |
| `KEY_ALIAS` | Your key alias | Alias used in keystore |
| `KEY_PASSWORD` | Your key password | Password for the key |

### Step 3: Update build.gradle.kts (if needed)

Make sure your `app/build.gradle.kts` has signing configuration:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(findProperty("KEYSTORE_FILE") ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... other configs
        }
    }
}
```

---

## 📊 Viewing CI Results

### On Pull Requests
- CI status will appear at the bottom of your PR
- Click "Details" to see logs
- Artifacts available in "Artifacts" section

### On Actions Tab
1. Go to your repo on GitHub
2. Click "Actions" tab
3. See all workflow runs
4. Download artifacts (APK, test reports, etc.)

---

## 🎯 Branch Protection (Recommended)

Enforce CI checks before merging:

1. **Settings** → **Branches**
2. **Add rule** for `main`/`master`
3. Enable:
   - ✅ Require status checks to pass
   - ✅ Require branches up to date
   - ✅ Select "Build and Test" check
4. **Create**

---

## 📦 Artifacts

| Artifact | Retention | When Created |
|----------|-----------|--------------|
| build-outputs | 7 days | Every CI build |
| app-debug | 14 days | Successful builds |
| test-results | 7 days | Every CI run |
| lint-results | 7 days | Every CI run |
| app-release-X.X.X | 30 days | Release tags |

---

## 🐛 Troubleshooting

### CI Build Fails with "Permission Denied"
```bash
chmod +x gradlew
git add gradlew
git commit -m "Fix gradlew permissions"
git push
```

### CI Build Fails with "Gradle Wrapper Not Found"
```bash
# Regenerate wrapper
gradle wrapper --gradle-version 8.2

# Commit wrapper files
git add gradle/wrapper/*
git add gradlew gradlew.bat
git commit -m "Add Gradle wrapper"
git push
```

### CI Build Succeeds but Local Build Fails
- Clean local build: `./gradlew clean`
- Invalidate cache: Android Studio → File → Invalidate Caches
- Check local JDK version (should be JDK 17)

### Release Build Fails with Signing Error
- Verify all 4 secrets are set correctly
- Check keystore is valid: `keytool -list -v -keystore keystore.jks`
- Ensure base64 encoding was done correctly (no line breaks)

---

## 🔄 Testing CI Locally

Install [act](https://github.com/nektos/act) to test workflows locally:

```bash
# Install act
brew install act  # macOS
choco install act-cli  # Windows

# Test CI workflow
act push

# Test specific workflow
act -W .github/workflows/android-ci.yml
```

---

## 📈 Performance Tips

Current optimizations:
- ✅ Gradle dependency caching (~2-3 min saved)
- ✅ Parallel execution
- ✅ Incremental builds

To further improve:
1. Use build cache: `org.gradle.caching=true` in gradle.properties
2. Increase heap size: `org.gradle.jvmargs=-Xmx4g`
3. Use configuration cache: `org.gradle.configuration-cache=true`

---

## 📝 Adding CI Badge to README

Add this to your main README.md:

```markdown
## Build Status

[![Android CI](https://github.com/YOUR_USERNAME/neo/actions/workflows/android-ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/neo/actions/workflows/android-ci.yml)
[![Release](https://github.com/YOUR_USERNAME/neo/actions/workflows/release.yml/badge.svg)](https://github.com/YOUR_USERNAME/neo/actions/workflows/release.yml)
[![Code Quality](https://github.com/YOUR_USERNAME/neo/actions/workflows/code-quality.yml/badge.svg)](https://github.com/YOUR_USERNAME/neo/actions/workflows/code-quality.yml)
```

Replace `YOUR_USERNAME` with your GitHub username.

---

## ✨ Next Steps

- [ ] Push changes to trigger first CI build
- [ ] Review first CI results
- [ ] Optionally set up release workflow with secrets
- [ ] Configure branch protection rules
- [ ] Add CI badge to README
- [ ] Set up Codecov for coverage tracking (optional)

---

## 🆘 Need Help?

- Check workflow logs in Actions tab
- Review [GitHub Actions documentation](https://docs.github.com/en/actions)
- Check [Android CI best practices](https://developer.android.com/studio/projects/continuous-integration)

---

**Status:** ✅ CI/CD workflows are ready to use!

Just push your changes and watch the magic happen! 🎉

