// --- ГЛОБАЛЬНЫЕ НАСТРОЙКИ ---
const API_BASE_URL = '';
const IMG_500 = 'https://image.tmdb.org/t/p/w500';

// 1. ЗАПОМИНАЕМ ВЫБОР ЯЗЫКА НАВСЕГДА
let currentLang = localStorage.getItem('app_language') || 'en';

let currentTimeWindow = 'day';
let currentMediaType = 'movies';
let currentPage = 1;
let totalPages = 1;
let isLoading = false;

let currentSelectedMovie = null;
let currentMovieBtnElement = null;

// --- УВЕДОМЛЕНИЯ ---
function showToast(message, isError = false) {
    const toast = document.getElementById('custom-toast');
    if (!toast) return;
    toast.innerText = message;
    toast.className = 'toast-notification';
    if (isError) toast.classList.add('error');

    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => toast.classList.remove('show'), 3000);
}

// --- НАВИГАЦИЯ И КАТАЛОГ ---
function updateMediaType(type) {
    currentMediaType = type;
    document.getElementById('type-movies').classList.toggle('active', type === 'movies');
    document.getElementById('type-tv').classList.toggle('active', type === 'tv-shows');
    const titleEl = document.getElementById('catalog-title');
    if (titleEl) titleEl.innerText = type === 'movies' ? 'Каталог фильмов' : 'Каталог сериалов';
    loadTrending();
    resetCatalog();
}

function updateTimeWindow(window, btnId) {
    currentTimeWindow = window;
    const btnDay = document.getElementById('btn-day');
    const btnWeek = document.getElementById('btn-week');
    if (btnDay) btnDay.classList.remove('active');
    if (btnWeek) btnWeek.classList.remove('active');
    const activeBtn = document.getElementById(btnId);
    if (activeBtn) activeBtn.classList.add('active');
    loadTrending();
    resetCatalog();
}

// УМНАЯ СМЕНА ЯЗЫКА
function changeLanguage(lang) {
    currentLang = lang;
    localStorage.setItem('app_language', lang); // Сохраняем в память

    applyTranslations();

    // Синхронизируем все селекторы на странице
    document.querySelectorAll('.lang-select').forEach(s => s.value = lang);

    if (document.getElementById('trending-slider')) {
        // Мы на главной
        loadTrending();
        resetCatalog();
    } else if (document.getElementById('search-load-status')) {
        // Мы на странице поиска
        const urlParams = new URLSearchParams(window.location.search);
        const query = urlParams.get('query');
        if (query && typeof loadFullSearchResults === 'function') {
            loadFullSearchResults(query);
        }
    }
}

function resetCatalog() {
    const grid = document.getElementById('movies-grid');
    if (!grid) return;
    currentPage = 1;
    grid.innerHTML = '';
    loadCatalog();
}

function scrollSlider(dist) {
    const slider = document.getElementById('trending-slider');
    if (slider) slider.scrollBy({left: dist, behavior: 'smooth'});
}

function updateNavButtons() {
    const slider = document.getElementById('trending-slider');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    if (!slider || !prevBtn || !nextBtn) return;
    prevBtn.style.display = slider.scrollLeft > 10 ? 'flex' : 'none';
    const isEnd = slider.scrollLeft + slider.clientWidth >= slider.scrollWidth - 15;
    nextBtn.style.display = isEnd ? 'none' : 'flex';
}

function getDisplayTitle(item) {
    return item.title || item.name || 'Без названия';
}

// --- ЗАГРУЗКА ДАННЫХ ---
async function loadTrending() {
    const slider = document.getElementById('trending-slider');
    if (!slider) return;
    slider.innerHTML = '<p style="padding: 20px;">Загрузка...</p>';
    try {
        const url = `${API_BASE_URL}/trending/${currentMediaType}/${currentTimeWindow}?page=1&language=${currentLang}`;
        const res = await fetch(url);
        const data = await res.json();
        const responseData = Array.isArray(data) ? data[0] : data;

        if (!responseData || !responseData.results) return;

        let items = responseData.results.filter(m => m.poster_path).slice(0, 10);

        slider.innerHTML = items.map(item => {
            const encodedItem = encodeURIComponent(JSON.stringify(item)).replace(/'/g, "%27");
            return `
            <div class="slider-item" onclick="handleFavoriteClick(this, '${encodedItem}')">
                <div class="image-container">
                    <span class="rating-badge">★ ${item.vote_average ? item.vote_average.toFixed(1) : '0.0'}</span>
                    <img src="${IMG_500}${item.poster_path}" alt="${getDisplayTitle(item)}" onerror="this.parentElement.parentElement.style.display='none'">
                </div>
                <div class="movie-title">${getDisplayTitle(item)}</div>
            </div>
            `;
        }).join('');

        setTimeout(updateNavButtons, 300);
        slider.scrollLeft = 0;
    } catch (e) {
        console.error(e);
    }
}

async function loadCatalog() {
    const statusEl = document.getElementById('load-status');
    const grid = document.getElementById('movies-grid');
    if (!statusEl || !grid) return;

    if (isLoading) return;
    isLoading = true;
    statusEl.innerText = 'Загрузка контента...';

    try {
        const url = `${API_BASE_URL}/trending/${currentMediaType}/${currentTimeWindow}?page=${currentPage}&language=${currentLang}`;
        const res = await fetch(url);
        const data = await res.json();
        const responseData = Array.isArray(data) ? data[0] : data;

        if (!responseData || !responseData.results) {
            statusEl.innerText = 'Сталася помилка при завантаженні';
            return;
        }

        totalPages = responseData.total_pages;
        const items = responseData.results.filter(m => m.poster_path);

        appendItems(items);

        if (currentPage >= totalPages) {
            statusEl.innerText = 'Больше ничего нет';
        } else {
            statusEl.innerText = '';
        }
    } catch (e) {
        console.error(e);
        statusEl.innerText = 'Ошибка сети';
    } finally {
        isLoading = false;
    }
}

function appendItems(items) {
    const grid = document.getElementById('movies-grid');
    if (!grid) return;
    const html = items.map(item => {
        const encodedItem = encodeURIComponent(JSON.stringify(item));
        return `
        <div class="movie-card">
            <div style="position:relative">
                <img src="${IMG_500}${item.poster_path || item.posterPath}" alt="${getDisplayTitle(item)}" onerror="this.src='https://placehold.co/500x750/333333/ffffff?text=No+Image'">
                <span class="rating-badge">★ ${item.vote_average ? item.vote_average.toFixed(1) : '0.0'}</span>
            </div>
            <div class="movie-card-body">
                <div class="movie-title" title="${getDisplayTitle(item)}">${getDisplayTitle(item)}</div>
                <button style="width:100%; padding:8px; background:var(--primary-color); border:none; color:white; border-radius:4px; cursor:pointer;"
                        onclick="handleFavoriteClick(this, '${encodedItem}')">
                    Детально
                </button>
            </div>
        </div>
    `
    }).join('');
    grid.insertAdjacentHTML('beforeend', html);
}

function handleFavoriteClick(btnElement, encodedItemStr) {
    const item = JSON.parse(decodeURIComponent(encodedItemStr));
    const type = item.media_type || item.mediaType || (currentMediaType === 'tv-shows' ? 'tv' : 'movie');

    if (type === 'tv' || type === 'tv-shows') {
        openTvModal(item);
    } else {
        openMovieModal(item, btnElement);
    }
}

// --- МОДАЛКА ФИЛЬМОВ ---
async function openMovieModal(movie, btnElement) {
    currentSelectedMovie = movie;
    currentMovieBtnElement = btnElement;
    const t = translations[currentLang];

    const modalEl = document.getElementById('movie-modal');
    if (!modalEl) {
        console.error("Модалка фильма не найдена!");
        return;
    }

    document.getElementById('movie-modal-title').innerText = getDisplayTitle(movie);
    document.getElementById('movie-modal-poster').src = movie.poster_path ? `${IMG_500}${movie.poster_path}` : 'https://placehold.co/500x750/333333/ffffff?text=No+Image';

    const date = movie.release_date || movie.releaseDate;
    document.getElementById('movie-modal-year').innerText = `${t['year-label']}: ${date ? date.substring(0, 4) : '...'}`;
    document.getElementById('movie-modal-rating').innerText = `★ ${movie.vote_average ? movie.vote_average.toFixed(1) : 'NR'}`;

    const countryEl = document.getElementById('movie-modal-country');
    if (countryEl) countryEl.innerText = `${t['country-label']}: ${t['loading']}`;

    // Добавляем и active (для главной) и show (для профиля)
    modalEl.classList.add('active', 'show');
    document.body.classList.add('modal-open');

    try {
        const res = await fetch(`${API_BASE_URL}/details/movies/${movie.id}?language=${currentLang}`);
        const details = await res.json();

        const originalId = currentSelectedMovie.id;
        currentSelectedMovie = { ...currentSelectedMovie, ...details };
        currentSelectedMovie.id = originalId || details.id;

        if (countryEl) countryEl.innerText = `${t['country-label']}: ${formatList(details.production_countries || details.productionCountries)}`;

        const runtimeEl = document.getElementById('movie-modal-runtime');
        if (runtimeEl) runtimeEl.innerText = `${details.runtime || '...'} ${t['runtime-label']}`;

        const genresCont = document.getElementById('movie-modal-genres');
        if (genresCont) genresCont.innerHTML = formatGenres(details.genres);

        // --- ВОТ ИСПРАВЛЕНИЕ ДЛЯ ОЦЕНКИ ---
        // Берем точную оценку из полных деталей и обновляем бейдж!
        const finalRating = details.vote_average || details.voteAverage;
        const ratingEl = document.getElementById('movie-modal-rating');
        if (ratingEl) {
            ratingEl.innerText = `★ ${(finalRating && finalRating > 0) ? finalRating.toFixed(1) : 'NR'}`;
        }

        if (details.overview) setupOverview('movie-modal-overview', 'movie-read-more', details.overview);
    } catch (e) { console.error(e); }
}

function closeMovieModal() {
    document.getElementById('movie-modal').classList.remove('active');
    document.body.classList.remove('modal-open');
}

// СОХРАНЕНИЕ ФИЛЬМА
async function saveMovieToWatchlist() {
    if (!currentSelectedMovie || !currentSelectedMovie.id) {
        showToast("Ошибка: ID фильма не найден", true);
        return;
    }

    const btnSave = document.querySelector('#movie-modal .btn-save');
    const originalText = btnSave.innerText;
    btnSave.innerText = '⏳...';
    btnSave.style.pointerEvents = 'none';

    const item = currentSelectedMovie;
    const releaseDate = item.release_date || item.releaseDate;
    let formattedDate = null;
    if (releaseDate && releaseDate.length >= 10) {
        formattedDate = releaseDate.substring(0, 10) + "T00:00:00";
    }

    // ИСПРАВЛЕНИЕ: Шлем ID в двух форматах, чтобы Spring Boot 100% его съел
    const payload = {
        poster_path: item.poster_path || item.posterPath,
        title: getDisplayTitle(item),
        movieId: item.id,      // Для camelCase
        movie_id: item.id,     // Для snake_case
        release_date: formattedDate,
        popularity: item.popularity || 0.0
    };

    try {
        const response = await fetch(`${API_BASE_URL}/watchlist-movies`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });

        if (response.status === 401 || response.status === 403) {
            window.location.href = '/login.html';
            return;
        }

        if (response.ok) {
            showToast(translations[currentLang]['toast-success']);
            closeMovieModal();
            if (currentMovieBtnElement) {
                currentMovieBtnElement.innerText = '✅';
                currentMovieBtnElement.style.background = '#28a745';
            }
        } else {
            showToast(translations[currentLang]['toast-error'], true);
        }
    } catch (e) {
        showToast("Error", true);
    } finally {
        btnSave.innerText = originalText;
        btnSave.style.pointerEvents = 'auto';
    }
}

// --- МОДАЛКА СЕРИАЛОВ ---
let currentShowData = null;
let currentShowSeasons = [];
let currentSelectedSeason = 1;
let currentSelectedEpisode = 0;

// --- МОДАЛКА СЕРИАЛОВ ---
async function openTvModal(show) {
    currentShowData = show;
    const t = translations[currentLang];

    // УМНЫЙ ПОИСК: Ищем tv-modal (на главной) ИЛИ progress-modal (в профиле)
    const modalEl = document.getElementById('tv-modal') || document.getElementById('progress-modal');
    if (!modalEl) {
        console.error("Модальное окно сериала не найдено на этой странице!");
        return;
    }

    const titleEl = document.getElementById('modal-title');
    if (titleEl) titleEl.innerText = getDisplayTitle(show);

    const posterEl = document.getElementById('modal-poster');
    if (posterEl) posterEl.src = show.poster_path ? `${IMG_500}${show.poster_path}` : 'https://placehold.co/500x750/333333/ffffff?text=No+Image';

    const date = show.first_air_date || show.firstAirDate;
    const yearEl = document.getElementById('modal-year');
    if (yearEl) yearEl.innerText = `${t['year-label']}: ${date ? date.substring(0, 4) : '...'}`;

    const countryEl = document.getElementById('modal-country');
    if (countryEl) countryEl.innerText = `${t['country-label']}: ${t['loading']}`;

    const overviewEl = document.getElementById('modal-overview');
    if (overviewEl) overviewEl.innerText = show.overview || t['no-results'];

    // Добавляем и active (для главной) и show (для профиля)
    modalEl.classList.add('active', 'show');
    document.body.classList.add('modal-open');

    try {
        const res = await fetch(`${API_BASE_URL}/details/tv-shows/${show.id}?language=${currentLang}`);
        const details = await res.json();

        // 1. ЗАЩИТА ID: сохраняем TMDB ID, чтобы null с бэкенда его не убил
        const originalId = currentShowData.id;
        currentShowData = { ...currentShowData, ...details };
        currentShowData.id = originalId || details.id;

        // 2. ОБНОВЛЕНИЕ СТРАНЫ (исправили баг)
        if (countryEl) {
            const countries = details.production_countries || details.productionCountries || [];
            countryEl.innerText = `${t['country-label']}: ${formatList(countries)}`;
        }

        // 3. ОБНОВЛЕНИЕ ОЦЕНКИ
        const finalRating = details.vote_average || details.voteAverage;
        const ratingEl = document.getElementById('modal-rating');
        if (ratingEl) {
            ratingEl.innerText = `★ ${(finalRating && finalRating > 0) ? finalRating.toFixed(1) : 'NR'}`;
        }

        const seasonsCountEl = document.getElementById('modal-seasons-count');
        if (seasonsCountEl) seasonsCountEl.innerText = `${t['seasons-label']}: ${details.number_of_seasons || '...'}`;

        const genresCont = document.getElementById('tv-modal-genres') || document.getElementById('modal-genres');
        if (genresCont) genresCont.innerHTML = formatGenres(details.genres);

        if (details.overview) setupOverview('modal-overview', 'tv-read-more', details.overview);

        currentShowSeasons = details.seasons || [];
        if (typeof renderSeasonTabs === 'function') renderSeasonTabs();
        if (currentShowSeasons.length > 0 && typeof loadEpisodes === 'function') loadEpisodes(0);
    } catch (e) { console.error(e); }
}

function renderSeasonTabs() {
    const tabsContainer = document.getElementById('season-tabs');
    if (!tabsContainer) return;
    tabsContainer.innerHTML = currentShowSeasons.map((s, index) => `
        <button class="season-tab ${index === 0 ? 'active' : ''}"
                onclick="loadEpisodes(${index}, this)">
            Сезон ${s.season_number || s.seasonNumber}
        </button>
    `).join('');
}

function loadEpisodes(seasonIndex, tabElement = null) {
    const seasonObj = currentShowSeasons[seasonIndex];
    currentSelectedSeason = seasonObj.season_number || seasonObj.seasonNumber;
    currentSelectedEpisode = 0;
    const checkAll = document.getElementById('check-all-season');
    if (checkAll) checkAll.checked = false;

    if (tabElement) {
        document.querySelectorAll('.season-tab').forEach(t => t.classList.remove('active'));
        tabElement.classList.add('active');
    }

    const grid = document.getElementById('episodes-grid');
    const epCount = seasonObj.episode_count || seasonObj.episodeCount;

    if (epCount && epCount > 0) {
        let html = '';
        for (let i = 1; i <= epCount; i++) {
            html += `<button class="episode-btn" id="ep-btn-${i}" onclick="selectEpisode(${i})">${i}</button>`;
        }
        grid.innerHTML = html;
    }
}

function selectEpisode(epNum) {
    currentSelectedEpisode = epNum;
    const btns = document.querySelectorAll('.episode-btn');
    btns.forEach(btn => {
        const n = parseInt(btn.innerText);
        btn.classList.toggle('active', n <= epNum);
    });
    const checkAll = document.getElementById('check-all-season');
    if (checkAll) checkAll.checked = (epNum === btns.length && btns.length > 0);
}

function toggleAllEpisodes(checkbox) {
    const btns = document.querySelectorAll('.episode-btn');
    selectEpisode(checkbox.checked ? btns.length : 0);
}

function closeModal() {
    // Снимаем классы сразу со всех возможных модалок, чтобы наверняка
    const modals = ['tv-modal', 'movie-modal', 'progress-modal', 'full-grid-modal'];
    modals.forEach(m => {
        const el = document.getElementById(m);
        if (el) {
            el.classList.remove('active', 'show');
        }
    });
    document.body.classList.remove('modal-open');
    if (typeof checkBodyScroll === 'function') checkBodyScroll();
}

async function saveTvShowProgress() {
    if (currentSelectedEpisode === 0) {
        showToast("Оберіть хоча б одну серію!", true);
        return;
    }

    // Ищем кнопку сохранения (она может быть в разных модалках)
    const btn = document.querySelector('#tv-modal .btn-save') || document.querySelector('#progress-modal .btn-save');
    const oldText = btn ? btn.innerText : '...';
    if (btn) {
        btn.innerText = '⏳...';
        btn.style.pointerEvents = 'none';
    }

    // Достаем страну (из новых данных TMDB)
    let originCountry = "US";
    if (currentShowData.production_countries && currentShowData.production_countries.length > 0) {
        originCountry = currentShowData.production_countries[0].name || currentShowData.production_countries[0].iso_3166_1;
    } else if (currentShowData.origin_country && currentShowData.origin_country.length > 0) {
        originCountry = currentShowData.origin_country[0];
    }

    const payload = {
        // Шлем ID в двух форматах (как делали с фильмами), чтобы Spring 100% его съел
        tvShowId: currentShowData.id,
        tv_show_id: currentShowData.id,
        name: currentShowData.name || currentShowData.title,
        posterUrl: currentShowData.poster_path || currentShowData.posterPath,
        firstAirDate: currentShowData.first_air_date || currentShowData.firstAirDate,
        originCountry: originCountry,
        currentSeason: currentSelectedSeason,
        currentEpisode: currentSelectedEpisode
    };

    try {
        const res = await fetch(`${API_BASE_URL}/watchlist-tv-shows`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });

        if (res.status === 401 || res.status === 403) {
            window.location.href = '/login.html';
            return;
        }

        if (res.ok) {
            closeModal();
            showToast(translations[currentLang]['toast-success']);
        } else {
            showToast(translations[currentLang]['toast-error'], true);
        }
    } catch (e) {
        showToast("Error", true);
    } finally {
        if (btn) {
            btn.innerText = oldText;
            btn.style.pointerEvents = 'auto';
        }
    }
}

// --- ГЛОБАЛЬНЫЕ СЛУШАТЕЛИ ---
window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) closeModal();
});

window.addEventListener('scroll', () => {
    // Защита: бесконечный скролл только на главной
    if (!document.getElementById('load-status')) return;
    if (isLoading || currentPage >= totalPages) return;
    if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 600) {
        currentPage++;
        loadCatalog();
    }
});

document.addEventListener('DOMContentLoaded', () => {
    // Синхронизация языка при загрузке
    const langSelect = document.getElementById('langSelect');
    if (langSelect) langSelect.value = currentLang;

    const slider = document.getElementById('trending-slider');
    if (slider) {
        slider.addEventListener('scroll', updateNavButtons);
        loadTrending();
        loadCatalog();
    }
});

function formatList(list) {
    if (!list || !Array.isArray(list) || list.length === 0) return '...';
    return list.map(item => typeof item === 'object' ? item.name : item).join(', ');
}

function formatGenres(genres) {
    if (!genres || !Array.isArray(genres) || genres.length === 0) return '';
    return genres.map(g => `<span class="genre-tag">${g.name || g}</span>`).join('');
}

function toggleOverview(textId, btn) {
    const el = document.getElementById(textId);
    if (!el) return;
    const isCollapsed = el.classList.contains('collapsed');
    el.classList.toggle('collapsed', !isCollapsed);
    el.classList.toggle('expanded', isCollapsed);
    btn.innerText = isCollapsed ? 'Згорнути' : 'Читати далі';
}

function setupOverview(textId, btnId, text) {
    const el = document.getElementById(textId);
    const btn = document.getElementById(btnId);
    if (!el || !btn) return;
    el.innerText = text || 'Опис відсутній.';
    el.className = 'modal-overview-text collapsed';
    btn.innerText = 'Читати далі';
    btn.style.display = 'none';
    setTimeout(() => {
        if (el.scrollHeight > el.clientHeight) btn.style.display = 'inline-block';
    }, 10);
}

// --- ЖИВОЙ ПОИСК ---
document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('search-input');
    const dropdown = document.getElementById('search-dropdown');
    const results = document.getElementById('search-results');
    const spinner = document.getElementById('search-spinner');
    const btnAll = document.getElementById('btn-view-all-search');

    let timeout = null;
    if (input) {
        input.addEventListener('input', (e) => {
            const q = e.target.value.trim();
            clearTimeout(timeout);
            if (q.length < 2) {
                dropdown.classList.add('hidden');
                spinner.classList.add('hidden');
                return;
            }
            spinner.classList.remove('hidden');
            timeout = setTimeout(async () => {
                try {
                    const res = await fetch(`/search/live?query=${encodeURIComponent(q)}&language=${currentLang}`);
                    const data = await res.json();
                    renderDropdown(data, q);
                } catch (e) {
                    console.error(e);
                } finally {
                    spinner.classList.add('hidden');
                }
            }, 500);
        });
    }

    function renderDropdown(items, query) {
        if (!results || !dropdown || !btnAll) return;
        results.innerHTML = '';
        if (!items || items.length === 0) {
            results.innerHTML = '<div class="search-no-results">Нічого не знайдено</div>';
            btnAll.classList.add('hidden');
        } else {
            items.forEach(item => {
                const type = item.media_type || item.mediaType;
                const poster = item.poster_url || item.posterUrl;
                const year = item.release_year || item.releaseYear;
                const title = item.title || item.name || 'Без назви';
                const img = poster ? `https://image.tmdb.org/t/p/w92${poster}` : 'https://placehold.co/50x75/333333/ffffff?text=No+Image';

                const div = document.createElement('div');
                div.className = 'search-item';
                div.onclick = () => {
                    dropdown.classList.add('hidden');
                    const comp = {
                        id: item.id, title, name: title,
                        poster_path: poster ? poster.replace('https://image.tmdb.org/t/p/w92', '') : null,
                        release_date: year ? `${year}-01-01` : null,
                        first_air_date: year ? `${year}-01-01` : null,
                        vote_average: 0, overview: 'Завантаження...'
                    };
                    type === 'movie' ? openMovieModal(comp, div) : openTvModal(comp);
                };
                div.innerHTML = `
                    <img src="${img}" alt="${title}" class="search-item-img" onerror="this.src='https://placehold.co/50x75/333333/ffffff?text=No+Image'">
                    <div class="search-item-info">
                        <div class="search-item-title">${title}</div>
                        <div class="search-item-meta">${type === 'movie' ? 'Фільм' : 'Серіал'} • ${year || 'N/A'}</div>
                        <div class="search-item-genres">${(item.genres || []).join(', ')}</div>
                    </div>`;
                results.appendChild(div);
            });
            btnAll.classList.remove('hidden');
            btnAll.dataset.query = query;
        }
        dropdown.classList.remove('hidden');
    }
});

function goToSearchPage() {
    const btn = document.getElementById('btn-view-all-search');
    if (btn && btn.dataset.query) window.location.href = `/search.html?query=${encodeURIComponent(btn.dataset.query)}`;
}

function applyTranslations() {
    const lang = currentLang;
    const dictionary = translations[lang];

    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (dictionary[key]) {
            if (el.tagName === 'INPUT') {
                el.placeholder = dictionary[key];
            } else {
                el.innerText = dictionary[key];
            }
        }
    });
}

function checkBodyScroll() {
    if (document.querySelectorAll('.modal-overlay.active, .modal-overlay.show').length === 0) {
        document.body.classList.remove('modal-open');
    }
}