/*
 * Minimal dependency-free SVG line chart, the web counterpart of the JavaFX
 * client's Charts class: price-per-share over trades for an event option, and
 * balance-after over ledger entries for the logged-in user.
 */
'use strict';

const Charts = (() => {
    const H = 280;
    const PAD = { top: 16, right: 16, bottom: 44, left: 64 };
    const NS = 'http://www.w3.org/2000/svg';

    function el(name, attrs, text) {
        const node = document.createElementNS(NS, name);
        for (const [k, v] of Object.entries(attrs || {})) node.setAttribute(k, v);
        if (text !== undefined) node.textContent = text;
        return node;
    }

    function niceTicks(min, max, count) {
        if (min === max) {
            const pad = Math.abs(min) * 0.1 || 1;
            min -= pad;
            max += pad;
        }
        const rawStep = (max - min) / count;
        const mag = Math.pow(10, Math.floor(Math.log10(rawStep)));
        const step = [1, 2, 5, 10].map(m => m * mag).find(s => s >= rawStep) || rawStep;
        const lo = Math.floor(min / step) * step;
        const hi = Math.ceil(max / step) * step;
        const ticks = [];
        for (let v = lo; v <= hi + step / 2; v += step) ticks.push(+v.toFixed(10));
        return ticks;
    }

    // Charts are drawn at the container's real pixel width (so text never scales down) and
    // redrawn whenever that width changes - including when a hidden tab becomes visible.
    const resizeObserver = new ResizeObserver((entries) => {
        for (const entry of entries) {
            const host = entry.target;
            if (host.clientWidth > 0 && host.clientWidth !== host._chartWidth) draw(host);
        }
    });

    /**
     * Renders a line chart into {@code container}.
     * @param points  [{label: string, value: number}] oldest first
     * @param opts    {xLabel, yLabel, format(value) -> string, emptyText}
     */
    function line(container, points, opts) {
        container._chartArgs = { points, opts };
        resizeObserver.observe(container);
        draw(container);
    }

    function draw(container) {
        const { points, opts } = container._chartArgs;
        const W = container.clientWidth || 640;
        container._chartWidth = container.clientWidth;
        const svg = el('svg', { viewBox: `0 0 ${W} ${H}`, width: W, height: H, role: 'img' });
        const plotW = W - PAD.left - PAD.right;
        const plotH = H - PAD.top - PAD.bottom;

        svg.appendChild(el('line', { class: 'axis', x1: PAD.left, y1: PAD.top + plotH, x2: PAD.left + plotW, y2: PAD.top + plotH }));
        svg.appendChild(el('line', { class: 'axis', x1: PAD.left, y1: PAD.top, x2: PAD.left, y2: PAD.top + plotH }));
        svg.appendChild(el('text', { class: 'axis-label', x: PAD.left + plotW / 2, y: H - 6, 'text-anchor': 'middle' }, opts.xLabel));
        svg.appendChild(el('text', {
            class: 'axis-label', x: 14, y: PAD.top + plotH / 2, 'text-anchor': 'middle',
            transform: `rotate(-90 14 ${PAD.top + plotH / 2})`,
        }, opts.yLabel));

        if (!points.length) {
            svg.appendChild(el('text', { class: 'empty-msg', x: PAD.left + plotW / 2, y: PAD.top + plotH / 2, 'text-anchor': 'middle' },
                opts.emptyText || 'No data yet'));
            container.replaceChildren(svg);
            return;
        }

        const values = points.map(p => p.value);
        const ticks = niceTicks(Math.min(...values), Math.max(...values), 5);
        const yMin = ticks[0], yMax = ticks[ticks.length - 1];
        const y = v => PAD.top + plotH - ((v - yMin) / (yMax - yMin)) * plotH;
        const x = i => points.length === 1 ? PAD.left + plotW / 2 : PAD.left + (i / (points.length - 1)) * plotW;

        for (const t of ticks) {
            svg.appendChild(el('line', { class: 'grid', x1: PAD.left, y1: y(t), x2: PAD.left + plotW, y2: y(t) }));
            svg.appendChild(el('text', { class: 'tick', x: PAD.left - 6, y: y(t) + 4, 'text-anchor': 'end' }, opts.format(t)));
        }
        // X labels: the 1-based sequence number, thinned out so they never overlap.
        const every = Math.max(1, Math.ceil(points.length / 12));
        points.forEach((p, i) => {
            if (i % every === 0 || i === points.length - 1) {
                svg.appendChild(el('text', { class: 'tick', x: x(i), y: PAD.top + plotH + 16, 'text-anchor': 'middle' }, String(i + 1)));
            }
        });

        svg.appendChild(el('polyline', { class: 'line', points: points.map((p, i) => `${x(i)},${y(p.value)}`).join(' ') }));
        points.forEach((p, i) => {
            const dot = el('circle', { class: 'dot', cx: x(i), cy: y(p.value), r: 4 });
            dot.appendChild(el('title', {}, `${i + 1}. ${p.label}\n${opts.format(p.value)}`));
            svg.appendChild(dot);
        });
        container.replaceChildren(svg);
    }

    return { line };
})();
