import json
import sys
import requests
from collections import defaultdict

API_URL = 'https://ibb-test.vm19002.virt.gwdg.de/ibb/api/'
BIBI_TRON_ID = 'TrOn:0000990'
BIBI_TRON_NAME = 'adult abdominal stink gland'

def addToImageDict(image_dict, fname: str):
    with open(fname) as f:
        next(f) # Skip header
        for line in f:
            phenId, imageId = line.strip().split('\t')
            image_dict[phenId].append(imageId)


def makeImageDict():
    image_dict = defaultdict(list)
    addToImageDict(image_dict, './data/images.tsv')
    addToImageDict(image_dict, './data/images_bibi_atika.tsv')
    return image_dict


def makeStructureDict():
    structure_dict = defaultdict(list)
    with open('./data/entities.tsv') as f:
        next(f) # Skip header
        for line in f:
            cols = line.strip().split('\t')
            structure_dict[cols[0]].append(cols[1].replace("_", ":"))

    tron_ids = set([v for values in structure_dict.values() for v in values])

    trons = fetchTron(tron_ids)
    tron_dict = {t['id']: {'termId': t['id'], 'termName': t['name']} for t in trons}
    struct_dict_with_names = {k: [tron_dict[tron_id] if tron_id in tron_dict else {'termId': tron_id} for tron_id in v] for k, v in structure_dict.items()}
    return struct_dict_with_names

def fetchTron(tronIds: list[str]):
    print(f'Fetching {len(tronIds)} TrOn terms')
    return requests.get(API_URL + '/ontologyservice/v1/ontologies/TrOn/terms/' + ','.join(tronIds)).json()


def fetchGeneSmall(ibs: list[str]):
    r = requests.get(API_URL + '/geneservice/v1/silencingseqs?ids=' + ','.join(ibs))
    if r.status_code != 200:
        print('Failed to fetch genes', file=sys.stderr)
        print(r.text, file=sys.stderr)
        sys.exit(1)
    return r.json()

def fetchGene(ibs: list[str]):
    print(f'Fetching {len(ibs)} ibs')
    res = []
    for i in range(0, len(ibs), 100):
        res.extend(fetchGeneSmall(ibs[i:i+100]))
    return res

def makePhenotypeList():
    res = []
    structure_dict = makeStructureDict()
    image_dict = makeImageDict()
    with open('./data/observations.tsv') as f:
        next(f)
        for i, line in enumerate(f):
            phenId, screenId, topicId, templateId, dayPostInjection, penetranceType, penetranceValue, description, comment = line.strip().split('\t')
            parts = screenId.split("_")
            phen = {
                'id': phenId,
                'dsRNA': {
                    'name': '_'.join(parts[:2])
                },
                'iBeetleExperiment': '_'.join(parts[2:]),
                'dayPostInjection': int(dayPostInjection),
                'description': description,
            }
            penetrance = None
            if penetranceType == 'PERCENTAGE':
                penetrance = float(penetranceValue) / 100
            elif penetranceType == 'ABSOLUTE':
                penetrance = float(penetranceValue) / 10
            elif penetranceType != 'MISSING':
                print('Unknown penetrance type: ' + penetranceType + ', line: ' + i, file=sys.stderr)
            if penetrance is not None:
                phen['penetrance'] = penetrance
            if comment != 'NULL':
                phen['comment'] = comment
            if topicId != 'NULL':
                phen['iBeetleTopic'] = topicId
            if phenId in structure_dict:
                phen['structures'] = structure_dict[phenId]
            if phenId in image_dict:
                phen['imageIds'] = image_dict[phenId]
            res.append(phen)

    with open('./data/observations_bibi_atika.tsv') as f:
        next(f)
        for line in f:
            phenId, screenId, description = line.strip().split('\t')
            phen = {
                'id': phenId,
                'dsRNA': {
                    'name': screenId.replace('bibi_', '')
                },
                'iBeetleExperiment': 'bibi_stink_gland',
                'description': description,
                'structures': [{'termId': BIBI_TRON_ID, 'termName': BIBI_TRON_NAME}],
            }
            if phenId in image_dict:
                phen['imageIds'] = image_dict[phenId]
            res.append(phen)
    return res

def makeGeneList():
    phenotypes = makePhenotypeList()
    ibs = set([p['dsRNA']['name'] for p in phenotypes])
    genes = fetchGene([ib for ib in ibs])
    gene_dict = {g['id']: g['geneIds'] for g in genes}
    res = defaultdict(list)
    for p in phenotypes:
        geneIds = gene_dict[p['dsRNA']['name']]
        for geneId in geneIds:
            res[geneId].append(p)

    with open('iBeetle_grouped_phenotypes.ndjson', 'w') as o:
        for k, v in res.items():
            print(json.dumps(dict(gene=k, phenotypes=v)), file=o)

makeGeneList()
