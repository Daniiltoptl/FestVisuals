<div class="settings-grid">
    <div class="glass-panel">
        <h2>Оперативная память</h2>
        <p>Вы можете указать нужное для вас количество оперативной памяти, которое будет выделено для клиента</p>
        <div class="memory-slider">
            <div class="slider-container">
                <div class="slider-track">
                    <div class="slider-fill"></div>
                </div>
            </div>
            <div class="memory-input">
                <input type="number" value="2560">
                <span>МБ</span>
            </div>
        </div>
    </div>

    <div class="glass-panel">
        <h2>Расположение</h2>
        <p>Место в системе, где будут храниться все необходимые файлы для работы лаунчера и клиентов</p>
        <div class="location-input">
            <input type="text" value="C:/FestVisuals" readonly>
            <button class="icon-button refresh">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.3"/></svg>
            </button>
            <button class="icon-button folder">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z"/></svg>
            </button>
            <button class="icon-button edit">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </button>
        </div>
    </div>

    <div class="glass-panel">
        <h2>Окно игры</h2>
        <p>Вы можете указать свой желаемый размер окна, который будет установлен по умолчанию при запуске клиента</p>
        <div class="window-size">
            <div>
                <label>Ширина</label>
                <input type="number" value="925">
            </div>
            <div>
                <label>Высота</label>
                <input type="number" value="530">
            </div>
        </div>
    </div>

    <div class="glass-panel">
        <h2>Другое</h2>
        <div class="settings-toggles">
            <div class="toggle-row">
                <span>Полноэкранный режим</span>
                <label class="switch">
                    <input type="checkbox">
                    <span class="slider"></span>
                </label>
            </div>
            <div class="toggle-row">
                <span>Переустановить клиент</span>
                <label class="switch">
                    <input type="checkbox">
                    <span class="slider"></span>
                </label>
            </div>
            <div class="toggle-row">
                <span>Режим отладки</span>
                <label class="switch">
                    <input type="checkbox">
                    <span class="slider"></span>
                </label>
            </div>
        </div>
    </div>
</div>