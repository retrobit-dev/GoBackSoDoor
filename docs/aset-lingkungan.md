# Aset Lingkungan

Gaya: **pixel art**. Font: Press Start 2P (OFL), di `app/src/main/assets/fonts/press_start.ttf`.

**Status pixel:** karakter sudah dibuat ulang sebagai pixel art. Obstacle, ubin, dekorasi, dan latar papan masih hasil pixelasi dari versi halus. Pembuatan ulang pixel asli tertunda karena batas pembuatan gambar per giliran sudah tercapai.

Daftar aset lingkungan permainan. Sumber mentah (sebelum diproses) disimpan di luar repo.

## Keputusan desain

- **Sumber obstacle:** keduanya. Obstacle muncul acak satu per giliran, dan karakter tertentu
  juga bisa menambah atau memindahkan obstacle lewat efeknya. Bagian efek karakter belum diimplementasikan.
- **Cakupan aset pertama:** ubin, obstacle, dan dekorasi.

## Aset yang sudah dibuat

| Berkas | Kegunaan | Status |
| --- | --- | --- |
| `drawable/env_hole.png` | Obstacle lubang | Dipasang di papan |
| `drawable/env_rock.png` | Obstacle batu | Dipasang di papan |
| `drawable/env_bush.png` | Obstacle semak | Dipasang di papan |
| `drawable/env_grass_tile.png` | Ubin rumput | Belum dipakai |
| `drawable/env_dirt_tile.png` | Ubin tanah | Belum dipakai |
| `drawable/env_tree.png` | Dekorasi pohon | Belum dipakai |
| `drawable/env_fence.png` | Dekorasi pagar | Belum dipakai |
| `drawable/env_windmill.png` | Dekorasi kincir angin | Belum dipakai |
| `drawable/board_bg.jpg` | Latar papan | Dipakai |

## Catatan teknis

- Objek dibuat di latar putih, lalu latar dihapus menjadi transparan dan dipotong sesuai isi.
- Ubin disimpan sebagai JPG persegi 256 px.
- Bila aset tidak ditemukan, GameView memakai bentuk sederhana sebagai cadangan.

## Status pixel (diperbarui)
- Sudah pixel: `env_hole`, `env_rock`, `env_bush`, `env_tree`, `env_fence`, `env_windmill` (latar putih dibuat transparan), `env_grass_tile`, `env_dirt_tile`.
- Catatan: `env_hole` masih punya kotak latar hijau; perlu dibersihkan lagi.
- `board_bg.jpg` tetap gambar referensi user.
