"""Local public configuration check before the signed commercial build. Never prints values."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
from urllib.parse import urlparse

root = Path(__file__).resolve().parents[1]
values = {element.attrib.get('name'): ''.join(element.itertext()).strip() for element in ET.parse(root / 'app/src/main/res/values/rumo_account.xml').getroot()}
missing = []
for field in ['rumo_firebase_api_key', 'rumo_firebase_project_id', 'rumo_firebase_app_id', 'rumo_license_public_key', 'rumo_account_origin', 'rumo_google_web_client_id']:
    if not values.get(field): missing.append(field)
for field in ['rumo_account_enabled', 'rumo_play_billing_enabled']:
    if values.get(field) != 'true': missing.append(field)
url = urlparse(values.get('rumo_account_origin', ''))
if url.scheme != 'https' or not url.netloc or url.path or url.query or url.fragment or url.username or url.password:
    missing.append('rumo_account_origin (origem HTTPS sem caminho/barra final)')
key = values.get('rumo_license_public_key', '')
if 'BEGIN PUBLIC KEY' not in key or 'PRIVATE KEY' in key: missing.append('rumo_license_public_key (somente chave pública SPKI)')
if values.get('rumo_purchase_mode') not in ['test', 'live']: missing.append('rumo_purchase_mode')
if values.get('rumo_play_product_id') != 'rumo_acesso_completo': missing.append('rumo_play_product_id (igual ao produto do servidor e Play Console)')
if missing:
    print('Configuração comercial incompleta. Confira CONFIGURAR_VENDAS_1_5.md:', file=sys.stderr)
    for item in sorted(set(missing)): print(' - ' + item, file=sys.stderr)
    sys.exit(1)
print('Configuração pública presente. Ainda é necessário testar os serviços e a loja reais.')
