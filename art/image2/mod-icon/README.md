# Earth on Minecraft Mod Icon

- Model workflow: Image-2 high-resolution generation, followed by deterministic alpha cleanup and resizing with `tools/prepare_mod_icon.py`.
- Raw master: `earth_on_minecraft_icon_raw.png`.
- Runtime asset: `neoforge-26.2/src/main/resources/earth_on_minecraft.png` at `1024x1024`.
- CurseForge upload asset: `earth_on_minecraft_logo_transparent.png`; this is the true-alpha export. Do not upload the raw master, whose checkerboard is painted into the pixels.
- Small-size evidence: `output/quality/mod-icon-contact-sheet.png` at 256, 128, 64, and 32 pixels on light and dark backgrounds.

## Art Brief

The icon represents the complete core mod rather than only its industrial route: a voxel-style Earth cutaway combines surface ecology, water, a small settlement, stratified geology, mineral veins, a clean steel gear, and an electrical node. It uses a modern scientific material language without text, weapons, pollution, steampunk rust, or a decorative UI frame.

## Generation Prompt

> Create a single polished square mod icon for the Minecraft mod "Earth on Minecraft / 我的地球". Original artwork, not copied from Minecraft assets. A centered voxel-inspired cutaway mini Earth with a strong simple silhouette: the upper surface combines green land, a small tree canopy and clear blue water; the lower cutaway visibly shows layered sedimentary, igneous and dark basement rock with a few distinct mineral veins. Integrate one subtle clean modern-science motif into the lower edge, such as a partial brushed-steel gear and a small cyan electrical node, so it represents geology, materials, settlements and modern technology without looking like a factory-only mod. Contemporary scientific visual language, crisp pixel-art-friendly shapes rendered from a high-resolution master, clean materials, rich but controlled green, blue, ochre, charcoal and steel palette. Readable at 32x32 and 64x64, generous safe margin, centered composition, transparent background. No text, no letters, no numbers, no pseudo-writing, no watermark, no UI frame, no weapons, no smokestacks, no pollution, no steampunk, no retro rust, no photorealistic landscape, no full scene.

The provider painted a checkerboard instead of returning alpha. The preparation script removes only bright near-neutral pixels connected to the canvas edge, preserving enclosed highlights and recording a true-transparent runtime PNG.
