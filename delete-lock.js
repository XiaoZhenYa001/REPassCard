const fs = require('fs');
const path = 'C:/Users/Administrator/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9-bin.zip.lck';
try {
    fs.unlinkSync(path);
    console.log('Deleted successfully');
} catch (e) {
    console.error('Error:', e.message);
}
