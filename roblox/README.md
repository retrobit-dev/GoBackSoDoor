# Gobak Sodor di Roblox Studio

Skrip ini dibuat dari `docs/roblox-aturan.md`. **Belum diuji di Roblox Studio.** Anggap ini kerangka awal.

## Pemasangan

1. **ReplicatedStorage**
   - Buat ModuleScript bernama `Config` dan tempel isi `ReplicatedStorage/Config.lua`.
   - Buat Folder bernama `Remotes`. Di dalamnya buat RemoteEvent: `ChooseRole`, `Sprint`, `Push`, `StateUpdate`.
2. **ServerScriptService**
   - Buat Script bernama `MatchServer` dan tempel isi `ServerScriptService/MatchServer.server.lua`.
3. **Papan Anda** (`Workspace.Board`) harus memiliki:
   - `StartZone`: Part area start penyerang.
   - `FinishLine`: Part di sisi atas. Atur `CanTouch = true`.
   - `RowBoundaries`: Folder berisi Part garis batas antar baris. Setiap Part punya atribut `Index` (angka) dan `Kind = "Row"`.
   - `ColumnBoundaries`: Folder berisi Part garis batas antar kolom. Setiap Part punya atribut `Kind = "Column"`.
4. Jika nama atau struktur papan berbeda, ubah bagian `board:WaitForChild(...)` di `MatchServer`.

## Yang sudah ada di skrip
- Lobi: permainan mulai saat 10 pemain masuk. Tim dibagi berdasarkan urutan masuk (5 dan 5).
- Giliran: berganti saat semua penyerang mendapat poin, saat penjaga menyentuh penyerang, atau saat 3 menit habis. Timer mulai ulang tiap giliran.
- Poin: penyerang yang menyentuh `FinishLine` mendapat 1 poin.
- Waktu permainan: 15 menit. Tim dengan poin terbanyak menang.
- Seri: babak tambahan 3 menit. Penyerang yang mendapat poin menang. Jika waktu habis dan masih seri, penjaga menang.
- Stamina: terisi saat tidak dipakai. Berlari (penyerang) dan dorongan tangan (penjaga) memakai stamina.
- Penyerang tidak bisa kembali melewati batas yang sudah dilewati.

## Yang belum selesai (perlu dikerjakan di Studio)
- **Input klien:** skrip `LocalScript` untuk tombol lari (Shift), dorong (misalnya tombol E), dan tombol memilih peran. Server sudah menerima remote-nya.
- **Penjaga tidak boleh menghadap ke belakang:** belum dibatasi di server. Ini perlu ditambahkan setelah gerak penjaga diuji.
- **Pembatasan penjaga baris dan kolom:** skrip memakai atribut `OnLine` pada `HumanoidRootPart` untuk mengetahui garis yang sedang dipegang. Atribut ini harus diisi saat penjaga memilih posisi. Belum ada logikanya.
- **Stamina lari:** sekarang dikurangi per panggilan `Sprint`. Sebaiknya dihitung per detik di Heartbeat, tetapi itu perlu diuji.
- **Animasi tangan:** belum ada. Dorongan hanya memakai jarak dan sudut.
- **Pilihan peran:** semua pemain masuk sebagai penjaga kecuali memilih penyerang lewat `ChooseRole`.

## Catatan
- Angka stamina, kecepatan, dan jangkauan dorongan ada di `Config`. Ubah sesuai hasil uji.
- Versi kartu di Android tetap ada di repo dan tidak diubah.
