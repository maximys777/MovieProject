const API_BASE_URL = '';
const IMG_500 = 'https://image.tmdb.org/t/p/w500';

let currentLang = 'ru';
let loadedMovies = {};
let loadedTvShows = {};
let activeModalData = null;

// ФЛАГ БЕЗОПАСНОСТИ: В будущем, если смотришь чужой профиль - ставь false
let isMyProfile = true;

// Пагинация для модалки "Всі збережені"
let gridType = '';
let gridPage = 0;
let gridTotalPages = 1;
let isGridLoading = false;

document.addEventListener('DOMContentLoaded', () => {
    const langSelect = document.getElementById('langSelect');
    if (langSelect) currentLang = langSelect.value;
    loadDashboard();
});

function changeLanguage(lang) {
    currentLang = lang;
    loadDashboard();
}

function showToast(message, isError = false) {
    const toast = document.getElementById('custom-toast');
    toast.innerText = message;
    toast.className = 'toast-notification show';
    if (isError) toast.classList.add('error');
    setTimeout(() => toast.classList.remove('show'), 3000);
}

function scrollSlider(id, amount) {
    document.getElementById(id).scrollBy({left: amount, behavior: 'smooth'});
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

// =======================
// 1. ЗАГРУЗКА ДАШБОРДА (КАРУСЕЛИ)
// =======================
async function loadDashboard() {
    fetchAndRender(`${API_BASE_URL}/watchlist-movies/me?page=0&size=20&language=${currentLang}`, 'movies-slider', true);
    fetchAndRender(`${API_BASE_URL}/watchlist-tv-shows/me?page=0&size=20&language=${currentLang}`, 'tv-shows-slider', false);
}

async function fetchAndRender(url, containerId, isMovie) {
    try {
        const response = await fetch(url);
        if (response.redirected && response.url.includes('/login.html')) return window.location.href = '/login.html';
        if (!response.ok) return;

        const data = await response.json();
        const items = data.content || [];
        const total = data.page?.totalElements || data.totalElements || 0;

        document.getElementById(isMovie ? 'total-movies' : 'total-tv').innerText = total;

        const container = document.getElementById(containerId);
        container.innerHTML = '';

        if (items.length === 0) {
            container.innerHTML = `<div class="empty-state" style="width:100%"><h3>Тут поки порожньо</h3></div>`;
            return;
        }

        items.forEach(item => {
            const id = isMovie ? item.movieId : item.tvShowId;
            const title = isMovie ? item.title : item.name;
            if (isMovie) loadedMovies[id] = item; else loadedTvShows[id] = item;

            const badge = !isMovie ? `<div style="position:absolute; top:8px; left:8px; background:var(--primary-color); color:white; padding:4px 8px; border-radius:6px; font-size:12px; font-weight:bold;">S${item.currentSeason} E${item.currentEpisode}</div>` : '';

            container.insertAdjacentHTML('beforeend', `
                <div class="movie-card" onclick="openItemModal(${id}, ${isMovie})">
                    <div style="position:relative">
                        <img src="${IMG_500}${item.posterUrl || item.posterPath || ''}" onerror="this.src='https://via.placeholder.com/500x750?text=No+Image'">
                        ${badge}
                    </div>
                    <div class="movie-card-body">
                        <div class="movie-title" title="${title}">${title}</div>
                        <button class="btn-detail-card">Детально</button>
                    </div>
                </div>
            `);
        });
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
    activeModalData = { ...data, isMovie };

    if (isMovie) {
        document.getElementById('movie-modal-title').innerText = data.title;
        document.getElementById('movie-modal-poster').src = data.posterUrl ? `${IMG_500}${data.posterUrl}` : '';
        document.getElementById('movie-modal-year').innerText = `Рік: ${data.releaseDate ? data.releaseDate.substring(0,4) : '...'}`;

        // НОВЫЕ ПОЛЯ ФИЛЬМА
        document.getElementById('movie-modal-country').innerText = `Країна: ${formatList(data.productionCountries)}`;
        document.getElementById('movie-modal-runtime').innerText = data.runtime ? `${data.runtime} хв` : '...';
        document.getElementById('movie-modal-genres').innerHTML = formatGenres(data.genres);
        document.getElementById('movie-modal-rating').innerHTML = `★ ${data.voteAverage ? data.voteAverage.toFixed(1) : 'NR'}`;

        // Умное описание
        setupOverview('movie-modal-overview', 'movie-read-more', data.overview);

        const deleteBtn = document.getElementById('btn-delete-movie');
        deleteBtn.style.display = isMyProfile ? 'block' : 'none';
        deleteBtn.onclick = () => deleteItem(data.movieId, true);

        document.getElementById('movie-modal').classList.add('show');
        document.body.classList.add('modal-open');
    } else {
        document.getElementById('modal-title').innerText = data.name;
        document.getElementById('modal-poster').src = data.posterUrl ? `${IMG_500}${data.posterUrl}` : '';
        document.getElementById('modal-year').innerText = `Рік: ${data.firstAirDate ? data.firstAirDate.substring(0,4) : '...'}`;

        // НОВЫЕ ПОЛЯ СЕРИАЛА
        // Используем fallbacks, если в старых данных поля назывались иначе
        const countries = data.productionCountries || data.originCountry || [];
        document.getElementById('modal-country').innerText = `Країна: ${formatList(Array.isArray(countries) ? countries : [countries])}`;
        document.getElementById('modal-genres').innerHTML = formatGenres(data.genres);
        document.getElementById('modal-rating').innerHTML = `★ ${data.voteAverage ? data.voteAverage.toFixed(1) : 'NR'}`;

        document.getElementById('modal-status-text').innerText = `Сезон ${data.currentSeason}, Серія ${data.currentEpisode}`;

        // Умное описание
        setupOverview('modal-overview', 'tv-read-more', data.overview);

        renderProgressSeasons(data);

        const deleteBtn = document.getElementById('btn-delete-tv');
        deleteBtn.style.display = isMyProfile ? 'block' : 'none';
        deleteBtn.onclick = () => deleteItem(data.tvShowId, false);

        document.getElementById('progress-modal').classList.add('show');
        document.body.classList.add('modal-open');
    }
}

function renderProgressSeasons(data) {
    const tabsContainer = document.getElementById('season-tabs');
    tabsContainer.innerHTML = '';
    const seasonsList = data.seasons || [];

    seasonsList.forEach(seasonObj => {
        const sNum = seasonObj.season_number ?? seasonObj.seasonNumber;
        const totalEps = seasonObj.episode_count ?? seasonObj.episodeCount ?? 10;

        const btn = document.createElement('button');
        btn.className = 'season-tab';
        btn.innerText = `Сезон ${sNum}`;

        const isFullyWatched = sNum < data.currentSeason || (sNum === data.currentSeason && data.currentEpisode >= totalEps);
        if (isFullyWatched) btn.classList.add('watched');
        if (sNum === data.currentSeason) {
            btn.classList.add('active');
            renderProgressEpisodes(sNum, data);
        }

        btn.onclick = () => {
            document.querySelectorAll('.season-tab').forEach(t => t.classList.remove('active'));
            btn.classList.add('active');
            renderProgressEpisodes(sNum, data);
        };
        tabsContainer.appendChild(btn);
    });
}

function renderProgressEpisodes(sNum, data) {
    const grid = document.getElementById('episodes-grid');
    grid.innerHTML = '';

    const titleEl = document.getElementById('episodes-title');
    if (titleEl) titleEl.innerText = `Епізоди ${sNum} сезону`;

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
    if (!confirm("Ви впевнені, що хочете видалити це зі списку?")) return;

    const endpoint = isMovie ? `/watchlist-movies?movieId=${id}` : `/watchlist-tv-shows?tvShowId=${id}`;

    try {
        const response = await fetch(API_BASE_URL + endpoint, {method: 'DELETE'});
        if (response.ok) {
            showToast("Успішно видалено!");
            closeModal(isMovie ? 'movie-modal' : 'progress-modal');
            loadDashboard();

            if (document.getElementById('full-grid-modal').classList.contains('show')) {
                openFullGrid(gridType);
            }
        } else {
            showToast("Помилка видалення", true);
        }
    } catch (e) {
        showToast("Помилка сервера", true);
    }
}

// =======================
// 4. ПОЛНАЯ КАРТОТЕКА (GRID MODAL)
// =======================
function openFullGrid(type) {
    gridType = type;
    gridPage = 0;
    document.getElementById('full-grid-content').innerHTML = '';
    document.getElementById('full-grid-title').innerText = type === 'movies' ? 'Всі збережені фільми' : 'Всі збережені серіали';
    document.getElementById('full-grid-modal').classList.add('show');
    document.body.classList.add('modal-open');
    loadGridPage();
}

async function loadGridPage() {
    if (isGridLoading) return;
    isGridLoading = true;
    document.getElementById('grid-load-status').innerText = 'Завантаження...';

    const isMovie = gridType === 'movies';
    const endpoint = isMovie ? `/watchlist-movies/me` : `/watchlist-tv-shows/me`;

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}?page=${gridPage}&size=20&language=${currentLang}`);
        if (!response.ok) return;

        const data = await response.json();
        const items = data.content || [];
        gridTotalPages = data.page?.totalPages || data.totalPages || 1;

        const container = document.getElementById('full-grid-content');

        items.forEach(item => {
            const id = isMovie ? item.movieId : item.tvShowId;
            const title = isMovie ? item.title : item.name;
            if (isMovie) loadedMovies[id] = item; else loadedTvShows[id] = item;

            const badge = !isMovie ? `<div style="position:absolute; top:8px; left:8px; background:var(--primary-color); color:white; padding:4px 8px; border-radius:6px; font-size:12px; font-weight:bold;">S${item.currentSeason} E${item.currentEpisode}</div>` : '';

            container.insertAdjacentHTML('beforeend', `
                <div class="movie-card" onclick="openItemModal(${id}, ${isMovie})">
                    <div style="position:relative">
                        <img src="${IMG_500}${item.posterUrl || item.posterPath || ''}" onerror="this.src='https://via.placeholder.com/500x750?text=No+Image'">
                        ${badge}
                    </div>
                    <div class="movie-card-body">
                        <div class="movie-title" title="${title}">${title}</div>
                        <button class="btn-detail-card">Детально</button>
                    </div>
                </div>
            `);
        });

        document.getElementById('grid-load-status').innerText = gridPage >= gridTotalPages - 1 ? 'Кінець списку' : '';
    } catch (e) {
        document.getElementById('grid-load-status').innerText = 'Помилка';
    } finally {
        isGridLoading = false;
    }
}

document.getElementById('grid-scroll-container').addEventListener('scroll', function () {
    if (isGridLoading || gridPage >= gridTotalPages - 1) return;
    if (this.scrollHeight - this.scrollTop <= this.clientHeight + 200) {
        gridPage++;
        loadGridPage();
    }
});

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
    checkBodyScroll(); // Проверяем, можно ли вернуть скролл
}

// Закрытие при клике на темный фон (в пустоту)
window.addEventListener('click', (e) => {
    // Если кликнули ровно по оверлею (а не по контенту внутри)
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('show'); // Закрываем ту модалку, по которой кликнули
        checkBodyScroll();
    }
});

// Проверка: остались ли еще открытые модалки?
function checkBodyScroll() {
    // Если открытых модалок больше нет - возвращаем скролл главной странице
    if (document.querySelectorAll('.modal-overlay.show').length === 0) {
        document.body.classList.remove('modal-open');
    }
}

// Помощник для красивого вывода списков (стран)
function formatList(list) {
    if (!list || !Array.isArray(list) || list.length === 0) return '...';
    return list.join(', ');
}

// Помощник для создания плашек жанров
function formatGenres(genres) {
    if (!genres || !Array.isArray(genres) || genres.length === 0) return '';
    return genres.map(g => `<span class="genre-tag">${g}</span>`).join('');
}

// Логика разворачивания текста
function toggleOverview(textId, btn) {
    const textEl = document.getElementById(textId);
    if (textEl.classList.contains('collapsed')) {
        textEl.classList.remove('collapsed');
        textEl.classList.add('expanded');
        btn.innerText = 'Згорнути';
    } else {
        textEl.classList.remove('expanded');
        textEl.classList.add('collapsed');
        btn.innerText = 'Читати далі';
    }
}

// Умная настройка описания (скрывает кнопку, если текст короткий)
function setupOverview(textId, btnId, text) {
    const textEl = document.getElementById(textId);
    const btnEl = document.getElementById(btnId);

    textEl.innerText = text || 'Опис відсутній.';
    textEl.classList.remove('expanded');
    textEl.classList.add('collapsed');
    btnEl.innerText = 'Читати далі';
    btnEl.style.display = 'none'; // Прячем кнопку по умолчанию

    // Ждем миллисекунду, пока браузер отрисует текст, и проверяем его реальную высоту
    setTimeout(() => {
        if (textEl.scrollHeight > textEl.clientHeight) {
            btnEl.style.display = 'inline-block'; // Показываем кнопку, если текст не влез
        }
    }, 10);
}