-- 規格書第 5 節：Phase one 資料表
-- DATETIME 一律存 Asia/Taipei 時間（連線時區 +08:00）

CREATE TABLE app_user (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  email         VARCHAR(255) NOT NULL,         -- 與 Google 登入的 email 相同；定序不分大小寫
  display_name  VARCHAR(50)  NOT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_user_email UNIQUE (email)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE domain (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(50)  NOT NULL,
  sort_order  INT          NOT NULL,          -- 新增時放在最後；編輯領域可調整
  hidden      BOOLEAN      NOT NULL DEFAULT FALSE,
  created_by  BIGINT       NOT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_domain_user FOREIGN KEY (created_by) REFERENCES app_user (id),
  CONSTRAINT uk_domain_name UNIQUE (name)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE goal (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  domain_id     BIGINT       NOT NULL,
  title         VARCHAR(200) NOT NULL,
  due_date      DATE         NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE / COMPLETED / ARCHIVED
  completed_at  DATETIME     NULL,
  archived_at   DATETIME     NULL,
  created_by    BIGINT       NOT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_goal_domain FOREIGN KEY (domain_id)  REFERENCES domain (id),
  CONSTRAINT fk_goal_user   FOREIGN KEY (created_by) REFERENCES app_user (id),
  INDEX idx_goal_domain_status (domain_id, status)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE subtask (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  goal_id     BIGINT           NOT NULL,
  title       VARCHAR(200)     NOT NULL,
  progress    TINYINT UNSIGNED NOT NULL DEFAULT 0,
  created_by  BIGINT           NOT NULL,
  created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_subtask_goal FOREIGN KEY (goal_id)    REFERENCES goal (id) ON DELETE CASCADE,
  CONSTRAINT fk_subtask_user FOREIGN KEY (created_by) REFERENCES app_user (id),
  CONSTRAINT chk_subtask_progress CHECK (progress IN (0, 20, 40, 60, 80, 100)),
  INDEX idx_subtask_goal (goal_id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
