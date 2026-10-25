# Guess Market – Exercise 4 (Bonus): Web Client

A browser client for the existing Guess Market server (exercise 3). The work is
on the client side only. The web client calls **exactly the same HTTP/JSON
endpoints** as the JavaFX client, and the Tomcat server is set up and run
**exactly as before**, with no changes to the server or the WAR.

Project source (GitHub): https://github.com/AlonGolembo/GuessMarket

> Submitted by: Alon Golembo 207684085 (alon.golembo@gmail.com), Snir Bar Shimon 315370106 (Snir221195@gmail.com) 

---

## 1. How to run (for the grader)

**Prerequisites**
- The exercise 3 server is running as usual: Tomcat on `localhost:8080` with
  `guess-market-server.war` deployed (context path `/guess-market-server`).
- Node.js is installed. The web client uses only Node's built-in modules, so
  there is nothing to download or install.

**Start the web client**
1. Double-click **`run-web-client.bat`** in this folder.
   It checks that Node is available, runs `npm install` (a no-op, since there
   are no dependencies), starts the web client server, and opens the browser.
2. If the browser doesn't open by itself, browse to:

   ## ➜ http://localhost:3000/

3. Keep the console window open while using the client. Close it, or press
   Ctrl+C, to stop.

**Notes for testing**
- **Several users at once:** every browser *tab* is its own logged-in user.
  Open a second tab at http://localhost:3000/ and log in with another name.
  The web client also works side by side with JavaFX clients; changes made in
  any client show up in all the others within about half a second.
- **Events come from JavaFX clients:** as the exercise defines, the web client
  has no file upload. Upload event XML files from the JavaFX client (the
  uploader becomes the events' market maker). `samples/sample-events.xml`
  (one LMSR event and one Order Book event) is included for convenience.
- If port 3000 is busy, run `set PORT=3001` in a console and then
  `node server.js --open` in this folder.
- If Tomcat runs somewhere other than `localhost:8080`, set
  `GM_SERVER=http://host:port` before starting.

---

## 2. What the web client does

| Screen | Matches JavaFX | What's in it |
|---|---|---|
| **Login** | login dialog | Enter a unique name. If the name is already taken, the server's error is shown and you can try again. No password or sign-up. |
| **Events** | Events tab | Filters (method, status, commission type); sortable events list; for the selected event: closed-event summary (winner and total shares per option), the two order books side by side with Last/Bid/Ask/Mid/Spread indicators (Order Book events), participations, price-over-time graphs per option; Financials & Control (event balance, commissions collected, description, **Activate** and **End Event** for the market maker); purchase history; **Buy Shares** for LMSR events (live price/probability, quoted commission and total, pay now or later); **Place a Limit Order** for Order Book events (bid/ask, price, quantity, fill result) with **My Open Orders** and **Cancel**. |
| **Users** | Users tab (no upload) | Pick any user to see their name, balance and market-maker flag, plus a table of all users. For yourself you also get **Deposit Funds**, the blocked warning, the events you take part in or own (role and shares; click one to see your shares per option, and for a closed event the winner and your net result), the **balance graph**, and the full **account ledger** (one line per action, newest first). |

The header shows the logged-in user, a live balance, a "Blocked" badge when
relevant, and a Live/Offline connection indicator. There's also a light/dark
theme toggle (the JavaFX client has themes too) and Log out.

**Permission rules** are the same as the JavaFX client's `TradeRules`. The
market maker can activate only a not-yet-active event, can end only an active
one, and can't trade in their own event. A blocked user can't do anything.
Controls that don't apply are disabled, with a one-line explanation. The
server still validates everything and its error messages are shown in a
dialog.

**Refresh** is pull-based, like the JavaFX client: every 0.5 s the client
fetches the events and users (plus the selected event's details, and your
ledger while you're viewing it). Each section is redrawn only when its data
actually changed, and a drop-down is never rebuilt while you're using it.

**Resize:** the layout is responsive. Panels sit side by side on wide windows
and stack on narrow ones, tables scroll inside their own frames, and charts
redraw at the new width, so nothing is clipped.

Like the JavaFX client, the exercise 3 chat bonus isn't part of the web client
(the exercise says bonuses aren't expected here).

---

## 3. Design choices

- **Plain HTML, CSS and JavaScript, with no framework and no build step.** The
  client is small, so React/Angular would add a build toolchain and an
  `npm install` from the internet without real benefit. With plain JS the
  grader needs nothing beyond Node, and the code reads in the same order as
  the JavaFX controllers it mirrors.
- **A tiny Node server (`server.js`) serves the pages and forwards the API.**
  The pages come from `localhost:3000`, but the API is on Tomcat at
  `localhost:8080`. The server doesn't send CORS headers, and the exercise
  forbids changing how the server is set up, so the browser would block
  direct cross-origin calls. `server.js` forwards every request under
  `/guess-market-server/` to Tomcat unchanged, so to the browser the API looks
  same-origin. It uses only Node's built-in `http`, `fs` and `path` modules.
- **Identity:** the server identifies users by the name sent in each request,
  not by cookies or sessions. The web client keeps the logged-in name in the
  tab's `sessionStorage`, so a page refresh keeps you logged in, and each tab
  can be a different user. If the server restarts (it keeps no data), the
  client notices that it no longer knows your name and returns to the login
  screen.
- **Log out** only clears the browser's session. The server has no logout
  endpoint and must not be changed, so the name stays taken while Tomcat runs.
  The client warns about this before logging out.
- **Prices and quotes come from the server.** The LMSR commission and total
  shown before buying come from the same `/trading/quote` endpoint the JavaFX
  client uses, so they match what `/trading/buy` charges. The client does no
  pricing math of its own.
- **Charts** are simple inline SVG (`charts.js`) instead of a charting
  library, so nothing is loaded from the internet and the client also works
  offline.

## 4. Files and main parts

```
run-web-client.bat     starts the client (checks for Node, runs server.js --open)
server.js              static file server + forwarding of /guess-market-server/* to Tomcat
package.json           "npm start" = node server.js --open (no dependencies)
public/index.html      all screens: login, Events tab, Users tab, dialogs
public/styles.css      layout (responsive grids), light/dark themes
public/api.js          Api: one function per server endpoint (same endpoints as HttpMarketEngine)
public/app.js          state, 0.5 s polling loop, rendering, actions, permission rules
public/charts.js       Charts.line: SVG line chart (price history / balance history)
samples/               a sample XML to upload from the JavaFX client
```

Inside `app.js`:

| Part | Role (JavaFX counterpart) |
|---|---|
| `Rules` | Port of `TradeRules`: `canActivate`, `canTrade`, `canEnd`, `isBlocked`. |
| `tick` / `refreshNow` | The poll loop (`HttpMarketEngine.startPolling`). Fetches events and users, then the selected event's details and your own ledger when needed. An action triggers an immediate refresh. |
| `renderEventsTab`, `renderEventDetails`, `renderEventControls`, `renderMyOrders` | `EventsController`: the list, details, order books, financials, trade entry. |
| `handleExecuteTrade`, `handleActivate`, `handleEndEvent`, `handlePlaceOrder`, `handleCancelOrder` | The Events tab's action handlers. |
| `renderUsersTab`, `renderMyAccount`, `renderInvolvement`, `handleDeposit` | `UsersController`. |
| `showModal` / `toast` | `Dialogs`: error, confirm and prompt dialogs, plus short success messages. |
| `renderIfChanged` | Redraws a section only when its data changed, which keeps polling flicker-free. |

---

## 5. Working with AI tools (section 4 of the exercise)

> Note to self before submitting: the answers below describe what happened.
> Fill in or adjust every **[TODO]** with your own experience.

**Which tool did you choose, and why? Did you try others?**


I used **Claude Code**, Anthropic's coding agent, which works directly inside
the project folder. I chose it because it can read the whole existing
codebase (the server servlets, the DTOs and the JavaFX controllers) and work
out from them which endpoints to call and how each screen should behave,
instead of me copy-pasting code into a chat window. It also built and ran the
server, ran the web client, and tested it in a headless browser by itself.
[TODO: did you also try GitHub Copilot or ChatGPT? What did you think of them?]

**Were there places where the AI didn't deliver and you had to step in as the programmer?**


The AI handled the code itself. The places that needed human decisions and
checks were:
- **Environment:** the machine had only Java 8 and no Maven or Node. The tool
  found the JDK 25 and Maven bundled with IntelliJ. When Maven couldn't verify
  TLS because antivirus software scans HTTPS traffic, it switched Java to the
  Windows certificate store. Downloading Tomcat and a portable Node for
  testing needed my approval.
- **Architecture:** the AI pointed out that the browser would block direct
  calls to Tomcat (no CORS headers), and that the exercise doesn't let us
  change the server. I approved its suggestion of a small Node proxy instead.
- **Visual polish:** after looking at screenshots, it fixed a few issues
  itself: stretched radio buttons, unreadable colored text on selected rows,
  chart text that shrank in narrow panels, and a stale price quote on a
  closed event.
[TODO: add anything you changed or checked yourself]

**Was there a bug or requirement the AI simply couldn't solve?**


No. Every requirement (login, events screen, user screen, live refresh,
resizing) was implemented and tested end to end. The only things it can't do
without the server changing are a real logout (the server has no such
endpoint) and freeing a taken user name.
[TODO: confirm, or describe anything you ran into]

**How long did it take? How long would it have taken without AI?**
Without AI: [TODO: your estimate, e.g. "2–3 weeks, since I'd first need
to learn HTML/CSS/JS, the fetch API and responsive layout"].

**Did you know frontend development before? Did it help?**

Absolutely, it helped my to understand better the underlying considerations and constraints in building such as system.

**Compared to the rest of the course's exercises, was this AI-driven style fun?**

Yes, comparing to other assignments it was very practical and fun.
 
