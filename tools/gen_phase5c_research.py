"""Phase 5c cycle 4: the Vessel's research (R1 field notes, our tab). Run after gen_phase5c_data.py, before gen_lang.py.

Five entries along row 1, right of the Hex of Predictability (-12, 1):
  rifts_that_hold (-11,1)  note 1, handed over with the hex research (warp 1)
  what_it_keeps   (-10,1)  note 2, a hex you fed passes a quarter of its threshold
  the_vessel      (-9,1)   note 3, appears at the first break, completes at the first subdue
  drawing_it_out  (-8,1)   the rite: parents the_vessel and Thaumaturge's unlock_eldritch; ordinary research (theory), warp 3
  primordial_mote (-7,1)   note 6, the first mote
Notes 1, 2, 3 and 6 are granted by code (world/VesselResearch), so they carry no theory requirement and no autounlock.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

ICON = f'{NS}:vessel_anchor'


def note_entry(name, col, parents, notes, icon=ICON, warp=0, meta=None):
    entry(name, col, 1, icon, parents, notes, warp=warp, meta=meta or ['hidden'], complexity=1, stages=2, autounlock=True)
    p = f'data/{NS}/thaumaturge/research_entry/{name}.json'
    o = r(p)
    o['meta'] = [m for m in o.get('meta', []) if m != 'autounlock']
    if not o['meta']:
        del o['meta']
    w(p, o)


# 1: appears with the lesser hex (the hex research); the book shows it after the hex entry. Warp 1.
note_entry('rifts_that_hold', -11, [f'{NS}:hex_of_predictability'], [('vitium', 8), ('auram', 8)], warp=1)
# 2: a hex of yours passes a quarter of its threshold.
note_entry('what_it_keeps', -10, [f'{NS}:rifts_that_hold'], [('vitium', 8), ('ordo', 8), ('vacuos', 8)])
# 3: hidden until the first break; stage 2 at the first subdue.
note_entry('the_vessel', -9, [f'{NS}:what_it_keeps'], [('vitium', 12), ('vacuos', 12), ('alienis', 8)], meta=['hidden', 'round'])
# 6: the first mote.
note_entry('primordial_mote', -7, [f'{NS}:the_vessel'], [('alienis', 8), ('vacuos', 8), ('humanus', 8)],
           icon=f'{NS}:primordial_mote', meta=['hidden', 'round'])

# 4: the rite. Needs entry 3 done and Thaumaturge's eldritch gate. Ordinary research with a theory requirement; warp 3.
entry('drawing_it_out', -8, 1, ICON, [f'{NS}:the_vessel', f'~thaumaturge:unlock_eldritch'],
      [('vitium', 12), ('alienis', 12), ('vacuos', 8), ('perditio', 8)], warp=3, meta=['hidden', 'spiky'], complexity=3)
