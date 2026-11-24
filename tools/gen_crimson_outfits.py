"""Crimson outfit tables (0.16.0): data/thaumicconcilium/crimson_outfits/<mob>.json.

Each of our cult mobs (paladin, archer, ranger, and a default table for the abstract cultist base) draws its worn armour at
spawn from a weighted list of outfits. Half of every table's weight sits on the host's (Thaumaturge's) crimson armour, half
on Armor Series - Crimson Cult (Enderghast, mod id armor_series__crimson_cult_, referenced by item id only). The `source`
field of an outfit ('host' or 'armor_series') exists so the pack test can check that split. Items missing at load time are
skipped by the loader with one log line.
Run from the module root (gen_all.py does).
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

AS = 'armor_series__crimson_cult_'
TH = 'thaumaturge'


def host(prefix, parts=('helm', 'chest', 'legs'), boots=True):
    o = {'head': f'{TH}:crimson_{prefix}_helm', 'chest': f'{TH}:crimson_{prefix}_chest', 'legs': f'{TH}:crimson_{prefix}_legs'}
    o['feet'] = f'{TH}:crimson_boots' if boots else ''
    return o


CLERIC = host('robe')            # Thaumaturge's cleric robes
KNIGHT = host('plate')           # its knight plate
PRAETOR = host('praetor')
RANGER_H = host('ranger')
GILDED = {'head': f'{NS}:gilded_crimson_helm', 'chest': f'{TH}:crimson_plate_chest', 'legs': f'{TH}:crimson_robe_legs', 'feet': f'{TH}:crimson_boots'}


def a_set(name, boots=False, head=None):
    return {'head': f'{AS}:{head or name + "_helmet"}', 'chest': f'{AS}:{name}_chestplate', 'legs': f'{AS}:{name}_leggings',
            'feet': f'{AS}:{name}_boots' if boots else ''}


A_ROBES = a_set('crimson_cultist_robes')
A_CROSSBOW = a_set('crimson_crossbowman')
A_RANGER = a_set('crimson_ranger')
A_MADCAP = a_set('crimson_madcap', boots=True)
A_KNIGHT = a_set('crimson_knight')
A_PRAETOR = a_set('crimson_praetor', boots=True)
A_PALADIN = {'head': f'{AS}:paladin_helm_helmet', 'chest': f'{AS}:crimson_knight_chestplate', 'legs': f'{AS}:crimson_knight_leggings', 'feet': ''}

# name -> list of (source, weight, outfit); each table's weights add to 100, 50 per source.
TABLES = {
    'crimson_paladin': [('host', 25, GILDED), ('host', 25, PRAETOR),
                        ('armor_series', 20, A_PALADIN), ('armor_series', 15, A_KNIGHT), ('armor_series', 15, A_PRAETOR)],
    'crimson_archer': [('host', 50, CLERIC), ('armor_series', 50, A_CROSSBOW)],
    'crimson_ranger': [('host', 50, RANGER_H), ('armor_series', 25, A_RANGER), ('armor_series', 25, A_MADCAP)],
    'crimson_cultist': [('host', 25, CLERIC), ('host', 25, KNIGHT), ('armor_series', 25, A_ROBES), ('armor_series', 25, A_KNIGHT)],
}

for mob, rows in TABLES.items():
    assert sum(r_[1] for r_ in rows) == 100 and sum(r_[1] for r_ in rows if r_[0] == 'host') == 50, mob
    w(f'data/{NS}/crimson_outfits/{mob}.json', {'outfits': [dict(source=s, weight=wt, **o) for s, wt, o in rows]})
print('crimson outfit tables written')
