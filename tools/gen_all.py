#!/usr/bin/env python3
"""Run every generator in dependency order (run from the module root: python -I tools/gen_all.py).

Phase 4 before 6a/6b/6c (data maps are read-modify-write); lang last. Add new gen_phaseNx_data.py to PHASES.
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PHASES = ['gen_phase1_data.py', 'gen_phase2_data.py', 'gen_phase3_data.py', 'gen_phase3b_data.py',
          'gen_phase4_data.py', 'gen_phase6a_data.py', 'gen_phase6b_data.py', 'gen_phase6c_data.py', 'gen_phase7_data.py', 'gen_phase5a_data.py', 'gen_phase5a_levitator.py', 'gen_phase5a_tower.py',
          'gen_phase5b_data.py', 'gen_phase5b_trunk.py', 'gen_phase5c_data.py', 'gen_phase5c_research.py', 'gen_phase5d_data.py', 'gen_phase5d_grove.py', 'gen_phase5d_pond.py', 'gen_phase5d_look.py', 'gen_phase5d_star.py', 'gen_landmark_trees.py', 'gen_grove_species.py', 'gen_shield_foci_data.py', 'gen_lang.py']

for name in PHASES:
    print('==', name, flush=True)
    r = subprocess.run([sys.executable, '-I', os.path.join(HERE, name)])
    if r.returncode != 0:
        sys.exit(f'{name} failed with exit code {r.returncode}')
print('all generators done')
