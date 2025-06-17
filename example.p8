-- the code editor works the same way as the pico8;
-- meaning, all characters are reversed (in appearance)
-- lowercase characters are shown as upper, and vice-versa.
--[[
 abcdefghijklmnopqrstuvxyz
 ABCDEFGHIJKLMNOPQRSTUVXYZ
]]

function __player_push()
 if p.x < 120 then
  p.y += 3
 end
end

function _init()
 p = {
  sp = 1, spd = 2,
  x = 10, y = 20,
 }
end

function _update()
 __player_push()

 -- movement
 if btn(0) then
  p.x -= spd
 elseif btn(1) then
  p.x += spd
 end
end

function _draw()
 print("hello, world!", 10, 10, 0)
 spr(p.sp, p.x, p.y)
end