import hashlib
import json
import pathlib
import shutil
import zipfile
import re

root=pathlib.Path(__file__).resolve().parent
version=re.search(r"version = '([^']+)'",(root/'build.gradle').read_text()).group(1)
assert (root/'run/Horse-Integration-QA/tfci-qa-result.txt').read_text().startswith('PASS 823'), 'Integration suite must pass'
riding_result=root/f'build/riding-client-qa-{version}/riding-qa-result.txt'
if version == '0.1.6':
    # HUD-only release: prove all existing gameplay classes are unchanged.
    with zipfile.ZipFile(root/'build/release-0.1.5/mods/tfc-icys-better-horses-0.1.5.jar') as before, zipfile.ZipFile(root/f'build/libs/tfc-icys-better-horses-{version}.jar') as current:
        for path in before.namelist():
            if path.endswith('.class') and not path.endswith(('StaminaHud.class','RidingClientChecks.class','TooltipClientFixture.class')):
                assert before.read(path)==current.read(path),'Gameplay changed: '+path
    riding_result=root/'build/riding-client-qa-0.1.5/riding-qa-result.txt'
if not riding_result.exists():
    # Reuse the previous motion measurement only if the shipped gait implementation
    # is byte-for-byte identical to the already tested class.
    riding_result=root/'build/riding-client-qa-0.1.2/riding-qa-result.txt'
    with zipfile.ZipFile(root/'build/libs/tfc-icys-better-horses-0.1.2.jar') as before, zipfile.ZipFile(root/f'build/libs/tfc-icys-better-horses-{version}.jar') as current:
        gait_class='com/gregnautics/tfchorses/mixin/ManualGaitMixin.class'
        assert before.read(gait_class)==current.read(gait_class),'Changed gait implementation needs new motion measurements'
assert riding_result.read_text().startswith('PASS:'), 'Riding client checks must pass'
if version == '0.1.5':
    assert (root/f'build/riding-client-qa-{version}/saves/Horse-Integration-QA/stamina-qa-result.txt').read_text().startswith('PASS 113'), 'Stamina checks must pass'
shoes_result=root/f'build/horseshoe-client-qa-{version}/saves/Horse-Integration-QA/horseshoe-qa-result.txt'
if not shoes_result.exists():
    with zipfile.ZipFile(root/'build/release-0.1.3/mods/tfc-icys-better-horses-0.1.3.jar') as before, zipfile.ZipFile(root/f'build/libs/tfc-icys-better-horses-{version}.jar') as current:
        for name in ['CoreHorseshoeMenuMixin','CoreHorseshoeScreenMixin']:
            path=f'com/gregnautics/tfchorses/mixin/{name}.class'
            assert before.read(path)==current.read(path),'Changed horseshoe implementation needs new checks'
    shoes_result=root/'build/horseshoe-client-qa-0.1.3/saves/Horse-Integration-QA/horseshoe-qa-result.txt'
assert shoes_result.read_text().startswith('PASS 286'), 'Core horseshoe checks must pass'
release=root/f'build/release-{version}'
release.mkdir(parents=True,exist_ok=True)
shutil.copytree(root/f'build/pack-overlay-{version}',release,dirs_exist_ok=True)
for name in ['README-RU.md','RESULTS.md']:
    shutil.copy2(root/name,release/name)
rules=json.loads((root/'src/main/resources/breed-rules.json').read_text())
lines=['# Правила естественного появления пород','', '| Порода | Температура, °C | Осадки TFC | Вес | Биомы |','|---|---:|---:|---:|---|']
for r in rules:
    lines.append(f"| {r['entity'].split(':')[1]} | {r['minTemperature']}…{r['maxTemperature']} | {r['minRainfall']}…{r['maxRainfall']} | {r['weight']} | {', '.join(r['biomes'])} |")
lines+=['','Эти границы определяют естественный спавн. Они не наносят климатический урон существующей лошади.','Правила меняются в config/tfc-icy-horses/breeds.json; затем требуется перезапуск сервера.']
(release/'BREEDS.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
checksums={str(p.relative_to(release)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest()
           for p in release.rglob('*') if p.is_file() and p.name!='SHA256.json'}
(release/'SHA256.json').write_text(json.dumps(checksums,indent=2)+'\n')
bundle=root/f'build/TFC-Icys-Better-Horses-{version}-Gregnautics.zip'
with zipfile.ZipFile(bundle,'w',zipfile.ZIP_DEFLATED) as z:
    for p in release.rglob('*'):
        if p.is_file(): z.write(p,str(p.relative_to(release)))
sources=root/f'build/TFC-Icys-Better-Horses-{version}-sources.zip'
with zipfile.ZipFile(sources,'w',zipfile.ZIP_DEFLATED) as z:
    for folder in ['src','gradle','pack']:
        for p in (root/folder).rglob('*'):
            if p.is_file(): z.write(p,str(p.relative_to(root)))
    for p in root.iterdir():
        if p.is_file() and p.suffix in ['.gradle','.properties','.py','.md','.bat'] or p.name=='gradlew': z.write(p,p.name)
print(bundle)
print(sources)
