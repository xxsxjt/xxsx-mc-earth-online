# Blockbench Source Pipeline

This directory owns editable model sources. Runtime JSON and textures remain under the NeoForge resource tree.

- `machines/<id>.bbmodel`: processing-machine sources.
- `devices/<id>.bbmodel`: generators, storage, logistics, and other functional blocks.
- `entities/<id>.bbmodel`: custom entity geometry and animation sources.

`tools/generate_blockbench_machine_models.py` exports editable sources plus idle, running, and fault runtime models. `tools/pipeline_blockbench_models.py --check` validates every required source and compares exported assets with the quality profiles. A missing `.bbmodel` source fails the pipeline; a cube texture reference is not counted as a completed geometry pass.

Before changing a source, keep the profile's real prototype, silhouette, six-face duties, state cues, collision, origin, pivots, UV ownership, and formed-multiblock role aligned. Retain idle, running, and fault variants in one source whenever practical.
