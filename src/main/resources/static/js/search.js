// --- СТРАНИЦА РЕЗУЛЬТАТОВ ПОИСКА ---
// Константы (API_BASE_URL, IMG_500, POSTER_FALLBACK) и общие функции берутся из main.js,
// язык (currentLang, t) — из i18n.js
let searchItems = [];

document.addEventListener('DOMContentLoaded', () => {
    const query = new URLSearchParams(window.location.search).get('query');
    const grid = document.getElementById('movies-grid');

    if (grid) {
        grid.addEventListener('click', e => {
            const card = e.target.closest('[data-search-index]');
            if (!card) return;
            const entry = searchItems[Number(card.dataset.searchIndex)];
            if (!entry) return;
            if (entry.type === 'movie') {
                openMovieModal(entry.item);
            } else {
                openTvModal(entry.item);
            }
        });
    }

    if (query) {
        document.getElementById('search-query-display').textContent = `«${query}»`;
        // Перезагружаем результаты при смене языка
        window.onLanguageChanged = () => loadFullSearchResults(query);
        loadFullSearchResults(query);
    } else {
        document.getElementById('search-load-status').textContent = t('search-enter-query');
    }
});

async function loadFullSearchResults(query) {
    const grid = document.getElementById('movies-grid');
    const status = document.getElementById('search-load-status');
    searchItems = [];

    try {
        const response = await fetch(`${API_BASE_URL}/search/full?query=${encodeURIComponent(query)}&language=${currentLang}&page=1`);
        if (!response.ok) throw new Error('Server error');

        const data = await response.json();
        status.classList.add('hidden');
        grid.innerHTML = '';

        const filteredResults = (data.results || []).filter(item => {
            const type = item.mediaType || item.media_type;
            return type === 'movie' || type === 'tv';
        });

        if (filteredResults.length === 0) {
            grid.innerHTML = `<h3 style="color: white; grid-column: 1 / -1; text-align: center;">${t('no-results')}</h3>`;
            return;
        }

        filteredResults.forEach(item => {
            const type = item.mediaType || item.media_type;
            const title = type === 'movie' ? (item.title || item.name) : (item.name || item.title);
            const poster = item.posterPath || item.poster_path;
            const vote = item.voteAverage || item.vote_average;

            const typeText = type === 'movie' ? t('film') : t('tv-show');
            const genresList = item.genreNames || item.genre_names || [];
            const genresHtml = genresList.length > 0
                ? `<div style="font-size: 11px; color: #888; margin-bottom: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${genresList.join(', ')}</div>`
                : '';

            const posterUrl = poster ? `${IMG_500}${poster}` : POSTER_FALLBACK;
            const rating = vote ? vote.toFixed(1) : '0.0';

            const compatibleItem = {
                id: item.id,
                title: title,
                name: title,
                poster_path: poster,
                release_date: item.releaseDate || item.release_date,
                first_air_date: item.firstAirDate || item.first_air_date,
                vote_average: vote,
                overview: item.overview || t('no-description'),
                popularity: item.popularity || 0.0,
                origin_country: item.originCountry || item.origin_country
            };

            // Объект держим в JS, в разметку кладем только числовой индекс
            const index = searchItems.push({item: compatibleItem, type}) - 1;

            const card = document.createElement('div');
            card.className = 'movie-card';
            card.dataset.searchIndex = index;
            card.innerHTML = `
                <div style="position:relative">
                    <img src="${posterUrl}" alt="${title}" data-fallback="${POSTER_FALLBACK}">
                    <span class="rating-badge">★ ${rating}</span>
                </div>
                <div class="movie-card-body">
                    <div class="movie-title" title="${title}">${title}</div>
                    <div style="font-size: 12px; color: #8b8b99; margin-bottom: 4px; font-weight: bold;">
                        ${typeText}
                    </div>
                    ${genresHtml}
                    <button class="btn-detail-card" style="width:100%; padding:8px; background:var(--primary-color); border:none; color:white; border-radius:4px; cursor:pointer;">
                        ${t('more-details')}
                    </button>
                </div>
            `;
            bindImageFallbacks(card);
            grid.appendChild(card);
        });
    } catch (error) {
        console.error(error);
        status.classList.remove('hidden');
        status.textContent = t('toast-error');
    }
}
