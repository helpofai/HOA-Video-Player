import urllib.request
import json
import ssl
import os

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Fetch ALL releases (paginated), not just first page
page = 1
download_url = None
while True:
    url = f"https://api.github.com/repos/arthenica/ffmpeg-kit/releases?per_page=100&page={page}"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    response = urllib.request.urlopen(req, context=ctx)
    data = json.loads(response.read().decode('utf-8'))
    
    if not data:
        break
    
    for release in data:
        assets = release.get('assets', [])
        if assets:
            print(f"Checking release: {release['tag_name']} ({len(assets)} assets)")
        for asset in assets:
            name = asset['name']
            if 'min-gpl' in name and 'android' in name and name.endswith('.aar'):
                download_url = asset['browser_download_url']
                print(f"Found: {name} ({asset['size']} bytes)")
                break
        if download_url:
            break
    
    if download_url:
        break
    page += 1
    if page > 5:  # Safety limit
        break

if download_url:
    os.makedirs("app/libs", exist_ok=True)
    print(f"Downloading from: {download_url}")
    urllib.request.urlretrieve(download_url, "app/libs/ffmpeg-kit.aar")
    file_size = os.path.getsize("app/libs/ffmpeg-kit.aar")
    print(f"Download complete: {file_size} bytes ({file_size / 1024 / 1024:.1f} MB)")
else:
    print("AAR not found in GitHub releases. Try Maven Central or manual download.")