# Code Signing (Отключение SmartScreen)

Чтобы Windows не ругался на установщик лаунчера («Неопознанное приложение»), его нужно подписать Code Signing сертификатом.
Я уже подготовил package.json для сборки.

## Что нужно сделать:
1. Приобрести сертификат подписи кода (Code Signing Certificate) у доверенного центра сертификации (например, Sectigo, DigiCert).
2. Поместить файл сертификата (обычно .pfx или .p12) в папку d:\FestVisuals\launcher\build\cert.pfx.
3. В файле d:\FestVisuals\launcher\package.json в разделе uild.win заменить your_cert_password_here на пароль от твоего сертификата.
4. Запустить 
pm run dist или 
pm run pack. electron-builder автоматически подпишет установщик и все DLL файлы.

Если ты используешь аппаратный токен (EV сертификат) или Azure Key Vault, настройка будет немного другой — нужно будет использовать переменные окружения WIN_CSC_LINK или утилиту SignTool.
