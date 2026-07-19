#!/usr/bin/env python3
"""Render deterministic connected-ore shape and family previews."""

from __future__ import annotations

from collections.abc import Iterable
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from generate_connected_ore_assets import (
    CENTER_CROP,
    CENTER_EDGE_BLEND,
    CONNECTED_TEXTURES,
    EDGE_BORDER,
    MODEL_SIZE,
    ROOT,
    TEXTURES,
    build_center_texture,
    ore_ids,
)


OUTPUT = ROOT / "output" / "connected-ore-previews"
SHAPES: dict[str, set[tuple[int, int]]] = {
    "single": {(0, 0)},
    "pair": {(0, 0), (1, 0)},
    "square_2x2": {(0, 0), (1, 0), (0, 1), (1, 1)},
    "l_shape": {(0, 0), (0, 1), (1, 1)},
    "t_shape": {(0, 0), (1, 0), (2, 0), (1, 1)},
    "solid_3x3": {(x, y) for y in range(3) for x in range(3)},
}
EXPECTED_EXPOSED_EDGES = {
    "single": 4,
    "pair": 6,
    "square_2x2": 8,
    "l_shape": 8,
    "t_shape": 10,
    "solid_3x3": 12,
}


def normalized(shape: Iterable[tuple[int, int]]) -> set[tuple[int, int]]:
    points = set(shape)
    min_x = min(x for x, _ in points)
    min_y = min(y for _, y in points)
    return {(x - min_x, y - min_y) for x, y in points}


def exposed_edge_count(shape: set[tuple[int, int]]) -> int:
    return sum(
        (x + dx, y + dy) not in shape
        for x, y in shape
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1))
    )


def render_shape(block_id: str, shape: set[tuple[int, int]],
                 center_override: Image.Image | None = None) -> Image.Image:
    shape = normalized(shape)
    with Image.open(TEXTURES / f"{block_id}.png").convert("RGBA") as source_image:
        source = source_image.copy()
    if center_override is None:
        with Image.open(CONNECTED_TEXTURES / f"{block_id}_center.png").convert("RGBA") as center_image:
            center = center_image.copy()
    else:
        center = center_override.copy()
    if source.size != center.size:
        raise ValueError(f"texture size mismatch for {block_id}: {source.size} != {center.size}")

    width, height = source.size
    border_x = max(1, round(width * EDGE_BORDER / MODEL_SIZE))
    border_y = max(1, round(height * EDGE_BORDER / MODEL_SIZE))
    columns = max(x for x, _ in shape) + 1
    rows = max(y for _, y in shape) + 1
    canvas = Image.new("RGBA", (columns * width, rows * height), (0, 0, 0, 0))

    for x, y in shape:
        canvas.alpha_composite(center, (x * width, y * height))

    # Match the model priority: vertical edges sit slightly above horizontal edges at corners.
    for x, y in shape:
        px = x * width
        py = y * height
        if (x, y - 1) not in shape:
            canvas.alpha_composite(source.crop((0, 0, width, border_y)), (px, py))
        if (x, y + 1) not in shape:
            canvas.alpha_composite(source.crop((0, height - border_y, width, height)),
                                   (px, py + height - border_y))
    for x, y in shape:
        px = x * width
        py = y * height
        if (x - 1, y) not in shape:
            canvas.alpha_composite(source.crop((0, 0, border_x, height)), (px, py))
        if (x + 1, y) not in shape:
            canvas.alpha_composite(source.crop((width - border_x, 0, width, height)),
                                   (px + width - border_x, py))
    return canvas


def fit(image: Image.Image, max_width: int, max_height: int) -> Image.Image:
    scale = min(max_width / image.width, max_height / image.height)
    size = (max(1, round(image.width * scale)), max(1, round(image.height * scale)))
    return image.resize(size, Image.Resampling.NEAREST)


def draw_panel(sheet: Image.Image, image: Image.Image, x: int, y: int, width: int, height: int,
               label: str, font: ImageFont.ImageFont) -> None:
    draw = ImageDraw.Draw(sheet)
    draw.rectangle((x, y, x + width - 1, y + height - 1), fill="#20262b", outline="#56616a")
    fitted = fit(image, width - 20, height - 42)
    sheet.alpha_composite(fitted, (x + (width - fitted.width) // 2, y + 10))
    draw.text((x + 8, y + height - 24), label, font=font, fill="#f1f4f6")


def render_shape_sheet(block_id: str) -> Path:
    cell_width = 330
    cell_height = 330
    columns = 3
    rows = 2
    sheet = Image.new("RGBA", (cell_width * columns, cell_height * rows), "#15191d")
    font = ImageFont.load_default()
    for index, (name, shape) in enumerate(SHAPES.items()):
        actual_edges = exposed_edge_count(shape)
        if actual_edges != EXPECTED_EXPOSED_EDGES[name]:
            raise AssertionError(f"bad fixture {name}: {actual_edges} exposed edges")
        preview = render_shape(block_id, shape)
        draw_panel(sheet, preview, (index % columns) * cell_width, (index // columns) * cell_height,
                   cell_width, cell_height, f"{name} | exposed edges: {actual_edges}", font)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / f"{block_id}-shape-matrix.png"
    sheet.convert("RGB").save(path, quality=95)
    return path


def render_family_sheet(block_ids: list[str]) -> Path:
    cell_width = 270
    cell_height = 300
    columns = 5
    rows = (len(block_ids) + columns - 1) // columns
    sheet = Image.new("RGBA", (cell_width * columns, cell_height * rows), "#15191d")
    font = ImageFont.load_default()
    for index, block_id in enumerate(block_ids):
        preview = render_shape(block_id, SHAPES["square_2x2"])
        draw_panel(sheet, preview, (index % columns) * cell_width, (index // columns) * cell_height,
                   cell_width, cell_height, block_id, font)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / "all-connected-ores-2x2.png"
    sheet.convert("RGB").save(path, quality=95)
    return path


def render_strategy_sheet(block_ids: list[str]) -> Path:
    preferred = [
        "poor_magnetite_ore",
        "chalcopyrite_ore",
        "kimberlite",
        "diamondiferous_kimberlite",
        "lapis_lazuli_ore",
        "redstone_mineral_ore",
        "bauxite_laterite_deposit",
        "evaporite_salt_bed",
    ]
    representatives = [block_id for block_id in preferred if block_id in block_ids]
    cell_width = 330
    cell_height = 300
    strategies = (
        ("current crop 1.5", EDGE_BORDER, 0.0),
        ("deep crop 2.5", CENTER_CROP, 0.0),
        ("deep + narrow periodic match", CENTER_CROP, CENTER_EDGE_BLEND),
    )
    sheet = Image.new("RGBA", (cell_width * len(strategies), cell_height * len(representatives)), "#15191d")
    font = ImageFont.load_default()
    for row, block_id in enumerate(representatives):
        with Image.open(TEXTURES / f"{block_id}.png").convert("RGBA") as source:
            for column, (label, crop_units, blend_units) in enumerate(strategies):
                center = build_center_texture(source, crop_units, blend_units)
                preview = render_shape(block_id, SHAPES["square_2x2"], center)
                draw_panel(sheet, preview, column * cell_width, row * cell_height,
                           cell_width, cell_height, f"{block_id} | {label}", font)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / "center-strategy-comparison.png"
    sheet.convert("RGB").save(path, quality=95)
    return path


def main() -> None:
    block_ids = ore_ids()
    if not block_ids:
        raise SystemExit("No oreBlock registrations found")
    representative = "diamondiferous_kimberlite" if "diamondiferous_kimberlite" in block_ids else block_ids[0]
    shape_sheet = render_shape_sheet(representative)
    family_sheet = render_family_sheet(block_ids)
    strategy_sheet = render_strategy_sheet(block_ids)
    print(f"Shape matrix: {shape_sheet}")
    print(f"Family sheet: {family_sheet}")
    print(f"Strategy comparison: {strategy_sheet}")


if __name__ == "__main__":
    main()
