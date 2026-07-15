## [1.0.0-dev.8](https://github.com/SouBryan/pinterest-morphed/compare/v1.0.0-dev.7...v1.0.0-dev.8) (2026-07-15)

### 🐛 Bug Fixes

* **ads:** cover third-party, sponsored and promoted-variant flags on first-boot feed ([85457cf](https://github.com/SouBryan/pinterest-morphed/commit/85457cf0d7c20e32ca07c2a7406173749964d618))

### ✨ New Features

* **tracking:** add DisableFirebaseServicesPatch to flip 5 Firebase auto-init flags ([822dbc4](https://github.com/SouBryan/pinterest-morphed/commit/822dbc4784d8550df35aa8733d1a43ddc520feee))

## [1.0.0-dev.7](https://github.com/SouBryan/pinterest-morphed/compare/v1.0.0-dev.6...v1.0.0-dev.7) (2026-07-14)

### ✨ New Features

* **safety:** pin Pinterest signing certificate SHA-256 ([2576aea](https://github.com/SouBryan/pinterest-morphed/commit/2576aeac40436416593dcc1c4a32ccfcdbc2ef84))

## [1.0.0-dev.6](https://github.com/SouBryan/pinterest-morphed/compare/v1.0.0-dev.5...v1.0.0-dev.6) (2026-07-14)

### ✨ New Features

* **compat:** support Pinterest 14.20.0-14.27.0 as stable ([159ace4](https://github.com/SouBryan/pinterest-morphed/commit/159ace43b72515b14b3ec15eb206e43385bfdfa8))

## [1.0.0-dev.5](https://github.com/SouBryan/pinterest-morphed/compare/v1.0.0-dev.4...v1.0.0-dev.5) (2026-07-14)

### ✨ New Features

* add HideAdViewsPatch — collapse ad-specific views to GONE ([eb96762](https://github.com/SouBryan/pinterest-morphed/commit/eb96762fb376907227899a65e7d0466a37eeda27))

## [1.0.0-dev.4](https://github.com/SouBryan/pinterest-morphed/compare/v1.0.0-dev.3...v1.0.0-dev.4) (2026-07-14)

### 🐛 Bug Fixes

* cover shopping ads and ad_data in HidePromotedPinsPatch ([c8a55c1](https://github.com/SouBryan/pinterest-morphed/commit/c8a55c1fade7eba7adca23b83010ca4989642b3a))

## [1.0.0-dev.3](https://github.com/SouBryan/pinterest-morphed/compare/v1.0.0-dev.2...v1.0.0-dev.3) (2026-07-14)

### ✨ New Features

* resolve pin.it and pinterest.com/url_shortener short links to canonical pin URLs, and sanitize clipboard ([fc25383](https://github.com/SouBryan/pinterest-morphed/commit/fc25383cfbf4888a596fffa7fb6484b927f830eb))

## [1.0.0-dev.2](https://github.com/SouBryan/pinterest-morphed/compare/v1.0.0-dev.1...v1.0.0-dev.2) (2026-07-07)

### ✨ New Features

* add Sanitize sharing links + make cross-version patches resilient ([10f2940](https://github.com/SouBryan/pinterest-morphed/commit/10f29407fc835fabae467b7a05aa66c75982e44c))

## 1.0.0-dev.1 (2026-07-07)

### ✨ New Features

* add Disable Google Engage and Privacy Sandbox Ad Services patches ([db79293](https://github.com/SouBryan/pinterest-morphed/commit/db79293e26486f1d8e5e34c002ac5a688efda59e))
* hide promoted pins app-wide via isPromoted getter shotgun ([1811d7a](https://github.com/SouBryan/pinterest-morphed/commit/1811d7a344be1600cdc57f1c8f009221b2422581))
* hook GoogleEngageWorker.createWork() to no-op success ([ae30cc2](https://github.com/SouBryan/pinterest-morphed/commit/ae30cc25a07da6796d1b8b05661a90469948f899))
* scaffold pinterest-morphed and add initial tracking / ads patches ([4fecc98](https://github.com/SouBryan/pinterest-morphed/commit/4fecc98f62ebe92759c6d3bf8853e98807de4f25))
