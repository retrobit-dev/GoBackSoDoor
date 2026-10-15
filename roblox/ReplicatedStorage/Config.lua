-- ModuleScript: letakkan di ReplicatedStorage > Config
-- Semua angka yang bisa diubah ada di sini.
local Config = {}

Config.MIN_PLAYERS = 10          -- permainan mulai saat jumlah pemain mencapai ini
Config.MATCH_TIME = 15 * 60      -- total waktu permainan (detik)
Config.TURN_TIME = 3 * 60        -- batas waktu tiap giliran (detik), juga untuk babak tambahan

-- Stamina (0..100)
Config.STAMINA_MAX = 100
Config.STAMINA_REGEN = 12        -- per detik, saat tidak dipakai
Config.STAMINA_REGEN_DELAY = 1   -- detik tanpa pemakaian sebelum mulai terisi
Config.RUN_COST = 20             -- per detik saat berlari
Config.PUSH_COST = 25            -- per dorongan tangan (penjaga)

-- Kecepatan
Config.WALK_SPEED = 8
Config.RUN_SPEED = 16

-- Dorongan tangan penjaga
Config.PUSH_RANGE = 5            -- studs
Config.PUSH_ANGLE = 60           -- derajat dari arah hadap, ke kiri atau kanan

-- Arah dunia yang tidak boleh dihadapi penjaga batas baris.
-- Asumsi: penyerang maju ke +Z, jadi +Z adalah belakang penjaga. Ubah jika papan Anda berbeda.
Config.GUARD_FORBIDDEN_FACING = Vector3.new(0, 0, 1)

return Config
