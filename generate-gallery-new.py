#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import shutil
from dataclasses import dataclass
from datetime import datetime, UTC
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageOps

IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".tif", ".tiff"}

CATEGORIES = {
    "przyroda": ["przyroda", "natura", "las", "krajobraz"],
    "figa": ["figa"],
    "zoja": ["zoja"],
    "motoryzacja": ["moto", "motocykl", "motoryzacja", "samochod", "auto"],
    "podroze": ["podroze", "podróże", "wyjazd", "trasa"],
    "budowa": ["budowa", "budynek", "dom", "chata"],
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
    repl = {"ą":"a","ć":"c","ę":"e","ł":"l","ń":"n","ó":"o","ś":"s","ż":"z","ź":"z"}
    for s,d in repl.items():
        value = value.replace(s,d)
    return re.sub(r"[^a-z0-9]+","-",value).strip("-") or "album"

def category_for(path: Path) -> str:
    text = str(path).lower()
    for category, keywords in CATEGORIES.items():
        if any(k in text for k in keywords):
            return category
    return "podroze"

def find_albums(sources: Iterable[Path]) -> list[Path]:
    albums = []
    for source in sources:
        if not source.exists():
            continue

        if any(p.is_file() and p.suffix.lower() in IMAGE_EXTENSIONS for p in source.iterdir()):
            albums.append(source)
            continue

        for folder in source.rglob("*"):
            if folder.is_dir() and any(
                p.is_file() and p.suffix.lower() in IMAGE_EXTENSIONS
                for p in folder.iterdir()
            ):
                albums.append(folder)

    return sorted(set(albums), key=lambda x: str(x).lower())

def save_image(source: Path, destination: Path, max_size: int, quality: int):
    destination.parent.mkdir(parents=True, exist_ok=True)
    with Image.open(source) as image:
        image = ImageOps.exif_transpose(image)
        image.thumbnail((max_size, max_size), Image.Resampling.LANCZOS)
        if image.mode not in ("RGB", "L"):
            image = image.convert("RGB")
        image.save(destination, "WEBP", quality=quality, method=6)

def build_album(source: Path, output: Path) -> Album:
    slug = slugify(source.name)
    album_id = f"{slug}-1"

    album_dir = output / "galleries" / category_for(source) / album_id
    photo_dir = album_dir / "photos"
    thumb_dir = album_dir / "thumbs"

    photos = []

    images = sorted(
        [p for p in source.rglob("*") if p.is_file() and p.suffix.lower() in IMAGE_EXTENSIONS],
        key=lambda x: x.name.lower()
    )

    for idx, image in enumerate(images, start=1):
        stem = f"{idx:05d}-{slugify(image.stem)}"
        photo_path = photo_dir / f"{stem}.webp"
        thumb_path = thumb_dir / f"{stem}.webp"

        save_image(image, photo_path, 2200, 86)
        save_image(image, thumb_path, 520, 72)

        photos.append({
            "src": photo_path.relative_to(output).as_posix(),
            "thumbnail": thumb_path.relative_to(output).as_posix(),
            "alt": f"{source.name} - zdjęcie {idx}",
            "width": 0,
            "height": 0
        })

    return Album(
        id=album_id,
        title=source.name,
        category=category_for(source),
        source=source,
        photos=photos
    )

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", action="append", default=[])
    parser.add_argument("--album")
    parser.add_argument("--output", default=".")
    parser.add_argument("--clean", action="store_true")
    args = parser.parse_args()

    output = Path(args.output).resolve()
    gallery_root = output / "galleries"

    portfolio_root = Path(r"C:\Users\Daniel\Pictures\Portfolio")

    if args.clean and not args.album and gallery_root.exists():
        shutil.rmtree(gallery_root)

    for category in CATEGORIES:
        (gallery_root / category).mkdir(parents=True, exist_ok=True)

    gallery_json = output / "gallery.json"

    existing_albums = []
    if gallery_json.exists():
        try:
            existing_albums = json.loads(gallery_json.read_text(encoding="utf-8")).get("albums", [])
        except Exception:
            existing_albums = []

    if args.album:
        source_albums = [portfolio_root / args.album]
    else:
        roots = [Path(x) for x in (args.source or [str(portfolio_root)])]
        source_albums = find_albums(roots)

    albums = [build_album(a, output) for a in source_albums if a.exists()]

    if args.album:
        slug = slugify(args.album)
        existing_albums = [a for a in existing_albums if a.get("id") != f"{slug}-1"]

        for album in albums:
            existing_albums.append({
                "id": album.id,
                "title": album.title,
                "category": album.category,
                "source": str(album.source),
                "photos": album.photos
            })

        albums_json = existing_albums
    else:
        albums_json = [{
            "id": a.id,
            "title": a.title,
            "category": a.category,
            "source": str(a.source),
            "photos": a.photos
        } for a in albums]

    payload = {
        "generatedAt": datetime.now(UTC).isoformat(),
        "sourceFilter": None,
        "albums": albums_json
    }

    gallery_json.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )

    print(f"Generated {sum(len(a.photos) for a in albums)} photos in {len(albums)} albums.")

if __name__ == "__main__":
    main()
