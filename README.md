# Разработка СУБД для классификации изображений с использованием нейронной сети

## Описание проекта

Данный проект представляет собой систему управления базами данных (СУБД), предназначенную для классификации изображений с применением нейронной сети. Система позволяет загружать фотографии с рабочего места пользователя, обрабатывать их нейросетью, сохранять результаты и анализировать точность предсказаний.

## Основной функционал
1. **Загрузка фотографий**
   - Пользователь загружает изображение.
   - Фотография заносится в БД хранения фотографий.

2. **Обработка нейросетью**
   - Загруженное изображение передается в нейросеть.
   - Нейросеть выполняет предсказание класса изображения.
   - Результаты заносятся в БД предсказаний.

3. **Анализ точности**
   - Предсказание сравнивается с корректной меткой.
   - В случае ошибки запись помечается как ошибочная и заносится в БД исправлений.
   - Если предсказание верно, оно остается в БД предсказаний.

## Структура базы данных
- **БД хранения фотографий** – содержит загруженные пользователем изображения.
- **БД предсказаний** – хранит результаты классификации изображений.
- **БД исправлений** – содержит ошибочные предсказания с исправленными метками.

## Используемые технологии
- **Язык программирования**: Java (Swing для GUI)
- **База данных**: PostgreSQL
- **Контейнеризация**: Docker
- **Нейросеть**: (уточнить используемую архитектуру)

## Установка и запуск
1. Установить **PostgreSQL** и создать необходимые таблицы.
2. Установить **Docker** и запустить контейнер с базой данных.
3. Скомпилировать и запустить Java-приложение.
4. Загрузить изображение и выполнить классификацию.
