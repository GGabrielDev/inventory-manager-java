# Changelog

## [1.13.1](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.13.0...v1.13.1) (2026-06-17)


### Bug Fixes

* Table columns no longer squeezed — remove CONSTRAINED_RESIZE_POLICY ([#79](https://github.com/GGabrielDev/inventory-manager-java/issues/79)) ([ac7104d](https://github.com/GGabrielDev/inventory-manager-java/commit/ac7104d6577baca6bf5f92dc256065fd6ac4de45))

## [1.13.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.12.0...v1.13.0) (2026-06-16)


### Features

* Add feature-level acceptance tests with TestFX + real backend ([#73](https://github.com/GGabrielDev/inventory-manager-java/issues/73)) ([d1259c4](https://github.com/GGabrielDev/inventory-manager-java/commit/d1259c45dc4588bda01bd207628a96e3cfdbf722))


### Bug Fixes

* Add @Transactional to BagController read endpoints ([#77](https://github.com/GGabrielDev/inventory-manager-java/issues/77)) ([edd7887](https://github.com/GGabrielDev/inventory-manager-java/commit/edd78872b8eb2f6461a78fc6855a70d264eea9c5))
* Redirect to login after changing API URL in settings ([#74](https://github.com/GGabrielDev/inventory-manager-java/issues/74)) ([2631fa1](https://github.com/GGabrielDev/inventory-manager-java/commit/2631fa1c6a41ab497c34d9fb4914a3a599dc0be7))
* Resolve Bags LazyInitializationException by making BagItem.item EAGER ([#78](https://github.com/GGabrielDev/inventory-manager-java/issues/78)) ([52591a6](https://github.com/GGabrielDev/inventory-manager-java/commit/52591a699853da5aa33efa9e2f06e05b195d7f18))

## [1.12.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.11.1...v1.12.0) (2026-05-26)


### Features

* Add paranoid soft-delete with referential integrity guards across all entities ([#70](https://github.com/GGabrielDev/inventory-manager-java/issues/70)) ([dad96e6](https://github.com/GGabrielDev/inventory-manager-java/commit/dad96e6eb2ab1e50f5ed2ef29f3cce8d6b845650))


### Bug Fixes

* **persistence:** Resolve municipality edit 500 and enrich error details ([#68](https://github.com/GGabrielDev/inventory-manager-java/issues/68)) ([6bfd8ec](https://github.com/GGabrielDev/inventory-manager-java/commit/6bfd8ec2859290fe3c85091eabf20eedfa75a0c5))
* **persistence:** Resolve municipality edit 500 and enrich error details ([#69](https://github.com/GGabrielDev/inventory-manager-java/issues/69)) ([8995eb4](https://github.com/GGabrielDev/inventory-manager-java/commit/8995eb4ef7a7011c2faa20ba9614029d41071022))
* **pipeline:** Harden frontend tests and resolve scene transition bug ([#66](https://github.com/GGabrielDev/inventory-manager-java/issues/66)) ([2546341](https://github.com/GGabrielDev/inventory-manager-java/commit/2546341f0a1b050500f42613f28036b614390820))

## [1.11.1](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.11.0...v1.11.1) (2026-05-20)


### Bug Fixes

* **frontend:** Make login asynchronous and surface auth errors ([#62](https://github.com/GGabrielDev/inventory-manager-java/issues/62)) ([c2da096](https://github.com/GGabrielDev/inventory-manager-java/commit/c2da0969c7810a60f0f6cbdab0d742e6f7bb06af))

## [1.11.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.10.1...v1.11.0) (2026-05-20)


### Features

* **guards:** Implement auto-remediation and caveman mode validation ([#61](https://github.com/GGabrielDev/inventory-manager-java/issues/61)) ([3224015](https://github.com/GGabrielDev/inventory-manager-java/commit/3224015a833241b72c132341cf1241bbecdc7171))


### Bug Fixes

* **frontend:** Resolve startup NPE and harden domain invariants ([#59](https://github.com/GGabrielDev/inventory-manager-java/issues/59)) ([d3bc3f9](https://github.com/GGabrielDev/inventory-manager-java/commit/d3bc3f97d8a6c57c553d2d508b3bfdae0b5db872))

## [1.10.1](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.10.0...v1.10.1) (2026-05-19)


### Bug Fixes

* **consistency:** Enforce validation and align contracts ([#57](https://github.com/GGabrielDev/inventory-manager-java/issues/57)) ([c75b280](https://github.com/GGabrielDev/inventory-manager-java/commit/c75b2806ff2f54e6766010f3f650f0da3e5e275a))

## [1.10.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.9.0...v1.10.0) (2026-05-19)


### Features

* Add master audit automation scripts ([#54](https://github.com/GGabrielDev/inventory-manager-java/issues/54)) ([2e58fe1](https://github.com/GGabrielDev/inventory-manager-java/commit/2e58fe19cd2816ac1f8971abcdc98848950d18e3))

## [1.9.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.8.3...v1.9.0) (2026-05-19)


### Features

* Improve parish UI, error handling, filters and audit trail ([#52](https://github.com/GGabrielDev/inventory-manager-java/issues/52)) ([04df7c5](https://github.com/GGabrielDev/inventory-manager-java/commit/04df7c50f6d159b57413937675b1b8689be62fc8))

## [1.8.3](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.8.2...v1.8.3) (2026-05-12)


### Bug Fixes

* **ui:** Finalize data rendering and expand sidebar ([#47](https://github.com/GGabrielDev/inventory-manager-java/issues/47)) ([b937019](https://github.com/GGabrielDev/inventory-manager-java/commit/b9370193bd6ad52f9aa58ffa8e6c5e8d9cd47d65))
* **ui:** Resolve missing table data for nested associations ([#46](https://github.com/GGabrielDev/inventory-manager-java/issues/46)) ([20709f9](https://github.com/GGabrielDev/inventory-manager-java/commit/20709f973700f3e3c7d9d27a2350d5c2bb45e0f7))

## [1.8.2](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.8.1...v1.8.2) (2026-05-11)


### Bug Fixes

* **mcp:** Resolve server startup and architectural audit violations ([#44](https://github.com/GGabrielDev/inventory-manager-java/issues/44)) ([5319ac4](https://github.com/GGabrielDev/inventory-manager-java/commit/5319ac42ba8507ba59880a2f86cf7992c21c0bc5))

## [1.8.1](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.8.0...v1.8.1) (2026-05-04)


### Bug Fixes

* **ui:** Resolve lazy-init crashes and refine UI interactions ([#42](https://github.com/GGabrielDev/inventory-manager-java/issues/42)) ([f7ddc5c](https://github.com/GGabrielDev/inventory-manager-java/commit/f7ddc5c3d0cb0102ab3f9f3f24ea184e41893265))

## [1.8.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.7.1...v1.8.0) (2026-05-04)


### Features

* **ui:** Complete form functionality and enhance table UX ([#40](https://github.com/GGabrielDev/inventory-manager-java/issues/40)) ([4a76955](https://github.com/GGabrielDev/inventory-manager-java/commit/4a7695532967391c916c68443fa40c8521d89966))

## [1.7.1](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.7.0...v1.7.1) (2026-05-03)


### Bug Fixes

* Resolve critical UI crashes and complete identity modules ([#38](https://github.com/GGabrielDev/inventory-manager-java/issues/38)) ([c2fd5ee](https://github.com/GGabrielDev/inventory-manager-java/commit/c2fd5ee1f7871a12bc2bf2e6f02f9d2514fdd4c8))

## [1.7.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.6.1...v1.7.0) (2026-05-03)


### Features

* Add verbose logging flag and enhance error popups ([#36](https://github.com/GGabrielDev/inventory-manager-java/issues/36)) ([803b1c6](https://github.com/GGabrielDev/inventory-manager-java/commit/803b1c647cc2ee20a464328a03dd62b5367dec1e))

## [1.6.1](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.6.0...v1.6.1) (2026-05-03)


### Bug Fixes

* **api:** Resolve malformed pagination path ([883b0c3](https://github.com/GGabrielDev/inventory-manager-java/commit/883b0c327c737ab8c504ca278636565bb7a6ba37))

## [1.6.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.5.1...v1.6.0) (2026-04-27)


### Features

* **ui:** Implement i18n support for english and spanish ([#30](https://github.com/GGabrielDev/inventory-manager-java/issues/30)) ([343afdc](https://github.com/GGabrielDev/inventory-manager-java/commit/343afdcf9a75d34c57df03200b6e7a034f9e435f))

## [1.5.1](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.5.0...v1.5.1) (2026-04-27)


### Bug Fixes

* Resolve login crash and reinforce testing suite ([#28](https://github.com/GGabrielDev/inventory-manager-java/issues/28)) ([09643f7](https://github.com/GGabrielDev/inventory-manager-java/commit/09643f70bfe3ccd516cdf9a5ff39605483cb9285))

## [1.5.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.4.0...v1.5.0) (2026-04-27)


### Features

* Inventory Manager 2.0 - Admin Panel and Reinforced Docs ([#26](https://github.com/GGabrielDev/inventory-manager-java/issues/26)) ([6430066](https://github.com/GGabrielDev/inventory-manager-java/commit/6430066c88bd0b8b6c416a3c52a4158f594fab24))

## [1.4.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.3.0...v1.4.0) (2026-04-26)


### Features

* Ui redesign phase 3 - bag and displacement workflows ([#23](https://github.com/GGabrielDev/inventory-manager-java/issues/23)) ([cb93bb6](https://github.com/GGabrielDev/inventory-manager-java/commit/cb93bb6b9e92a8edf49515d0b300f9097bc5f465))
* **ui:** Complete all creation forms and finalize redesign ([#25](https://github.com/GGabrielDev/inventory-manager-java/issues/25)) ([0702378](https://github.com/GGabrielDev/inventory-manager-java/commit/0702378255d1ca644cd90b8ca88fed2872917b38))

## [1.3.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.2.0...v1.3.0) (2026-04-26)


### Features

* Ui redesign phase 2 - ui foundation and core screens ([#21](https://github.com/GGabrielDev/inventory-manager-java/issues/21)) ([0f15b90](https://github.com/GGabrielDev/inventory-manager-java/commit/0f15b90eb34aff1512d4824c09e7cd77d81487d5))

## [1.2.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.1.2...v1.2.0) (2026-04-26)


### Features

* Ui redesign phase 1 - backend infrastructure ([#19](https://github.com/GGabrielDev/inventory-manager-java/issues/19)) ([0340e24](https://github.com/GGabrielDev/inventory-manager-java/commit/0340e24edd0593c974b321eaadcbca1ce6171466))

## [1.1.2](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.1.1...v1.1.2) (2026-04-26)


### Bug Fixes

* **backend:** Use mutable HashSet in seed runner ([#14](https://github.com/GGabrielDev/inventory-manager-java/issues/14)) ([7e23b96](https://github.com/GGabrielDev/inventory-manager-java/commit/7e23b963146b7fbed25bf6950daa5ebbb2332e9b))

## [1.1.1](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.1.0...v1.1.1) (2026-04-26)


### Bug Fixes

* **backend:** Use OffsetDateTime for JPA auditing ([5f358b4](https://github.com/GGabrielDev/inventory-manager-java/commit/5f358b46c6ef97b565dae8d834b589f8f1b293dd))
* Use OffsetDateTime for JPA auditing ([80dc64e](https://github.com/GGabrielDev/inventory-manager-java/commit/80dc64e1dc6c2908cbbda447e1220760672d603c))


### Documentation

* Explain backend JAR configuration methods ([a11b202](https://github.com/GGabrielDev/inventory-manager-java/commit/a11b202c82433c387cb046bf040b1247416c8d5c))

## [1.1.0](https://github.com/GGabrielDev/inventory-manager-java/compare/v1.0.0...v1.1.0) (2026-04-25)


### Features

* Environment hardening, cross-platform build, and release fixes ([43ef065](https://github.com/GGabrielDev/inventory-manager-java/commit/43ef065c0a5b8b1c8b0651aed6d2a977b53b6e0e))
* Environment hardening, docs, and release fixes ([334a7f0](https://github.com/GGabrielDev/inventory-manager-java/commit/334a7f083245943cbed8121a685a0c77ca5b0de3))

## 1.0.0 (2026-04-25)


### Features

* add item request UX and workflow docs ([f8b4662](https://github.com/GGabrielDev/inventory-manager-java/commit/f8b4662a558adef9d891e5d563bfa4f99376e354))
* add operator item request workflow ([40335ab](https://github.com/GGabrielDev/inventory-manager-java/commit/40335ab88127ce89732e8be8481d4abc76654ab6))
* **backend:** add diagnostics endpoint and DB startup check ([a792862](https://github.com/GGabrielDev/inventory-manager-java/commit/a79286269b61d780186a46b8f231153ea3c3a7a8))
* create Java fullstack baseline ([a445651](https://github.com/GGabrielDev/inventory-manager-java/commit/a44565167a15100d09f49b32fd275ab5fa16e7ef))
* **frontend:** add persistent API URL config and connection UI ([e441d7e](https://github.com/GGabrielDev/inventory-manager-java/commit/e441d7e82fec496e908cd8070816dba70821aacb))


### Documentation

* add packaging and migration notes ([fc53c9c](https://github.com/GGabrielDev/inventory-manager-java/commit/fc53c9c1a696663d262db65c1ed17c879623c89c))
* add rule for committing AI changes ([14c27cd](https://github.com/GGabrielDev/inventory-manager-java/commit/14c27cd906dbe226ecb8afdeed068a7c06413092))
