## [1.4.0](https://github.com/SouBryan/pinterest-morphed/compare/v1.3.0...v1.4.0) (2026-07-14)

### ✨ New Features

* **compat:** support Pinterest 14.20.0-14.27.0 as stable ([0e10291](https://github.com/SouBryan/pinterest-morphed/commit/0e1029111ef8530982d215bd1af05694767de7ae))

## [1.3.0](https://github.com/SouBryan/pinterest-morphed/compare/v1.2.1...v1.3.0) (2026-07-14)

### ✨ New Features

* add HideAdViewsPatch — collapse ad-specific views to GONE ([064d99c](https://github.com/SouBryan/pinterest-morphed/commit/064d99ccb5cbcd7b70955f7b6de0b2d298b83163))

## [1.2.1](https://github.com/SouBryan/pinterest-morphed/compare/v1.2.0...v1.2.1) (2026-07-14)

### 🐛 Bug Fixes

* cover shopping ads and ad_data in HidePromotedPinsPatch ([2ac9672](https://github.com/SouBryan/pinterest-morphed/commit/2ac967234e54f113e693454fd6066cc5ba13e312))

## [1.2.0](https://github.com/SouBryan/pinterest-morphed/compare/v1.1.0...v1.2.0) (2026-07-14)

### ✨ New Features

* resolve pin.it and pinterest.com/url_shortener short links to canonical pin URLs, and sanitize clipboard ([1e6b6e4](https://github.com/SouBryan/pinterest-morphed/commit/1e6b6e461300c3ae21000a427ba26f28086e62fa))

## [1.1.0](https://github.com/SouBryan/pinterest-morphed/compare/v1.0.0...v1.1.0) (2026-07-07)

### ✨ New Features

* add Sanitize sharing links + make cross-version patches resilient ([0989429](https://github.com/SouBryan/pinterest-morphed/commit/0989429e1e85dc021c8985cf2affe934fe283207))

## 1.0.0 (2026-07-07)

### ✨ New Features

* add Disable Google Engage and Privacy Sandbox Ad Services patches ([db79293](https://github.com/SouBryan/pinterest-morphed/commit/db79293e26486f1d8e5e34c002ac5a688efda59e))
* hide promoted pins app-wide via isPromoted getter shotgun ([1811d7a](https://github.com/SouBryan/pinterest-morphed/commit/1811d7a344be1600cdc57f1c8f009221b2422581))
* hook GoogleEngageWorker.createWork() to no-op success ([ae30cc2](https://github.com/SouBryan/pinterest-morphed/commit/ae30cc25a07da6796d1b8b05661a90469948f899))
* scaffold pinterest-morphed and add initial tracking / ads patches ([4fecc98](https://github.com/SouBryan/pinterest-morphed/commit/4fecc98f62ebe92759c6d3bf8853e98807de4f25))
