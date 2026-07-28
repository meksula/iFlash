"use strict";

(function () {
    const ticker = window.TICKER;
    const priceEl = document.getElementById("live-price");
    const chartEl = document.getElementById("chart");
    const chartEmpty = document.getElementById("chart-empty");

    // ----- Order type → show limit price only when relevant -----
    const orderType = document.getElementById("orderType");
    const priceRow = document.getElementById("price-row");
    function syncPriceRow() {
        const isMarket = orderType.value === "MARKET";
        priceRow.style.display = isMarket ? "none" : "";
        if (isMarket) {
            document.getElementById("price").value = "";
        }
    }
    if (orderType) {
        orderType.addEventListener("change", syncPriceRow);
        syncPriceRow();
    }

    // ----- Live price -----
    function pollPrice() {
        fetch("/app/price/" + encodeURIComponent(ticker))
            .then(r => r.ok ? r.json() : null)
            .then(data => {
                if (data && data.price !== null && data.price !== undefined) {
                    priceEl.textContent = "$" + Number(data.price).toFixed(4);
                }
            })
            .catch(() => {});
    }

    // ----- Chart -----
    let series = null;
    function initChart() {
        if (!chartEl || typeof LightweightCharts === "undefined") {
            return;
        }
        const chart = LightweightCharts.createChart(chartEl, {
            height: 320,
            layout: { background: { color: "transparent" }, textColor: "#6b7b8f" },
            grid: { vertLines: { color: "#eef3f8" }, horzLines: { color: "#eef3f8" } },
            rightPriceScale: { borderColor: "#d9e3ef" },
            timeScale: { borderColor: "#d9e3ef", timeVisible: true, secondsVisible: true },
        });
        series = chart.addLineSeries({ color: "#1f6f9c", lineWidth: 2 });
        new ResizeObserver(entries => {
            for (const e of entries) {
                chart.applyOptions({ width: e.contentRect.width });
            }
        }).observe(chartEl);
        loadChart();
    }

    function loadChart() {
        if (!series) {
            return;
        }
        fetch("/app/quotes/" + encodeURIComponent(ticker))
            .then(r => r.ok ? r.json() : [])
            .then(points => {
                if (!points || points.length === 0) {
                    if (chartEmpty) chartEmpty.hidden = false;
                    return;
                }
                if (chartEmpty) chartEmpty.hidden = true;
                series.setData(points.map(p => ({ time: p.time, value: Number(p.value) })));
            })
            .catch(() => {});
    }

    // ----- Order book -----
    function renderLevels(tbodyId, levels, cssClass) {
        const tbody = document.getElementById(tbodyId);
        if (!tbody) {
            return;
        }
        if (!levels || levels.length === 0) {
            tbody.innerHTML = '<tr><td class="muted small">—</td><td></td></tr>';
            return;
        }
        tbody.innerHTML = levels.map(l =>
            '<td class="mono ' + cssClass + '">$' + Number(l.price).toFixed(4) + '</td>' +
            '<td class="num mono">' + l.volume + '</td>')
            .map(cells => "<tr>" + cells + "</tr>")
            .join("");
    }

    function pollBook() {
        fetch("/app/orderbook/" + encodeURIComponent(ticker))
            .then(r => r.ok ? r.json() : null)
            .then(book => {
                if (!book) {
                    return;
                }
                renderLevels("book-bids", book.bids, "");
                renderLevels("book-asks", book.asks, "");
            })
            .catch(() => {});
    }

    document.addEventListener("DOMContentLoaded", () => {
        initChart();
        pollPrice();
        pollBook();
        setInterval(pollPrice, 4000);
        setInterval(loadChart, 5000);
        setInterval(pollBook, 5000);
    });
})();
