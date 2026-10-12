# Desain Karakter dan Kartu Bintang (draf)

Status: keputusan user, belum diimplementasi.

## Kartu
- Kartu gerak: 27 per tim (atas-bawah, kanan-kiri, silang × angka 1–3 × 3 salinan).
- Kartu sentuh (penjaga) / menghindar (penyerang): 5 per tim.
- Kartu bintang (spesial): 5 per tim. Hanya berisi ikon bintang, tanpa efek sendiri.
  - Dipakai setelah 1 kartu gerak normal dalam giliran yang sama.
  - Setelah dipakai, kembali ke dek.
  - Efeknya ditentukan oleh karakter pemain yang dipilih.

## Alur giliran
1. Pilih pemain.
2. Ambil 1 kartu dari dek ke tangan (tangan 6).
3. Mainkan 1 kartu gerak normal.
4. Opsional: mainkan 1 bintang untuk mengaktifkan efek karakter.
5. Giliran berakhir (tangan kembali 5).

## Karakter
- Setiap karakter punya **1 efek** dan **1 tipe utama**: penyerang atau penjaga.
- Loadout user: **10 karakter**, yaitu 5 bertipe penyerang dan 5 bertipe penjaga.
- Peran dipakai bergantian: penyerang hanya menyerang, penjaga hanya menjaga.
  Saat tim menyerang, karakter bertipe penyerang yang aktif. Saat menjaga, karakter bertipe penjaga yang aktif.
- Contoh: Sprinter (penyerang), efek langkah ganda: angka kartu gerak normal dikali 2.

## Level
- Naik level dengan kartu duplikat (gaya Clash Royale). Kebutuhan duplikat naik tiap level.
- Efek menguat tiap level.

## Kelangkaan
- Umum, Langka, Epik, Legenda.
- Kelangkaan menentukan peluang dari peti, batas level, dan kekuatan efek.

## Sumber karakter
- Peti dari kemenangan babak atau pertandingan, berisi karakter acak sesuai kelangkaan.

## Belum diputuskan
- Daftar nama dan efek 10 karakter awal.
- Angka pasti batas level dan peluang peti per kelangkaan.
- Bagaimana bintang dipilih jika tim memakai karakter yang sudah memiliki efek.

## 10 karakter awal (usulan, belum diuji)

Penyerang (aktif saat tim menyerang):
| Karakter | Asal | Kelangkaan | Efek |
|---|---|---|---|
| Lincah | Jawa | Umum | Langkah ganda: angka kartu gerak normal dikali 2. |
| Pesilat | Minangkabau | Langka | Boleh melompati satu penjaga di garis lurus tanpa tersentuh. |
| Mata Elang | Dayak | Umum | Sentuhan pertama dibatalkan tanpa perlu kartu menghindar. |
| Penari | Bali | Umum | Tukar posisi dengan penyerang rekan di sebelahnya. |
| Angin Laut | Bugis | Epik | Gerak kanan-kiri atau silang tidak memantul di dinding, tetapi melanjutkan ke sisi lain. |

Penjaga (aktif saat tim menjaga):
| Karakter | Asal | Kelangkaan | Efek |
|---|---|---|---|
| Penjaga Huta | Batak | Umum | Area merah bertambah satu kotak ke depan. |
| Tongkonan | Toraja | Umum | Satu penyerang lawan tidak bisa dipilih pada giliran berikutnya. |
| Pitung | Betawi | Langka | Penyerang yang menghindar tetap tersentuh (kartu menghindar tidak berlaku). |
| Pepadu | Sasak | Umum | Penjaga yang dipilih boleh bergerak satu kotak tambahan. |
| Panah Lurus | Papua | Epik | Dapat menyentuh penyerang yang berjarak dua kotak lurus, bukan hanya di area merah. |

Catatan: nilai kelangkaan dan efek perlu diseimbangkan setelah diuji. Detail budaya perlu diverifikasi dengan sumber lokal.

## Revisi: kartu tanpa angka, stamina, dan nama karakter (keputusan user, belum diimplementasi)

### Nama karakter (usulan nama umum per suku)
| Karakter | Suku | Nama usulan | Catatan |
|---|---|---|---|
| Lincah | Jawa | Slamet | nama umum |
| Pesilat | Minangkabau | Sutan | gelar, perlu cek nama lokal |
| Mata Elang | Dayak (Ngaju) | Ekot | nama Dayak Ngaju |
| Penari | Bali | Wayan | nama urutan lahir |
| Angin Laut | Bugis | Andi | nama umum Bugis |
| Penjaga Huta | Batak | Togar | nama Batak |
| Tongkonan | Toraja | Dalle | nama khas Toraja |
| Pitung | Betawi | Pitung | tokoh Betawi |
| Pepadu | Sasak | Lalu | gelar laki-laki Sasak |
| Panah Lurus | Papua | Wanggai | pilihan user |

### Kartu (per tim, 30 kartu)
- Arah tanpa angka: 5 atas-bawah, 5 kiri-kanan, 5 silang.
- 5 kartu sentuh/hindar.
- 5 kartu bintang (hanya ikon, efek dari karakter).
- 5 kartu stamina: +1, +2, +3, +4, +5 (masing-masing 1 kartu), atau −1 sampai −5.
  - Dimainkan tanpa biaya stamina.
  - Pemain memilih karakter tujuan (karakter tim sendiri untuk menambah, atau karakter lawan untuk mengurangi).

### Stamina
- Setiap karakter punya bar stamina awal, dan nilai awal berbeda tiap karakter.
- Karakter memainkan kartu dengan membayar stamina sesuai harga kartu:
  - Kartu arah (normal): 1 stamina.
  - Kartu sentuh/hindar: 3 stamina.
  - Kartu bintang: 5 stamina.
  - Kartu stamina: 0 stamina.
- Sisa stamina tidak hilang.
- Stamina bertambah 1 setiap pergantian giliran.
- Kartu arah: jarak gerak = stamina yang dibayar (bayar N stamina untuk gerak N kotak). Biaya dasar kartu arah tetap 1 stamina sebagai harga minimum, sehingga jarak 1 = 1 stamina.

### Aturan tambahan
- Penyerang tidak bisa menabrak penjaga, kecuali lewat efek karakter.
- Kartu sentuh/hindar bisa dipakai kapan saja selama ada penjaga yang bisa menyentuh penyerang di area merah, atau penyerang bisa membalas dengan kartu hindar.

### Pertanyaan terbuka
- Kartu arah: dijawab (bayar N untuk N kotak), lihat di atas.
- Penambahan stamina per giliran: semua karakter tim (+1 setiap pergantian giliran).

### Keputusan terbaru
- Nama karakter Papua: Wanggai.
- Kartu stamina memilih karakter tujuan saat dimainkan.

## Status implementasi preview
- Diterapkan: deck 30 kartu per tim (kartu arah tanpa angka, sentuh/hindar, bintang, stamina ±1 sampai ±5), stamina per karakter (+1 saat giliran tim dimulai), biaya kartu (arah = jumlah kotak, sentuh/hindar 3, bintang 5, stamina 0), kartu stamina memilih karakter tujuan, bintang setelah gerak lalu kembali ke dek, penyerang tidak menabrak penjaga, dan 10 karakter awal dengan nama.
- Belum aktif: efek karakter (bintang belum mengubah apa pun), kelangkaan dan level, peti, dan tampilan kartu karakter seperti Clash Royale. Kartu karakter saat ini hanya panel informasi.

### Kartu arah (keputusan terbaru)
- Kartu normal membayar 1 stamina per gerakan (per kotak). Bisa bergerak sejumlah kotak sesuai stamina yang ditentukan pemain, dengan minimal 1 kotak dan maksimal stamina karakter.

### Tangan (keputusan terbaru)
- Tangan awal 3 kartu per tim.
- Di awal giliran, pemain boleh mengambil 0 sampai (3 − jumlah kartu di tangan) kartu, sekali per giliran.
- Kartu yang tidak dimainkan tetap di tangan. Tangan tidak diisi otomatis setelah kartu dimainkan.
- Kartu di tangan boleh dibuang dengan tombol "Buang kartu terpilih" (tidak mengakhiri giliran).
