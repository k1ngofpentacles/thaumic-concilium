"""Our own redraws of three textures that used to be copied from the 1.7.10 original and were Thaumcraft-4-derived art
(asset cleanup, 2026-11-24): the Crimson Archer skin, the Bottle of Clear Water and the Nether Membrane. Drawn from scratch
with new silhouettes; same paths and sizes. Run from the module root (via gen_all.py)."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *
# the workspace art kit (tools/art-style): the item icons at the end of this file are drawn with it
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..', 'tools', 'art-style'))
from artkit import Sprite, tone as kit_tone


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


# ================================================================ art pass 2026-11-27: icons drawn with the workspace art kit
# (tools/art-style/artkit.py: hue-shifted material ramps, top-left light, outline in each material's own dark tones).
# These sit beside host items (phials, plates, crystals), so they keep the kit's default grain.
def item_png(name):
    return os.path.join(A, 'textures', 'item', name + '.png')


def crimson_plating():
    """Same item type as the host's metal plates: the host iron plate (referenced, never copied), tinted crimson by
    TCClient.itemColors. No texture of ours."""
    w('assets/%s/models/item/crimson_plating.json' % NS, {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': 'thaumaturge:item/plate_iron'}})
    old = item_png('crimson_plating')
    if os.path.exists(old):
        os.remove(old)


# crimson blood: one heavy drop of cult blood caught as it falls, its tail leaning a little to the right
def crimson_blood():
    s = Sprite([
        '................',
        '........b.......',
        '........bb......',
        '.......bbb......',
        '.......bbbb.....',
        '......bbbbb.....',
        '.....bbbbbbb....',
        '....bbbbbbbbb...',
        '...bbbbbbbbbbb..',
        '...bbbbbbbbbbb..',
        '...bbbbbbbbbbb..',
        '...bbbbbbbbbbb..',
        '....bbbbbbbbb...',
        '.....bbbbbbb....',
        '.......bbb......',
        '................',
    ], {'b': 'blood'}, seed=2)
    s.detail(5, 9, kit_tone('blood', 5)).detail(6, 8, kit_tone('blood', 4))
    s.save(item_png('crimson_blood'))


# primordial mote: a speck of dull light the size of a seed, resting in the palm; it does not warm and does not go out
def primordial_mote():
    s = Sprite([
        '................',
        '................',
        '................',
        '................',
        '................',
        '.......mm.......',
        '......mmmm......',
        '.....mmmmmm.....',
        '.....mmmmmmm....',
        '......mmmmm.....',
        '.......mmm......',
        '................',
        '................',
        '................',
        '................',
        '................',
    ], {'m': 'primordial'}, seed=4, shiny='')
    for x, y in ((7, 7), (8, 7), (7, 8)):                       # the core
        s.detail(x, y, kit_tone('primordial', 5))
    s.halo('primordial', alpha=110, reach=2)
    for x, y in ((3, 4), (12, 5), (11, 12), (4, 11)):           # stray glints around it
        s.detail(x, y, kit_tone('primordial', 4)[:3] + (150,))
    s.save(item_png('primordial_mote'))


# research hint: a torn sheet of parchment with hurried violet scrawl and a blot, lying askew, its lower corner curled over
def research_hint():
    s = Sprite([
        '................',
        '.....ppppppp....',
        '....ppppppppp...',
        '....pppppppppp..',
        '...ppppppppppp..',
        '...ppppppppppp..',
        '...pppppppppp...',
        '..ppppppppppp...',
        '..ppppppppppp...',
        '..pppppppppp....',
        '..pppppppppp....',
        '.ppppppppppp....',
        '.cccppppppp.....',
        '.ccccpppppp.....',
        '..cccppppp......',
        '................',
    ], {'p': 'parchment', 'c': 'parchment'}, seed=6, flat='p')
    for y, x0, x1 in ((3, 6, 11), (5, 5, 10), (7, 4, 10), (9, 4, 8), (11, 5, 9)):   # five lines of scrawl, leaning with the sheet
        for x in range(x0, x1 + 1):
            if (x * 7 + y) % 5 != 0:
                s.detail(x, y, kit_tone('ink', 2 if (x + y) % 3 else 3))
    for x, y in ((9, 9), (10, 9), (9, 10), (10, 8)):            # the blot
        s.detail(x, y, kit_tone('ink', 1))
    s.save(item_png('research_hint'))


# polishment device: a jeweller's loupe, violet glass in a brass ring on a turned greatwood handle, held by the handle on the diagonal
def polishment_device():
    s = Sprite([
        '................',
        '........bbbbb...',
        '.......bbgggbb..',
        '......bbgggggbb.',
        '......bgggggggb.',
        '......bgggggggb.',
        '......bgggggggb.',
        '......bbgggggbb.',
        '.....wbbbgggbb..',
        '....www.bbbbb...',
        '...www..........',
        '..www...........',
        '.www............',
        '.ww.............',
        '................',
        '................',
    ], {'b': 'brass', 'g': 'rift_glass', 'w': 'greatwood'}, seed=8)
    s.detail(9, 3, kit_tone('rift_glass', 5)).detail(8, 4, kit_tone('rift_glass', 5)).detail(9, 4, kit_tone('rift_glass', 4))
    s.detail(4, 9, kit_tone('brass', 3))                         # the ferrule where the handle meets the ring
    s.save(item_png('polishment_device'))


# the gems (break-particle texture only: the items are drawn by GemRenderer): a brilliant-cut stone, table up, point down
def gems():
    rows = [
        '................',
        '................',
        '.....tttttt.....',
        '....tttttttt....',
        '...tttttttttt...',
        '..gggggggggggg..',
        '..gggggggggggg..',
        '...gggggggggg...',
        '...gggggggggg...',
        '....gggggggg....',
        '....gggggggg....',
        '.....gggggg.....',
        '......gggg......',
        '.......gg.......',
        '................',
        '................',
    ]
    for name, mat in (('inert_rift_gem', 'gem_rift_inert'), ('rift_gem', 'gem_rift'),
                      ('inert_terra_cast_gem', 'gem_terra_inert'), ('terra_cast_gem', 'gem_terra')):
        s = Sprite(rows, {'t': mat, 'g': mat}, seed=10)
        for x, y in ((5, 7), (6, 9), (7, 11), (10, 7), (9, 9)):  # pavilion facet lines running to the point
            s.detail(x, y, kit_tone(mat, 1 if x < 8 else 2))
        s.detail(5, 3, kit_tone(mat, 5)).detail(6, 3, kit_tone(mat, 4))
        s.save(item_png(name))


if __name__ == '__main__':
    bottle_of_clear_water(); nether_membrane(); crimson_plating()
    crimson_blood(); primordial_mote(); research_hint(); polishment_device(); gems()
    print('concilium redrawn art written')
