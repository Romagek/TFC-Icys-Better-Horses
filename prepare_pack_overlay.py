"""Prepare a reviewable pack overlay; --apply installs with a timestamped backup."""
import copy
import datetime
import hashlib
import json
import pathlib
import re
import shutil
import sys

root=pathlib.Path(__file__).resolve().parent
version=re.search(r"version = '([^']+)'",(root/'build.gradle').read_text()).group(1)
pack=pathlib.Path(r'C:\Users\romag\curseforge\minecraft\Instances\Gregnautics (1)')
overlay=root/f'build/pack-overlay-{version}'
rules=json.loads((root/'src/main/resources/breed-rules.json').read_text())
breeds=[r['entity'] for r in rules]
changed={}
def emit(relative,text):
    target=overlay/relative
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(text,encoding='utf-8')
    original=pack/relative
    changed[relative]=hashlib.sha256(original.read_bytes()).hexdigest() if original.exists() else None

# Preserve the pack's existing animal-injury profile rather than inventing new medical rules.
for source in (root/'pack').rglob('*.js'):
    emit(source.relative_to(root/'pack').as_posix(),source.read_text(encoding='utf-8'))

relative='config/gregnautics-medicine-animal-attacks.json'
if (pack/relative).exists():
    data=json.loads((pack/relative).read_text(encoding='utf-8-sig'))
    for breed in breeds:
        if breed not in data['animals']: data['animals'][breed]=copy.deepcopy(data['animals']['tfc:horse'])
    emit(relative,json.dumps(data,ensure_ascii=False,indent=2)+'\n')

relative='config/carryon-common.toml'
if (pack/relative).exists():
    text=(pack/relative).read_text(encoding='utf-8-sig')
    match=re.search(r'(forbiddenStacking\s*=\s*)(\[[^\]]*\])',text)
    if not match: raise ValueError('Carry On forbiddenStacking entry missing')
    entries=json.loads(match[2])
    entries+= [breed for breed in breeds if breed not in entries]
    text=text[:match.start(2)]+json.dumps(entries)+text[match.end(2):]
    emit(relative,text)

relative='config/coldsweat/entity.toml'
if (pack/relative).exists():
    text=(pack/relative).read_text(encoding='utf-8-sig')
    # Cold Sweat explicitly supports comma-separated entity IDs in the same rule.
    # Keep temperature damage disabled if the user has it disabled.
    text=re.sub(r'\["minecraft:horse",', '["minecraft:horse,'+','.join(breeds)+'",',text)
    emit(relative,text)

relative='config/gadgetsandgizmos_mob_haunting.json'
if (pack/relative).exists():
    data=json.loads((pack/relative).read_text(encoding='utf-8-sig'))
    conversions=data.get('conversions',data)
    if 'minecraft:horse' in conversions:
        for breed in breeds: conversions.setdefault(breed,conversions['minecraft:horse'])
        emit(relative,json.dumps(data,ensure_ascii=False,indent=2)+'\n')

relative='config/tfc-icy-horses/breeds.json'
emit(relative,(pack/relative).read_text(encoding='utf-8-sig') if (pack/relative).exists() else json.dumps(rules,indent=2)+'\n')
relative=f'mods/tfc-icys-better-horses-{version}.jar'
(overlay/'mods').mkdir(parents=True,exist_ok=True)
shutil.copy2(root/f'build/libs/tfc-icys-better-horses-{version}.jar',overlay/relative)
changed[relative]=hashlib.sha256((pack/relative).read_bytes()).hexdigest() if (pack/relative).exists() else None
(overlay/'manifest.json').write_text(json.dumps(changed,indent=2)+'\n')
if '--apply' in sys.argv:
    backup=pack/('backup-tfc-icy-horses-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
    for relative,expected in changed.items():
        target=pack/relative
        actual=hashlib.sha256(target.read_bytes()).hexdigest() if target.exists() else None
        if actual!=expected: raise RuntimeError('File changed during preparation: '+relative)
    for relative in changed:
        target=pack/relative
        if target.exists():
            saved=backup/relative
            saved.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(target,saved)
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(overlay/relative,target)
    for previous in (pack/'mods').glob('tfc-icys-better-horses-*.jar'):
        if previous.name != f'tfc-icys-better-horses-{version}.jar':
            if previous.resolve().parent != (pack/'mods').resolve(): raise RuntimeError('Unexpected old addon path')
            saved=backup/'mods'/previous.name
            saved.parent.mkdir(parents=True,exist_ok=True)
            shutil.move(str(previous),str(saved))
    print('Installed addon and compatibility config. Backup:',backup)
else:
    print('Prepared overlay:',overlay)
    print('Files:',*changed,sep='\n')
