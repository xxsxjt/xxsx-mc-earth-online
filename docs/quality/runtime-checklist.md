# Runtime Verification Checklist

Record the date, commit, jar name, SHA256, instance, world, language, GUI scale, and installed optional integrations for every run.

## Automated Gate

- [ ] `gradlew verifyGuiPreviews`
- [ ] `gradlew clean build`（通过 `check` 自动运行地质、物品、Blockbench、声音、资产清单、资源/API 门禁和 GameTest）
- [ ] `pwsh -NoProfile -File tools/run_client_smoke.ps1`
- [ ] Jar contains expected sounds, particles, models, language files, API classes, and GameTest classes.
- [ ] Blockbench report distinguishes completed `.bbmodel` geometry sources from texture-reference models; no pending entry is described as finished geometry.
- [ ] Every feature marked complete has an eight-layer manifest with concrete evidence.
- [ ] Deployed jar SHA256 equals the verified build artifact.
- [ ] Exactly one active `earth-on-minecraft-neoforge-26.2-*.jar` exists in the instance.

## Client Startup

- [ ] Main menu reaches title screen without registry, model, sound, particle, or data-pack errors.
- [ ] A fresh world can be created and entered.
- [ ] An existing pre-update world can be entered without migration failure.
- [ ] `latest.log` and `debug.log` contain no new Earth on Minecraft error stack.

## Screenshot Matrix

- [ ] Run `/test run earth_on_minecraft:quality_screenshot_field` in a development client.
- [ ] Capture ore clusters from front, top, and oblique angles; internal seams are absent.
- [ ] Capture each processing-family representative in idle, running, and fault/blocked state.
- [ ] Capture every UI state listed in `ui-state-matrix.json` in `zh_cn` and `en_us`.
- [ ] Repeat critical screens at GUI scales 2, 3, and Auto on the smallest supported window.
- [ ] Capture formed multiblocks, projections, ports, conveyors, cables, and state transitions.

## Gameplay and Recovery

- [ ] First iron, copper, power, cable, machine, and automated transfer loops are reachable in survival.
- [ ] Invalid input, missing power, redstone pause, full output, invalid structure, and disconnected logistics explain recovery.
- [ ] Processing does not consume inputs or power when outputs cannot fit.
- [ ] Chunk unload/reload preserves inventories, progress, routes, side modes, energy, and settlement data.
- [ ] Optional JEI or addon absence does not prevent startup or normal play.

## Release Evidence

- [ ] Version strings agree across Gradle, metadata, README, release notes, jar name, and Git tag.
- [ ] Remote release jar exists and its downloaded SHA256 equals the local verified artifact.
- [ ] Release is explicitly marked development/testing until the client checklist is complete.
