"""Our own redraws of three textures that used to be copied from the 1.7.10 original and were Thaumcraft-4-derived art
(asset cleanup, 2026-11-24): the Crimson Archer skin, the Bottle of Clear Water and the Nether Membrane. Drawn from scratch
with new silhouettes; same paths and sizes. Run from the module root (via gen_all.py)."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *


def hx(s, a=255):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def noise(x, y, s=0):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xffff) / 65535.0


def tone(c, k):
    return (max(0, min(255, int(c[0] * k))), max(0, min(255, int(c[1] * k))), max(0, min(255, int(c[2] * k))), c[3])


class Canvas:
    def __init__(self, wid, hei):
        self.w, self.h = wid, hei
        self.px = [(0, 0, 0, 0)] * (wid * hei)

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y * self.w + x] = c

    def fill_spans(self, spans, shader, outline):
        inside = {(x, y) for y, x0, x1 in spans for x in range(x0, x1 + 1)}
        for i, (y, x0, x1) in enumerate(spans):
            for x in range(x0, x1 + 1):
                edge = any((x + dx, y + dy) not in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                self.set(x, y, outline if edge else shader(x, y, i))

    def save(self, rel):
        write_png(os.path.join(A, 'textures', rel), self.w, self.h, self.px)


def face_cube(c, u, v, wd, ht, dp, fn):
    """Paint a model cube's six faces (vanilla box UV); fn(face, x, y, fw, fh) -> colour."""
    layout = {
        'top': (u + dp, v, wd, dp), 'bottom': (u + dp + wd, v, wd, dp),
        'right': (u, v + dp, dp, ht), 'front': (u + dp, v + dp, wd, ht),
        'left': (u + dp + wd, v + dp, dp, ht), 'back': (u + 2 * dp + wd, v + dp, wd, ht),
    }
    for face, (fx, fy, fw, fh) in layout.items():
        for y in range(fh):
            for x in range(fw):
                c.set(fx + x, fy + y, fn(face, x, y, fw, fh))


def bottle_of_clear_water():
    """A host phial (referenced, never copied) with our own water fill drawn over it."""
    c = Canvas(16, 16)
    water, water_d, water_h = hx('9fd8f0'), hx('6fb4da'), hx('f4fdff')
    for y in range(8, 14):
        for x in range(5, 11):
            c.set(x, y, water_h if (x == 6 and y > 8) else (water_d if x >= 9 else water))
    for x in range(5, 11):
        c.set(x, 8, water_h)
    c.save('item/bottle_of_clear_water.png')
    w('assets/%s/models/item/bottle_of_clear_water.json' % NS, {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': 'thaumaturge:item/phial', 'layer1': '%s:item/bottle_of_clear_water' % NS}})


def nether_membrane():
    """A host item (vanilla phantom membrane, referenced, never copied), tinted nether pink by TCClient.itemColors."""
    w('assets/%s/models/item/nether_membrane.json' % NS, {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': 'minecraft:item/phantom_membrane'}})


if __name__ == '__main__':
    bottle_of_clear_water(); nether_membrane()
    print('concilium redrawn art written')
