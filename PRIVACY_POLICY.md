# Privacy Policy for AIO Video Downloader

**Last Updated**: 17/07/2025  
**Project Repository**: [AIO Video Downloader on GitHub](https://github.com/shibaFoss/AIO-Video-Downloader)  
**License**: [AGPL-3](https://www.gnu.org/licenses/agpl-3.0.en.html)  
**Contact**: shiba.spj@hotmail.com

## 🔍 Open-Source Transparency

As an AGPL-3 licensed open-source project:
- All code is publicly auditable at [our GitHub repository](https://github.com/shibaFoss/AIO-Video-Downloader)
- Community contributions are governed by the project's CONTRIBUTING guidelines
- No hidden data collection mechanisms exist in the codebase

## 📦 Data Collection Principles

We practice minimal data collection by design:
- **No user accounts or registration**
- **No mandatory personal data collection**
- All core features work without sensitive permissions

## 🚫 What We Don't Collect

The application explicitly does not collect or transmit:
- Downloaded video content or metadata
- Device identifiers (IMEI, MEID, etc.)
- Contact lists, messages, or call logs
- Precise location data (GPS, network-based)
- Payment or financial information

## ⚙️ Technical Data Handling

### Local Storage
- Download history stored only in device storage (non-synced)
- Preferences saved using Android's SharedPreferences API
- All storage uses Android's sandboxed storage system

### Network Operations
- Video URL analysis performed locally on device
- Direct peer-to-server downloads (no proxy servers)
- No intermediate processing of your downloads

## 📢 Advertising (Optional Module)

For ad-supported builds:
- Uses Android's resettable Advertising ID
- Network requests limited to ad providers only
- Ad implementation code is in separate verifiable module

## 🔐 Permissions Breakdown

| Permission | Purpose | Required | User Control |
|------------|---------|----------|--------------|
| `INTERNET` | Download videos | Yes | N/A |
| `WRITE_EXTERNAL_STORAGE` | Save files to storage | Optional | Revocable |
| `FOREGROUND_SERVICE` | Download notifications | Optional | Disable in settings |

## 🛡️ Security Measures

- Implements certificate pinning for supported platforms
- Uses Android's Network Security Configuration
- Regular security audits documented in GitHub issues
- All dependencies tracked in build.gradle

## 👶 Children's Privacy

Compliant with global standards:
- COPPA (US) compliant by design
- GDPR (EU) compliant data practices
- No features specifically targeting minors

## ♻️ User Control Options

You can:
1. Inspect all network requests via Android Studio
2. Review all data storage locations in `/data/data/[package]`
3. Build your own ad-free version from source
4. Disable all analytics in Settings > Privacy

## 🔄 Policy Updates

Changes will be:
1. Versioned in the repository's `/docs` folder
2. Announced in GitHub Releases
3. Never reduce existing privacy protections retroactively

## 📬 Contact & Contributions

For privacy concerns or improvements:
- Open an issue: [GitHub Issues](https://github.com/shibaFoss/AIO-Video-Downloader/issues)
- Email: [shiba.spj@hotmail.com](mailto:shiba.spj@hotmail.com)

---

*This policy is valid as of the last updated date above. The AGPL-3 license guarantees your right to verify these claims by inspecting the source code.*