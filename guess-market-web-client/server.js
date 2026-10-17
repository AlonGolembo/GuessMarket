/*
 * Guess Market web client - tiny static file server + API proxy.
 *
 * Why a proxy: the browser loads this client from http://localhost:3000, while
 * the Guess Market server lives on Tomcat at http://localhost:8080. The server
 * sends no CORS headers (and must not be changed for this client), so the
 * browser would block direct cross-origin calls. Instead every request under
 * /guess-market-server/ is forwarded as-is to Tomcat, making the API look
 * same-origin to the browser. The web client therefore calls exactly the same
 * endpoints the JavaFX client does.
 *
 * Only Node's built-in modules are used - no npm install is needed.
 *
 * Environment overrides (optional):
 *   PORT         port for this web client          (default 3000)
 *   GM_SERVER    Tomcat origin to forward API calls (default http://localhost:8080)
 * Flags:
 *   --open       open the default browser once the server is listening
 */
'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const PORT = Number(process.env.PORT) || 3000;
const TARGET = new URL(process.env.GM_SERVER || 'http://localhost:8080');
const API_PREFIX = '/guess-market-server/';
const PUBLIC_DIR = path.join(__dirname, 'public');

const MIME = {
    '.html': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.png': 'image/png',
    '.ico': 'image/x-icon',
};

function proxy(req, res) {
    const upstream = http.request({
        protocol: TARGET.protocol,
        hostname: TARGET.hostname,
        port: TARGET.port || 80,
        method: req.method,
        path: req.url,
        headers: { ...req.headers, host: TARGET.host },
    }, (upRes) => {
        res.writeHead(upRes.statusCode, upRes.headers);
        upRes.pipe(res);
    });
    upstream.on('error', (err) => {
        if (res.headersSent) {
            res.destroy();
            return;
        }
        res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({
            error: `Cannot reach the Guess Market server at ${TARGET.origin}${API_PREFIX} (${err.code || err.message}). ` +
                'Make sure Tomcat is running with guess-market-server.war deployed.',
        }));
    });
    req.pipe(upstream);
}

function serveStatic(req, res) {
    let urlPath = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    if (urlPath === '/') urlPath = '/index.html';
    const filePath = path.normalize(path.join(PUBLIC_DIR, urlPath));
    if (!filePath.startsWith(PUBLIC_DIR + path.sep)) {
        res.writeHead(403);
        res.end('Forbidden');
        return;
    }
    fs.readFile(filePath, (err, data) => {
        if (err) {
            res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
            res.end('Not found');
            return;
        }
        res.writeHead(200, {
            'Content-Type': MIME[path.extname(filePath).toLowerCase()] || 'application/octet-stream',
            'Cache-Control': 'no-cache',
        });
        res.end(data);
    });
}

const server = http.createServer((req, res) => {
    if (req.url.startsWith(API_PREFIX)) {
        proxy(req, res);
    } else if (req.method === 'GET' || req.method === 'HEAD') {
        serveStatic(req, res);
    } else {
        res.writeHead(405);
        res.end('Method not allowed');
    }
});

server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
        console.error(`Port ${PORT} is already in use. Close the program using it, or run with a different port, e.g.:  set PORT=3001`);
    } else {
        console.error('Web client server failed:', err.message);
    }
    process.exit(1);
});

server.listen(PORT, () => {
    const url = `http://localhost:${PORT}/`;
    console.log('Guess Market web client is running.');
    console.log(`  Open:            ${url}`);
    console.log(`  API forwarded to ${TARGET.origin}${API_PREFIX}api`);
    console.log('  Press Ctrl+C to stop.');
    if (process.argv.includes('--open')) {
        const opener = process.platform === 'win32' ? `start "" "${url}"`
            : process.platform === 'darwin' ? `open "${url}"` : `xdg-open "${url}"`;
        exec(opener, () => {});
    }
});
