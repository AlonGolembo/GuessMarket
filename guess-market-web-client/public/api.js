/*
 * Thin wrapper over the Guess Market server's HTTP/JSON API - the very same
 * endpoints the JavaFX client's HttpMarketEngine calls. The page is served by
 * server.js, which forwards /guess-market-server/* to Tomcat, so these are
 * same-origin requests from the browser's point of view.
 *
 * Every call resolves with the parsed JSON body (or null for 204), and rejects
 * with an Error carrying the server's {"error": "..."} message on 4xx/5xx.
 */
'use strict';

const Api = (() => {
    const BASE = '/guess-market-server/api';

    async function request(method, path, body) {
        let response;
        try {
            response = await fetch(BASE + path, {
                method,
                headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
                body: body === undefined ? undefined : JSON.stringify(body),
                cache: 'no-store',
            });
        } catch (networkError) {
            const err = new Error('Cannot reach the web client server. Is run-web-client.bat still running?');
            err.network = true;
            throw err;
        }
        if (response.status === 204) {
            return null;
        }
        const text = await response.text();
        let data = null;
        if (text) {
            try {
                data = JSON.parse(text);
            } catch (ignored) {
                data = null;
            }
        }
        if (!response.ok) {
            const err = new Error((data && data.error) || `Server returned HTTP ${response.status}.`);
            err.status = response.status;
            err.network = response.status === 502 || response.status === 404 && !data;
            throw err;
        }
        return data;
    }

    const q = encodeURIComponent;

    return {
        // ----- users -----
        login: (name) => request('POST', '/users/login', { name }),
        getAllUsers: () => request('GET', '/users'),                       // { name: UserDTO }
        getUserDetails: (name) => request('GET', `/users/details?name=${q(name)}`),
        deposit: (userName, amount) => request('POST', '/users/deposit', { userName, amount }),

        // ----- events -----
        getAllEvents: () => request('GET', '/events'),                     // [EventDTO]
        getEventDetails: (name) => request('GET', `/events/details?name=${q(name)}`),
        activateEvent: (eventName, userName) => request('POST', '/events/activate', { eventName, userName }),
        closeEvent: (eventName, winningOptionIndex1Based) =>
            request('POST', '/events/close', { eventName, winningOptionIndex1Based }),

        // ----- trading -----
        quoteTrade: (userName, eventName, optionIndex1Based, quantity) =>
            request('POST', '/trading/quote', { userName, eventName, optionIndex1Based, quantity }),
        buyShares: (userName, eventName, optionIndex1Based, quantity) =>
            request('POST', '/trading/buy', { userName, eventName, optionIndex1Based, quantity }),
        placeOrder: (userName, eventName, optionIndex1Based, side, quantity, price) =>
            request('POST', '/trading/place-order', { userName, eventName, optionIndex1Based, side, quantity, price }),
        cancelOrder: (userName, eventName, orderId) =>
            request('POST', '/trading/cancel-order', { userName, eventName, orderId }),
    };
})();
