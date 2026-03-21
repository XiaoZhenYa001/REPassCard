const https = require('https');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const url = 'https://services.gradle.org/distributions/gradle-8.7-bin.zip';
const dest = 'D:\\gradle-8.7.zip';

console.log('Downloading Gradle 8.7...');

const file = fs.createWriteStream(dest);
https.get(url, (response) => {
    if (response.statusCode === 302 || response.statusCode === 301) {
        console.log('Following redirect to:', response.headers.location);
        https.get(response.headers.location, (response2) => {
            response2.pipe(file);
            file.on('finish', () => {
                file.close();
                console.log('Download complete!');
                console.log('Extracting...');
                // Note: Node can't easily unzip, we'll use Java for that
            });
        });
    } else {
        response.pipe(file);
        file.on('finish', () => {
            file.close();
            console.log('Download complete!');
        });
    }
}).on('error', (err) => {
    fs.unlink(dest, () => {});
    console.error('Error:', err.message);
});
