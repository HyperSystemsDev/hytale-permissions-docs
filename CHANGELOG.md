# Changelog

All notable changes to this documentation will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [1.0.0] - 2025-01-31

### Added

#### Core Documentation
- `PERMISSIONS_SYSTEM.md` - Complete system documentation with 12 sections
- `QUICK_REFERENCE.md` - One-page cheat sheet for daily use
- `TROUBLESHOOTING.md` - FAQ and debugging guide
- `README.md` - Repository index and quick start guide

#### Guides
- `guides/MIGRATION_GUIDE.md` - Importing from vanilla permissions.json
- `guides/COMPARISON_MATRIX.md` - Comparison with LuckPerms/PermissionsEx patterns

#### Examples
- `examples/CustomProviderTemplate.java` - Starter template for custom providers
- `examples/EventSubscriptionExamples.java` - Event listener examples

#### Diagrams
- `diagrams/PERMISSION_RESOLUTION_FLOW.md` - Visual flowchart (Mermaid)

#### Testing
- `testing/EDGE_CASES.md` - Test cases and expected behavior

#### Reference
- `VERSION.md` - Decompilation and source information
- `CHANGELOG.md` - This file
- `PERMISSION_NODES.md` - Comprehensive permission node registry
- `BEST_PRACTICES.md` - Design patterns and guidelines
- `GLOSSARY.md` - Term definitions

### Source Material

All documentation derived from Vineflower-decompiled Hytale server sources:
- 17 Java source files analyzed
- 5 package hierarchies documented
- Full API coverage for permissions system

---

## [1.1.0] - 2026-02-21

### Added
- `hytale.mods.outdated.notify` (`MODS_OUTDATED_NOTIFY`) permission node — previously undocumented
- Mods Permissions section in `PERMISSION_NODES.md`
- Nondeterministic group iteration warning in `PERMISSIONS_SYSTEM.md`, `BEST_PRACTICES.md`, and `EDGE_CASES.md`
- OP/Default group overwrite behavior documentation in `PERMISSIONS_SYSTEM.md`, `QUICK_REFERENCE.md`, `TROUBLESHOOTING.md`, `BEST_PRACTICES.md`, and `MIGRATION_GUIDE.md`
- Multi-provider Default group aggregation note in `PERMISSIONS_SYSTEM.md`
- FastUtil internal map type note for plugin developers in `PERMISSIONS_SYSTEM.md`
- Null vs empty set semantics in `PERMISSION_RESOLUTION_FLOW.md`
- Wildcard restriction note (no middle wildcards) in `PERMISSION_NODES.md`
- OP/Default overwrite test case in `EDGE_CASES.md`
- Negation wildcards (`-*`, `-prefix.*`) to `COMPARISON_MATRIX.md` Hytale column
- Group inheritance flattening warning in `COMPARISON_MATRIX.md`
- OP/Default data loss warning in `MIGRATION_GUIDE.md`
- Dual event class clarification in `EventSubscriptionExamples.java`

### Changed
- Updated Hytale build reference from January 2025 to February 2026
- Updated documentation version from 1.0.0 to 1.1.0
- Expanded Virtual Group definition in `GLOSSARY.md` (default Creative group, `setVirtualGroups()`, global scope)
- Expanded Provider Chain definition in `GLOSSARY.md` (denials also stop the chain)
- Strengthened thread safety documentation in `CustomProviderTemplate.java`
- Clarified Default group return semantics in `CustomProviderTemplate.java`
- Improved prefix wildcard check order description in `PERMISSION_RESOLUTION_FLOW.md`
- Upgraded multi-group conflict test case 2.2 from "caveat" to CRITICAL in `EDGE_CASES.md`

### Fixed
- `PERMISSIONS_SYSTEM.md` Section 11: `create()` writes `{}`, not a minimal file with groups
- `PERMISSIONS_SYSTEM.md` Section 11: OP/Default groups are overwritten (not "restored") on load — critical behavioral distinction
- `EDGE_CASES.md` Test 1.3: Removed confusing self-correcting format, now shows correct answer directly
- `TROUBLESHOOTING.md` Mistake 3: Clarified that `*` beats `-*`, and `-*` beats exact matches
- Negation wildcard patterns in `PERMISSION_NODES.md` Wildcards section restructured for clarity

### Notes
- Permissions code is **unchanged** since January 2025 — all updates in this release are documentation accuracy fixes
- Decompiled from `AppData/Roaming/Hytale/install/release/package/game/latest/Server/HytaleServer.jar`

---

## [Unreleased]

### Planned
- Additional code examples for common use cases
- Video/animated diagram alternatives
- Integration examples with HyperPerms
- Multi-language translations

---

## Template for Future Entries

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features or documentation

### Changed
- Changes to existing documentation

### Deprecated
- Features that will be removed in future versions

### Removed
- Removed features or documentation

### Fixed
- Bug fixes or corrections

### Security
- Security-related changes
```
