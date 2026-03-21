import os
path = r'C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9-bin.zip.lck'
try:
    os.remove(path)
    print('Deleted successfully')
except Exception as e:
    print(f'Error: {e}')
