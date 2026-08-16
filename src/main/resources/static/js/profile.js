// ВАЖНО: константы API_BASE_URL / IMG_500 / POSTER_FALLBACK и общие функции берутся из main.js,
// язык (currentLang, t, tf) — из i18n.js
let loadedMovies = {};
let loadedTvShows = {};
let activeModalData = null;

let isMyProfile = true;
let currentProfileUserId = null;

// ПЕРЕМЕННЫЕ ДЛЯ ПОИСКА ПО КАРТОТЕКЕ
let currentGridQuery = '';
let gridSearchTimeout = null;

let gridType = '';
let gridPage = 0;
let gridTotalPages = 1;
let isGridLoading = false;

document.addEventListener('DOMContentLoaded', () => {
    // 1. ПРОВЕРЯЕМ URL: Мы у себя или в гостях?
    const urlParams = new URLSearchParams(window.location.search);
    const urlUserId = urlParams.get('userId');
    const gridSearchInput = document.getElementById('grid-search-input');

    if (urlUserId) {
        // РЕЖИМ ГОСТЯ
        isMyProfile = false;
        currentProfileUserId = urlUserId;

        // Меняем ключ перевода, чтобы заголовок оставался корректным и после смены языка
        const nameEl = document.getElementById('profile-name');
        if (nameEl) {
            nameEl.setAttribute('data-i18n', 'profile-title-guest');
            nameEl.innerText = t('profile-title-guest');
        }
        document.querySelectorAll('.btn-delete-action').forEach(btn => btn.style.display = 'none');

        // Скрываем поиск в чужом профиле, так как бэкенд ищет только по OidcUser (владельцу сессии)
        if (gridSearchInput) gridSearchInput.style.display = 'none';
    } else {
        // РЕЖИМ ХОЗЯИНА
        isMyProfile = true;
        if (gridSearchInput) gridSearchInput.style.display = 'block';
    }

    // 2. СЛУШАТЕЛЬ ДЛЯ ЖИВОГО ПОИСКА В КАРТОТЕКЕ
    if (gridSearchInput) {
        gridSearchInput.addEventListener('input', e => {
            currentGridQuery = e.target.value.trim();
            clearTimeout(gridSearchTimeout);

            // Ждем 500мс после ввода, чтобы не ДДОСить бэкенд
            gridSearchTimeout = setTimeout(() => {
                gridPage = 0;
                document.getElementById('full-grid-content').innerHTML = '';
                loadGridPage();
            }, 500);
        });
    }

    // 3. КЛИКИ ПО КАРТОЧКАМ (вместо inline onclick в разметке карточки)
    ['movies-slider', 'tv-shows-slider', 'full-grid-content'].forEach(containerId => {
        const container = document.getElementById(containerId);
        if (!container) return;
        container.addEventListener('click', e => {
            const card = e.target.closest('[data-profile-id]');
            if (!card) return;
            openItemModal(Number(card.dataset.profileId), card.dataset.profileMovie === 'true');
        });
    });

    // Перезагружаем данные при смене языка
    window.onLanguageChanged = loadDashboard;

    // Сразу грузим дашборд
    loadDashboard();
});

// =======================
// 1. ЗАГРУЗКА ДАШБОРДА (КАРУСЕЛИ)
// =======================
async function loadDashboard() {
    // Пути без API_BASE_URL — его подставляет watchlistFetch из main.js
    const movieUrl = isMyProfile
        ? `/watchlist-movies/me?page=0&size=20&language=${currentLang}`
        : `/watchlist-movies/${currentProfileUserId}/user?page=0&size=20&language=${currentLang}`;

    const tvUrl = isMyProfile
        ? `/watchlist-tv-shows/me?page=0&size=20&language=${currentLang}`
        : `/watchlist-tv-shows/${currentProfileUserId}/user?page=0&size=20&language=${currentLang}`;

    fetchAndRender(movieUrl, 'movies-slider', true);
    fetchAndRender(tvUrl, 'tv-shows-slider', false);
}

function buildCardHtml(item, id, title, isMovie, detailBtnText) {
    const badge = !isMovie
        ? `<div style="position:absolute; top:8px; left:8px; background:var(--primary-color); color:white; padding:4px 8px; border-radius:6px; font-size:12px; font-weight:bold;">S${item.currentSeason} E${item.currentEpisode}</div>`
        : '';

    return `
        <div class="movie-card" data-profile-id="${id}" data-profile-movie="${isMovie}">
            <div style="position:relative">
                <img src="${IMG_500}${item.posterUrl || item.posterPath || ''}" alt="${title}" data-fallback="${POSTER_FALLBACK}">
                ${badge}
            </div>
            <div class="movie-card-body">
                <div class="movie-title" title="${title}">${title}</div>
                <button class="btn-detail-card">${detailBtnText}</button>
            </div>
        </div>
    `;
}

async function fetchAndRender(url, containerId, isMovie) {
    try {
        const response = await watchlistFetch(url);
        if (response.status === 401) return redirectToLogin();
        if (!response.ok) return;

        const data = await response.json();
        const items = data.content || [];
        const total = data.page?.totalElements || data.totalElements || 0;

        const totalEl = document.getElementById(isMovie ? 'total-movies' : 'total-tv');
        if (totalEl) totalEl.innerText = total;

        const container = document.getElementById(containerId);
        if (!container) return;
        container.innerHTML = '';

        if (items.length === 0) {
            container.innerHTML = `<div class="empty-state" style="width:100%"><h3>${t('no-results')}</h3></div>`;
            return;
        }

        const detailBtnText = t('more-details');

        items.forEach(item => {
            const id = isMovie ? item.movieId : item.tvShowId;
            const title = isMovie ? item.title : item.name;
            if (isMovie) loadedMovies[id] = item; else loadedTvShows[id] = item;

            container.insertAdjacentHTML('beforeend', buildCardHtml(item, id, title, isMovie, detailBtnText));
        });

        bindImageFallbacks(container);
    } catch (e) {
        console.error(e);
    }
}

// =======================
// 2. ОТКРЫТИЕ МОДАЛОК
// =======================
function openItemModal(id, isMovie) {
    const data = isMovie ? loadedMovies[id] : loadedTvShows[id];
    if (!data) return;
    activeModalData = {...data, isMovie};

    if (isMovie) {
        document.getElementById('movie-modal-title').innerText = data.title;
        document.getElementById('movie-modal-poster').src = data.posterUrl ? `${IMG_500}${data.posterUrl}` : '';
        document.getElementById('movie-modal-year').innerText = `${t('year-label')}: ${data.releaseDate ? data.releaseDate.substring(0, 4) : '...'}`;
        document.getElementById('movie-modal-country').innerText = `${t('country-label')}: ${formatList(data.productionCountries)}`;
        document.getElementById('movie-modal-runtime').innerText = `${data.runtime || '...'} ${t('runtime-label')}`;
        document.getElementById('movie-modal-genres').innerHTML = formatGenres(data.genres);
        document.getElementById('movie-modal-rating').innerHTML = `★ ${data.voteAverage ? data.voteAverage.toFixed(1) : 'NR'}`;

        setupOverview('movie-modal-overview', 'movie-read-more', data.overview);

        const deleteBtn = document.getElementById('btn-delete-movie');
        if (deleteBtn) {
            deleteBtn.style.display = isMyProfile ? 'block' : 'none';
            deleteBtn.onclick = () => deleteItem(data.movieId, true);
        }

        document.getElementById('movie-modal').classList.add('show');
        document.body.classList.add('modal-open');
    } else {
        document.getElementById('modal-title').innerText = data.name;
        document.getElementById('modal-poster').src = data.posterUrl ? `${IMG_500}${data.posterUrl}` : '';
        document.getElementById('modal-year').innerText = `${t('year-label')}: ${data.firstAirDate ? data.firstAirDate.substring(0, 4) : '...'}`;

        const countries = data.productionCountries || data.originCountry || [];
        document.getElementById('modal-country').innerText = `${t('country-label')}: ${formatList(Array.isArray(countries) ? countries : [countries])}`;
        document.getElementById('modal-genres').innerHTML = formatGenres(data.genres);
        document.getElementById('modal-rating').innerHTML = `★ ${data.voteAverage ? data.voteAverage.toFixed(1) : 'NR'}`;

        document.getElementById('modal-status-text').innerText =
            `${t('season-label')} ${data.currentSeason}, ${t('episode-label')} ${data.currentEpisode}`;

        setupOverview('modal-overview', 'tv-read-more', data.overview);
        renderProgressSeasons(data);

        const deleteBtn = document.getElementById('btn-delete-tv');
        if (deleteBtn) {
            deleteBtn.style.display = isMyProfile ? 'block' : 'none';
            deleteBtn.onclick = () => deleteItem(data.tvShowId, false);
        }

        document.getElementById('progress-modal').classList.add('show');
        document.body.classList.add('modal-open');
    }
}

function renderProgressSeasons(data) {
    const tabsContainer = document.getElementById('season-tabs');
    if (!tabsContainer) return;
    tabsContainer.innerHTML = '';
    const seasonsList = data.seasons || [];

    seasonsList.forEach(seasonObj => {
        const sNum = seasonObj.season_number ?? seasonObj.seasonNumber;
        const totalEps = seasonObj.episode_count ?? seasonObj.episodeCount ?? 10;

        const btn = document.createElement('button');
        btn.className = 'season-tab';
        btn.innerText = `${t('season-label')} ${sNum}`;

        const isFullyWatched = sNum < data.currentSeason || (sNum === data.currentSeason && data.currentEpisode >= totalEps);
        if (isFullyWatched) btn.classList.add('watched');
        if (sNum === data.currentSeason) {
            btn.classList.add('active');
            renderProgressEpisodes(sNum, data);
        }

        btn.addEventListener('click', () => {
            document.querySelectorAll('.season-tab').forEach(tab => tab.classList.remove('active'));
            btn.classList.add('active');
            renderProgressEpisodes(sNum, data);
        });
        tabsContainer.appendChild(btn);
    });
}

function renderProgressEpisodes(sNum, data) {
    const grid = document.getElementById('episodes-grid');
    const titleEl = document.getElementById('episodes-title');
    if (!grid) return;
    grid.innerHTML = '';

    if (titleEl) titleEl.innerText = tf('episodes-of-season', {n: sNum});

    const seasonData = (data.seasons || []).find(s => (s.season_number ?? s.seasonNumber) === sNum);
    const totalEps = seasonData ? (seasonData.episode_count ?? seasonData.episodeCount ?? 10) : 10;

    for (let i = 1; i <= totalEps; i++) {
        const btn = document.createElement('button');
        btn.className = 'episode-btn';
        btn.innerText = i;
        if (sNum < data.currentSeason || (sNum === data.currentSeason && i <= data.currentEpisode)) {
            btn.classList.add('watched');
        }
        grid.appendChild(btn);
    }
}

// =======================
// 3. УДАЛЕНИЕ
// =======================
async function deleteItem(id, isMovie) {
    if (!isMyProfile) return;
    if (!confirm(t('confirm-delete'))) return;

    const endpoint = isMovie ? `/watchlist-movies?movieId=${id}` : `/watchlist-tv-shows?tvShowId=${id}`;

    try {
        const response = await watchlistFetch(endpoint, {method: 'DELETE'});

        // Удаление намеренно не сохраняем в pendingAction: тихий повтор после логина
        // обошел бы confirm() выше
        if (response.status === 401) return redirectToLogin();

        if (response.ok) {
            showToast(t('toast-success'));
            closeModal(isMovie ? 'movie-modal' : 'progress-modal');
            loadDashboard();

            if (document.getElementById('full-grid-modal').classList.contains('show')) {
                gridPage = 0;
                document.getElementById('full-grid-content').innerHTML = '';
                loadGridPage();
            }
        } else {
            showToast(t('toast-error'), true);
        }
    } catch (e) {
        showToast(t('toast-error'), true);
    }
}

// =======================
// 4. ПОЛНАЯ КАРТОТЕКА (GRID MODAL)
// =======================
function openFullGrid(type) {
    gridType = type;
    gridPage = 0;
    currentGridQuery = '';

    const searchInput = document.getElementById('grid-search-input');
    if (searchInput) searchInput.value = '';

    const content = document.getElementById('full-grid-content');
    const title = document.getElementById('full-grid-title');
    if (content) content.innerHTML = '';
    if (title) title.innerText = t('all-saved-title');

    document.getElementById('full-grid-modal').classList.add('show');
    document.body.classList.add('modal-open');
    loadGridPage();
}

async function loadGridPage() {
    if (isGridLoading) return;
    isGridLoading = true;
    const status = document.getElementById('grid-load-status');
    if (status) status.innerText = t('loading');

    const isMovie = gridType === 'movies';
    const basePath = isMovie ? `/watchlist-movies` : `/watchlist-tv-shows`;
    let endpoint = '';

    // ЛОГИКА ФОРМИРОВАНИЯ URL
    if (currentGridQuery.length > 0) {
        // РАБОТАЕТ ПОИСК (без userId, бэкенд берет пользователя из сессии)
        endpoint = `${basePath}/search/${encodeURIComponent(currentGridQuery)}?page=${gridPage}&size=20&language=${currentLang}`;
    } else {
        // ПРОСТО ЛИСТАЕМ КАРТОТЕКУ
        endpoint = isMyProfile
            ? `${basePath}/me?page=${gridPage}&size=20&language=${currentLang}`
            : `${basePath}/${currentProfileUserId}/user?page=${gridPage}&size=20&language=${currentLang}`;
    }

    try {
        const response = await watchlistFetch(endpoint);
        if (response.status === 401) return redirectToLogin();
        if (!response.ok) throw new Error('Server error');

        const data = await response.json();
        const items = data.content || [];
        gridTotalPages = data.page?.totalPages || data.totalPages || 1;

        const container = document.getElementById('full-grid-content');
        const detailBtnText = t('more-details');

        if (items.length === 0 && gridPage === 0) {
            container.innerHTML = `<h3 style="color: white; text-align: center; width: 100%; grid-column: 1 / -1;">${t('no-results')}</h3>`;
        }

        items.forEach(item => {
            const id = isMovie ? item.movieId : item.tvShowId;
            const title = isMovie ? item.title : item.name;

            // Сохраняем в кэш
            if (isMovie) loadedMovies[id] = item; else loadedTvShows[id] = item;

            container.insertAdjacentHTML('beforeend', buildCardHtml(item, id, title, isMovie, detailBtnText));
        });

        bindImageFallbacks(container);

        if (status) status.innerText = '';
    } catch (e) {
        if (status) status.innerText = t('toast-error');
    } finally {
        isGridLoading = false;
    }
}

document.getElementById('grid-scroll-container')?.addEventListener('scroll', function () {
    if (isGridLoading || gridPage >= gridTotalPages - 1) return;
    if (this.scrollHeight - this.scrollTop <= this.clientHeight + 200) {
        gridPage++;
        loadGridPage();
    }
});
