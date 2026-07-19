from __future__ import annotations

import json
import re
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
MODULE = ROOT / "neoforge-26.2"
JAVA_REGISTRY = MODULE / "src/main/java/com/xxsx/earthonminecraft/EarthOnMinecraft.java"
ASSETS = MODULE / "src/main/resources/assets/earth_on_minecraft"
TEXTURES = ASSETS / "textures/block"
CONNECTED_TEXTURES = TEXTURES / "connected"
CONNECTED_MODELS = ASSETS / "models/block/connected"

MODEL_SIZE = 16.0
EDGE_BORDER = 1.5
CENTER_CROP = 2.5
CENTER_EDGE_BLEND = 0.5


def ore_ids() -> list[str]:
    source = JAVA_REGISTRY.read_text(encoding="utf-8")
    return re.findall(r'oreBlock\("([a-z0-9_]+)"', source)


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


def _blend_toward_pair_average(first: tuple[int, ...], second: tuple[int, ...], weight: float) \
        -> tuple[tuple[int, ...], tuple[int, ...]]:
    average = tuple(round((left + right) / 2) for left, right in zip(first, second, strict=True))
    blended_first = tuple(round(value + (target - value) * weight)
                          for value, target in zip(first, average, strict=True))
    blended_second = tuple(round(value + (target - value) * weight)
                           for value, target in zip(second, average, strict=True))
    return blended_first, blended_second


def match_periodic_edges(image: Image.Image, blend_pixels: int) -> Image.Image:
    """Match opposite texture edges without moving or mirroring the central mineral pattern."""
    result = image.copy()
    pixels = result.load()
    width, height = result.size
    blend_pixels = max(0, min(blend_pixels, width // 4, height // 4))
    if blend_pixels == 0:
        return result

    for inset in range(blend_pixels):
        weight = ((blend_pixels - inset) / blend_pixels) ** 2
        left_x = inset
        right_x = width - 1 - inset
        for y in range(height):
            pixels[left_x, y], pixels[right_x, y] = _blend_toward_pair_average(
                pixels[left_x, y], pixels[right_x, y], weight)

    for inset in range(blend_pixels):
        weight = ((blend_pixels - inset) / blend_pixels) ** 2
        top_y = inset
        bottom_y = height - 1 - inset
        for x in range(width):
            pixels[x, top_y], pixels[x, bottom_y] = _blend_toward_pair_average(
                pixels[x, top_y], pixels[x, bottom_y], weight)
    return result


def build_center_texture(source: Image.Image, crop_units: float = CENTER_CROP,
                         blend_units: float = CENTER_EDGE_BLEND) -> Image.Image:
    inset_x = max(1, round(source.width * crop_units / MODEL_SIZE))
    inset_y = max(1, round(source.height * crop_units / MODEL_SIZE))
    center = source.crop((inset_x, inset_y, source.width - inset_x, source.height - inset_y))
    center = center.resize(source.size, Image.Resampling.LANCZOS)
    blend_pixels = round(min(source.size) * blend_units / MODEL_SIZE)
    return match_periodic_edges(center, blend_pixels)


def make_center_texture(block_id: str) -> None:
    source_path = TEXTURES / f"{block_id}.png"
    target_path = CONNECTED_TEXTURES / f"{block_id}_center.png"
    with Image.open(source_path).convert("RGBA") as source:
        center = build_center_texture(source)
        target_path.parent.mkdir(parents=True, exist_ok=True)
        center.save(target_path, optimize=True)


def face_element(face: str, horizontal: tuple[float, float], vertical: tuple[float, float],
                 uv: tuple[float, float, float, float], offset: float) -> dict[str, object]:
    h0, h1 = horizontal
    v0, v1 = vertical
    if face == "north":
        start, end = [h0, v0, -offset], [h1, v1, 0]
    elif face == "south":
        start, end = [h0, v0, MODEL_SIZE], [h1, v1, MODEL_SIZE + offset]
    elif face == "west":
        start, end = [-offset, v0, h0], [0, v1, h1]
    elif face == "east":
        start, end = [MODEL_SIZE, v0, h0], [MODEL_SIZE + offset, v1, h1]
    elif face == "down":
        start, end = [h0, -offset, v0], [h1, 0, v1]
    elif face == "up":
        start, end = [h0, MODEL_SIZE, v0], [h1, MODEL_SIZE + offset, v1]
    else:
        raise ValueError(face)
    return {
        "from": start,
        "to": end,
        "shade": False,
        "faces": {
            face: {
                "uv": list(uv),
                "texture": "#ore",
                "cullface": face,
            }
        },
    }


def edge_elements(direction: str) -> list[dict[str, object]]:
    lo = 0.0
    hi = MODEL_SIZE
    edge = EDGE_BORDER
    far = MODEL_SIZE - EDGE_BORDER
    low_offset = 0.005
    medium_offset = 0.010
    high_offset = 0.015

    if direction == "west":
        uv = (lo, lo, edge, hi)
        return [face_element(face, (lo, edge), (lo, hi), uv, high_offset)
                for face in ("north", "south", "down", "up")]
    if direction == "east":
        uv = (far, lo, hi, hi)
        return [face_element(face, (far, hi), (lo, hi), uv, high_offset)
                for face in ("north", "south", "down", "up")]
    if direction == "north":
        vertical_uv = (lo, lo, edge, hi)
        horizontal_uv = (lo, lo, hi, edge)
        return [
            face_element("west", (lo, edge), (lo, hi), vertical_uv, high_offset),
            face_element("east", (lo, edge), (lo, hi), vertical_uv, high_offset),
            face_element("down", (lo, hi), (lo, edge), horizontal_uv, medium_offset),
            face_element("up", (lo, hi), (lo, edge), horizontal_uv, medium_offset),
        ]
    if direction == "south":
        vertical_uv = (far, lo, hi, hi)
        horizontal_uv = (lo, far, hi, hi)
        return [
            face_element("west", (far, hi), (lo, hi), vertical_uv, high_offset),
            face_element("east", (far, hi), (lo, hi), vertical_uv, high_offset),
            face_element("down", (lo, hi), (far, hi), horizontal_uv, medium_offset),
            face_element("up", (lo, hi), (far, hi), horizontal_uv, medium_offset),
        ]
    if direction == "down":
        uv = (lo, far, hi, hi)
        return [face_element(face, (lo, hi), (lo, edge), uv, low_offset)
                for face in ("north", "south", "west", "east")]
    if direction == "up":
        uv = (lo, lo, hi, edge)
        return [face_element(face, (lo, hi), (far, hi), uv, low_offset)
                for face in ("north", "south", "west", "east")]
    raise ValueError(direction)


def make_models(block_id: str) -> None:
    original = f"earth_on_minecraft:block/{block_id}"
    center = f"earth_on_minecraft:block/connected/{block_id}_center"
    write_json(CONNECTED_MODELS / f"{block_id}_center.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": center},
    })
    for direction in ("down", "up", "north", "south", "west", "east"):
        write_json(CONNECTED_MODELS / f"{block_id}_edge_{direction}.json", {
            "ambientocclusion": False,
            "textures": {"particle": original, "ore": original},
            "elements": edge_elements(direction),
        })


def main() -> None:
    ids = ore_ids()
    if not ids:
        raise SystemExit("No oreBlock registrations found")
    for block_id in ids:
        make_center_texture(block_id)
        make_models(block_id)
    print(f"Generated connected ore assets for {len(ids)} blocks")


if __name__ == "__main__":
    main()
