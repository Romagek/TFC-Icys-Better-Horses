"""Offline production-client smoke test in an isolated game directory.
Reads installed libraries. Never reads launcher accounts or modifies pack files.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import uuid
import zipfile
import sys
import re

root=Path(__file__).parent.resolve()
install=Path(r'C:\Users\romag\curseforge\minecraft\Install')
pack=Path(r'C:\Users\romag\curseforge\minecraft\Instances\Gregnautics (1)')
hud='--hud' in sys.argv
riding='--riding' in sys.argv or hud
shoes='--horseshoes' in sys.argv
visual='--tooltip' in sys.argv or riding or shoes
version=re.search(r"version = '([^']+)'",(root/'build.gradle').read_text()).group(1)
game=root/(f'build/horseshoe-client-qa-{version}' if shoes else f'build/riding-client-qa-{version}' if riding else 'build/tooltip-client-qa' if visual else 'build/full-client-qa')
game.mkdir(parents=True,exist_ok=True)
for folder in ([] if visual else ['config','defaultconfigs','kubejs','scripts','moonlight-global-datapacks']):
    if (pack/folder).exists(): shutil.copytree(pack/folder,game/folder,dirs_exist_ok=True)
(game/'mods').mkdir(exist_ok=True)
for original in (pack/'mods').glob('*.jar'):
    if original.name.startswith('tfc-icys-better-horses-'): continue
    if visual:
        import fnmatch
        patterns=['TerraFirmaCraft-*.jar','Patchouli-*.jar','icys-better-horses-*.jar','modonomicon-*.jar','geckolib-*.jar','architectury-*.jar','cloth-config-*.jar','Jade-*.jar']
        if shoes: patterns+=['gregnautics-core-*.jar','curios-neoforge-*.jar','create-1.21.1-*.jar','gtceu-*.jar','ldlib2-*.jar','configuration-*.jar','modernfix-*.jar','copycats-*.jar','sable-neoforge-*.jar','ferritecore-*.jar']
        if not any(fnmatch.fnmatch(original.name,p) for p in patterns): continue
    target=game/'mods'/original.name
    if not target.exists(): shutil.copy2(original,target)
for previous in (game/'mods').glob('tfc-icys-better-horses-*.jar'):
    if previous.name!=f'tfc-icys-better-horses-{version}.jar':
        archive=game/'old-addon-jars'
        archive.mkdir(exist_ok=True)
        shutil.move(str(previous),str(archive/previous.name))
shutil.copy2(root/f'build/libs/tfc-icys-better-horses-{version}.jar',game/f'mods/tfc-icys-better-horses-{version}.jar')
# The current Medicine dev build embeds these scripts and rejects duplicate loose copies.
# Isolate that pre-existing pack issue here, never in the user's real instance.
with zipfile.ZipFile(pack/'mods/gregnautics-medicine-0.0.0-dev.jar') as z:
    for name in z.namelist():
        if name.startswith('medicine/gregnautics_medicine_production/') and name.endswith('.js'):
            loose=game/'kubejs/server_scripts'/Path(name).name
            if loose.is_file(): loose.rename(loose.with_suffix('.js.qa-disabled'))
# Client QA uses a copy of the disposable server test world.
shutil.copytree(root/'run/Horse-Integration-QA',game/'saves/Horse-Integration-QA',dirs_exist_ok=True)
if visual:
    (game/'options.txt').write_text('onboardAccessibility:false\nlang:ru_ru\nfullscreen:false\npauseOnLostFocus:false\nrenderDistance:4\nsimulationDistance:4\nmaxFps:60\n',encoding='utf-8')
    if shoes:
        (game/'config').mkdir(exist_ok=True)
        (game/'config/modernfix-mixins.properties').write_text('mixin.perf.dynamic_resources=true\n')
base=json.loads((install/'versions/1.21.1/1.21.1.json').read_text())
neo=json.loads((install/'versions/neoforge-21.1.265/neoforge-21.1.265.json').read_text())
def allowed(rules):
    rules=rules or []
    result=not rules
    for rule in rules:
        matches=rule.get('os',{}).get('name','windows')=='windows' and rule.get('os',{}).get('arch','x86_64') in ['x86_64','amd64']
        if rule.get('features'): matches=False
        if matches: result=rule['action']=='allow'
    return result
libraries={}
for lib in base['libraries']+neo['libraries']:
    if allowed(lib.get('rules',[])):
        coordinates=lib['name'].split(':')
        key=tuple(coordinates[:2]+coordinates[3:])
        libraries[key]=lib
cp=[]
natives=game/'natives'
natives.mkdir(exist_ok=True)
for lib in libraries.values():
    artifact=lib.get('downloads',{}).get('artifact')
    if artifact:
        jar=install/'libraries'/artifact['path']
        if not jar.exists(): raise FileNotFoundError(jar)
        cp.append(str(jar))
        if 'natives-windows' in lib['name']:
            with zipfile.ZipFile(jar) as z:
                for entry in z.infolist():
                    if entry.filename.lower().endswith('.dll'):
                        (natives/Path(entry.filename).name).write_bytes(z.read(entry))
    classifier=(lib.get('natives') or {}).get('windows')
    if classifier:
        artifact=lib['downloads']['classifiers'][classifier.replace('${arch}','64')]
        with zipfile.ZipFile(install/'libraries'/artifact['path']) as z:
            for entry in z.infolist():
                if entry.filename.lower().endswith('.dll'):
                    (natives/Path(entry.filename).name).write_bytes(z.read(entry))
# Vanilla jar is resources/ignored by NeoForge; the installed patched client supplies code.
cp.append(str(install/'versions/1.21.1/1.21.1.jar'))
values={
 'library_directory':str(install/'libraries'),'classpath_separator':';','version_name':'1.21.1',
 'natives_directory':str(natives),'launcher_name':'TFCI-QA','launcher_version':'1',
 'classpath':';'.join(cp),'auth_player_name':'HorseCompatQA','version_type':'release',
 'game_directory':str(game),'assets_root':str(install/'assets'),'assets_index_name':base['assetIndex']['id'],
 'auth_uuid':str(uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:HorseCompatQA').digest(),version=3)),
 'auth_access_token':'0','clientid':'0','auth_xuid':'0','user_type':'legacy'
}
def expand(args):
    output=[]
    for arg in args:
        if isinstance(arg,dict):
            if not allowed(arg.get('rules',[])): continue
            value=arg['value']
            output+=expand(value if isinstance(value,list) else [value])
        else:
            for key,value in values.items(): arg=arg.replace('${'+key+'}',value)
            if '${' in arg: raise ValueError('Unresolved launch placeholder: '+arg)
            output.append(arg)
    return output
jvm=expand(base['arguments']['jvm']+neo['arguments']['jvm'])
args=expand(base['arguments']['game']+neo['arguments']['game'])
args+=['--width','1280','--height','800','--quickPlaySingleplayer','Horse-Integration-QA']
cmd=[r'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe','-Xmx6G' if visual else '-Xmx10G','-Dtfci.tooltipQa=true' if visual else '-Dtfci.qa=true']+jvm+[neo['mainClass']]+args
if riding: cmd.insert(2,'-Dtfci.ridingQa=true')
if hud: cmd.insert(2,'-Dtfci.hudQa=true')
if shoes: cmd.insert(2,'-Dtfci.shoesQa=true')
log=open(game/'client-console.log','w',encoding='utf-8')
proc=subprocess.Popen(cmd,cwd=game,stdout=log,stderr=subprocess.STDOUT)
(game/'qa-client-pid.txt').write_text(str(proc.pid))
print('Started isolated full-pack client PID',proc.pid)
raise SystemExit(proc.wait())
