-- Script: letakkan di ServerScriptService > MatchServer
-- Mengatur lobi, giliran, timer, poin, babak tambahan, stamina, dan batas gerak.
--
-- Nama yang harus ada di Workspace (sesuaikan dengan papan Anda):
--   Workspace.Board.StartZone        Part: area start penyerang
--   Workspace.Board.FinishLine       Part: garis finis di sisi atas (CanTouch = true)
--   Workspace.Board.RowBoundaries    Folder berisi Part batas antar baris
--   Workspace.Board.ColumnBoundaries Folder berisi Part batas antar kolom
-- Setiap Part batas punya atribut Index (angka, 1 = paling dekat start).
-- Remote di ReplicatedStorage > Remotes: ChooseRole, Sprint, Push, StateUpdate.

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local RS = game:GetService("ReplicatedStorage")

local Config = require(RS:WaitForChild("Config"))
local Remotes = RS:WaitForChild("Remotes")
local ChooseRole = Remotes:WaitForChild("ChooseRole")
local Sprint = Remotes:WaitForChild("Sprint")
local Push = Remotes:WaitForChild("Push")
local StateUpdate = Remotes:WaitForChild("StateUpdate")

local board = workspace:WaitForChild("Board")
local finishLine = board:WaitForChild("FinishLine")
local rowBounds = board:WaitForChild("RowBoundaries"):GetChildren()
local colBounds = board:WaitForChild("ColumnBoundaries"):GetChildren()

-- ===== State =====
local phase = "lobby"            -- lobby | play | tiebreak | ended
local attackingTeam = "A"
local scores = {A = 0, B = 0}
local matchLeft = Config.MATCH_TIME
local turnLeft = Config.TURN_TIME
local info = {}                  -- [player] = {team = "A"|"B", role = "attacker"|"guard"|nil}
local stamina = {}               -- [player] = angka
local lastUse = {}               -- [player] = waktu terakhir stamina dipakai
local scored = {}                -- [player] = true jika penyerang sudah mencapai finis di giliran ini
local progress = {}              -- [player] = indeks batas terjauh yang pernah dilewati penyerang
local lastGood = {}              -- [player] = CFrame terakhir yang sah untuk penyerang

-- ===== Helper =====
local function teamPlayers(team, role)
	local out = {}
	for p, d in pairs(info) do
		if d.team == team and (role == nil or d.role == role) then
			table.insert(out, p)
		end
	end
	return out
end

local function otherTeam(team)
	return team == "A" and "B" or "A"
end

local function broadcast()
	StateUpdate:FireAllClients({
		phase = phase,
		attackingTeam = attackingTeam,
		scores = scores,
		matchLeft = matchLeft,
		turnLeft = turnLeft,
	})
end

-- Indeks batas yang sudah dilewati, berdasarkan posisi Z relatif terhadap start.
local function progressIndex(pos)
	local idx = 0
	for _, part in ipairs(rowBounds) do
		local n = part:GetAttribute("Index") or 0
		local forwardOfLine = pos.Z > part.Position.Z
		if forwardOfLine and n > idx then idx = n end
	end
	return idx
end

-- ===== Giliran =====
local function endTurn(reason)
	if phase == "ended" then return end
	if phase == "tiebreak" then
		-- babak tambahan: jika belum ada pemenang, penjaga menang saat waktu habis
		if reason == "time" then
			phase = "ended"
			print("Babak tambahan selesai, penjaga menang (skor seri).")
			broadcast()
			return
		end
	end
	attackingTeam = otherTeam(attackingTeam)
	turnLeft = Config.TURN_TIME
	scored = {}
	progress = {}
	print("Giliran berganti. Alasan:", reason, "Penyerang sekarang: Tim", attackingTeam)
	broadcast()
end

local function attackerScored(p)
	if scored[p] then return end
	scored[p] = true
	scores[info[p].team] += 1
	print(p.Name, "mencapai finis. Skor:", scores.A, scores.B)
	if phase == "tiebreak" then
		phase = "ended"
		print("Babak tambahan: penyerang mendapat poin, Tim", info[p].team, "menang.")
		broadcast()
		return
	end
	local all = true
	for _, a in ipairs(teamPlayers(attackingTeam, "attacker")) do
		if not scored[a] then all = false break end
	end
	if all then endTurn("semua penyerang sudah mendapat poin") else broadcast() end
end

-- Penyentuhan finis
finishLine.Touched:Connect(function(hit)
	local p = Players:GetPlayerFromCharacter(hit.Parent)
	if not p or phase == "lobby" or phase == "ended" then return end
	local d = info[p]
	if d and d.role == "attacker" and d.team == attackingTeam then
		attackerScored(p)
	end
end)

-- ===== Penjaga menyentuh penyerang (dari Push) =====
local function guardTouchedAttacker(guard, attacker)
	if phase == "lobby" or phase == "ended" then return end
	print(guard.Name, "menyentuh", attacker.Name)
	if phase == "tiebreak" then
		phase = "ended"
		print("Babak tambahan: penjaga menyentuh penyerang, penjaga menang.")
		broadcast()
		return
	end
	endTurn("penjaga menyentuh penyerang")
end

-- ===== Lobi & pemilihan peran =====
Players.PlayerAdded:Connect(function(p)
	info[p] = {team = nil, role = nil}
	stamina[p] = Config.STAMINA_MAX
	lastUse[p] = 0
end)

Players.PlayerRemoving:Connect(function(p)
	info[p] = nil
	stamina[p] = nil
end)

ChooseRole.OnServerEvent:Connect(function(p, role)
	if phase ~= "lobby" or not info[p] then return end
	if role ~= "attacker" and role ~= "guard" then return end
	info[p].role = role
end)

local function startMatchIfReady()
	if phase ~= "lobby" then return end
	local list = Players:GetPlayers()
	if #list < Config.MIN_PLAYERS then return end
	-- Bagi tim berdasarkan urutan masuk: 5 pertama tim A, 5 berikutnya tim B.
	for i, p in ipairs(list) do
		info[p].team = (i <= #list / 2) and "A" or "B"
		if not info[p].role then info[p].role = "guard" end
	end
	phase = "play"
	print("Permainan dimulai. Penyerang pertama: Tim A")
	broadcast()
end

-- ===== Loop utama (setiap detik) =====
task.spawn(function()
	while true do
		task.wait(1)
		if phase == "lobby" then
			startMatchIfReady()
		elseif phase == "play" then
			matchLeft -= 1
			turnLeft -= 1
			if matchLeft <= 0 then
				if scores.A == scores.B then
					phase = "tiebreak"
					turnLeft = Config.TURN_TIME
					print("Skor seri. Babak tambahan 3 menit dimulai.")
				else
					phase = "ended"
					local winner = scores.A > scores.B and "A" or "B"
					print("Waktu habis. Tim", winner, "menang.")
				end
			elseif turnLeft <= 0 then
				endTurn("waktu giliran habis")
			end
			broadcast()
		elseif phase == "tiebreak" then
			turnLeft -= 1
			if turnLeft <= 0 then endTurn("time") end
			broadcast()
		end
	end
end)

-- ===== Stamina dan batas gerak (setiap frame) =====
RunService.Heartbeat:Connect(function(dt)
	for p, d in pairs(info) do
		local char = p.Character
		local hum = char and char:FindFirstChildOfClass("Humanoid")
		if hum then
			-- Stamina terisi jika tidak dipakai.
			if os.clock() - lastUse[p] >= Config.STAMINA_REGEN_DELAY then
				stamina[p] = math.min(Config.STAMINA_MAX, stamina[p] + Config.STAMINA_REGEN * dt)
			end
		end
		-- Penyerang tidak boleh kembali melewati batas yang sudah dilewati.
		if char and d.role == "attacker" and d.team == attackingTeam then
			local root = char:FindFirstChild("HumanoidRootPart")
			if root then
				local idx = progressIndex(root.Position)
				local best = progress[p] or 0
				if idx >= best then
					progress[p] = idx
					lastGood[p] = root.CFrame
				elseif lastGood[p] then
					root.CFrame = lastGood[p]
				end
			end
		end
	end
end)

-- ===== Sprint =====
Sprint.OnServerEvent:Connect(function(p, wantRun)
	local char = p.Character
	local hum = char and char:FindFirstChildOfClass("Humanoid")
	if not hum or not stamina[p] then return end
	local d = info[p]
	if wantRun and d and d.role == "attacker" and stamina[p] > 0 then
		hum.WalkSpeed = Config.RUN_SPEED
		stamina[p] = math.max(0, stamina[p] - Config.RUN_COST * 0.05)
		lastUse[p] = os.clock()
	else
		hum.WalkSpeed = Config.WALK_SPEED
	end
end)

-- ===== Dorongan tangan penjaga =====
Push.OnServerEvent:Connect(function(p)
	local d = info[p]
	if not d or d.role ~= "guard" or d.team == attackingTeam then return end
	if stamina[p] < Config.PUSH_COST then return end
	local char = p.Character
	local root = char and char:FindFirstChild("HumanoidRootPart")
	if not root then return end
	stamina[p] -= Config.PUSH_COST
	lastUse[p] = os.clock()

	local cosLimit = math.cos(math.rad(Config.PUSH_ANGLE))
	for _, attacker in ipairs(teamPlayers(attackingTeam, "attacker")) do
		local aroot = attacker.Character and attacker.Character:FindFirstChild("HumanoidRootPart")
		if aroot then
			local offset = aroot.Position - root.Position
			local dist = offset.Magnitude
			if dist <= Config.PUSH_RANGE and dist > 0 then
				if root.CFrame.LookVector:Dot(offset.Unit) >= cosLimit then
					guardTouchedAttacker(p, attacker)
					return
				end
			end
		end
	end
end)

-- ===== Batas gerak penjaga =====
-- Penjaga batas baris: hanya bergerak kiri-kanan (sumbu X) pada garis Part-nya.
-- Penjaga batas kolom: bergerak sepanjang Z pada garis kolomnya.
-- Atribut Part pada batas: Kind = "Row" atau "Column".
RunService.Heartbeat:Connect(function()
	for p, d in pairs(info) do
		if d.role == "guard" and p.Character then
			local root = p.Character:FindFirstChild("HumanoidRootPart")
			local line = root and root:GetAttribute("OnLine") and board:FindFirstChild(root:GetAttribute("OnLine"), true)
			if line then
				local pos = root.Position
				if line:GetAttribute("Kind") == "Row" then
					root.CFrame = CFrame.new(pos.X, pos.Y, line.Position.Z) * (root.CFrame - root.CFrame.Position)
				else
					root.CFrame = CFrame.new(line.Position.X, pos.Y, pos.Z) * (root.CFrame - root.CFrame.Position)
				end
			end
		end
	end
end)

print("MatchServer siap. Menunggu", Config.MIN_PLAYERS, "pemain.")
