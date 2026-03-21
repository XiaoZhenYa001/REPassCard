import os
import sys
import urllib.request
import zipfile
import shutil

# Download Gradle to user home directory
url = "https://services.gradle.org/distributions/gradle-8.7-bin.zip"
home = os.path.expanduser("~")
dest_zip = os.path.join(home, "gradle-8.7.zip")
dest_dir = os.path.join(home, "gradle-8.7")

print(f"Home: {home}")
print(f"Downloading {url}...")
print(f"To: {dest_zip}")

try:
    urllib.request.urlretrieve(url, dest_zip)
    print("Download complete!")
    
    print("Extracting...")
    with zipfile.ZipFile(dest_zip, 'r') as zip_ref:
        zip_ref.extractall(home)
    
    print("Running Gradle...")
    os.chdir(r"D:\Temp\REPassCard")
    gradle_bat = os.path.join(dest_dir, "bin", "gradle.bat")
    print(f"Gradle: {gradle_bat}")
    os.system(f'"{gradle_bat}" assembleDebug --no-daemon')
except Exception as e:
    print(f"Error: {e}")
