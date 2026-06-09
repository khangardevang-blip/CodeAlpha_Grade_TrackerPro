const fetch = require('node-fetch'); // wait, built-in fetch is available in Node 18+
fetch('http://localhost:8080/api/grades/calculate', {
    method: 'OPTIONS'
}).then(res => console.log('OPTIONS status:', res.status))
.catch(err => console.error('OPTIONS error:', err));
