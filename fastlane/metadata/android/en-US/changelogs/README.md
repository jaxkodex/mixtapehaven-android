# Release Notes / Changelogs

This directory contains release notes that will be automatically uploaded to Google Play Store when deploying via GitHub Actions.

## How to Use

1. **Create a file** named with your version code: `<VERSION_CODE>.txt`
   - Example: `10203.txt` for version v1.2.3 (version code 10203)
   - Example: `20005.txt` for version v2.0.5 (version code 20005)

2. **Add your release notes** (max 500 characters):
   ```
   - New feature: Playlist sharing
   - Fixed playback issues
   - Improved performance
   - Bug fixes and improvements
   ```

3. **Commit the file** before creating your GitHub release:
   ```bash
   git add fastlane/metadata/android/en-US/changelogs/10203.txt
   git commit -m "Add release notes for v1.2.3"
   git push
   ```

## Version Code Calculation

Version codes are automatically calculated from git tags:

| Git Tag | Version Code | Changelog File |
|---------|--------------|----------------|
| v1.0.0  | 10000        | 10000.txt      |
| v1.2.3  | 10203        | 10203.txt      |
| v2.0.5  | 20005        | 20005.txt      |
| v3.1.0  | 30100        | 30100.txt      |

Formula: `MAJOR * 10000 + MINOR * 100 + PATCH`

## Tips

- Keep release notes concise and user-focused
- Highlight new features and important fixes
- Use bullet points for readability
- Avoid technical jargon when possible
- Maximum length: 500 characters
