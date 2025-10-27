# GitHub Actions Workflows

This directory contains GitHub Actions workflows for continuous integration and deployment.

## Workflows

### 1. **android-ci.yml** - Main CI Pipeline
**Triggers:**
- Push to `master`, `main`, or `develop` branches
- Pull requests to `master`, `main`, or `develop` branches

**Jobs:**
- ✅ Checkout code
- ✅ Setup JDK 17 and Android SDK
- ✅ Cache Gradle dependencies
- ✅ Clean project
- ✅ Build APK
- ✅ Run unit tests
- ✅ Run lint checks
- ✅ Upload build artifacts (APK, test results, lint reports)
- ✅ Comment on PR with results

**Artifacts:**
- `build-outputs` - All build outputs (7 days retention)
- `app-debug` - Debug APK (14 days retention)
- `test-results` - Unit test results (7 days retention)
- `lint-results` - Lint reports (7 days retention)

---

### 2. **release.yml** - Release Build Pipeline
**Triggers:**
- Push tags matching `v*.*.*` (e.g., v1.0.0)
- Manual workflow dispatch with version input

**Jobs:**
- ✅ Build signed release APK
- ✅ Build signed release AAB
- ✅ Upload artifacts (30 days retention)
- ✅ Create GitHub release with APK/AAB attached

**Prerequisites:**
To use this workflow, you need to set up the following GitHub Secrets:
- `KEYSTORE_BASE64` - Base64 encoded keystore file
- `KEYSTORE_PASSWORD` - Keystore password
- `KEY_ALIAS` - Key alias
- `KEY_PASSWORD` - Key password

**How to encode keystore:**
```bash
base64 -i keystore.jks | tr -d '\n' > keystore_base64.txt
```

---

### 3. **code-quality.yml** - Code Quality Checks
**Triggers:**
- Push to `master`, `main`, or `develop` branches
- Pull requests to `master`, `main`, or `develop` branches
- Scheduled: Every Monday at 9:00 AM UTC

**Jobs:**
1. **Detekt Static Analysis**
   - Runs Detekt for Kotlin code quality
   - Uploads Detekt reports

2. **Dependency Vulnerability Check**
   - Checks for outdated dependencies
   - Uploads dependency update reports

3. **Test Coverage Report**
   - Runs tests with Jacoco coverage
   - Uploads coverage to Codecov (optional)
   - Uploads coverage reports

**Optional Setup:**
- For Codecov integration, add `CODECOV_TOKEN` to GitHub Secrets

---

## Local Testing

You can test the workflows locally using [act](https://github.com/nektos/act):

```bash
# Install act
brew install act  # macOS
# or
choco install act-cli  # Windows

# Test the CI workflow
act -j build

# Test with specific event
act pull_request
```

---

## Gradle Tasks Used

| Task | Description |
|------|-------------|
| `./gradlew clean` | Clean build artifacts |
| `./gradlew build` | Build debug and release variants |
| `./gradlew test` | Run unit tests |
| `./gradlew lint` | Run lint checks |
| `./gradlew assembleRelease` | Build release APK |
| `./gradlew bundleRelease` | Build release AAB |
| `./gradlew detekt` | Run Detekt static analysis |
| `./gradlew dependencyUpdates` | Check for dependency updates |
| `./gradlew jacocoTestReport` | Generate test coverage report |

---

## Troubleshooting

### Build Fails on CI but Works Locally
1. Check Gradle version compatibility
2. Ensure all dependencies are available
3. Check environment variables and secrets

### Cache Issues
If cache causes issues, you can clear it:
1. Go to Actions tab
2. Click on "Caches"
3. Delete problematic cache

### Permission Denied on gradlew
The workflow includes `chmod +x gradlew`, but if issues persist:
```bash
git update-index --chmod=+x gradlew
git commit -m "Make gradlew executable"
git push
```

---

## Branch Protection Rules (Recommended)

To enforce CI checks before merging:

1. Go to Settings → Branches
2. Add rule for `main`/`master`
3. Enable:
   - ✅ Require status checks to pass before merging
   - ✅ Require branches to be up to date before merging
   - ✅ Select "Build and Test" as required check

---

## Badge for README

Add this badge to your main README.md:

```markdown
[![Android CI](https://github.com/YOUR_USERNAME/neo/actions/workflows/android-ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/neo/actions/workflows/android-ci.yml)
```

Replace `YOUR_USERNAME` with your GitHub username.

---

## Performance Optimization

Current optimizations:
- ✅ Gradle dependency caching (saves ~2-3 minutes per build)
- ✅ Parallel test execution
- ✅ Incremental builds
- ✅ Strategic artifact retention periods

---

## Future Enhancements

- [ ] Add instrumented tests on Firebase Test Lab
- [ ] Add screenshot tests
- [ ] Add automated Play Store deployment
- [ ] Add Slack/Discord notifications
- [ ] Add performance benchmarking
- [ ] Add security scanning with Snyk

