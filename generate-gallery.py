#!/usr/bin/env python3
"""
Generate gallery.json and optimized gallery assets for dhryciuk.eu.

Default behavior scans all folders under the selected source roots and imports
folders that contain supported image files. Requires Pillow:

    python -m pip install pillow
    python generate-gallery.py --source D:\\ --source I:\\
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageOps

IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".tif", ".tiff"}
CATEGORIES = {
    "przyroda": ["przyroda", "natura", "las", "krajobraz"],
    "figa": ["figa"],"zoja": ["zoja"],
    "motoryzacja": ["moto", "motocykl", "motoryzacja", "samochod", "auto"],
    "podroze": ["podroze", "podróże", "wyjazd", "trasa"],"budowa": ["budowa", "budynek", "dom", "chata"],
}


@dataclass
class Album:
    id: str
    title: str
    category: str
    source: Path
    photos: list[dict]


def slugify(value: str) -> str:
    value = value.lower()
    replacements = {"ą": "a", "ć": "c", "ę": "e", "ł": "l", "ń": "n", "ó": "o", "ś": "s", "ż": "z", "ź": "z"}
    for src, dst in replacements.items():
        value = value.replace(src, dst)
    value = re.sub(r"[^a-z0-9]+", "-", value).strip("-")
    return value or "album"


def category_for(path: Path) -> str:
    text = str(path).lower()
    for category, keywords in CATEGORIES.items():
        if any(keyword in text for keyword in keywords):
            return category
    return "podroze"


def find_albums(sources: Iterable[Path]) -> list[Path]:
    albums: list[Path] = []
    for source in sources:
        if not source.exists():
            continue
        for folder in source.rglob("*"):
            if folder.is_dir():
                if any(child.suffix.lower() in IMAGE_EXTENSIONS for child in folder.rglob("*") if child.is_file()):
                    albums.append(folder)
    return sorted(set(albums), key=lambda item: str(item).lower())


def save_image(source: Path, destination: Path, max_size: int, quality: int) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    with Image.open(source) as image:
        image = ImageOps.exif_transpose(image)
        image.thumbnail((max_size, max_size), Image.Resampling.LANCZOS)
        if image.mode not in ("RGB", "L"):
            image = image.convert("RGB")
        image.save(destination, "WEBP", quality=quality, method=6)


def build_album(source: Path, output: Path, album_index: int) -> Album:
    album_id = f"{slugify(source.name)}-{album_index + 1}"
    album_dir = output / "galleries" / category_for(source) / album_id
    photo_dir = album_dir / "photos"
    thumb_dir = album_dir / "thumbs"
    photos: list[dict] = []

    images = sorted(
        [path for path in source.rglob("*") if path.is_file() and path.suffix.lower() in IMAGE_EXTENSIONS],
        key=lambda item: item.name.lower(),
    )

    for index, image in enumerate(images, start=1):
        stem = f"{index:05d}-{slugify(image.stem)}"
        photo_path = photo_dir / f"{stem}.webp"
        thumb_path = thumb_dir / f"{stem}.webp"
        save_image(image, photo_path, max_size=2200, quality=86)
        save_image(image, thumb_path, max_size=520, quality=72)
        photos.append(
            {
                "src": photo_path.relative_to(output).as_posix(),
                "thumbnail": thumb_path.relative_to(output).as_posix(),
                "alt": f"{source.name} - zdjęcie {index}",
                "width": 0,
                "height": 0,
            }
        )

    return Album(
        id=album_id,
        title=source.name,
        category=category_for(source),
        source=source,
        photos=photos,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate static gallery assets.")
    parser.add_argument("--source", action="append", default=[], help="Root folder to scan. Can be used multiple times.")
    parser.add_argument("--output", default=".", help="Project output directory.")
    parser.add_argument("--clean", action="store_true", help="Remove generated gallery folders before creating new ones.")
    args = parser.parse_args()

    output = Path(args.output).resolve()
    gallery_root = output / "galleries"
    if args.clean and gallery_root.exists():
        shutil.rmtree(gallery_root)

    for category in CATEGORIES:
        (gallery_root / category).mkdir(parents=True, exist_ok=True)

    sources = [Path(item) for item in (args.source or ["D:\\"])]
    source_albums = find_albums(sources)
    albums = [build_album(path, output, index) for index, path in enumerate(source_albums)]

    payload = {
        "generatedAt": __import__("datetime").datetime.utcnow().isoformat(timespec="seconds") + "Z",
        "sourceFilter": None,
        "albums": [
            {
                "id": album.id,
                "title": album.title,
                "category": album.category,
                "source": str(album.source),
                "photos": album.photos,
            }
            for album in albums
        ],
    }

    (output / "gallery.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Generated {sum(len(album.photos) for album in albums)} photos in {len(albums)} albums.")


if __name__ == "__main__":
    main()
