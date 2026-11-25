# couple_quest_board_backend

真實人生遊戲感任務版（情侶共用的人生目標任務版）的後端 API。

規格與開發計畫以 Claude 專案中的「專案規格書」與「後端企劃書」為準。

## 技術

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2（Spring MVC、Spring Data JPA、Bean Validation） |
| 建置 | Maven（使用 `./mvnw`，不需另外安裝 Maven） |
| 資料庫 | MySQL 8.4、Flyway |
| 測試 | JUnit 5、MockMvc、Testcontainers |
| 容器 | Docker、Docker Compose |

## 需求

- JDK 21
- Docker（跑整合測試與 Compose）

## 本機開發

```bash
cp .env.example .env     # 範本預設即為本機開發設定
docker compose up -d db  # 起資料庫（127.0.0.1:3306）
./mvnw spring-boot:run   # 啟動 app（http://localhost:3000），啟動時自動套用 Flyway migration
```

`.env` 中的 `COMPOSE_FILE` 會讓 `docker compose` 自動疊加 `docker-compose.dev.yml`。app 直接在本機啟動時也會讀取 `.env`。

開發模式（`AUTH_MODE=dev`）下，所有請求都視為 `DEV_USER_EMAIL` 這位使用者，且啟動時自動建立該帳號：

```bash
curl http://localhost:3000/api/auth/me
```

以正式 image 整合驗證：

```bash
docker compose up --build
```

## 測試

```bash
./mvnw verify
```

整合測試以 Testcontainers 啟動 `mysql:8.4`。沒有 Docker 時，可指向既有的 MySQL（會清空該資料庫的資料，請使用專用的測試資料庫）：

```bash
TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/cqb_test?connectionTimeZone=Asia/Taipei' \
TEST_DB_USER=root TEST_DB_PASSWORD=xxx ./mvnw verify
```

## 環境變數

| 變數 | 說明 |
|---|---|
| `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD` | 資料庫連線 |
| `AUTH_MODE` | `access`（預設，驗證 Cloudflare Access JWT）或 `dev`（固定使用者，僅限本機） |
| `DEV_USER_EMAIL`、`DEV_USER_NAME` | 開發模式的固定使用者；`AUTH_MODE=dev` 時 `DEV_USER_EMAIL` 必填 |
| `ACCESS_TEAM_DOMAIN`、`ACCESS_AUD` | Cloudflare Access 團隊網域與 Audience Tag（B4 起使用） |

## 專案結構

```
src/main/java/com/hannzer/cqb/
├── CqbApplication.java   進入點
├── auth/                 登入（開發模式、Access JWT）、CurrentUser
├── common/error/         統一錯誤格式 { code, message }
├── common/validation/    自訂驗證（日期格式）
├── common/health/        /api/health
├── areas/                領域（頁籤）：新增、改名、排序、隱藏／恢復顯示
├── goals/                目標與子任務：狀態自動轉換、進度計算、目標列鎖
└── users/                app_user
src/main/resources/db/migration/   Flyway migration（既有檔案合併後不再修改）
deploy/                   備份腳本、使用者 seed 範本
```

## 使用者與備份

- 新增使用者：複製 `deploy/seed-users.sql.example` 為 `deploy/seed-users.sql`（不進版控），填入 email 後執行檔案內的指令
- 備份：`./deploy/backup.sh` 匯出到 `backups/`，保留 14 天；排程方式見腳本開頭
