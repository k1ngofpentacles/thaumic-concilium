"""Lang for phase 10-1 (merged by gen_lang.py): the Chort, the Sloppy Alchemist, their lines and the bestiary pages.
Original text (WTFPL) from tools/original/en_US.lang."""
import os

_ORIG = os.environ.get('TC_ORIGINAL_LANG', 'tools/original/en_US.lang')
_old = {}
for _line in open(_ORIG, encoding='utf-8'):
    _line = _line.rstrip('\n')
    if '=' in _line and not _line.startswith('#'):
        _k, _v = _line.split('=', 1)
        _old[_k.strip()] = _v

NS = 'thaumicconcilium'
LANG = {
    f'entity.{NS}.chort': _old['entity.ThaumicConcilium.Chort.name'],
    f'item.{NS}.chort_spawn_egg': _old['entity.ThaumicConcilium.Chort.name'] + ' Spawn Egg',
    f'entity.{NS}.sloppy_alchemist': _old['entity.ThaumicConcilium.SloppyAlchemist.name'],
    f'item.{NS}.sloppy_alchemist_spawn_egg': _old['entity.ThaumicConcilium.SloppyAlchemist.name'] + ' Spawn Egg',
    f'entity.{NS}.chort_spit': _old['entity.ThaumicConcilium.ChortSpit.name'],
    f'{NS}.chort.has_pact': _old['tc.chort.has_pact'],
    f'{NS}.sloppy_alchemist.confused': _old['tc.sloppy_alchemist.confused'],
}
# 10-1b: the alchemist's own research entry (the original's virtual research TCWITCHERYBM had no title; the entity name serves).
LANG[f'research.{NS}.sloppy_alchemist.title'] = _old['entity.ThaumicConcilium.SloppyAlchemist.name']
LANG[f'research.{NS}.sloppy_alchemist.stage_0'] = _old['entry.thaumicconcilium.sloppyalchemist1']
# 10-2: the Burned Witch (original pages burnedwitch1 text, burnedwitch2 the picture, one stage like the other bestiary pages)
LANG[f'entity.{NS}.burned_witch'] = _old['entity.ThaumicConcilium.BurnedWitch.name']
LANG[f'item.{NS}.burned_witch_spawn_egg'] = _old['entity.ThaumicConcilium.BurnedWitch.name'] + ' Spawn Egg'
LANG[f'research.{NS}.burned_witch.title'] = _old['entity.ThaumicConcilium.BurnedWitch.name']
LANG[f'research.{NS}.burned_witch.stage_0'] = _old['entry.thaumicconcilium.burnedwitch1'] + _old['entry.thaumicconcilium.burnedwitch2']
for _i in (1, 2, 3):
    LANG[f'{NS}.sloppy_alchemist.thanks{_i}'] = _old[f'tc.sloppy_alchemist.thanks{_i}']
