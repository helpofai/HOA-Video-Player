import urllib.request
import json
import ssl
import os

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

url = "https://api.github.com/repos/arthenica/ffmpeg-kit/releases"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
response = urllib.request.urlopen(req, context=ctx)
data = json.loads(response.read().decode('utf-8'))

download_url = None
for release in data:
    for asset in release.get('assets', []):
        if "min-gpl" in asset['name'] and "android" in asset['name'] and asset['name'].endswith(".aar"):
            download_url = asset['browser_download_url']
            break
    if download_url:
        break

if download_url:
    print(f"Found AAR: {download_url}")
    os.makedirs("app/libs", exist_ok=True)
    urllib.request.urlretrieve(download_url, "app/libs/ffmpeg-kit.aar")
    print("Download complete.")
else:
    print("AAR not found.")
