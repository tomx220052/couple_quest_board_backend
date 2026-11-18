#!/usr/bin/env bash
# 備份資料庫（規格書 7.5）：匯出到 backups/，保留 14 天
# 排程範例（crontab -e）：
#   0 3 * * * cd /path/to/couple_quest_board_backend && ./deploy/backup.sh >> backups/backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")/.."

BACKUP_DIR="backups"
RETENTION_DAYS=14
mkdir -p "$BACKUP_DIR"

file="$BACKUP_DIR/cqb-$(date +%Y%m%d-%H%M%S).sql.gz"

docker compose exec -T db sh -c \
  'exec mysqldump --single-transaction --routines --triggers -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  | gzip > "$file.tmp"
mv "$file.tmp" "$file"

find "$BACKUP_DIR" -name 'cqb-*.sql.gz' -mtime +"$RETENTION_DAYS" -delete

echo "$(date '+%F %T') 備份完成：$file"
