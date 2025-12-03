# Play Store Deployment Setup

This guide explains how to set up automated deployment to Google Play Store (Alpha track) when you create a new release.

## Overview

The deployment workflow is triggered automatically when you publish a new release on GitHub. The workflow will:
1. Build a signed release AAB (Android App Bundle)
2. Upload it to Google Play Store's Alpha track
3. Set the release status to "Completed" (ready for rollout)

## Prerequisites

Before you can deploy to the Play Store, you need to:

### 1. Create a Release Keystore

You need a keystore to sign your release builds. If you don't have one:

```bash
keytool -genkey -v -keystore release-keystore.jks \
  -alias mixtapehaven \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**Important:**
- Keep this file secure and NEVER commit it to your repository
- Remember the passwords you set for the keystore and the key
- Back up this file in a secure location

### 2. Set Up Google Play Console

1. **Create your app** in the [Google Play Console](https://play.google.com/console)
2. **Complete the store listing** (required before you can publish)
3. **Create an Alpha track** (should exist by default)
4. **Set up API access:**
   - Go to: Setup → API access
   - Create a new service account or link an existing one
   - Grant the service account **"Admin"** permissions (or at minimum: "Manage releases")
   - Download the JSON key file

### 3. Configure GitHub Secrets

Go to your GitHub repository → Settings → Secrets and variables → Actions

Add the following secrets:

#### KEYSTORE_BASE64
Your keystore file encoded in base64:
```bash
base64 -i release-keystore.jks | tr -d '\n' | pbcopy  # macOS
base64 -w 0 release-keystore.jks  # Linux
```
Copy the output and paste it as the secret value.

#### KEYSTORE_PASSWORD
The password you used when creating the keystore.

#### KEY_ALIAS
The alias you used when creating the key (e.g., `mixtapehaven`).

#### KEY_PASSWORD
The password for the specific key (may be the same as keystore password).

#### PLAYSTORE_SERVICE_ACCOUNT_JSON
The contents of the service account JSON file you downloaded from Google Play Console.
Open the JSON file and copy its entire contents as the secret value.

## Creating a Release

To trigger a deployment to the Play Store:

1. **Tag your commit** with a version number:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **Create a GitHub release:**
   - Go to your repository on GitHub
   - Click "Releases" → "Create a new release"
   - Select the tag you just created
   - Fill in the release title and description
   - Click "Publish release"

3. **Monitor the deployment:**
   - Go to Actions tab in your GitHub repository
   - Watch the "Deploy to Play Store (Alpha)" workflow
   - If successful, your app will be available in the Alpha track

## Version Numbering

The workflow automatically calculates version codes from your tag:

- Tag `v1.2.3` → Version Code: `10203` (1×10000 + 2×100 + 3)
- Tag `v2.0.5` → Version Code: `20005` (2×10000 + 0×100 + 5)

**Version naming convention:**
- Use semantic versioning: `vMAJOR.MINOR.PATCH`
- Always prefix tags with `v` (e.g., `v1.0.0`)
- Each new release must have a higher version code than the previous

## Release Notes (Optional)

To include release notes in your Play Store listing:

1. Create directory: `fastlane/metadata/android/en-US/changelogs/`
2. Create a file named with your version code: `<VERSION_CODE>.txt`
   - Example: `10203.txt` for version 1.2.3
3. Add your release notes (max 500 characters)
4. Commit this file before creating your release

Example:
```bash
mkdir -p fastlane/metadata/android/en-US/changelogs
echo "- Fixed playback issues\n- Improved UI performance\n- Bug fixes" > fastlane/metadata/android/en-US/changelogs/10203.txt
git add fastlane/metadata/android/en-US/changelogs/10203.txt
git commit -m "Add release notes for v1.2.3"
git push
```

## Troubleshooting

### Build Fails with "Keystore not found"
- Verify that `KEYSTORE_BASE64` secret is set correctly
- Make sure there are no extra spaces or newlines in the secret

### "Invalid credentials" error
- Verify that `PLAYSTORE_SERVICE_ACCOUNT_JSON` is valid JSON
- Check that the service account has proper permissions in Play Console

### "Version code already exists"
- Each release must have a unique version code
- Make sure your new tag has a higher version number than previous releases

### Build succeeds but upload fails
- Ensure you've completed all required sections in Play Console
- Verify your app has been published at least once manually
- Check that the Alpha track exists in your Play Console

## Local Testing

To test the release build locally:

```bash
# Set environment variables
export KEYSTORE_FILE="./release-keystore.jks"
export KEYSTORE_PASSWORD="your-keystore-password"
export KEY_ALIAS="mixtapehaven"
export KEY_PASSWORD="your-key-password"
export VERSION_CODE="10000"
export VERSION_NAME="1.0.0"

# Build release AAB
./gradlew bundleRelease

# The AAB will be at: app/build/outputs/bundle/release/app-release.aab
```

## Security Best Practices

1. **Never commit** your keystore file or passwords to the repository
2. **Rotate service account keys** periodically
3. **Use separate keystores** for different apps
4. **Back up your keystore** in multiple secure locations
5. **Limit access** to GitHub secrets to trusted team members only

## Additional Resources

- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [Google Play Console](https://play.google.com/console)
- [Using Play Console API](https://developers.google.com/android-publisher)
