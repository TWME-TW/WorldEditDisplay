WorldEditDisplay v2.1.1 (2026-04-20)

Summary
- Patch release fixing a rendering issue with polyhedron selections.

Changelog
- fix(renderer): Polyhedron vertex visibility
  - Fixed a bug where polyhedron vertex markers were not displayed when only vertex (`p`) events had been received and faces (`poly`) had not yet been reported by WorldEdit. Vertex markers are now rendered immediately as vertex events arrive.

Notes
- This is a backward-compatible bugfix release. No API changes.
- Bumped version from 2.1.0 to 2.1.1.

Acknowledgements
- Thanks to the contributor who reported the missing vertex markers and provided reproduction steps.