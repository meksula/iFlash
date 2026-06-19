"use strict";

// Format a number as a fixed-decimal string, or "—" when missing.
function fmt(value, decimals) {
    if (value === null || value === undefined || isNaN(value)) {
        return "—";
    }
    return "$" + Number(value).toFixed(decimals);
}

// Live price polling for any element carrying data-price-ticker.
function pollPrices() {
    const cells = document.querySelectorAll("[data-price-ticker]");
    cells.forEach(cell => {
        const ticker = cell.getAttribute("data-price-ticker");
        fetch("/app/price/" + encodeURIComponent(ticker))
            .then(r => r.ok ? r.json() : null)
            .then(data => {
                if (data && data.price !== null && data.price !== undefined) {
                    cell.textContent = fmt(data.price, 4);
                }
            })
            .catch(() => { /* keep last value */ });
    });
}

// Client-side filtering of the instruments table.
function wireInstrumentFilter() {
    const input = document.getElementById("filter");
    const table = document.getElementById("instrument-table");
    if (!input || !table) {
        return;
    }
    input.addEventListener("input", () => {
        const needle = input.value.trim().toUpperCase();
        table.querySelectorAll("tbody tr").forEach(row => {
            const ticker = (row.getAttribute("data-ticker") || "").toUpperCase();
            row.style.display = ticker.includes(needle) ? "" : "none";
        });
    });
}

// Wallet quick-deposit buttons.
function wireQuickAmounts() {
    const amount = document.getElementById("amount");
    if (!amount) {
        return;
    }
    document.querySelectorAll(".quick[data-amount]").forEach(btn => {
        btn.addEventListener("click", () => {
            amount.value = btn.getAttribute("data-amount");
        });
    });
}

document.addEventListener("DOMContentLoaded", () => {
    wireInstrumentFilter();
    wireQuickAmounts();
    if (document.querySelector("[data-price-ticker]")) {
        pollPrices();
        setInterval(pollPrices, 5000);
    }
});
