// Подставьте свои owner/repo — используется для ссылки на последний релиз.
const GITHUB_OWNER = 'YOUR_GITHUB_USERNAME';
const GITHUB_REPO = 'FestVisuals';

async function wireDownloadButtons() {
  try {
    const res = await fetch(`https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`);
    if (!res.ok) return;
    const release = await res.json();

    const freeAsset = (release.assets || []).find(a => a.name === 'festvisuals-free.jar');
    const paidAsset = (release.assets || []).find(a => a.name === 'festvisuals-paid.jar');

    const freeBtn = document.getElementById('download-free');
    if (freeAsset && freeBtn) freeBtn.href = freeAsset.browser_download_url;

    // Платная версия не должна раздаваться напрямую с публичного релиза —
    // здесь должна быть ссылка на страницу оплаты/выдачи лицензии.
    const paidBtn = document.getElementById('download-paid');
    if (paidBtn) paidBtn.href = '#paid-checkout';
  } catch (e) {
    console.warn('Не удалось получить данные о последнем релизе:', e);
  }
}

wireDownloadButtons();
