# Changelog

All notable changes to DevSwitch will be documented in this file.

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
