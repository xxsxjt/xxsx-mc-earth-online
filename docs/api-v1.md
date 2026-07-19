# Earth on Minecraft Integration API v1

Package: `com.xxsx.earthonminecraft.api.v1`

Entry point: `EarthOnMinecraftApi`

The package version is part of the compatibility contract. Additive changes may increase the minor version; incompatible record or method changes require a new package such as `api.v2`. Consumers must not reference implementation classes outside the API package.

## Services

- `geology()` inspects a block position or block identifier and reports geological role and material facts.
- `materials()` returns formula, category, source, processing, use, and simplification keys.
- `processing()` exposes immutable machine specifications and executable route summaries.
- `energy()` resolves EOU storage ports and performs simulated or committed bounded transfers.
- `logistics()` resolves sided inventory ports and performs simulated or committed insertion/extraction.
- `settlements()` exposes resident identity and settlement snapshots without exposing saved-data internals.

All item, block, and machine references use namespaced `Identifier` values. Query methods return `Optional` for unsupported blocks, missing block entities, or absent settlement data. Optional integrations must treat an empty result as a normal fallback, not as a startup error.
