-- LocalScript: letakkan di StarterPlayer > StarterPlayerScripts > InputController
-- Input keyboard:
--   1 = pilih penyerang   2 = pilih penjaga
--   Shift (tahan) = lari (penyerang)
--   E = dorong tangan (penjaga)
--   Q, lalu klik tombol garis = pilih garis batas (penjaga)
-- Mobile belum didukung di skrip ini.

local UserInputService = game:GetService("UserInputService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local Remotes = ReplicatedStorage:WaitForChild("Remotes")
local ChooseRole = Remotes:WaitForChild("ChooseRole")
local Sprint = Remotes:WaitForChild("Sprint")
local Push = Remotes:WaitForChild("Push")
local PickLine = Remotes:WaitForChild("PickLine")

local mouse = game.Players.LocalPlayer:GetMouse()
local pickingLine = false

local function sendSprint(on)
	Sprint:FireServer(on)
end

UserInputService.InputBegan:Connect(function(input, processed)
	if processed then return end

	if input.KeyCode == Enum.KeyCode.One then
		ChooseRole:FireServer("attacker")
	elseif input.KeyCode == Enum.KeyCode.Two then
		ChooseRole:FireServer("guard")
	elseif input.KeyCode == Enum.KeyCode.LeftShift then
		sendSprint(true)
	elseif input.KeyCode == Enum.KeyCode.E then
		Push:FireServer()
	elseif input.KeyCode == Enum.KeyCode.Q then
		pickingLine = true
	elseif input.UserInputType == Enum.UserInputType.MouseButton1 and pickingLine then
		local target = mouse.Target
		if target then
			PickLine:FireServer(target)
		end
		pickingLine = false
	end
end)

UserInputService.InputEnded:Connect(function(input)
	if input.KeyCode == Enum.KeyCode.LeftShift then
		sendSprint(false)
	end
end)
