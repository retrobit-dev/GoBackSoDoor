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

## Input (LocalScript)
- Letakkan `StarterPlayer/StarterPlayerScripts/InputController.client.lua` di StarterPlayerScripts.
- Tombol: `1` penyerang, `2` penjaga, tahan `Shift` untuk lari, `E` untuk dorong tangan, `Q` lalu klik garis batas untuk memilih garis (penjaga).
- Tambahkan RemoteEvent `PickLine` di folder `Remotes`.

## Pembatasan penjaga
- **Batas baris:** penjaga hanya bergerak kiri-kanan di garisnya. Ia tidak boleh menghadap `Config.GUARD_FORBIDDEN_FACING`.
- **Batas kolom:** penjaga bergerak sepanjang garisnya dan boleh menghadap ke arah mana pun.
- Penjaga memilih garis lewat `PickLine`. Server hanya menerima Part yang berada di `RowBoundaries` atau `ColumnBoundaries`.

## Asumsi yang perlu dicek di Studio
- **Arah maju penyerang:** +Z (menuju garis finis). Jika papan Anda berbeda, ubah `progressIndex` dan `GUARD_FORBIDDEN_FACING`.
- **Lebar garis:** garis batas harus memanjang sesuai ruang geraknya. Penjaga dibatasi dalam `Size` Part-nya.
- **Stamina lari:** dikurangi per detik di Heartbeat, hanya saat penyerang sedang berlari dan bergerak.

## Yang belum selesai
- **Animasi tangan:** belum ada. Dorongan hanya memakai jarak dan sudut.
- **Input mobile:** belum ada.
- **Pemilihan peran:** semua pemain masuk sebagai penjaga kecuali memilih penyerang lewat `ChooseRole`. Belum ada tampilan tombolnya.
- **Pengujian:** belum dijalankan di Roblox Studio. Pembatasan dengan `CFrame` setiap frame bisa bertabrakan dengan fisika karakter, jadi perlu dicek saat uji coba.

## Catatan
- Angka stamina, kecepatan, dan jangkauan dorongan ada di `Config`. Ubah sesuai hasil uji.
- Versi kartu di Android tetap ada di repo dan tidak diubah.
