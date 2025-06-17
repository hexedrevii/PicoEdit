-- the code editor works the same way as the pico8;
-- meaning, all characters are reversed (in appearance)
-- lowercase characters are shown as upper, and vice-versa.
--[[
  abcdefghijklmnopqrstuvxyz
  ABCDEFGHIJKLMNOPQRSTUVXYZ
]]

function _init()
  p = {
    sp = 1, spd = 2,
    x = 10, y = 20,
  }
end

function _update()
  -- gravity
  if p.x < 120 then
    p.y += 3
  end

  -- movement
  if btn(0) then
    p.x -= spd
  elseif btn(1) then
    p.x += spd
  end
end

function _draw()
  spr(p.sp, p.x, p.y)
end