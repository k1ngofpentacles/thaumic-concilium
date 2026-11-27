"""Lang for phase 10-5 (merged by gen_lang.py): Earmuffs, the Blood Bracelet and the Drainage Syringe (items, research, messages).
Original text (WTFPL) from tools/original/en_US.lang."""
import os
NS = 'thaumicconcilium'
_ORIG = os.environ.get('TC_ORIGINAL_LANG', 'tools/original/en_US.lang')
_old = {}
for _line in open(_ORIG, encoding='utf-8'):
    _line = _line.rstrip('\n')
    if '=' in _line and not _line.startswith('#'):
        _k, _v = _line.split('=', 1)
        _old[_k.strip()] = _v

_HOW_BLOOD = ("Worn, it takes life points from my soul network, five for every hundredth of a vis (four with its alchemical cap), "
              "a little of every primal at a time. A network that cannot pay takes my own health instead, and at three points of "
              "health or fewer, all of it. It holds 50 vis.")

LANG = {
    f'item.{NS}.earmuffs': 'Earmuffs',
    f'item.{NS}.earmuffs.desc': 'Muffles the Vengeful Golem\'s bell',
    f'item.{NS}.blood_bracelet': _old['item.CastingBracelet.alchemist'],
    f'research.{NS}.blood_bracelet.title': _old['tc.research_name.BLOODBRACELET'],
    f'research.{NS}.blood_bracelet.stage_0': _old['tc.research_text.BLOODBRACELET'] + '<BR><BR>' + _old['entry.bloodbracelet.first'],
    f'research.{NS}.blood_bracelet.stage_1': _HOW_BLOOD,
    f'item.{NS}.drainage_syringe': _old['item.DrainageSyringe.name'],
    f'item.{NS}.drainage_syringe.type.vis': 'Draws: vis',
    f'item.{NS}.drainage_syringe.type.mana': 'Draws: mana',
    f'item.{NS}.drainage_syringe.type.blood': 'Draws: life points',
    f'item.{NS}.drainage_syringe.amount': 'Held: %s / %s',
    f'research.{NS}.drainage_syringe.title': _old['tc.research_name.DRAINAGESYRINGE'],
    f'research.{NS}.drainage_syringe.stage_0': _old['tc.research_text.DRAINAGESYRINGE'] + '<BR><BR>' + _old['entry.drainagesyringe.first'],
    f'research.{NS}.drainage_syringe.stage_1': _old['entry.drainagesyringe.second'].replace('LMB', 'left click').replace('RMB', 'right click'),
}
