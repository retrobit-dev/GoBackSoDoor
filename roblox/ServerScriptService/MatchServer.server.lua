-- Script: letakkan di ServerScriptService > MatchServer
-- Mengatur lobi, giliran, timer, poin, babak tambahan, stamina, dan batas gerak.
--
-- Nama yang harus ada di Workspace (sesuaikan dengan papan Anda):
--   Workspace.Board.StartZone        Part: area start penyerang
--   Workspace.Board.FinishLine       Part: garis finis di sisi atas (CanTouch = true)
--   Workspace.Board.RowBoundaries    Folder berisi Part batas antar baris (Kind = "Row", Index = angka)
--   Workspace.Board.ColumnBoundaries Folder berisi Part batas antar kolom (Kind = "Column")
-- Remote di ReplicatedStorage > Remotes: ChooseRole, Sprint, Push, PickLine, StateUpdate.

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local RS = game:GetService("ReplicatedStorage")

local Config = require(RS:WaitForChild("Config"))
local Remotes = RS:WaitForChild("Remotes")
local ChooseRole = Remotes:WaitForChild("ChooseRole")
local Sprint = Remotes:WaitForChild("Sprint")
local Push = Remotes:WaitForChild("Push")
local PickLine = Remotes:WaitForChild("PickLine")
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
local info = {}                  -- [player] = {team, role, line, running}
local stamina = {}               -- [player] = angka
local lastUse = {}               -- [player] = os.clock() terakhir stamina dipakai
local scored = {}                -- [player] = true jika penyerang sudah mencapai finis di giliran ini
local progress = {}              -- [player] = indeks batas terjauh yang sudah dilewati penyerang
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

-- Indeks batas yang sudah dilewati, berdasarkan posisi Z. Maju = Z bertambah.
local function progressIndex(pos)
	local idx = 0
	for _, part in ipairs(rowBounds) do
		local n = part:GetAttribute("Index") or 0
		if pos.Z > part.Position.Z and n > idx then idx = n end
	end
	return idx
end

-- ===== Giliran =====
local function endTurn(reason)
	if phase == "ended" then return end
	if phase == "tiebreak" and reason == "time" then
		phase = "ended"
		print("Babak tambahan selesai, penjaga menang (skor seri).")
		broadcast()
		return
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

finishLine.Touched:Connect(function(hit)
	local p = Players:GetPlayerFromCharacter(hit.Parent)
	if not p or phase == "lobby" or phase == "ended" then return end
	local d = info[p]
	if d and d.role == "attacker" and d.team == attackingTeam then
		attackerScored(p)
	end
end)

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

-- ===== Lobi, peran, dan garis penjaga =====
Players.PlayerAdded:Connect(function(p)
	info[p] = {team = nil, role = nil, line = nil, running = false}
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

-- Penjaga memilih garis batas. Server memeriksa bahwa Part itu memang garis batas.
PickLine.OnServerEvent:Connect(function(p, part)
	local d = info[p]
	if not d or d.role ~= "guard" or typeof(part) ~= "Instance" then return end
	if part.Parent ~= board.RowBoundaries and part.Parent ~= board.ColumnBoundaries then return end
	d.line = part
	local root = p.Character and p.Character:FindFirstChild("HumanoidRootPart")
	if root then
		-- Pindahkan penjaga ke garis, di sisi X atau Z sesuai jenis garis.
		local pos = root.Position
		if part:GetAttribute("Kind") == "Row" then
			root.CFrame = CFrame.new(pos.X, pos.Y, part.Position.Z)
		else
			root.CFrame = CFrame.new(part.Position.X, pos.Y, pos.Z)
		end
	end
end)

local function startMatchIfReady()
	if phase ~= "lobby" then return end
	local list = Players:GetPlayers()
	if #list < Config.MIN_PLAYERS then return end
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
					print("Waktu habis. Tim", scores.A > scores.B and "A" or "B", "menang.")
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

-- ===== Sprint =====
Sprint.OnServerEvent:Connect(function(p, on)
	local d = info[p]
	if d then d.running = on == true end
end)

-- ===== Dorongan tangan penjaga =====
Push.OnServerEvent:Connect(function(p)
	local d = info[p]
	if not d or d.role ~= "guard" or d.team == attackingTeam then return end
	if stamina[p] < Config.PUSH_COST then return end
	local root = p.Character and p.Character:FindFirstChild("HumanoidRootPart")
	if not root then return end
	stamina[p] -= Config.PUSH_COST
	lastUse[p] = os.clock()

	local cosLimit = math.cos(math.rad(Config.PUSH_ANGLE))
	for _, attacker in ipairs(teamPlayers(attackingTeam, "attacker")) do
		local aroot = attacker.Character and attacker.Character:FindFirstChild("HumanoidRootPart")
		if aroot then
			local offset = aroot.Position - root.Position
			local dist = offset.Magnitude
			if dist > 0 and dist <= Config.PUSH_RANGE and root.CFrame.LookVector:Dot(offset.Unit) >= cosLimit then
				guardTouchedAttacker(p, attacker)
				return
			end
		end
	end
end)

-- ===== Stamina, lari, dan batas gerak (setiap frame) =====
RunService.Heartbeat:Connect(function(dt)
	for p, d in pairs(info) do
		local char = p.Character
		local hum = char and char:FindFirstChildOfClass("Humanoid")
		local root = char and char:FindFirstChild("HumanoidRootPart")
		if not hum or not root then continue end

		-- Lari: hanya penyerang, hanya saat bergerak, dan memakai stamina per detik.
		local moving = hum.MoveDirection.Magnitude > 0
		local canRun = d.running and d.role == "attacker" and stamina[p] > 0 and moving
		hum.WalkSpeed = canRun and Config.RUN_SPEED or Config.WALK_SPEED
		if canRun then
			stamina[p] = math.max(0, stamina[p] - Config.RUN_COST * dt)
			lastUse[p] = os.clock()
		end

		-- Stamina terisi jika tidak dipakai.
		if os.clock() - (lastUse[p] or 0) >= Config.STAMINA_REGEN_DELAY then
			stamina[p] = math.min(Config.STAMINA_MAX, stamina[p] + Config.STAMINA_REGEN * dt)
		end

		if d.role == "attacker" and d.team == attackingTeam then
			-- Penyerang tidak boleh kembali melewati batas yang sudah dilewati.
			local idx = progressIndex(root.Position)
			local best = progress[p] or 0
			if idx >= best then
				progress[p] = idx
				lastGood[p] = root.CFrame
			elseif lastGood[p] then
				root.CFrame = lastGood[p]
			end
		elseif d.role == "guard" and d.line then
			-- Penjaga tetap di garisnya.
			local line = d.line
			local pos = root.Position
			if line:GetAttribute("Kind") == "Row" then
				-- Batas baris: hanya kiri-kanan (sumbu X) di garis Z-nya.
				local half = line.Size.X / 2
				local x = math.clamp(pos.X, line.Position.X - half, line.Position.X + half)
				local look = root.CFrame.LookVector
				-- Penjaga tidak boleh menghadap ke arah Config.GUARD_FORBIDDEN_FACING.
				if look:Dot(Config.GUARD_FORBIDDEN_FACING) > 0.5 then
					look = -Config.GUARD_FORBIDDEN_FACING
				end
				root.CFrame = CFrame.lookAt(Vector3.new(x, pos.Y, line.Position.Z), Vector3.new(x, pos.Y, line.Position.Z) + look)
			else
				-- Batas kolom: bergerak di sepanjang garis (sumbu Z). Boleh menghadap ke mana saja.
				local half = line.Size.Z / 2
				local z = math.clamp(pos.Z, line.Position.Z - half, line.Position.Z + half)
				root.CFrame = CFrame.new(line.Position.X, pos.Y, z) * (root.CFrame - root.CFrame.Position)
			end
		end
	end
end)

print("MatchServer siap. Menunggu", Config.MIN_PLAYERS, "pemain.")
