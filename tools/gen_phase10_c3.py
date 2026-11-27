"""Lang for phase 10-3 (merged by gen_lang.py): the Crimson Spells research, their names and the hint note's tooltip.
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
    f'research.{NS}.crimson_spells.title': _old['tc.research_name.CRIMSONSPELLS'],
    f'research.{NS}.crimson_spells.stage_0': _old['tc.research_text.CRIMSONSPELLS'] + '<BR><BR>' + _old['entry.crimsonspells.first'],
    # Page 2 is the original's picture; the paragraph is ours (the original left the pictograms to speak for themselves).
    f'research.{NS}.crimson_spells.stage_1': _old['entry.crimsonspells.second'] + '<BR><BR>'
        'Cast these as the Witch does her symbols: hold an Ent Twig, be infused, and draw the pictogram. Incarcerous (a thrown shackle, or '
        'orbs of sloth dust), Intrare Polosae (a critical arrow), Ariania Exumai (a bolt against arthropods), Trup Ehsolvi (a bolt against the undead) '
        'and Oculus Madesco (blurs the sight of what you point at). The Overworld infusion strengthens the arrow by your Aversio, the Light '
        'infusion the bolts by your Lux, and the Infernal infusion turns Incarcerous into a shackle and blinds outright, by your Tenebrae.',
    f'{NS}.tooltip.hint.crimsonspells': "Pictograms from the Pontifex's scrolls. They look like instructions for a kind of magic you once practised...",
}
# The remake shows a spell by 'witchery.symbology.<path>'.
for _path, _key in (('crimson_incarcerous', 'tc.spell.incarcerous'), ('crimson_arrow', 'tc.spell.arrow'), ('crimson_spider', 'tc.spell.spider'),
                    ('crimson_undead', 'tc.spell.undead'), ('crimson_undead_greater', 'tc.spell.undead'), ('crimson_blind', 'tc.spell.blind')):
    LANG[f'witchery.symbology.{_path}'] = _old[_key]
