"""Build addon-owned data; original JARs are read only. No copied art or code."""
import json
import pathlib
import zipfile

root = pathlib.Path(__file__).parent / 'src/main/resources'
mods = pathlib.Path(r'C:\Users\romag\curseforge\minecraft\Instances\Gregnautics (1)\mods')
def write(path, obj):
    target = root / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

# Gameplay habitat envelopes, not claims about historical breed distributions.
# Biome tags restrict natural migration to land; TFC still controls herd spawning.
presets = [
 ('icelandic', -15, 8, 100, 500, 12),
 ('haflinger', -8, 16, 100, 450, 9),
 ('friesian', 3, 19, 220, 500, 7),
 ('shire', 0, 18, 180, 500, 5),
 ('clydesdale', -3, 16, 180, 500, 5),
 ('belgian', 4, 22, 150, 450, 6),
 ('percheron', 1, 21, 130, 400, 6),
 ('thoroughbred', 8, 25, 130, 400, 5),
 ('andalusian', 12, 30, 60, 260, 8),
 ('arabian', 18, 40, 0, 180, 12),
 ('quarter', 7, 30, 40, 260, 9),
 ('american_paint', 3, 28, 60, 300, 9),
 ('appaloosa', -5, 22, 70, 320, 9),
 ('morgan', -5, 24, 120, 450, 9),
 ('mustang', -15, 40, 0, 500, 3),
]
rules = [dict(entity=f'icys-better-horses:{breed}_horse', minTemperature=t0,maxTemperature=t1,
              minRainfall=r0,maxRainfall=r1,weight=w,biomes=['#tfc_icy_horses:horse_habitat'])
         for breed,t0,t1,r0,r1,w in presets]
write('breed-rules.json',rules)
with zipfile.ZipFile(mods/'TerraFirmaCraft-NeoForge-1.21.1-4.2.13.jar') as z:
    biome_ids = ['tfc:' + n.removeprefix('data/tfc/worldgen/biome/').removesuffix('.json')
                 for n in z.namelist() if n.startswith('data/tfc/worldgen/biome/') and n.endswith('.json')]
    dry_land = [b for b in biome_ids if not any(s in b for s in ['ocean','reef','river','lake','swamp','shore','tidal','salt_marsh'])]
    write('data/tfc_icy_horses/tags/worldgen/biome/horse_habitat.json',
          {'replace':False,'values':dry_land + ['minecraft:plains','minecraft:savanna','minecraft:meadow','minecraft:snowy_plains','minecraft:desert']})
    terrain_groups = {
        'uplands': [b for b in dry_land if any(s in b for s in ['mountain','highland','hill','plateau','tuya','shield','burren'])],
        'open_country': [b for b in dry_land if any(s in b for s in ['plain','hill','valley','lowland','drumlin','shield','steppe'])],
        'dry_country': [b for b in dry_land if any(s in b for s in ['plain','dune','badland','plateau','canyon','hill'])],
    }
    for group, values in terrain_groups.items():
        write(f'data/tfc_icy_horses/tags/worldgen/biome/{group}.json',{'replace':False,'values':values})
    for rule in rules:
        breed = rule['entity'].split(':')[1].removesuffix('_horse')
        group = 'uplands' if breed in ['haflinger','icelandic'] else 'dry_country' if breed in ['arabian','mustang','quarter','andalusian','american_paint','appaloosa'] else 'open_country'
        rule['biomes'] = [f'#tfc_icy_horses:{group}']
    write('breed-rules.json',rules)
    breed_ids = [rule['entity'] for rule in rules]
    for name in z.namelist():
        if name.startswith('data/tfc/tags/entity_type/') and name.endswith('.json'):
            if 'tfc:horse' in json.loads(z.read(name)).get('values',[]):
                write(name, {'replace':False,'values':breed_ids})
    # TFC uses groundwater for the broad fauna gate. Breed choice uses annual rainfall.
    write('data/tfc/tfc/fauna/horse.json',dict(min_temperature=-15,max_temperature=40,min_groundwater=0,max_groundwater=500,max_forest=2))

# Reference the original TFC loot table instead of copying it.
for breed,*_ in presets:
    write(f'data/icys-better-horses/loot_table/entities/{breed}_horse.json',
          {'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table','value':'tfc:entities/horse'}]}]})
write('pack.mcmeta',{'pack':{'pack_format':48,'description':"TFC Icy's Better Horses compatibility data"}})

def shaped(name, pattern, keys):
    write(f'data/icys-better-horses/recipe/{name}.json', dict(
        type='minecraft:crafting_shaped', category='equipment', pattern=pattern, key=keys,
        result={'id':f'icys-better-horses:{name}','count':1}))
def item(name): return {'item':name}
shaped('canister',[' C ','G G',' C '],{'C':item('tfc:metal/sheet/copper'),'G':item('tfc:metal/sheet/gold')})
shaped('horse_hooves_gear',['I I','ILI','I I'],{'I':item('tfc:metal/ingot/wrought_iron'),'L':item('minecraft:leather')})
shaped('horse_medkit_gear',['WWW','SGS','WWW'],{'W':item('tfc:wool_cloth'),'S':item('tfc:silk_cloth'),'G':item('tfc:glue')})
shaped('horse_stabilizer_gear',[' S ','CLC',' S '],{'S':item('tfc:metal/sheet/steel'),'C':item('icys-better-horses:canister'),'L':item('minecraft:leather')})
shaped('upgraded_saddle',[' L ','DSD',' B '],{'L':item('minecraft:leather'),'D':item('tfc:gem/diamond'),'S':item('minecraft:saddle'),'B':item('tfc:metal/sheet/bronze')})
