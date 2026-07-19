#!/usr/bin/env python3
"""Prepare a transparent, small-size-safe NeoForge mod icon from an Image-2 master."""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont


FINAL_SIZE = 1024


def external_background_mask(image: Image.Image) -> Image.Image:
    rgb = image.convert("RGB")
    candidate = Image.new("L", rgb.size)
    candidate.putdata([
        255 if min(pixel) >= 225 and max(pixel) - min(pixel) <= 12 else 0
        for pixel in rgb.get_flattened_data()
    ])

    # Image models sometimes paint a checkerboard instead of returning alpha. Only
    # remove near-neutral bright pixels connected to the canvas edge, preserving
    # enclosed white highlights on the subject.
    exterior = candidate.copy()
    ImageDraw.floodfill(exterior, (0, 0), 128, thresh=0)
    outside = exterior.point(lambda value: 255 if value == 128 else 0)
    return ImageChops.invert(outside)


def square_crop(image: Image.Image, alpha: Image.Image) -> Image.Image:
    bbox = alpha.getbbox()
    if bbox is None:
        raise ValueError("No foreground remained after background extraction")

    left, top, right, bottom = bbox
    subject_size = max(right - left, bottom - top)
    side = min(max(image.size), int(subject_size * 1.08))
    center_x = (left + right) // 2
    center_y = (top + bottom) // 2
    crop_left = max(0, min(image.width - side, center_x - side // 2))
    crop_top = max(0, min(image.height - side, center_y - side // 2))
    crop_box = (crop_left, crop_top, crop_left + side, crop_top + side)

    prepared = image.copy()
    prepared.putalpha(alpha)
    return prepared.crop(crop_box)


def composite_on(image: Image.Image, size: int, color: tuple[int, int, int]) -> Image.Image:
    icon = image.resize((size, size), Image.Resampling.LANCZOS)
    background = Image.new("RGBA", (size, size), (*color, 255))
    background.alpha_composite(icon)
    return background.convert("RGB")


def render_contact_sheet(icon: Image.Image, output: Path) -> None:
    font = ImageFont.load_default()
    margin = 18
    sizes = (256, 128, 64, 32)
    width = sum(sizes) + margin * (len(sizes) + 1)
    row_height = 256 + 42
    sheet = Image.new("RGB", (width, row_height * 2), "#171b1f")
    draw = ImageDraw.Draw(sheet)
    for row, background in enumerate(((231, 234, 235), (31, 37, 42))):
        x = margin
        y = row * row_height + 12
        for size in sizes:
            preview = composite_on(icon, size, background)
            sheet.paste(preview, (x, y + (256 - size)))
            draw.text((x, y + 262), f"{size}x{size}", fill="#f2f4f5", font=font)
            x += size + margin
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, format="PNG")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--raw-copy", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--contact-sheet", type=Path, required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    args.raw_copy.parent.mkdir(parents=True, exist_ok=True)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(args.input, args.raw_copy)

    with Image.open(args.input).convert("RGBA") as source:
        alpha = external_background_mask(source)
        cropped = square_crop(source, alpha)
        icon = cropped.resize((FINAL_SIZE, FINAL_SIZE), Image.Resampling.LANCZOS)
        alpha_min, alpha_max = icon.getchannel("A").getextrema()
        if alpha_min != 0 or alpha_max != 255:
            raise ValueError(f"Expected transparent and opaque pixels, got alpha {alpha_min}..{alpha_max}")
        if any(icon.getpixel(point)[3] != 0 for point in (
                (0, 0), (FINAL_SIZE - 1, 0), (0, FINAL_SIZE - 1),
                (FINAL_SIZE - 1, FINAL_SIZE - 1))):
            raise ValueError("Prepared icon does not have transparent corners")
        icon.save(args.output, format="PNG", optimize=True)
        render_contact_sheet(icon, args.contact_sheet)

    print(f"MOD_ICON_OK output={args.output} raw={args.raw_copy} contactSheet={args.contact_sheet}")


if __name__ == "__main__":
    main()
