#!/usr/bin/env python3
"""Konversi SVG -> PNG tekstur ringan untuk portal.
Pakai:  python svg2png.py portal.svg [ukuran=64] [output.png]
Butuh:  pip install cairosvg pillow
Tips ringan: pakai 32/64/128 (pangkat 2), warna sedikit (palet di-quantize)."""
import sys, os

def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    src = sys.argv[1]
    size = int(sys.argv[2]) if len(sys.argv) > 2 else 64
    out = sys.argv[3] if len(sys.argv) > 3 else os.path.splitext(src)[0] + ".png"
    if size & (size - 1):
        print("Peringatan: ukuran sebaiknya pangkat 2 (16/32/64/128).")
    try:
        import cairosvg
    except ImportError:
        sys.exit("Install dulu: pip install cairosvg pillow")
    cairosvg.svg2png(url=src, write_to=out, output_width=size, output_height=size)
    try:
        from PIL import Image
        im = Image.open(out).convert("RGBA")
        # quantize 64 warna (alpha dipertahankan) biar file kecil
        q = im.quantize(colors=64, method=Image.FASTOCTREE).convert("RGBA")
        q.putalpha(im.getchannel("A"))
        q.save(out, optimize=True)
    except ImportError:
        pass
    print("OK ->", out, os.path.getsize(out), "bytes")

main()
