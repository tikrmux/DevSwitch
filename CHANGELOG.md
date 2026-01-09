# Changelog

All notable changes to DevSwitch will be documented in this file.

## [1.0.3] - 2026-01-09

### Changed
- **BREAKING**: Migrated to Jewel UI framework for native IntelliJ theme integration
- **BREAKING**: Minimum supported version is now IntelliJ Platform 243+ (2024.3+)
  - IntelliJ IDEA 2024.3+
  - Android Studio Meerkat (2024.3+)
- Replaced Material Design components with Jewel components
- UI now automatically matches IntelliJ's light/dark theme
- Using `ToolWindow.addComposeTab()` for proper Compose integration

### Fixed
- Fixed `NoClassDefFoundError: Could not initialize class SwingSkiaLayerComponent`
- Fixed Skiko native library loading issues in IntelliJ plugin environment
- Fixed UI layout issues - text no longer wraps vertically when window shrinks
- Dropdown widths are now constrained to prevent excessive expansion
- Clicking on toggle label text now toggles the checkbox
- Device selector and Auto Refresh checkbox now wrap to next line when space is limited

### Improved
- Auto Refresh preference is now persisted between sessions
- FPS Scale reading no longer uses expensive `dumpsys` command during auto refresh
- FPS Scale shows "Default" when device hasn't set an explicit refresh rate

### Removed
- Removed support for IntelliJ Platform 231-242 (use v1.0.1 for older IDEs)
- Removed Material Design dependency

## [1.0.2] - 2026-01-08

### Fixed
- Fixed `NoClassDefFoundError: Could not initialize class SwingSkiaLayerComponent` by replacing `compose.desktop.currentOs` with `compose.desktop.common` and explicit Compose dependencies

### Changed
- Expanded plugin verification to support more IDEs:
  - IntelliJ IDEA Community: 2024.3.2, 2024.2.4, 2024.1.7, 2023.3.8, 2023.2.8
  - IntelliJ IDEA Ultimate: 2024.3.2, 2024.2.4, 2024.1.7, 2023.3.8, 2023.2.8
  - Android Studio: Ladybug (2024.2.1.12), Koala (2024.1.2.13), Jellyfish (2023.3.1.20), Iguana (2023.2.1.25), Hedgehog (2023.1.1.28)

## [1.0.1]

### Fixed
- Fixed IDE compatibility (since-build 231)

## [1.0.0]

### Added
- Initial release
- Device selection with auto-detection
- Developer settings toggles
- Auto-refresh functionality
