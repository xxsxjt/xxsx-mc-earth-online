#!/usr/bin/env python3
"""Generate editable Blockbench sources and distinct runtime geometry for profiled devices."""

from __future__ import annotations

import json
import pathlib
import uuid


ROOT = pathlib.Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "neoforge-26.2" / "src" / "main" / "resources"
ASSETS = RESOURCES / "assets" / "earth_on_minecraft"
MODELS = ASSETS / "models" / "block"
BLOCKSTATES = ASSETS / "blockstates"
PROFILES = RESOURCES / "data" / "earth_on_minecraft" / "earth" / "quality" / "machine_profiles.json"
BLOCKBENCH = ROOT / "art" / "blockbench"
OUTPUT = ROOT / "output" / "quality"
FACES = ("north", "south", "west", "east", "up", "down")


def box(name: str, start: tuple[float, float, float], end: tuple[float, float, float],
        rotation: tuple[str, float, tuple[float, float, float]] | None = None) -> dict[str, object]:
    result: dict[str, object] = {"name": name, "from": list(start), "to": list(end)}
    if rotation is not None:
        result["rotation"] = {"axis": rotation[0], "angle": rotation[1], "origin": list(rotation[2])}
    return result


B = box
SHAPES: dict[str, list[dict[str, object]]] = {
    "jaw_crusher": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("jaw_chamber", (3, 2, 3), (13, 12, 14)),
        B("left_cheek", (1, 3, 2), (4, 13, 15)), B("right_cheek", (12, 3, 2), (15, 13, 15)),
        B("feed_hopper", (4, 12, 5), (12, 16, 14)), B("discharge", (5, 2, 0), (11, 6, 4)),
    ],
    "ball_mill": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("left_bearing", (1, 2, 4), (4, 8, 12)),
        B("drum", (3, 5, 2), (13, 13, 14)), B("right_bearing", (12, 2, 4), (15, 8, 12)),
        B("feed_trunnion", (0, 7, 6), (4, 11, 10)), B("discharge_trunnion", (12, 7, 6), (16, 11, 10)),
    ],
    "sieve": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("left_springs", (2, 2, 3), (4, 7, 13)),
        B("right_springs", (12, 2, 3), (14, 7, 13)),
        B("screen_deck", (2, 6, 3), (14, 9, 14), ("x", -22.5, (8, 8, 8))),
        B("feed_hood", (3, 9, 10), (13, 14, 15)), B("graded_chutes", (3, 2, 0), (13, 7, 5)),
    ],
    "magnetic_separator": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("drum", (3, 5, 3), (13, 13, 13)),
        B("left_yoke", (1, 3, 4), (4, 15, 12)), B("right_yoke", (12, 3, 4), (15, 15, 12)),
        B("feed_box", (4, 11, 11), (12, 16, 15)), B("split_discharge", (4, 2, 0), (12, 7, 5)),
    ],
    "flotation_cell": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("tank", (2, 2, 2), (14, 11, 14)),
        B("froth_launder", (1, 9, 1), (15, 12, 15)), B("agitator_shaft", (7, 10, 7), (9, 16, 9)),
        B("motor", (5, 13, 5), (11, 16, 11)), B("concentrate_outlet", (11, 5, 0), (15, 9, 4)),
    ],
    "ore_roaster": [
        B("windbox", (2, 0, 2), (14, 4, 14)), B("fluidized_vessel", (3, 3, 3), (13, 14, 13)),
        B("refractory_ring_low", (2, 4, 2), (14, 7, 14)), B("refractory_ring_high", (2, 10, 2), (14, 13, 14)),
        B("offgas_neck", (6, 13, 6), (10, 16, 10)), B("calcine_chute", (5, 1, 0), (11, 6, 4)),
    ],
    "reduction_furnace": [
        B("hearth", (1, 0, 1), (15, 5, 15)), B("shaft", (3, 4, 3), (13, 15, 13)),
        B("lower_bosh", (2, 3, 2), (14, 8, 14)), B("charge_throat", (5, 14, 5), (11, 16, 11)),
        B("tap_hole", (5, 1, 0), (11, 5, 3)), B("gas_manifold", (13, 6, 5), (16, 11, 11)),
    ],
    "leaching_tank": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("lined_tank", (2, 2, 2), (14, 12, 14)),
        B("tank_rim", (1, 10, 1), (15, 13, 15)), B("agitator", (7, 11, 7), (9, 16, 9)),
        B("drive", (5, 13, 5), (11, 16, 11)), B("sampling_box", (3, 5, 0), (8, 10, 3)),
    ],
    "electrolytic_cell": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("electrolyte_tank", (1, 2, 2), (15, 10, 14)),
        B("anode_bank", (3, 8, 3), (5, 15, 13)), B("cathode_bank", (7, 8, 3), (9, 15, 13)),
        B("return_bank", (11, 8, 3), (13, 15, 13)), B("busbar", (1, 13, 1), (15, 16, 4)),
    ],
    "powder_press": [
        B("press_bed", (1, 0, 1), (15, 3, 15)), B("left_columns", (2, 2, 2), (4, 15, 14)),
        B("right_columns", (12, 2, 2), (14, 15, 14)), B("top_crosshead", (1, 13, 1), (15, 16, 15)),
        B("hydraulic_ram", (6, 7, 5), (10, 14, 11)), B("die_table", (4, 3, 3), (12, 7, 13)),
    ],
    "chemical_reactor": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("pressure_vessel", (3, 2, 3), (13, 13, 13)),
        B("jacket_low", (2, 3, 2), (14, 7, 14)), B("jacket_high", (2, 9, 2), (14, 12, 14)),
        B("agitator_drive", (5, 12, 5), (11, 16, 11)), B("product_nozzle", (12, 5, 5), (16, 9, 11)),
    ],
    "distillation_column": [
        B("reboiler", (1, 0, 2), (15, 5, 14)), B("column", (5, 3, 5), (11, 16, 11)),
        B("lower_tray_ring", (3, 5, 3), (13, 7, 13)), B("middle_tray_ring", (3, 9, 3), (13, 11, 13)),
        B("upper_tray_ring", (3, 13, 3), (13, 15, 13)), B("side_draw", (10, 8, 6), (16, 11, 10)),
    ],
    "mixer": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("mixing_trough", (2, 4, 2), (14, 12, 14)),
        B("left_end", (1, 5, 4), (4, 11, 12)), B("right_end", (12, 5, 4), (15, 11, 12)),
        B("top_feed", (5, 11, 5), (11, 16, 11)), B("bottom_gate", (5, 2, 5), (11, 6, 11)),
    ],
    "crystallizer": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("crystal_vessel", (3, 2, 3), (13, 13, 13)),
        B("circulation_chamber", (1, 5, 5), (5, 12, 11)), B("vapor_head", (5, 11, 5), (11, 16, 11)),
        B("product_leg", (5, 1, 5), (11, 6, 11)), B("service_box", (12, 6, 4), (16, 11, 12)),
    ],
    "industrial_kiln": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("left_support", (2, 2, 3), (5, 7, 13)),
        B("rotary_shell", (2, 5, 2), (14, 13, 14), ("z", -22.5, (8, 9, 8))),
        B("right_support", (11, 2, 3), (14, 7, 13)), B("burner_hood", (0, 5, 5), (4, 12, 11)),
        B("feed_hood", (12, 7, 4), (16, 14, 12)),
    ],
    "gas_separator": [
        B("skid", (1, 0, 1), (15, 2, 15)), B("left_adsorber", (2, 2, 3), (7, 15, 13)),
        B("right_adsorber", (9, 2, 3), (14, 15, 13)), B("top_manifold", (1, 13, 2), (15, 16, 6)),
        B("feed_header", (0, 4, 5), (4, 8, 11)), B("product_header", (12, 4, 5), (16, 8, 11)),
    ],
    "fertilizer_granulator": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("left_bearing", (1, 2, 4), (4, 7, 12)),
        B("granulation_drum", (3, 5, 2), (13, 13, 14), ("z", -22.5, (8, 9, 8))),
        B("right_bearing", (12, 2, 4), (15, 8, 12)), B("powder_feed", (0, 8, 5), (5, 15, 12)),
        B("product_hood", (11, 4, 4), (16, 12, 13)),
    ],
    "polymerizer": [
        B("foundation", (1, 0, 1), (15, 2, 15)), B("autoclave", (3, 2, 3), (13, 13, 13)),
        B("cooling_jacket", (2, 4, 2), (14, 11, 14)), B("sealed_drive", (5, 12, 5), (11, 16, 11)),
        B("monomer_feed", (0, 7, 5), (5, 11, 11)), B("resin_outlet", (11, 4, 5), (16, 8, 11)),
    ],
    "steam_cracker": [
        B("radiant_furnace", (1, 0, 2), (10, 15, 14)), B("coil_chamber", (2, 3, 0), (9, 13, 4)),
        B("convection_bank", (9, 7, 3), (14, 16, 13)), B("quench_section", (12, 3, 4), (16, 13, 12)),
        B("burner_floor", (0, 0, 1), (11, 4, 15)), B("stack", (11, 12, 6), (15, 16, 10)),
    ],
    "synthesis_loop": [
        B("skid", (1, 0, 1), (15, 2, 15)), B("reactor", (5, 2, 5), (11, 16, 11)),
        B("compressor", (1, 3, 3), (6, 9, 13)), B("separator", (11, 4, 4), (15, 14, 12)),
        B("upper_recycle", (2, 12, 2), (14, 15, 5)), B("lower_recycle", (2, 2, 11), (14, 5, 14)),
    ],
    "absorption_tower": [
        B("sump", (1, 0, 1), (15, 5, 15)), B("packed_column", (5, 3, 5), (11, 16, 11)),
        B("lower_packing_ring", (3, 5, 3), (13, 7, 13)), B("upper_packing_ring", (3, 11, 3), (13, 13, 13)),
        B("recirculation_pump", (0, 3, 5), (5, 9, 11)), B("spray_header", (4, 14, 4), (12, 16, 12)),
    ],
    "combustion_generator": [
        B("skid", (1, 0, 1), (15, 2, 15)), B("prime_mover", (2, 2, 3), (9, 12, 13)),
        B("alternator", (8, 4, 4), (14, 11, 12)), B("coupling_guard", (7, 5, 5), (10, 10, 11)),
        B("control_cabinet", (1, 8, 1), (6, 15, 5)), B("exhaust_stack", (11, 10, 8), (14, 16, 11)),
    ],
    "steam_turbine_generator": [
        B("skid", (1, 0, 1), (15, 2, 15)), B("turbine_low", (1, 4, 3), (8, 12, 13)),
        B("turbine_high", (3, 7, 2), (10, 14, 14)), B("generator", (9, 5, 4), (15, 13, 12)),
        B("shaft_guard", (7, 6, 6), (11, 11, 10)), B("steam_chest", (1, 11, 5), (6, 16, 11)),
    ],
}


def load_profiles() -> list[tuple[str, dict[str, object]]]:
    data = json.loads(PROFILES.read_text(encoding="utf-8"))
    return [
        *(("machines", profile) for profile in data["machines"]),
        *(("devices", profile) for profile in data.get("devices", [])),
    ]


def texture_roles(model_path: pathlib.Path) -> dict[str, str]:
    data = json.loads(model_path.read_text(encoding="utf-8-sig"))
    textures = data.get("textures", {})
    fallback = textures.get("all", textures.get("particle", "minecraft:block/iron_block"))
    return {
        "particle": textures.get("particle", fallback),
        "front": textures.get("front", textures.get("north", fallback)),
        "back": textures.get("back", textures.get("south", fallback)),
        "left": textures.get("left", textures.get("west", fallback)),
        "right": textures.get("right", textures.get("east", fallback)),
        "top": textures.get("top", textures.get("up", fallback)),
        "bottom": textures.get("bottom", textures.get("down", fallback)),
        "fault": "minecraft:block/redstone_lamp_on",
    }


def uv_for(face: str, start: list[float], end: list[float]) -> list[float]:
    x1, y1, z1 = start
    x2, y2, z2 = end
    return {
        "north": [x1, 16 - y2, x2, 16 - y1],
        "south": [16 - x2, 16 - y2, 16 - x1, 16 - y1],
        "west": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
        "east": [z1, 16 - y2, z2, 16 - y1],
        "up": [x1, z1, x2, z2],
        "down": [x1, 16 - z2, x2, 16 - z1],
    }[face]


def runtime_element(machine_id: str, entry: dict[str, object], fault: bool = False) -> dict[str, object]:
    start = entry["from"]
    end = entry["to"]
    roles = {"north": "front", "south": "back", "west": "left", "east": "right", "up": "top", "down": "bottom"}
    element: dict[str, object] = {
        "name": entry["name"],
        "from": start,
        "to": end,
        "faces": {face: {"uv": uv_for(face, start, end), "texture": f"#{'fault' if fault else roles[face]}"}
                  for face in FACES},
    }
    if "rotation" in entry:
        element["rotation"] = {**entry["rotation"], "rescale": True}
    return element


def runtime_model(machine_id: str, textures: dict[str, str], state: str) -> dict[str, object]:
    entries = list(SHAPES[machine_id])
    elements = [runtime_element(machine_id, entry) for entry in entries]
    if state == "fault":
        alarm = B("fault_beacon", (6.5, 5, 0), (9.5, 8, 1))
        elements.append(runtime_element(machine_id, alarm, fault=True))
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": True,
        "gui_light": "side",
        "textures": textures,
        "elements": elements,
    }


def blockstate(machine_id: str, processing_machine: bool) -> dict[str, object]:
    variants: dict[str, object] = {}
    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    if processing_machine:
        for active in (False, True):
            for fault in (False, True):
                model = f"earth_on_minecraft:block/{machine_id}{'_fault' if fault else '_active' if active else ''}"
                for facing, y in rotations.items():
                    value: dict[str, object] = {"model": model}
                    if y:
                        value["y"] = y
                    variants[f"active={str(active).lower()},facing={facing},fault={str(fault).lower()}"] = value
    else:
        for active in (False, True):
            model = f"earth_on_minecraft:block/{machine_id}{'_active' if active else ''}"
            for facing, y in rotations.items():
                value = {"model": model}
                if y:
                    value["y"] = y
                variants[f"active={str(active).lower()},facing={facing}"] = value
    return {"variants": variants}


def blockbench_source(machine_id: str, profile: dict[str, object], entries: list[dict[str, object]],
                      idle: dict[str, str], active: dict[str, str]) -> dict[str, object]:
    texture_ids = list(dict.fromkeys(idle[role] for role in ("front", "back", "left", "right", "top", "bottom")))
    texture_index = {texture: index for index, texture in enumerate(texture_ids)}
    elements: list[dict[str, object]] = []
    outliner: list[str] = []
    roles = {"north": "front", "south": "back", "west": "left", "east": "right", "up": "top", "down": "bottom"}
    for index, entry in enumerate(entries):
        element_uuid = str(uuid.uuid5(uuid.NAMESPACE_URL, f"earth-on-minecraft:{machine_id}:{index}:{entry['name']}"))
        start = entry["from"]
        end = entry["to"]
        element: dict[str, object] = {
            "name": entry["name"], "box_uv": False, "rescale": bool(entry.get("rotation")),
            "locked": False, "from": start, "to": end, "autouv": 0, "color": index % 8,
            "origin": entry.get("rotation", {}).get("origin", [8, 8, 8]), "uuid": element_uuid,
            "faces": {face: {"uv": uv_for(face, start, end), "texture": texture_index[idle[roles[face]]]}
                      for face in FACES},
            "type": "cube",
        }
        if "rotation" in entry:
            rotation = [0.0, 0.0, 0.0]
            rotation[{"x": 0, "y": 1, "z": 2}[entry["rotation"]["axis"]]] = entry["rotation"]["angle"]
            element["rotation"] = rotation
        elements.append(element)
        outliner.append(element_uuid)

    textures = []
    source_dir = BLOCKBENCH / ("machines" if profile.get("kind") not in {"COMBUSTION_GENERATOR", "STEAM_TURBINE_GENERATOR"} else "devices")
    for index, texture_id in enumerate(texture_ids):
        namespace, separator, texture_path = texture_id.partition(":")
        if not separator:
            namespace, texture_path = "minecraft", namespace
        path = "" if namespace != "earth_on_minecraft" else str(pathlib.Path("../../..") / "neoforge-26.2" / "src" / "main" / "resources" / "assets" / namespace / "textures" / f"{texture_path}.png").replace("\\", "/")
        texture = {
            "name": pathlib.Path(texture_path).name + ".png", "folder": "",
            "namespace": namespace, "id": str(index), "particle": texture_id == idle["particle"],
            "render_mode": "default", "render_sides": "auto", "frame_time": 1,
            "frame_order_type": "loop", "frame_order": "", "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"earth-texture:{texture_id}")),
        }
        if path:
            texture["relative_path"] = path
        textures.append(texture)
    return {
        "meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False},
        "name": machine_id, "model_identifier": f"earth_on_minecraft:{machine_id}",
        "visible_box": [1, 1, 0], "variable_placeholders": "", "resolution": {"width": 128, "height": 128},
        "elements": elements, "outliner": outliner, "textures": textures,
        "earth_on_minecraft": {
            "schema_version": 1, "prototype": profile["prototype"], "silhouette": profile["silhouette"],
            "faces": profile["faces"], "states": {"idle": idle, "running": active, "fault": {**idle, "fault": "minecraft:block/redstone_lamp_on"}},
        },
    }


def validate(profiles: list[tuple[str, dict[str, object]]]) -> list[str]:
    failures: list[str] = []
    profile_ids = {str(profile["id"]) for _, profile in profiles}
    if profile_ids != set(SHAPES):
        failures.append(f"shape coverage mismatch missing={sorted(profile_ids - set(SHAPES))} extra={sorted(set(SHAPES) - profile_ids)}")
    signatures: set[tuple[tuple[float, ...], ...]] = set()
    for machine_id in sorted(profile_ids & set(SHAPES)):
        entries = SHAPES[machine_id]
        if len(entries) < 5:
            failures.append(f"{machine_id} has fewer than five geometry elements")
        names: set[str] = set()
        signature: list[tuple[float, ...]] = []
        for entry in entries:
            names.add(str(entry["name"]))
            start = [float(value) for value in entry["from"]]
            end = [float(value) for value in entry["to"]]
            if any(value < 0 or value > 16 for value in (*start, *end)):
                failures.append(f"{machine_id}/{entry['name']} exceeds block bounds")
            if any(end[index] <= start[index] for index in range(3)):
                failures.append(f"{machine_id}/{entry['name']} has non-positive dimensions")
            signature.append(tuple((*start, *end)))
        if len(names) != len(entries):
            failures.append(f"{machine_id} has duplicate element names")
        frozen = tuple(sorted(signature))
        if frozen in signatures:
            failures.append(f"{machine_id} duplicates another machine geometry signature")
        signatures.add(frozen)
    return failures


def main() -> None:
    profiles = load_profiles()
    failures = validate(profiles)
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}")
        raise SystemExit(1)

    records: list[dict[str, object]] = []
    for group, profile in profiles:
        machine_id = str(profile["id"])
        idle_path = MODELS / f"{machine_id}.json"
        active_path = MODELS / f"{machine_id}_active.json"
        idle = texture_roles(idle_path)
        active = texture_roles(active_path)
        for state, textures in (("idle", idle), ("active", active), ("fault", idle)):
            suffix = "" if state == "idle" else f"_{state}"
            (MODELS / f"{machine_id}{suffix}.json").write_text(
                json.dumps(runtime_model(machine_id, textures, state), indent=2) + "\n", encoding="utf-8")
        (BLOCKSTATES / f"{machine_id}.json").write_text(
            json.dumps(blockstate(machine_id, group == "machines"), indent=2) + "\n", encoding="utf-8")
        source_dir = BLOCKBENCH / group
        source_dir.mkdir(parents=True, exist_ok=True)
        source_path = source_dir / f"{machine_id}.bbmodel"
        source_path.write_text(
            json.dumps(blockbench_source(machine_id, profile, SHAPES[machine_id], idle, active), indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8")
        records.append({
            "id": machine_id, "group": group, "elements": len(SHAPES[machine_id]),
            "source": source_path.relative_to(ROOT).as_posix(),
            "models": [f"models/block/{machine_id}.json", f"models/block/{machine_id}_active.json", f"models/block/{machine_id}_fault.json"],
        })

    OUTPUT.mkdir(parents=True, exist_ok=True)
    report = {"schema_version": 1, "profiles": len(records), "geometry_sources": len(records), "records": records, "failures": []}
    report_path = OUTPUT / "blockbench-generation-report.json"
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"BLOCKBENCH_GENERATION_OK profiles={len(records)} sources={len(records)} report={report_path}")


if __name__ == "__main__":
    main()
