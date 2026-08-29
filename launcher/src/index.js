#!/usr/bin/env node
'use strict';

/**
 * FestVisuals Launcher
 *
 * Минимальный кроссплатформенный лаунчер (только встроенные модули Node.js,
 * без npm install). Задачи:
 *   1. Спросить/прочитать конфиг (tier: free | paid, license key для paid).
 *   2. Найти последний релиз мода на GitHub (через Releases API).
 *   3. Скачать нужный jar (free.jar или paid.jar) и положить его в mods/.
 *
 * Запуск самой игры выполняется штатным лаунчером Minecraft/Fabric —
 * этот инструмент отвечает только за доставку и обновление мода.
 */

const fs = require('fs');
const path = require('path');
const https = require('https');
const os = require('os');

const CONFIG_PATH = path.join(__dirname, '..', 'config.json');
const EXAMPLE_CONFIG_PATH = path.join(__dirname, '..', 'config.example.json');

function loadConfig() {
  if (!fs.existsSync(CONFIG_PATH)) {
    fs.copyFileSync(EXAMPLE_CONFIG_PATH, CONFIG_PATH);
    console.log(`Создан config.json из шаблона. Заполните его и запустите снова: ${CONFIG_PATH}`);
    process.exit(0);
  }
  return JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf8'));
}

function defaultModsDir() {
  const platform = os.platform();
  const home = os.homedir();
  if (platform === 'win32') {
    return path.join(process.env.APPDATA || path.join(home, 'AppData', 'Roaming'), '.minecraft', 'mods');
  }
  if (platform === 'darwin') {
    return path.join(home, 'Library', 'Application Support', 'minecraft', 'mods');
  }
  return path.join(home, '.minecraft', 'mods');
}

function httpsGetJson(url) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'User-Agent': 'FestVisuals-Launcher' } }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return resolve(httpsGetJson(res.headers.location));
      }
      if (res.statusCode !== 200) {
        return reject(new Error(`GitHub API вернул статус ${res.statusCode} для ${url}`));
      }
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          reject(e);
        }
      });
    }).on('error', reject);
  });
}

function downloadFile(url, destPath) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(destPath);
    https.get(url, { headers: { 'User-Agent': 'FestVisuals-Launcher' } }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        file.close();
        fs.unlinkSync(destPath);
        return resolve(downloadFile(res.headers.location, destPath));
      }
      if (res.statusCode !== 200) {
        file.close();
        return reject(new Error(`Не удалось скачать файл, статус ${res.statusCode}`));
      }
      res.pipe(file);
      file.on('finish', () => file.close(resolve));
    }).on('error', reject);
  });
}

/**
 * Проверка лицензии для платной версии — заглушка.
 * В реальном проекте здесь должен быть запрос к вашему бэкенду
 * (сервер, который валидирует licenseKey и выдаёт подписанный токен/URL).
 * Сейчас функция ничего не проверяет по-настоящему и просто блокирует
 * скачивание paid-сборки, если ключ не указан.
 */
function assertLicense(config) {
  if (config.tier === 'paid' && !config.licenseKey) {
    throw new Error(
      'Для платной версии нужен licenseKey в config.json. ' +
      'Реальная проверка лицензии должна выполняться на вашем сервере ' +
      '— эта заглушка её не делает.'
    );
  }
}

async function main() {
  const config = loadConfig();
  assertLicense(config);

  const modsDir = config.modsDir === 'auto' ? defaultModsDir() : config.modsDir;
  fs.mkdirSync(modsDir, { recursive: true });

  console.log(`Ищу последний релиз ${config.githubOwner}/${config.githubRepo}...`);
  const release = await httpsGetJson(
    `https://api.github.com/repos/${config.githubOwner}/${config.githubRepo}/releases/latest`
  );

  const assetName = config.tier === 'paid' ? 'festvisuals-paid.jar' : 'festvisuals-free.jar';
  const asset = (release.assets || []).find((a) => a.name === assetName);

  if (!asset) {
    throw new Error(`В релизе ${release.tag_name} не найден файл ${assetName}`);
  }

  const destPath = path.join(modsDir, assetName);
  console.log(`Скачиваю ${asset.name} (${release.tag_name}) в ${destPath}...`);
  await downloadFile(asset.browser_download_url, destPath);

  console.log('Готово! Запустите Minecraft через Fabric-профиль — мод уже установлен.');
}

main().catch((err) => {
  console.error('Ошибка:', err.message);
  process.exit(1);
});
