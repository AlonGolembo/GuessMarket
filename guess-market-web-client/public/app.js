/*
 * Guess Market web client - screens and behaviour.
 *
 * Mirrors the JavaFX client (guess-market-client):
 *   - Login screen          ~ GuessMarketApp.promptForLogin
 *   - Events tab            ~ EventsController + events-tab.fxml
 *   - Users tab             ~ UsersController + users-tab.fxml (without file upload)
 *   - Permission rules      ~ TradeRules (the server still has the final say)
 *   - Pull-based refresh    ~ HttpMarketEngine's 500 ms poll loop
 *
 * Plain JavaScript, no framework and no build step. Each browser tab holds one
 * logged-in identity (sessionStorage), so opening a second tab lets you log in
 * as another user side by side.
 */
'use strict';

// =============================================================================
// Constants & helpers
// =============================================================================
const POLL_INTERVAL_MS = 500;              // same as the JavaFX client's HttpMarketEngine
const SESSION_KEY = 'guessMarket.userName';
const THEME_KEY = 'guessMarket.theme';

const STATUS_DISPLAY = { NOT_ACTIVE: 'Not Active', ACTIVE: 'Active', CLOSED: 'Closed' };
const COMMISSION_DISPLAY = { ON_PURCHASE: 'On Purchase', ON_CLOSE: 'On Close' };

const $ = (id) => document.getElementById(id);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g,
    (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const NUM2 = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const money = (v) => (v === null || v === undefined || Number.isNaN(v)) ? '-' : (v < 0 ? '-$' : '$') + NUM2.format(Math.abs(v));
const signedMoney = (v) => (v < 0 ? '-$' : '+$') + NUM2.format(Math.abs(v));

const storage = {
    get(store, key) { try { return store.getItem(key); } catch (e) { return null; } },
    set(store, key, value) { try { store.setItem(key, value); } catch (e) { /* storage unavailable */ } },
    remove(store, key) { try { store.removeItem(key); } catch (e) { /* storage unavailable */ } },
};

// =============================================================================
// State
// =============================================================================
const state = {
    me: null,                    // logged-in user name (this tab's identity)
    users: {},                   // name -> UserDTO
    events: [],                  // [EventDTO]
    selectedEventName: null,
    eventDetails: null,          // EventDetailsDTO of the selected event
    myDetails: null,             // UserDetailsDTO of me (fetched while the Users tab shows me)
    sort: { key: null, dir: 1 },
    selectedOrderId: null,
    selectedUserName: null,      // Users-tab picker
    selectedMyEventName: null,   // Users-tab "Events Participation" selection
    activeTab: 'eventsTab',
    wasBlocked: false,
    online: true,
    rendered: {},                // section -> last rendered JSON, to skip identical redraws
};

// =============================================================================
// Permission rules - a direct port of the JavaFX client's TradeRules
// =============================================================================
const Rules = {
    isMarketMaker: (user, event) => !!(user && event && (user.marketMakerEventNames || []).includes(event.name)),
    isBlocked: (user) => !!(user && user.blocked),
    canActivate: (user, event) => !Rules.isBlocked(user) && Rules.isMarketMaker(user, event) && event.status === 'NOT_ACTIVE',
    canTrade: (user, event) => !!(user && event) && !Rules.isBlocked(user) && event.status === 'ACTIVE' && !Rules.isMarketMaker(user, event),
    canEnd: (user, event) => !Rules.isBlocked(user) && Rules.isMarketMaker(user, event) && event.status === 'ACTIVE',
    payTimingLabel: (type) => type === 'ON_CLOSE' ? '(Pay later)' : type === 'ON_PURCHASE' ? '(Pay now)' : '',
};

const myUser = () => state.users[state.me] || null;
const selectedEvent = () => state.events.find((e) => e.name === state.selectedEventName) || null;
const marketMakerOf = (eventName) =>
    Object.values(state.users).find((u) => (u.marketMakerEventNames || []).includes(eventName))?.name || null;

/** Runs {@code render} only when {@code value} differs from what was last drawn under {@code key}. */
function renderIfChanged(key, value, render) {
    const json = JSON.stringify(value);
    if (state.rendered[key] === json) return;
    state.rendered[key] = json;
    render();
}

function fillTable(tableId, rowsHtml, emptyText, colspan) {
    $(tableId).tBodies[0].innerHTML = rowsHtml.length
        ? rowsHtml.join('')
        : `<tr><td class="empty" colspan="${colspan}">${esc(emptyText)}</td></tr>`;
}

/** Keeps a <select>'s options in sync without disturbing the user's current choice. */
function syncSelectOptions(select, values, placeholder) {
    const key = JSON.stringify([placeholder, values]);
    if (select.dataset.key === key) return false;
    const previous = select.value;
    select.dataset.key = key;
    select.innerHTML = (placeholder !== undefined ? `<option value="">${esc(placeholder)}</option>` : '')
        + values.map((v) => `<option value="${esc(v)}">${esc(v)}</option>`).join('');
    if (values.includes(previous)) select.value = previous;
    return true;
}

// =============================================================================
// Dialogs & toast (counterpart of the JavaFX Dialogs helper)
// =============================================================================
let modalChain = Promise.resolve();

function showModal({ title, message, input = false, defaultValue = '', okText = 'OK', cancel = false }) {
    const run = () => new Promise((resolve) => {
        const overlay = $('modalOverlay');
        const inputEl = $('modalInput');
        $('modalTitle').textContent = title;
        $('modalMessage').textContent = message || '';
        $('modalOk').textContent = okText;
        $('modalCancel').hidden = !(cancel || input);
        inputEl.hidden = !input;
        inputEl.value = defaultValue;
        overlay.hidden = false;
        (input ? inputEl : $('modalOk')).focus();

        const finish = (result) => {
            overlay.hidden = true;
            $('modalOk').onclick = $('modalCancel').onclick = null;
            document.removeEventListener('keydown', onKey, true);
            resolve(result);
        };
        const onKey = (e) => {
            if (e.key === 'Escape') { e.preventDefault(); finish(null); }
            if (e.key === 'Enter') { e.preventDefault(); finish(input ? inputEl.value : true); }
        };
        $('modalOk').onclick = () => finish(input ? inputEl.value : true);
        $('modalCancel').onclick = () => finish(null);
        document.addEventListener('keydown', onKey, true);
    });
    const result = modalChain.then(run);
    modalChain = result.catch(() => {});
    return result;
}

const showError = (title, err) => showModal({ title, message: err instanceof Error ? err.message : String(err) });
const confirmDialog = (title, message, okText) => showModal({ title, message, okText, cancel: true });
const promptDialog = (title, message) => showModal({ title, message, input: true });

let toastTimer = null;
function toast(message) {
    const el = $('toast');
    el.textContent = message;
    el.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.hidden = true; }, 2500);
}

// =============================================================================
// Theme
// =============================================================================
function applyTheme(theme) {
    document.documentElement.dataset.theme = theme;
    storage.set(localStorage, THEME_KEY, theme);
}

// =============================================================================
// Login / logout
// =============================================================================
function showLogin(message) {
    stopPolling();
    state.me = null;
    $('mainScreen').hidden = true;
    $('loginScreen').hidden = false;
    $('loginError').textContent = message || '';
    $('loginName').focus();
}

async function handleLogin(e) {
    e.preventDefault();
    const name = $('loginName').value.trim();
    if (!name) {
        $('loginError').textContent = 'A user name is required.';
        return;
    }
    $('loginButton').disabled = true;
    try {
        await Api.login(name);
        enterMain(name);
    } catch (err) {
        // e.g. "A user named 'x' is already logged in." - let the user pick another name.
        $('loginError').textContent = err.message;
        $('loginName').select();
    } finally {
        $('loginButton').disabled = false;
    }
}

function enterMain(name) {
    state.me = name;
    state.rendered = {};
    state.selectedEventName = null;
    state.selectedUserName = name;
    state.selectedMyEventName = null;
    state.wasBlocked = false;
    storage.set(sessionStorage, SESSION_KEY, name);
    document.title = `Guess Market - ${name}`;
    $('headerUser').textContent = name;
    $('loginScreen').hidden = true;
    $('mainScreen').hidden = false;
    startPolling();
}

async function handleLogout() {
    const ok = await confirmDialog('Log out',
        `Log out of "${state.me}"?\n\nThe server keeps every user for as long as it runs (it has no logout ` +
        'endpoint), so this name stays taken - log in again with a different name.', 'Log out');
    if (!ok) return;
    storage.remove(sessionStorage, SESSION_KEY);
    $('loginName').value = '';
    showLogin();
}

// =============================================================================
// Polling (pull-based refresh, like the JavaFX client)
// =============================================================================
let pollTimer = null;
let polling = false;
let pollAgain = false;
let pollGeneration = 0;

function startPolling() {
    pollGeneration++;
    refreshNow();
}

function stopPolling() {
    pollGeneration++;
    clearTimeout(pollTimer);
    pollTimer = null;
}

/** Runs a poll tick right away (e.g. after an action), coalescing with one in flight. */
function refreshNow() {
    if (polling) {
        pollAgain = true;
        return;
    }
    clearTimeout(pollTimer);
    tick();
}

async function tick() {
    const generation = pollGeneration;
    polling = true;
    try {
        const [events, users] = await Promise.all([Api.getAllEvents(), Api.getAllUsers()]);
        if (generation !== pollGeneration) return;
        state.events = events || [];
        state.users = users || {};

        if (!state.users[state.me]) {
            // The server forgot us - it was restarted, and it keeps nothing between runs.
            const name = state.me;
            storage.remove(sessionStorage, SESSION_KEY);
            showLogin(`The server no longer knows "${name}" (it was probably restarted). Please log in again.`);
            return;
        }

        if (state.selectedEventName && !selectedEvent()) state.selectedEventName = null;
        state.eventDetails = state.selectedEventName ? await Api.getEventDetails(state.selectedEventName) : null;

        const needMyDetails = state.activeTab === 'usersTab' && state.selectedUserName === state.me;
        state.myDetails = needMyDetails ? await Api.getUserDetails(state.me) : state.myDetails;
        if (generation !== pollGeneration) return;

        setOnline(true);
        renderAll();
    } catch (err) {
        if (generation === pollGeneration) setOnline(false, err.message);
    } finally {
        polling = false;
        if (generation === pollGeneration) {
            if (pollAgain) {
                pollAgain = false;
                tick();
            } else {
                pollTimer = setTimeout(tick, POLL_INTERVAL_MS);
            }
        }
    }
}

function setOnline(online, message) {
    state.online = online;
    const badge = $('connectionStatus');
    badge.classList.toggle('offline', !online);
    badge.textContent = online ? '● Live' : '● Offline';
    badge.title = online ? 'Connected - refreshing every 0.5 s' : (message || 'Server unreachable');
}

// =============================================================================
// Rendering - shared header
// =============================================================================
function renderAll() {
    renderHeader();
    renderEventsTab();
    renderUsersTab();
}

function renderHeader() {
    const me = myUser();
    $('headerBalance').textContent = money(me?.balance);
    $('headerBalance').className = me && me.balance < 0 ? 'negative' : '';
    $('headerBlocked').hidden = !Rules.isBlocked(me);

    const blockedNow = Rules.isBlocked(me);
    if (blockedNow && !state.wasBlocked) {
        showModal({
            title: 'You are blocked',
            message: 'Your balance was forced negative while a resting order was filled, ' +
                'and you can no longer perform any action.',
        });
    }
    state.wasBlocked = blockedNow;
}

// =============================================================================
// Rendering - Events tab
// =============================================================================
function filteredSortedEvents() {
    const method = $('methodFilter').value;
    const status = $('statusFilter').value;
    const commission = $('commissionFilter').value;
    const matches = (selected, value) => selected === 'All' || selected === value;
    const list = state.events.filter((e) => matches(method, e.tradingMethod)
        && matches(status, e.status) && matches(commission, e.commissionType));
    const { key, dir } = state.sort;
    if (key) {
        const display = (e) => key === 'status' ? STATUS_DISPLAY[e.status]
            : key === 'commissionType' ? COMMISSION_DISPLAY[e.commissionType] : String(e[key]);
        list.sort((a, b) => dir * display(a).localeCompare(display(b), undefined, { numeric: true, sensitivity: 'base' }));
    }
    return list;
}

function renderEventsTab() {
    const list = filteredSortedEvents();
    renderIfChanged('eventsTable', [list, state.selectedEventName, state.sort], () => {
        fillTable('eventsTable', list.map((e) => `
            <tr data-name="${esc(e.name)}" class="${e.name === state.selectedEventName ? 'selected' : ''}">
                <td>${esc(e.name)}</td>
                <td>${esc(STATUS_DISPLAY[e.status] || e.status)}</td>
                <td>${esc(e.tradingMethod)}</td>
                <td>${esc(COMMISSION_DISPLAY[e.commissionType] || e.commissionType)}</td>
            </tr>`), 'No events match the selected filters.', 4);
        $('eventsTable').tHead.querySelectorAll('th').forEach((th) => {
            th.classList.toggle('asc', th.dataset.sort === state.sort.key && state.sort.dir === 1);
            th.classList.toggle('desc', th.dataset.sort === state.sort.key && state.sort.dir === -1);
        });
    });
    $('eventsEmpty').hidden = state.events.length > 0;
    $('eventsTable').closest('.table-wrap').hidden = state.events.length === 0;

    const event = selectedEvent();
    const details = state.eventDetails;
    const hasSelection = !!(event && details);
    $('noEventSelected').hidden = hasSelection;
    $('eventDetailsContainer').hidden = !hasSelection;
    $('eventsBottom').hidden = !hasSelection;
    if (!hasSelection) {
        clearGraphs();
        return;
    }

    renderIfChanged('eventDetails', details, () => renderEventDetails(event, details));
    renderIfChanged('eventControls', [event, myUser(), state.users], () => renderEventControls(event, details));
    renderIfChanged('myOrders', [details.restingOrders, state.selectedOrderId], () => renderMyOrders(details));
}

function renderEventDetails(event, d) {
    const options = event.options || [];
    $('selectedEventTitle').textContent = event.name;

    // Closed-event summary: winner + total shares bought per option.
    const closed = event.status === 'CLOSED';
    $('closedSummary').hidden = !closed;
    if (closed) {
        const totals = options.map((o) => `${o} = ${(d.totalSharesBought || {})[o] || 0}`).join('   ');
        $('closedSummary').textContent =
            `Event closed  •  Winner: ${d.winningOption || '-'}  •  Total shares bought:   ${totals}`;
    }

    // Side-by-side order books (Order Book events only).
    const isOrderBook = event.tradingMethod === 'ORDERBOOK';
    $('orderBookSection').hidden = !isOrderBook;
    if (isOrderBook && options.length === 2) {
        options.forEach((option, i) => {
            const n = i + 1;
            $(`option${n}BookLabel`).textContent = `${option} Order Book`;
            const levels = (d.orderBookLevels || []).filter((l) => l.optionName === option);
            fillTable(`option${n}BookTable`, levels.map((l) => `
                <tr><td class="${l.side === 'BID' ? 'bid' : 'ask'}">${l.side === 'BID' ? 'Bid' : 'Ask'}</td>
                    <td>${money(l.price)}</td><td>${l.quantity}</td></tr>`), 'Empty book', 3);
            const q = (d.orderBookQuotes || {})[option] || {};
            $(`option${n}Indicators`).textContent =
                `Last: ${money(q.lastTrade)} | Bid: ${money(q.bestBid)} | Ask: ${money(q.bestAsk)} | Mid: ${money(q.mid)} | Spread: ${money(q.spread)}`;
        });
    }

    fillTable('participationsTable', (d.participantHoldings || []).map((h) => `
        <tr><td>${esc(h.userName)}${h.userName === state.me ? ' (you)' : ''}</td>
            <td>${esc(h.optionName)}</td><td>${h.shares}</td></tr>`), 'No participants yet.', 3);

    // Financials.
    $('eventBalance').textContent = money(d.eventAccountBalance);
    $('eventCommission').textContent = money(d.totalCommissionCollected);
    $('eventCommissionTerms').textContent =
        `${event.commissionPercentage}% ${COMMISSION_DISPLAY[event.commissionType] || ''}`;
    $('eventDescription').textContent = event.description || '-';

    fillTable('tradeHistoryTable', (d.tradeHistory || []).map((t) => `
        <tr><td>${esc(t.timestamp)}</td><td>${esc(t.buyerName)}</td><td>${esc(t.optionName)}</td>
            <td>${t.quantity}</td><td>${money(t.pricePaid)}</td></tr>`), 'No trades yet.', 5);

    // Trade-entry option pickers follow the event's options.
    syncSelectOptions($('tradeOption'), options);
    syncSelectOptions($('orderOption'), options);
    syncSelectOptions($('winningOptionSelect'), options, 'Choose Winning Option...');
    updateOptionPrice();
    requestQuote();

    renderGraphs(event, d);
}

function renderEventControls(event, d) {
    const me = myUser();
    const isMm = Rules.isMarketMaker(me, event);
    $('activateButton').disabled = !Rules.canActivate(me, event);
    $('endEventButton').disabled = !Rules.canEnd(me, event);
    $('winningOptionSelect').disabled = !Rules.canEnd(me, event);

    const mmName = marketMakerOf(event.name);
    $('mmHint').textContent = isMm
        ? (event.status === 'NOT_ACTIVE' ? 'You are this event\'s market maker - activate it to open trading.'
            : event.status === 'ACTIVE' ? 'You are this event\'s market maker - end it by choosing the winning option.'
                : 'You were this event\'s market maker. The event is closed.')
        : `Only the event's market maker${mmName ? ` (${mmName})` : ''} can activate or end it.`;

    const canTrade = Rules.canTrade(me, event);
    const reason = canTrade ? ''
        : Rules.isBlocked(me) ? 'You are blocked and cannot trade.'
            : isMm ? 'A market maker cannot trade in their own event.'
                : event.status === 'NOT_ACTIVE' ? 'Trading opens once the market maker activates the event.'
                    : 'This event is closed.';

    const isLmsr = event.tradingMethod === 'LMSR';
    $('buySharesSection').hidden = !isLmsr;
    $('buySharesFieldset').disabled = !canTrade;
    $('tradeDisabledHint').textContent = reason;
    $('payNowOrLater').textContent = Rules.payTimingLabel(event.commissionType);

    $('orderEntrySection').hidden = event.tradingMethod !== 'ORDERBOOK';
    $('orderEntryFieldset').disabled = !canTrade;
    $('orderDisabledHint').textContent = reason;
}

function renderMyOrders(d) {
    const mine = (d.restingOrders || []).filter((o) => o.userName === state.me);
    if (!mine.some((o) => o.id === state.selectedOrderId)) state.selectedOrderId = null;
    fillTable('myOrdersTable', mine.map((o) => `
        <tr data-id="${o.id}" class="${o.id === state.selectedOrderId ? 'selected' : ''}">
            <td>${esc(o.optionName)}</td><td class="${o.side === 'BID' ? 'bid' : 'ask'}">${o.side === 'BID' ? 'Bid' : 'Ask'}</td>
            <td>${money(o.price)}</td><td>${o.remaining}</td></tr>`), 'You have no open orders here.', 4);
    $('cancelOrderButton').disabled = state.selectedOrderId === null;
}

function updateOptionPrice() {
    const d = state.eventDetails;
    const price = d && (d.currentOptionPrices || {})[$('tradeOption').value];
    $('optionPrice').textContent = price === undefined || price === null
        ? '$0.00' : `${money(price)}  (${(price * 100).toFixed(1)}%)`;
}

// ---- LMSR quote: shown numbers come straight from the server's /trading/quote ----
let quoteSeq = 0;
let quoteTimer = null;

function requestQuote() {
    clearTimeout(quoteTimer);
    quoteTimer = setTimeout(fetchQuote, 120);
}

async function fetchQuote() {
    const seq = ++quoteSeq;
    const event = selectedEvent();
    const options = event?.options || [];
    const optionIndex = options.indexOf($('tradeOption').value) + 1;
    const quantity = parseInt($('sharesCount').value, 10);
    let quote = null;
    if (event && event.tradingMethod === 'LMSR' && Rules.canTrade(myUser(), event) && optionIndex > 0 && quantity > 0) {
        try {
            quote = await Api.quoteTrade(state.me, event.name, optionIndex, quantity);
        } catch (notQuotable) {
            quote = null;   // e.g. event not active - same as the JavaFX client, show $0.00
        }
    }
    if (seq !== quoteSeq) return;   // a newer quote request superseded this one
    $('commissionToPay').textContent = money(quote ? quote.commission : 0);
    $('totalPrice').textContent = money(quote ? quote.total : 0);
}

// ---- Graphs: one price-over-time chart per option ----
function renderGraphs(event, d) {
    const options = event.options || [];
    if (options.length < 2) {
        clearGraphs();
        return;
    }
    [['leftGraph', 'leftGraphLabel'], ['rightGraph', 'rightGraphLabel']].forEach(([host, label], i) => {
        const option = options[i];
        $(label).textContent = `${option} Price`;
        const trades = (d.tradeHistory || []).filter((t) => t.optionName === option).slice().reverse();  // oldest first
        Charts.line($(host), trades.map((t) => ({
            label: `${t.timestamp} - ${t.buyerName} x${t.quantity}`,
            value: t.quantity > 0 ? t.pricePaid / t.quantity : t.pricePaid,
        })), { xLabel: 'Trade #', yLabel: 'Price per share ($)', format: money, emptyText: 'No trades for this option yet' });
    });
}

function clearGraphs() {
    $('leftGraph').replaceChildren();
    $('rightGraph').replaceChildren();
    $('leftGraphLabel').textContent = 'Option 1 Price';
    $('rightGraphLabel').textContent = 'Option 2 Price';
}

// =============================================================================
// Events tab - actions
// =============================================================================
function selectEvent(name) {
    if (state.selectedEventName === name) return;
    state.selectedEventName = name;
    state.selectedOrderId = null;
    state.eventDetails = null;
    for (const key of ['eventDetails', 'eventControls', 'myOrders']) delete state.rendered[key];
    $('orderEntryStatus').textContent = '';
    $('sharesCount').value = 1;
    $('orderQuantity').value = 1;
    $('orderPrice').value = '';
    for (const id of ['tradeOption', 'orderOption', 'winningOptionSelect']) delete $(id).dataset.key;
    renderEventsTab();
    refreshNow();
}

function readPositiveInt(input, what) {
    const value = Number(input.value);
    if (!Number.isInteger(value) || value < 1) {
        showError('Invalid quantity', `${what} must be a whole number of at least 1.`);
        return null;
    }
    return value;
}

async function runAction(button, errorTitle, action) {
    button.disabled = true;
    try {
        await action();
    } catch (err) {
        showError(errorTitle, err);
    } finally {
        delete state.rendered.eventControls;   // re-evaluate enabled/disabled state
        delete state.rendered.myOrders;
        button.disabled = false;
        refreshNow();
    }
}

function handleExecuteTrade() {
    const event = selectedEvent();
    const optionIndex = (event?.options || []).indexOf($('tradeOption').value) + 1;
    if (!event || optionIndex < 1) {
        showError('Invalid Selection', 'Please select an active event, an option, and a valid quantity of shares.');
        return;
    }
    const quantity = readPositiveInt($('sharesCount'), 'Shares quantity');
    if (quantity === null) return;
    runAction($('executeTradeButton'), 'Trade Execution Failed', async () => {
        const result = await Api.buyShares(state.me, event.name, optionIndex, quantity);
        $('sharesCount').value = 1;
        toast(`Bought ${result.filledQuantity} × ${$('tradeOption').value} for ${money(result.totalPaid)}`);
    });
}

function handleActivate() {
    const event = selectedEvent();
    if (!event) return;
    runAction($('activateButton'), 'Could not activate event', async () => {
        await Api.activateEvent(event.name, state.me);
        toast(`"${event.name}" is now active.`);
    });
}

async function handleEndEvent() {
    const event = selectedEvent();
    if (!event) return;
    const winner = $('winningOptionSelect').value;
    const winnerIndex = (event.options || []).indexOf(winner) + 1;
    if (winnerIndex < 1) {
        showError('Choose a winner', 'Choose the winning option before ending the event.');
        return;
    }
    const ok = await confirmDialog('End Event',
        `End "${event.name}" with "${winner}" as the winning option?\nHolders of "${winner}" will be paid out. This cannot be undone.`,
        'End Event');
    if (!ok) return;
    runAction($('endEventButton'), 'Could not close event', async () => {
        await Api.closeEvent(event.name, winnerIndex);
        toast(`"${event.name}" closed. Winner: ${winner}`);
    });
}

function handlePlaceOrder() {
    const event = selectedEvent();
    const optionIndex = (event?.options || []).indexOf($('orderOption').value) + 1;
    if (!event || optionIndex < 1) {
        showError('Invalid Selection', 'Select an active Order Book event, an option, and a valid quantity.');
        return;
    }
    const quantity = readPositiveInt($('orderQuantity'), 'Quantity');
    if (quantity === null) return;
    const priceText = $('orderPrice').value.trim();
    const price = Number(priceText);
    if (priceText === '' || !Number.isFinite(price)) {
        showError('Invalid Price', 'Enter a numeric price.');
        return;
    }
    const side = document.querySelector('input[name="orderSide"]:checked').value;
    runAction($('placeOrderButton'), 'Order Failed', async () => {
        const result = await Api.placeOrder(state.me, event.name, optionIndex, side, quantity, price);
        $('orderEntryStatus').textContent = `Filled ${result.filledQuantity}, resting ${result.restingQuantity}.`;
    });
}

function handleCancelOrder() {
    const event = selectedEvent();
    if (!event || state.selectedOrderId === null) {
        showError('Invalid Selection', 'Select one of your open orders to cancel.');
        return;
    }
    const orderId = state.selectedOrderId;
    runAction($('cancelOrderButton'), 'Cancel Failed', async () => {
        await Api.cancelOrder(state.me, event.name, orderId);
        state.selectedOrderId = null;
        toast('Order cancelled.');
    });
}

// =============================================================================
// Rendering - Users tab
// =============================================================================
function renderUsersTab() {
    const names = Object.keys(state.users);
    const select = $('usersSelect');
    // Like the JavaFX client, never rebuild the picker while the user is using it.
    if (document.activeElement !== select) {
        syncSelectOptions(select, names, 'Select a user...');
        select.value = names.includes(state.selectedUserName) ? state.selectedUserName : '';
    }

    const selected = state.users[state.selectedUserName] || null;
    const isSelf = !!selected && selected.name === state.me;

    renderIfChanged('userInfo', selected, () => {
        $('noUserSelected').hidden = !!selected;
        $('userInfo').hidden = !selected;
        if (selected) {
            $('userDetailsHeader').textContent = `User Details: ${selected.name}`;
            $('selectedUserName').textContent = selected.name + (isSelf ? ' (you)' : '');
            $('selectedUserBalance').textContent = money(selected.balance);
            $('selectedUserBalance').className = selected.balance < 0 ? 'negative' : 'positive';
            $('selectedUserIsMm').textContent = (selected.marketMakerEventNames || []).length ? 'Yes' : 'No';
        }
    });

    $('selfOnlyPanel').hidden = !isSelf;
    if (isSelf) renderMyAccount(selected);

    // Everyone else at a glance: name, balance, market-maker flag (all a user may see about others).
    renderIfChanged('allUsers', [state.users, state.me], () => {
        $('otherUsersPanel').hidden = names.length === 0;
        fillTable('allUsersTable', Object.values(state.users).map((u) => `
            <tr><td>${esc(u.name)}${u.name === state.me ? ' (you)' : ''}</td>
                <td class="${u.balance < 0 ? 'negative' : ''}">${money(u.balance)}</td>
                <td>${(u.marketMakerEventNames || []).length ? 'Yes' : 'No'}</td></tr>`), 'No users.', 3);
    });

    const showMine = isSelf && !!state.myDetails;
    $('graphPlaceholder').hidden = showMine;
    $('balanceGraph').hidden = !showMine;
    $('ledgerPlaceholder').hidden = showMine;
    $('ledgerWrap').hidden = !showMine;
    if (showMine) {
        const history = state.myDetails.balanceHistory || [];
        renderIfChanged('myLedger', history, () => {
            Charts.line($('balanceGraph'), history.map((h) => ({
                label: `${h.timestamp} - ${h.type}${h.eventName ? ` (${h.eventName})` : ''}`,
                value: h.balanceAfter,
            })), { xLabel: 'Ledger entry #', yLabel: 'Balance ($)', format: money, emptyText: 'No balance history yet' });
            fillTable('ledgerTable', history.slice().reverse().map((h) => `
                <tr><td>${esc(h.timestamp)}</td><td>${esc(h.type)}</td><td>${esc(h.eventName || '-')}</td>
                    <td class="${h.delta < 0 ? 'negative' : 'positive'}">${signedMoney(h.delta)}</td>
                    <td>${money(h.balanceAfter)}</td></tr>`), 'No ledger entries yet.', 5);
        });
    }
}

function renderMyAccount(me) {
    $('userBlocked').hidden = !me.blocked;
    $('depositButton').disabled = false;

    // Events I hold shares in, plus events I own as market maker.
    const mine = new Set([...Object.keys(me.holdings || {}), ...(me.marketMakerEventNames || [])]);
    const myEvents = state.events.filter((e) => mine.has(e.name));
    if (!myEvents.some((e) => e.name === state.selectedMyEventName)) state.selectedMyEventName = null;

    renderIfChanged('myEvents', [myEvents, me, state.selectedMyEventName], () => {
        fillTable('userEventsTable', myEvents.map((e) => {
            const role = (me.marketMakerEventNames || []).includes(e.name) ? 'Market Maker' : 'Participant';
            const shares = Object.values((me.holdings || {})[e.name] || {}).reduce((a, b) => a + b, 0);
            return `<tr data-name="${esc(e.name)}" class="${e.name === state.selectedMyEventName ? 'selected' : ''}">
                <td>${esc(e.name)}</td><td>${role}</td><td>${shares}</td></tr>`;
        }), 'You are not participating in any event yet.', 3);
    });
    renderIfChanged('myInvolvement', [state.selectedMyEventName, myEvents, me, state.myDetails], () => renderInvolvement(me));
}

/** My shares per option in the selected event, plus winner and my net result once it is closed. */
async function renderInvolvement(me) {
    const label = $('userEventInvolvement');
    const event = state.events.find((e) => e.name === state.selectedMyEventName);
    if (!event) {
        label.textContent = 'Select one of your events above to see your involvement in it.';
        return;
    }
    const held = (me.holdings || {})[event.name] || {};
    let text = `${event.name}  •  ${STATUS_DISPLAY[event.status] || event.status}`;
    if (Object.keys(held).length) {
        text += '  •  Your shares:' + Object.entries(held).map(([o, q]) => `  ${o} = ${q}`).join('');
    }
    if (event.status === 'CLOSED') {
        try {
            const details = await Api.getEventDetails(event.name);
            const net = (state.myDetails?.balanceHistory || [])
                .filter((h) => h.eventName === event.name)
                .reduce((sum, h) => sum + h.delta, 0);
            text += `\nWinner: ${details.winningOption || '-'}  •  Your net result on this event: ${signedMoney(net)}`;
        } catch (err) {
            text += `\n(Could not load the result: ${err.message})`;
        }
    }
    if (state.selectedMyEventName === event.name) label.textContent = text;
}

async function handleDeposit() {
    const text = await promptDialog('Deposit Funds', 'Amount to add to your account');
    if (text === null) return;
    const amount = Number(String(text).trim());
    if (!Number.isFinite(amount) || String(text).trim() === '') {
        showError('Invalid amount', 'Enter a positive number.');
        return;
    }
    if (amount <= 0) {
        showError('Invalid amount', 'Amount must be positive.');
        return;
    }
    const button = $('depositButton');
    button.disabled = true;
    try {
        await Api.deposit(state.me, amount);
        toast(`Deposited ${money(amount)}.`);
    } catch (err) {
        showError('Deposit Failed', err);
    } finally {
        button.disabled = false;
        refreshNow();
    }
}

// =============================================================================
// Tabs
// =============================================================================
function showTab(tabId) {
    state.activeTab = tabId;
    document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === tabId));
    document.querySelectorAll('.tab-page').forEach((p) => { p.hidden = p.id !== tabId; });
    refreshNow();
}

function showSubtab(button) {
    const card = button.closest('.card');
    card.querySelectorAll('.subtab').forEach((b) => b.classList.toggle('active', b === button));
    card.querySelectorAll('.subtab-page').forEach((p) => { p.hidden = p.id !== button.dataset.subtab; });
}

// =============================================================================
// Wiring
// =============================================================================
function wire() {
    $('loginForm').addEventListener('submit', handleLogin);
    $('logoutButton').addEventListener('click', handleLogout);
    $('themeButton').addEventListener('click', () =>
        applyTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark'));

    document.querySelectorAll('.tab').forEach((t) => t.addEventListener('click', () => showTab(t.dataset.tab)));
    document.querySelectorAll('.subtab').forEach((b) => b.addEventListener('click', () => showSubtab(b)));

    // Events tab
    for (const id of ['methodFilter', 'statusFilter', 'commissionFilter']) {
        $(id).addEventListener('change', () => renderEventsTab());
    }
    $('eventsTable').tHead.addEventListener('click', (e) => {
        const th = e.target.closest('th[data-sort]');
        if (!th) return;
        state.sort = state.sort.key === th.dataset.sort
            ? { key: th.dataset.sort, dir: -state.sort.dir }
            : { key: th.dataset.sort, dir: 1 };
        renderEventsTab();
    });
    $('eventsTable').tBodies[0].addEventListener('click', (e) => {
        const row = e.target.closest('tr[data-name]');
        if (row) selectEvent(row.dataset.name);
    });
    $('tradeOption').addEventListener('change', () => { updateOptionPrice(); requestQuote(); });
    $('sharesCount').addEventListener('input', requestQuote);
    $('executeTradeButton').addEventListener('click', handleExecuteTrade);
    $('activateButton').addEventListener('click', handleActivate);
    $('endEventButton').addEventListener('click', handleEndEvent);
    $('placeOrderButton').addEventListener('click', handlePlaceOrder);
    $('cancelOrderButton').addEventListener('click', handleCancelOrder);
    $('myOrdersTable').tBodies[0].addEventListener('click', (e) => {
        const row = e.target.closest('tr[data-id]');
        if (!row || $('orderEntryFieldset').disabled) return;
        state.selectedOrderId = Number(row.dataset.id);
        delete state.rendered.myOrders;
        renderEventsTab();
    });

    // Users tab
    $('usersSelect').addEventListener('change', () => {
        state.selectedUserName = $('usersSelect').value || null;
        state.myDetails = null;
        renderUsersTab();
        refreshNow();
    });
    $('userEventsTable').tBodies[0].addEventListener('click', (e) => {
        const row = e.target.closest('tr[data-name]');
        if (!row) return;
        state.selectedMyEventName = row.dataset.name;
        renderUsersTab();
    });
    $('depositButton').addEventListener('click', handleDeposit);
}

async function init() {
    const savedTheme = storage.get(localStorage, THEME_KEY);
    applyTheme(savedTheme || (window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'));
    wire();

    // A page refresh keeps this tab's identity, as long as the server still knows it.
    const saved = storage.get(sessionStorage, SESSION_KEY);
    if (!saved) {
        showLogin();
        return;
    }
    try {
        const users = await Api.getAllUsers();
        if (users && users[saved]) {
            enterMain(saved);
            return;
        }
        storage.remove(sessionStorage, SESSION_KEY);
        showLogin();
    } catch (err) {
        showLogin(err.message);
    }
}

init();
