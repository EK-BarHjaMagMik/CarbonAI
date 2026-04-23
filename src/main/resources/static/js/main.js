const API_BASE = 'http://localhost:8080/api/v1';

// --- DOM refs ---
const tripForm       = document.getElementById('tripForm');
const analyzeBtn     = document.getElementById('analyzeBtn');
const btnText        = document.getElementById('btnText');
const btnSpinner     = document.getElementById('btnSpinner');
const errorAlert     = document.getElementById('errorAlert');
const errorMessage   = document.getElementById('errorMessage');
const resultsSection = document.getElementById('results');

// Summary
const summaryDistance = document.getElementById('summaryDistance');
const summaryLowest   = document.getElementById('summaryLowest');
const summaryHighest  = document.getElementById('summaryHighest');

// Modes + AI
const modesContainer    = document.getElementById('modesContainer');
const aiRecommendation  = document.getElementById('aiRecommendation');

// Debug
const debugCity       = document.getElementById('debugCity');
const debugGeocodeBtn = document.getElementById('debugGeocodeBtn');
const debugOutput     = document.getElementById('debugOutput');

// --- Trip Analysis ---
tripForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const from       = document.getElementById('fromCity').value.trim();
    const to         = document.getElementById('toCity').value.trim();
    const passengers = document.getElementById('passengers').value;

    if (!from || !to) return;

    setLoading(true);
    hideError();
    hideResults();

    try {
        const url = `${API_BASE}/analyze-trip?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&passengers=${passengers}`;
        const response = await fetch(url);

        if (!response.ok) {
            const text = await response.text();
            throw new Error(`Server returned ${response.status}: ${text || response.statusText}`);
        }

        const data = await response.json();
        renderResults(data);

    } catch (err) {
        showError(err.message);
    } finally {
        setLoading(false);
    }
});

function renderResults(data) {
    // Summary cards
    summaryDistance.textContent = data.distanceKm?.toLocaleString() ?? '—';
    summaryLowest.textContent   = fmt(data.lowestCo2);
    summaryHighest.textContent  = fmt(data.highestCo2);

    // Mode rows
    modesContainer.innerHTML = '';
    const highest = data.highestCo2 || 1;

    const modeIcons = {
        FLIGHT: '✈️', TRAIN: '🚆', CAR: '🚗',
        FERRY: '⛴️', BUS: '🚌', BIKE: '🚲'
    };

    (data.modes || []).forEach(mode => {
        const pct      = Math.round((mode.co2 / highest) * 100);
        const barClass = co2BarClass(pct);
        const icon     = modeIcons[mode.name?.toUpperCase()] ?? '🚀';

        const row = document.createElement('div');
        row.className = 'mode-row';
        row.innerHTML = `
            <div class="mode-header">
                <span class="mode-name">${icon} ${mode.name}</span>
                <div class="mode-meta">
                    <span class="badge bg-secondary">${mode.time}</span>
                    <span class="badge bg-light text-dark border">${mode.cost}</span>
                    <span class="co2-label ${barClass}-text">${fmt(mode.co2)} kg CO₂e</span>
                </div>
            </div>
            <div class="progress mode-progress" title="${fmt(mode.co2)} kg CO₂e">
                <div class="progress-bar ${barClass}" role="progressbar"
                     style="width: ${pct}%"
                     aria-valuenow="${pct}" aria-valuemin="0" aria-valuemax="100">
                </div>
            </div>
        `;
        modesContainer.appendChild(row);
    });

    // AI recommendation — preserve line breaks and emoji
    const raw = data.aiRecommendation ?? 'No recommendation available.';
    aiRecommendation.innerHTML = raw
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\n/g, '<br>');

    resultsSection.classList.remove('d-none');
    resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// --- Debug: Geocoding ---
debugGeocodeBtn.addEventListener('click', async () => {
    const city = debugCity.value.trim();
    if (!city) return;

    debugGeocodeBtn.disabled = true;
    debugOutput.classList.remove('d-none');
    debugOutput.textContent = 'Loading…';

    try {
        const response = await fetch(`${API_BASE}/analyze-trip/geocode?city=${encodeURIComponent(city)}`);
        const text = await response.text();
        debugOutput.textContent = response.ok ? text : `Error ${response.status}: ${text}`;
    } catch (err) {
        debugOutput.textContent = `Network error: ${err.message}`;
    } finally {
        debugGeocodeBtn.disabled = false;
    }
});

// --- Helpers ---
function setLoading(on) {
    analyzeBtn.disabled = on;
    btnSpinner.classList.toggle('d-none', !on);
    btnText.textContent = on ? 'Analyzing…' : 'Analyze';
}

function showError(msg) {
    errorMessage.textContent = msg;
    errorAlert.classList.remove('d-none');
}

function hideError() {
    errorAlert.classList.add('d-none');
}

function hideResults() {
    resultsSection.classList.add('d-none');
}

function fmt(val) {
    if (val == null) return '—';
    return Number(val).toLocaleString('en-DK', { maximumFractionDigits: 2 });
}

function co2BarClass(pct) {
    if (pct <= 20) return 'bg-success';
    if (pct <= 50) return 'bg-info';
    if (pct <= 75) return 'bg-warning';
    return 'bg-danger';
}
