# MediCall AI

医療機関向けAI電話応答システム。Amazon Connect 経由の着信を OpenAI で処理し、職員向け Web 管理画面で通話履歴・FAQ・予約を管理します。

## アーキテクチャ

```
Amazon Connect → Spring Boot API → PostgreSQL (pgvector)
                      ↓
              OpenAI / Google Speech / Amazon Polly
                      ↓
              Next.js 管理画面 (職員)
```

## 機能

| 機能 | 説明 |
|------|------|
| 自動応答 | 診療時間・休診日・アクセス・持ち物を案内 |
| 予約受付 | 新規・変更・キャンセル（本人確認後） |
| 本人確認 | 氏名・生年月日・電話番号 |
| 要件分類 | 予約/検査/会計/薬/紹介状などに振分け |
| 緊急判定 | 胸痛・意識障害・呼吸困難は即転送（AI非対応） |
| 人への転送 | 苦情・医療判断・判断不能時 |
| 通話要約 | OpenAI による要約を職員画面に表示 |
| 履歴管理 | 全会話ターンを記録 |
| FAQ管理 | 医院側が回答文を編集可能 |
| 禁止応答制御 | 診断・処方・重症度判断をブロック |

## クイックスタート

### 前提

- Docker / Docker Compose
- Java 21 + Maven（ローカル開発時）
- Node.js 20（フロント開発時）

### 起動

```bash
cd docker
docker compose up -d postgres
# API（別ターミナル）
cd apps/api && mvn spring-boot:run
# Web（別ターミナル）
cd apps/web && npm install && npm run dev
```

- API: http://localhost:8080
- 管理画面: http://localhost:3000
- PostgreSQL: localhost:5434

### 環境変数

| 変数 | 説明 |
|------|------|
| `OPENAI_API_KEY` | OpenAI API キー |
| `DEV_MODE` | `true` で OAuth2 無効（開発用） |
| `OAUTH2_ISSUER_URI` | Azure AD / Entra ID 等の Issuer |
| `POLLY_ENABLED` | Amazon Polly 音声合成 |
| `GOOGLE_SPEECH_ENABLED` | Google Speech 音声認識 |
| `AWS_REGION` | AWS リージョン |

## API エンドポイント

### Amazon Connect Webhook

- `POST /api/connect/start` — 通話開始
- `POST /api/connect/utterance` — 発話処理
- `POST /api/connect/end` — 通話終了・要約生成

### 管理画面 API

- `GET /api/admin/calls` — 通話一覧
- `GET /api/admin/calls/{id}/turns` — 会話ログ
- `GET/POST/PUT/DELETE /api/admin/faq` — FAQ CRUD
- `GET/PUT /api/admin/clinic` — 医院設定
- `GET /api/admin/appointments` — 予約一覧

## Railway デプロイ

リポジトリ直下の `Dockerfile` + `railway.toml` で **Next.js + Spring Boot を1サービス** としてデプロイします。

### 設定手順

1. Railway で Postgres サービスを追加
2. アプリサービスの Variables に以下を設定:
   - `DATABASE_URL` = `${{Postgres.DATABASE_URL}}`
   - `OPENAI_API_KEY` = （任意）
   - `DEV_MODE` = `true`（OAuth2 無効、本番では `false` + `OAUTH2_ISSUER_URI`）
3. **Root Directory** は空のまま（リポジトリルート）
4. **Config file path** = `/railway.toml`

### 注意

- pgvector 拡張が必要です。Railway Postgres で `CREATE EXTENSION vector;` が使えない場合は pgvector 対応の DB を使用してください。
- ヘルスチェック: `/api/health`

## インフラ

- `infra/terraform/` — AWS VPC, RDS PostgreSQL, ECS, CloudWatch
- `.github/workflows/ci.yml` — API / Web / Terraform CI

## ライセンス

Proprietary
