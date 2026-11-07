# Portal Stick (Forge 1.16.5)
1. Ambil Forge MDK 1.16.5 (36.2.x), salin folder `src/` ini ke dalam MDK (timpa), jalankan `gradlew build`.
2. Pakai: klik kanan sudut bawah -> sudut bawah lain (Y sama) -> sudut atas = Portal A. Ulangi = Portal B (terhubung otomatis).
3. Sneak + klik kanan di udara = reset pilihan.
4. Perintah (OP): /portalstick list | remove <id> | clear
5. Ganti tekstur: python tools/svg2png.py tools/portal.svg 64 src/main/resources/assets/portalstick/textures/portal/portal.png
