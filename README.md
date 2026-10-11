# GoBackSoDoor

Permainan tradisional **Gobak Sodor** versi Android native (Kotlin, tanpa Compose).

## Cara bermain

- Kamu adalah **penyerang** (kotak biru). Mulai dari garis awal (kolom hijau kiri).
- Capai **garis sodor** (kolom hijau kanan), lalu kembali ke garis awal untuk mendapat 1 poin.
- **Penjaga** (lingkaran merah) bergerak mendekatimu setiap kali kamu melangkah.
- Jika penjaga berada tepat di sebelahmu (atas, bawah, kiri, kanan), kamu tersentuh dan kehilangan 1 nyawa.
- Kamu punya 3 nyawa. Jumlah penjaga bertambah seiring skor.
- Gunakan tombol panah di bawah layar untuk bergerak. Ketuk layar setelah ronde selesai untuk melanjutkan.

## Struktur proyek

```
app/src/main/java/com/retrobit/gobacksodoor/
├── MainActivity.kt          # Activity utama
├── game/GameEngine.kt       # Logika permainan murni (tanpa Android), mudah diuji
└── ui/GameView.kt           # Tampilan (Canvas) dan input sentuh
app/src/test/.../GameEngineTest.kt   # Unit test logika permainan
```

## Membangun dan menjalankan

Persyaratan: **Android Studio** (Koala atau lebih baru) dengan JDK 17.

1. Buka folder repositori ini di Android Studio (`File > Open`).
2. Tunggu proses *Gradle sync* selesai. Android Studio akan membuat Gradle wrapper secara otomatis jika belum ada.
3. Jalankan di emulator atau perangkat Android (minSdk 24) dengan tombol **Run**.

Menjalankan unit test:

```bash
./gradlew testDebugUnitTest
```

Membuat APK debug:

```bash
./gradlew assembleDebug
```

## Rencana pengembangan

- Mode dua pemain (pemain bergantian menjadi penyerang dan penjaga)
- Level dan variasi lapangan
- Suara dan animasi
- Papan skor lokal
