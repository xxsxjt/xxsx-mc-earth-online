#!/usr/bin/env python3
"""Generate Blockbench briefs and audit machine model face/state coverage."""

from __future__ import annotations

import argparse
import json
import math
import pathlib

from PIL import Image, ImageDraw, ImageFont


ROOT = pathlib.Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "neoforge-26.2" / "src" / "main" / "resources"
ASSETS = RESOURCES / "assets" / "earth_on_minecraft"
MODELS = ASSETS / "models" / "block"
TEXTURES = ASSETS / "textures" / "block"
PROFILES = RESOURCES / "data" / "earth_on_minecraft" / "earth" / "quality" / "machine_profiles.json"
BLOCKBENCH_ROOT = ROOT / "art" / "blockbench"
OUTPUT = ROOT / "output" / "quality"
BRIEFS = OUTPUT / "blockbench-briefs"
FACE_KEYS = ("north", "south", "west", "east", "up", "down")
FACE_TEXTURE_KEYS = {
    "north": "front", "south": "back", "west": "left", "east": "right", "up": "top", "down": "bottom",
}


def texture_id_to_path(texture_id: str) -> pathlib.Path | None:
    if texture_id.startswith("#"):
        return None
    namespace, separator, path = texture_id.partition(":")
    if not separator:
        namespace, path = "minecraft", namespace
    if namespace != "earth_on_minecraft" or not path.startswith("block/"):
        return None
    return TEXTURES / f"{path.removeprefix('block/')}.png"


def model_faces(path: pathlib.Path) -> dict[str, str]:
    data = json.loads(path.read_text(encoding="utf-8-sig"))
    textures = data.get("textures", {})
    return {
        face: textures.get(FACE_TEXTURE_KEYS[face], textures.get(face, textures.get("all", "")))
        for face in FACE_KEYS
    }


def audit_and_write_briefs() -> tuple[list[dict[str, object]], list[str], list[str]]:
    data = json.loads(PROFILES.read_text(encoding="utf-8"))
    machines: list[dict[str, object]] = [
        *({**profile, "profile_group": "machine"} for profile in data["machines"]),
        *({**profile, "profile_group": "device"} for profile in data.get("devices", [])),
    ]
    failures: list[str] = []
    warnings: list[str] = []
    records: list[dict[str, object]] = []
    BRIEFS.mkdir(parents=True, exist_ok=True)
    for profile in machines:
        machine_id = str(profile["id"])
        profile_group = str(profile["profile_group"])
        idle_model = MODELS / f"{machine_id}.json"
        active_model = MODELS / f"{machine_id}_active.json"
        fault_model = MODELS / f"{machine_id}_fault.json"
        blockstate = ASSETS / "blockstates" / f"{machine_id}.json"
        source = BLOCKBENCH_ROOT / ("machines" if profile_group == "machine" else "devices") / f"{machine_id}.bbmodel"
        for required in (idle_model, active_model, fault_model, blockstate):
            if not required.exists():
                failures.append(f"missing machine asset {required.relative_to(ROOT)}")
        idle_faces = model_faces(idle_model) if idle_model.exists() else {}
        active_faces = model_faces(active_model) if active_model.exists() else {}
        referenced = {value for value in (*idle_faces.values(), *active_faces.values()) if value}
        missing_textures = sorted(
            texture_id for texture_id in referenced
            if (path := texture_id_to_path(texture_id)) is not None and not path.exists()
        )
        failures.extend(f"missing texture {texture_id} for {machine_id}" for texture_id in missing_textures)
        unique_idle_faces = len(set(idle_faces.values()))
        if idle_faces and unique_idle_faces < 3:
            failures.append(f"{machine_id} has fewer than three distinguishable idle face roles")
        if source.exists():
            try:
                source_data = json.loads(source.read_text(encoding="utf-8-sig"))
                meta = source_data.get("meta", {}) if isinstance(source_data, dict) else {}
                if meta.get("format_version") != "5.0" or meta.get("model_format") != "java_block":
                    failures.append(f"invalid Blockbench source metadata in {source.relative_to(ROOT)}")
                elements = source_data.get("elements", [])
                if not isinstance(elements, list) or len(elements) < 5:
                    failures.append(f"insufficient Blockbench geometry in {source.relative_to(ROOT)}")
                for element in elements:
                    rotation = element.get("rotation")
                    if rotation is not None and (not isinstance(rotation, list) or len(rotation) != 3):
                        failures.append(f"invalid Blockbench rotation in {source.relative_to(ROOT)}: {element.get('name')}")
                for texture in source_data.get("textures", []):
                    relative_path = texture.get("relative_path")
                    if texture.get("namespace") == "earth_on_minecraft" and (
                            not relative_path or not (source.parent / relative_path).resolve().exists()):
                        failures.append(f"unresolved Blockbench texture in {source.relative_to(ROOT)}: {texture.get('name')}")
            except (json.JSONDecodeError, UnicodeDecodeError) as error:
                failures.append(f"invalid Blockbench source {source.relative_to(ROOT)}: {error}")
        else:
            failures.append(f"missing editable Blockbench geometry source {source.relative_to(ROOT)}")
        brief = {
            "schema_version": 1,
            "id": machine_id,
            "real_prototype": profile["prototype"],
            "real_prototype_zh": profile["prototype_zh"],
            "silhouette": profile["silhouette"],
            "face_responsibilities": profile["faces"],
            "state_cues": {state: profile[f"{state}_cue"] for state in ("idle", "running", "fault")},
            "sound_event": profile["sound_event"],
            "particle_profile": profile["particle_profile"],
            "profile_group": profile_group,
            "blockbench_source": source.relative_to(ROOT).as_posix() if source.exists() else None,
            "geometry_status": "source_present" if source.exists() else "missing_source",
            "model_rules": [
                "Use coherent exterior geometry; never paste a cutaway interior onto one cube face.",
                "Keep idle, running, and fault geometry aligned; animate only physically meaningful parts.",
                "Define collision, selection, origin, pivots, UV ownership, and formed-multiblock role.",
                "Export final Minecraft model JSON and retain the .bbmodel source outside generated resources."
            ],
            "current_idle_faces": idle_faces,
            "current_active_faces": active_faces,
            "current_unique_idle_face_textures": unique_idle_faces,
        }
        (BRIEFS / f"{machine_id}.json").write_text(
            json.dumps(brief, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        records.append(brief)
    for source in sorted((BLOCKBENCH_ROOT / "entities").glob("*.bbmodel")):
        try:
            data = json.loads(source.read_text(encoding="utf-8-sig"))
            if not isinstance(data, dict) or not data.get("meta"):
                failures.append(f"invalid entity Blockbench source metadata in {source.relative_to(ROOT)}")
        except (json.JSONDecodeError, UnicodeDecodeError) as error:
            failures.append(f"invalid entity Blockbench source {source.relative_to(ROOT)}: {error}")
    return records, failures, warnings


def first_texture(faces: dict[str, str], role: str) -> pathlib.Path | None:
    path = texture_id_to_path(faces.get(role, ""))
    return path if path is not None and path.exists() else None


def rotate_point(point: tuple[float, float, float], axis: str, angle: float,
                 origin: tuple[float, float, float]) -> tuple[float, float, float]:
    radians = math.radians(angle)
    cosine = math.cos(radians)
    sine = math.sin(radians)
    x, y, z = (point[index] - origin[index] for index in range(3))
    if axis == "x":
        y, z = y * cosine - z * sine, y * sine + z * cosine
    elif axis == "y":
        x, z = x * cosine + z * sine, -x * sine + z * cosine
    else:
        x, y = x * cosine - y * sine, x * sine + y * cosine
    return x + origin[0], y + origin[1], z + origin[2]


def average_color(path: pathlib.Path | None) -> tuple[int, int, int]:
    if path is None:
        return 105, 125, 138
    with Image.open(path).convert("RGB") as image:
        pixel = image.resize((1, 1), Image.Resampling.BOX).getpixel((0, 0))
    return tuple(max(54, min(210, channel)) for channel in pixel)


def shade(color: tuple[int, int, int], factor: float) -> tuple[int, int, int]:
    return tuple(max(0, min(255, round(channel * factor))) for channel in color)


def render_geometry(record: dict[str, object], size: int = 104) -> Image.Image:
    source = ROOT / str(record["blockbench_source"])
    data = json.loads(source.read_text(encoding="utf-8-sig"))
    base = average_color(first_texture(record["current_idle_faces"], "north"))
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)

    def project(point: tuple[float, float, float]) -> tuple[float, float]:
        x, y, z = point
        return size / 2 + (x - z) * 2.55, size - 15 + (x + z) * 1.22 - y * 2.85

    elements = sorted(data.get("elements", []), key=lambda element: sum(element["from"]) + sum(element["to"]))
    for element in elements:
        x1, y1, z1 = (float(value) for value in element["from"])
        x2, y2, z2 = (float(value) for value in element["to"])
        faces = [
            ([(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)], shade(base, 1.24)),
            ([(x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1)], shade(base, 0.92)),
            ([(x2, y1, z1), (x2, y1, z2), (x2, y2, z2), (x2, y2, z1)], shade(base, 0.70)),
        ]
        rotation = element.get("rotation", [0.0, 0.0, 0.0])
        if isinstance(rotation, list):
            axis_index = max(range(3), key=lambda index: abs(float(rotation[index])))
            angle = float(rotation[axis_index])
            axis = "xyz"[axis_index]
        else:
            angle = float(rotation)
            axis = str(element.get("rotation_axis", "y"))
        origin = tuple(float(value) for value in element.get("origin", [8, 8, 8]))
        for points, color in faces:
            transformed = [rotate_point(point, axis, angle, origin) if angle else point for point in points]
            draw.polygon([project(point) for point in transformed], fill=(*color, 255), outline=(25, 31, 35, 255))
    return canvas


def render_contact_sheet(records: list[dict[str, object]]) -> pathlib.Path:
    columns = 4
    cell_w = 350
    cell_h = 208
    rows = math.ceil(len(records) / columns)
    sheet = Image.new("RGBA", (columns * cell_w, rows * cell_h), "#161a1e")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    roles = ("north", "west", "up")
    for index, record in enumerate(records):
        x = (index % columns) * cell_w
        y = (index // columns) * cell_h
        draw.rectangle((x, y, x + cell_w - 1, y + cell_h - 1), outline="#53606a")
        draw.text((x + 8, y + 6), str(record["id"]), fill="#ffffff", font=font)
        faces = record["current_idle_faces"]
        sheet.alpha_composite(render_geometry(record), (x + 4, y + 21))
        for role_index, role in enumerate(roles):
            texture = first_texture(faces, role)
            px = x + 112 + role_index * 72
            py = y + 24
            if texture is not None:
                with Image.open(texture).convert("RGBA") as image:
                    preview = image.resize((64, 64), Image.Resampling.NEAREST)
                sheet.alpha_composite(preview, (px, py))
            draw.text((px, py + 67), role, fill="#c9d0d5", font=font)
        prototype = str(record["real_prototype"])
        draw.text((x + 8, y + 132), prototype[:52], fill="#aeb9c0", font=font)
        draw.text((x + 8, y + 152), f"elements: {len(json.loads((ROOT / str(record['blockbench_source'])).read_text(encoding='utf-8-sig')).get('elements', []))}", fill="#78b8d6", font=font)
        draw.text((x + 112, y + 152), f"unique face textures: {record['current_unique_idle_face_textures']}", fill="#78b8d6", font=font)
        source_status = ("Blockbench source present" if record["blockbench_source"]
                         else "Blockbench source pending; texture-reference model")
        draw.text((x + 8, y + 174), source_status, fill="#e0aa63", font=font)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / "machine-model-contact-sheet.png"
    sheet.convert("RGB").save(path, quality=95)
    return path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.parse_args()
    records, failures, warnings = audit_and_write_briefs()
    contact_sheet = render_contact_sheet(records)
    entity_sources = list((BLOCKBENCH_ROOT / "entities").glob("*.bbmodel"))
    report = {
        "pipeline": "blockbench_models",
        "schema_version": 1,
        "machine_profiles": sum(record["profile_group"] == "machine" for record in records),
        "device_profiles": sum(record["profile_group"] == "device" for record in records),
        "machine_sources": sum(record["blockbench_source"] is not None for record in records),
        "entity_sources": len(entity_sources),
        "pending_machine_sources": [record["id"] for record in records if record["blockbench_source"] is None],
        "briefs": str(BRIEFS.relative_to(ROOT)).replace("\\", "/"),
        "contact_sheet": str(contact_sheet.relative_to(ROOT)).replace("\\", "/"),
        "warnings": warnings,
        "failures": failures,
    }
    report_path = OUTPUT / "blockbench-pipeline-report.json"
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}")
        raise SystemExit(1)
    print(
        f"BLOCKBENCH_PIPELINE_OK profiles={len(records)} sources={report['machine_sources']} "
        f"entities={report['entity_sources']} pending={len(report['pending_machine_sources'])} "
        f"contactSheet={contact_sheet} report={report_path}"
    )


if __name__ == "__main__":
    main()
