'use client';

/**
 * 画面右下のドラッグ可能な利用手順パネル（localStorage で位置・開閉を保存）。
 * MediCall AI — アーキテクチャ・通話フロー・管理画面操作を表示。
 */
import { useCallback, useEffect, useRef, useState } from 'react';

const STORAGE_KEY = 'medicall-usage-guide-v1';
const PANEL_WIDTH = 440;

type GuideStep = {
  title: string;
  body: string;
  items?: readonly string[];
};

type FeaturedBlock = {
  badge: string;
  title: string;
  body: string;
  items?: readonly string[];
  variant?:
    | 'architecture'
    | 'callflow'
    | 'auto'
    | 'appointment'
    | 'emergency'
    | 'admin'
    | 'connect'
    | 'security'
    | 'default';
};

const architectureFeatured: FeaturedBlock = {
  badge: 'Architecture',
  title: 'Next.js 管理画面 + Spring Boot 通話エンジン',
  body:
    'Amazon Connect からの着信を Spring Boot が処理。OpenAI・Google Speech・Amazon Polly と連携し、PostgreSQL + pgvector で FAQ/RAG・通話履歴を永続化。管理画面は Next.js が同一オリジンで API をプロキシします。',
  variant: 'architecture',
  items: [
    'Next.js — ダッシュボード / 通話履歴 / FAQ / 医院設定 / 予約一覧',
    'Spring Boot :8081 — 通話オーケストレーション · 意図分類 · 禁止応答ガード',
    'PostgreSQL + pgvector — 患者 · 予約 · 通話 · FAQ · ナレッジ検索',
    'Amazon Connect — 電話着信 Webhook · 職員転送',
    'OpenAI — 応答生成 · 通話要約 · 埋め込みベクトル',
  ],
};

const callFlowFeatured: FeaturedBlock = {
  badge: 'Call Flow',
  title: '通話処理パイプライン（CallOrchestrationService）',
  body:
    'すべての発話は緊急判定を最優先で実行。AI 応答前後で禁止応答ガードを通し、診断・処方・重症度判断をブロックします。',
  variant: 'callflow',
  items: [
    '① 着信 — Connect Webhook → POST /api/connect/start でセッション開始',
    '② 発話 — POST /api/connect/utterance（テキスト or Google Speech 音声）',
    '③ 緊急判定 — 胸痛・意識障害・呼吸困難 → 即職員転送（AI 非対応）',
    '④ 意図分類 — 予約 / 検査 / 会計 / 薬 / 紹介状 / FAQ 等に振分け',
    '⑤ 本人確認 — 予約系は氏名・生年月日・電話番号を検証',
    '⑥ 応答 — FAQ/RAG/医院設定テキスト or OpenAI（禁止応答チェック後）',
    '⑦ 終了 — POST /api/connect/end → OpenAI で通話要約を職員画面へ',
  ],
};

const autoResponseFeatured: FeaturedBlock = {
  badge: 'Auto Response',
  title: '自動応答 — 診療時間・休診日・アクセス・持ち物',
  body:
    '医院設定（clinic_settings）と FAQ（faq_items）から定型案内を返答。管理画面の「医院設定」「FAQ管理」から内容を編集できます。',
  variant: 'auto',
  items: [
    '診療時間 — 「何時まで」「開院」等 → hours_text を案内',
    '休診日 — 「休み」「休診」→ holidays_text を案内',
    'アクセス — 「行き方」「駐車場」→ access_text を案内',
    '持ち物 — 「保険証」「初診」→ belongings_text を案内',
    'FAQ — 質問文の部分一致で最適な回答を検索 · 未ヒット時は RAG + OpenAI',
  ],
};

const appointmentFeatured: FeaturedBlock = {
  badge: 'Appointment',
  title: '予約受付 — 新規・変更・キャンセル',
  body:
    '予約関連の意図は本人確認後に処理。新規予約は患者 ID に紐づけて appointments テーブルへ保存します。',
  variant: 'appointment',
  items: [
    '新規予約 — 「予約したい」「予約取り」→ 本人確認 → 日時確認 → 登録',
    '予約変更 — 「予約変更」「変更したい」→ 変更前後の日時を確認',
    'キャンセル — 「キャンセル」→ 対象予約日を確認して status=CANCELLED',
    '管理画面 — /appointments で CONFIRMED 予約一覧を確認',
    'API — AppointmentService.create / reschedule / cancel',
  ],
};

const emergencyFeatured: FeaturedBlock = {
  badge: 'Emergency',
  title: '緊急判定 & 人への転送',
  body:
    '緊急キーワード検出時は AI を一切介さず職員へ転送。苦情・医療判断・判断不能時も同様に転送します。',
  variant: 'emergency',
  items: [
    '緊急キーワード — 胸痛 · 意識障害 · 呼吸困難 · 大量出血 · 痙攣 · 119 等',
    '緊急時 — emergency_flag=true · transfer_reason=EMERGENCY',
    '苦情 — COMPLAINT 意図 → 職員転送',
    '医療質問 — 診断・症状相談 → MEDICAL_QUESTION → 職員転送',
    '禁止応答 — AI 出力に診断/処方/重症度が含まれる場合も転送',
    'ダッシュボード — 緊急・転送バッジで一覧確認',
  ],
};

const adminFeatured: FeaturedBlock = {
  badge: 'Admin UI',
  title: '職員向け Web 管理画面',
  body:
    'サイドバーから各機能にアクセス。通話要約・会話ログはリアルタイムに近い形で確認できます。',
  variant: 'admin',
  items: [
    '/ — ダッシュボード（進行中通話 · 総通話 · 転送 · 緊急 · OpenAI 接続状態）',
    '/calls — 通話履歴一覧（意図 · 本人確認 · 転送理由 · 要約）',
    '/calls/[id] — 通話詳細（要約 · 全会話ターン · 緊急フラグ）',
    '/faq — FAQ 追加・編集・削除（医院側が回答文を修正）',
    '/clinic — 医院名 · 診療時間 · 休診日 · アクセス · 持ち物',
    '/appointments — 予約一覧（日時 · 診療科 · 状態）',
  ],
};

const connectFeatured: FeaturedBlock = {
  badge: 'Connect',
  title: 'Amazon Connect · 音声連携',
  body:
    'Connect コンタクトフローから Webhook で API を呼び出します。Polly で応答音声、Google Speech で音声認識（任意）を利用。',
  variant: 'connect',
  items: [
    'POST /api/connect/start — contactId · callerPhone でセッション開始',
    'POST /api/connect/utterance — utterance または audioBase64',
    'POST /api/connect/end — 通話終了 · 要約生成',
    'Polly — POLLY_ENABLED=true · 音声合成（Mizuki）',
    'Google Speech — GOOGLE_SPEECH_ENABLED=true · 音声→テキスト',
    'Connect — CONNECT_INSTANCE_ID · 職員キューへ転送設定',
  ],
};

const securityFeatured: FeaturedBlock = {
  badge: 'Security',
  title: '認証 · 禁止応答制御 · 監視',
  body:
    '本番は OAuth2（Azure AD 等）で管理 API を保護。開発時は DEV_MODE=true で認証スキップ可能。',
  variant: 'security',
  items: [
    'OAuth2 — OAUTH2_ISSUER_URI · JWT リソースサーバー',
    'DEV_MODE — true で全 API 許可（ローカル/Railway 開発用）',
    'ProhibitedResponseGuard — 診断 · 処方 · 重症度表現をブロック',
    '本人確認 — 氏名 + 生年月日 + 電話番号の 3 点照合',
    'CloudWatch — CLOUDWATCH_METRICS_ENABLED · Actuator メトリクス',
    'Actuator — /actuator/health · /actuator/metrics',
  ],
};

const techStack = [
  'Java 21 · Spring Boot 3.3',
  'Next.js 14 · React 18',
  'PostgreSQL · pgvector',
  'OpenAI gpt-4o-mini',
  'Amazon Connect · Polly',
  'Google Speech-to-Text',
  'Railway · Terraform · GitHub Actions',
] as const;

const archDiagram = `Patient Phone
    │ Amazon Connect
    ▼
Connect Contact Flow
    │ Webhook (HTTPS)
    ▼
Spring Boot :8081 (internal)
    ├─ EmergencyDetection → 即転送
    ├─ IntentClassification → 意図振分け
    ├─ IdentityVerification → 本人確認
    ├─ FaqService / RagService → FAQ+RAG
    ├─ OpenAiService → 応答・要約
    ├─ ProhibitedResponseGuard → 禁止応答
    └─ PostgreSQL (sessions · turns · patients)
              ▲
Next.js :PORT (Railway public)
    ├─ /              ダッシュボード
    ├─ /calls         通話履歴・要約
    ├─ /faq           FAQ 管理
    ├─ /clinic        医院設定
    ├─ /appointments  予約一覧
    └─ /api/* ──proxy──► Spring Boot`;

type GuideSection = {
  label: string;
  steps: readonly GuideStep[];
};

const guideSections: readonly GuideSection[] = [
  {
    label: 'クイックスタート',
    steps: [
      {
        title: 'パネル操作・画面遷移',
        body: '本パネルは全画面で表示されます。ヘッダーをドラッグして位置を変更でき、▼▲ で折りたたみ可能です。',
        items: [
          'サイドバー — ダッシュボード / 通話履歴 / FAQ管理 / 医院設定 / 予約一覧',
          '本パネル — 右下付近に表示（位置・開閉状態はブラウザに自動保存）',
          '推奨フロー — 医院設定 → FAQ登録 → テスト通話 → 通話履歴で要約確認',
          'デモ時 — パネルを画面端に寄せ、メイン画面を広く使う',
        ],
      },
      {
        title: '接続確認（最初に）',
        body: 'ローカル・Railway 共通。障害切り分けとデモ前チェックの起点です。',
        items: [
          'GET /api/health — status: ok · openai: true/false',
          'ダッシュボード — OpenAI 接続状態バッジを確認',
          'PostgreSQL — Flyway マイグレーション完了 · pgvector 拡張',
          'DEV_MODE=true — OAuth2 無効（開発・デモ用）',
        ],
      },
      {
        title: '初回セットアップ（10 分）',
        body: '新規医院導入時の最短手順です。',
        items: [
          '① /clinic — 医院名 · 診療時間 · 休診日 · アクセス · 持ち物を入力',
          '② /faq — よくある質問 5〜10 件を登録',
          '③ POST /api/connect/start — テスト通話セッション開始',
          '④ POST /api/connect/utterance — 「診療時間を教えて」等を送信',
          '⑤ /calls — 通話ログ・要約を確認',
          '⑥ OPENAI_API_KEY 設定 — 要約・RAG 応答を有効化',
        ],
      },
    ],
  },
  {
    label: '通話シミュレーション',
    steps: [
      {
        title: 'API で通話を試す（curl / Postman）',
        body: 'Connect 未設定でも Webhook API で通話フローを検証できます。',
        items: [
          'start — curl -X POST .../api/connect/start -d \'{"callerPhone":"09012345678"}\'',
          'utterance — sessionId + utterance を POST（例: 「予約したいです」）',
          '本人確認 — fullName · dateOfBirth · callerPhone を同リクエストに含める',
          '緊急テスト — 「胸が痛い」→ transfer: true · intent: EMERGENCY',
          'end — sessionId で POST → summary が生成される',
        ],
      },
      {
        title: '意図分類のキーワード例',
        body: 'IntentClassificationService がキーワードで意図を判定します。',
        items: [
          'HOURS — 診療時間 · 開院 · 何時',
          'HOLIDAY — 休診 · 休み',
          'ACCESS — アクセス · 行き方 · 駐車',
          'BELONGINGS — 持ち物 · 保険証',
          'APPOINTMENT_* — 予約 · 変更 · キャンセル',
          'LAB / BILLING / PHARMACY / REFERRAL — 検査 · 会計 · 薬 · 紹介状',
        ],
      },
      {
        title: '禁止応答のテスト',
        body: 'AI が医療判断をしようとした場合、ProhibitedResponseGuard が転送に切り替えます。',
        items: [
          '「頭痛が続きます、何の病気ですか」→ 職員転送',
          '「この薬を処方してもらえますか」→ 職員転送',
          '正常系 — 「診療時間を教えて」→ 医院設定テキストを返答',
        ],
      },
    ],
  },
  {
    label: '管理画面 詳細',
    steps: [
      {
        title: 'ダッシュボード（/）',
        body: '通話状況の概要と OpenAI 接続状態を一覧表示します。',
        items: [
          '進行中通話 — status=ACTIVE のセッション数',
          '総通話数 · 職員転送数 · 緊急対応数',
          '直近 10 件 — 開始日時 · 電話番号 · 意図 · 要約プレビュー',
          '緊急バッジ — emergency_flag=true の通話',
        ],
      },
      {
        title: '通話履歴（/calls）',
        body: '全通話の一覧と詳細ログを確認。職員が折り返し連絡する際の参考資料になります。',
        items: [
          '一覧 — 本人確認済みか · 転送理由 · AI 要約',
          '詳細 — 患者/AI の全会話ターン（role: caller / assistant）',
          '要約 — OpenAI 生成の 3〜5 行サマリー（診断・処方は含まない）',
          '緊急 — 赤バッジで緊急転送を強調表示',
        ],
      },
      {
        title: 'FAQ管理（/faq）',
        body: '医院側が AI の回答文を直接修正できる画面です。',
        items: [
          '新規追加 — カテゴリ · 質問 · 回答 · 表示順',
          '編集 — 既存 FAQ の回答文を更新（即座に自動応答に反映）',
          '有効/無効 — active フラグで表示制御',
          'カテゴリ例 — 診療時間 · 休診日 · アクセス · 持ち物 · 予約',
        ],
      },
      {
        title: '医院設定（/clinic）',
        body: '自動応答の定型文の源泉。Connect 着信時の挨拶にも医院名が使われます。',
        items: [
          '医院名 — 着信挨拶「〇〇です。自動応答がご案内いたします」',
          '診療時間 · 休診日 · アクセス · 持ち物 — 各テキストエリア',
          '保存 — PUT /api/admin/clinic で即時反映',
        ],
      },
    ],
  },
  {
    label: 'Railway デプロイ',
    steps: [
      {
        title: '統合デプロイ構成',
        body: 'リポジトリ直下の Dockerfile で Next.js + Spring Boot を 1 サービスとして起動します。',
        items: [
          'Dockerfile + railway.toml + start.sh — ルートに配置済み',
          'Next.js — Railway の PORT で公開待受',
          'Spring Boot — 内部 8081 · /api/* は Next.js がプロキシ',
          'DATABASE_URL — ${{Postgres.DATABASE_URL}} を設定',
          'Config file path — /railway.toml',
        ],
      },
      {
        title: '必須環境変数',
        body: 'Railway ダッシュボードの Variables で設定します。',
        items: [
          'DATABASE_URL — Postgres サービス参照',
          'OPENAI_API_KEY — 要約 · RAG · AI 応答（任意だが推奨）',
          'DEV_MODE — 開発:true / 本番:false + OAUTH2_ISSUER_URI',
          'POLLY_ENABLED — Amazon Polly 音声合成',
          'GOOGLE_SPEECH_ENABLED — 音声認識',
        ],
      },
      {
        title: 'pgvector について',
        body: 'RAG 検索に pgvector 拡張が必要です。',
        items: [
          'Flyway V1 — CREATE EXTENSION vector',
          'Railway Postgres — 拡張未対応の場合は pgvector 対応 DB を使用',
          'knowledge_chunks — embedding vector(1536) · OpenAI text-embedding-3-small',
        ],
      },
    ],
  },
  {
    label: 'トラブルシュート',
    steps: [
      {
        title: 'よくあるエラーと対処',
        body: 'デプロイ・運用時の確認手順です。',
        items: [
          'Dockerfile not found — ルートに Dockerfile があるか確認 · railway.toml 参照',
          'DB 接続失敗 — DATABASE_URL 設定 · Postgres サービス追加',
          'Flyway 失敗 — pgvector 拡張 · マイグレーション V1 の実行権限',
          'OpenAI 未設定 — ダッシュボード openai: false · 要約はフォールバック文',
          '/api/health 503 — バックエンド未起動 · start.sh ログを確認',
        ],
      },
      {
        title: 'API エンドポイント一覧',
        body: 'Connect Webhook と管理 API の参照です。',
        items: [
          'POST /api/connect/start · utterance · end — 通話 Webhook',
          'GET /api/admin/calls · /calls/{id}/turns — 通話履歴',
          'GET/POST/PUT/DELETE /api/admin/faq — FAQ CRUD',
          'GET/PUT /api/admin/clinic — 医院設定',
          'GET /api/admin/appointments — 予約一覧',
          'GET /api/health — 生存確認 · OpenAI 設定状態',
        ],
      },
      {
        title: 'デモ向け 15 分シナリオ',
        body: '医院向けプレゼンの推奨トークトラックです。',
        items: [
          '0–3分: 本パネルでアーキテクチャ・通話フロー・Service Topology を説明',
          '3–6分: /clinic · /faq — 医院側カスタマイズのデモ',
          '6–9分: API 通話シミュレーション — 診療時間 · 予約 · 緊急転送',
          '9–12分: /calls — 要約・会話ログの職員画面デモ',
          '12–15分: 禁止応答制御 · OAuth2 · Railway デプロイ · Q&A',
        ],
      },
    ],
  },
];

const L = {
  title: '利用手順',
  subtitle: 'Architecture & Ops',
  dragHint: 'ドラッグで移動',
  expand: '開く',
  collapse: '閉じる',
  heroTitle: 'MediCall AI 電話応答システム',
  heroLead:
    '医療機関向け AI コールセンター。Amazon Connect 着信を OpenAI で処理し、緊急時は即職員転送。FAQ・予約・通話要約を職員 Web 管理画面で一元管理します。',
  stackLabel: 'Tech stack',
  diagramLabel: 'Service topology',
  workflowLabel: '詳細利用手順',
  scrollHint: '↓ 画面別の詳細手順・デモフローは下へ',
  footer: '▼▲ で開閉 · ヘッダーをドラッグして移動 · 表示位置は自動保存されます。',
} as const;

type SavedState = {
  x: number;
  y: number;
  expanded: boolean;
};

function defaultPosition() {
  if (typeof window === 'undefined') return { x: 24, y: 24 };
  const x = Math.max(16, window.innerWidth - PANEL_WIDTH - 24);
  const y = Math.max(72, window.innerHeight - 520);
  return { x, y };
}

function clampPosition(x: number, y: number, width: number, height: number) {
  const maxX = Math.max(8, window.innerWidth - width - 8);
  const maxY = Math.max(8, window.innerHeight - height - 8);
  return {
    x: Math.min(Math.max(8, x), maxX),
    y: Math.min(Math.max(8, y), maxY),
  };
}

function FeaturedSection({ block }: { block: FeaturedBlock }) {
  const variant = block.variant ?? 'default';
  return (
    <section
      className={`usage-guide-featured usage-guide-featured--${variant}`}
      aria-label={block.title}
    >
      <div className="usage-guide-featured-head">
        <span className="usage-guide-featured-badge">{block.badge}</span>
        <strong>{block.title}</strong>
      </div>
      <p>{block.body}</p>
      {block.items?.length ? (
        <ul className="usage-guide-items">
          {block.items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}

export function UsageGuidePanel() {
  const panelRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef<{
    pointerId: number;
    startX: number;
    startY: number;
    originX: number;
    originY: number;
  } | null>(null);

  const [ready, setReady] = useState(false);
  const [expanded, setExpanded] = useState(true);
  const [pos, setPos] = useState({ x: 24, y: 24 });
  const [dragging, setDragging] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const parsed = JSON.parse(saved) as SavedState;
        setPos({ x: parsed.x, y: parsed.y });
        setExpanded(parsed.expanded);
      } catch {
        setPos(defaultPosition());
      }
    } else {
      setPos(defaultPosition());
    }
    setReady(true);
  }, []);

  useEffect(() => {
    if (!ready) return;
    const payload: SavedState = { ...pos, expanded };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
  }, [pos, expanded, ready]);

  useEffect(() => {
    if (!ready) return;
    const onResize = () => {
      const el = panelRef.current;
      if (!el) return;
      setPos((current) => clampPosition(current.x, current.y, el.offsetWidth, el.offsetHeight));
    };
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [ready]);

  const onHeaderPointerDown = useCallback(
    (e: React.PointerEvent<HTMLElement>) => {
      if ((e.target as HTMLElement).closest('.usage-guide-toggle')) return;
      dragRef.current = {
        pointerId: e.pointerId,
        startX: e.clientX,
        startY: e.clientY,
        originX: pos.x,
        originY: pos.y,
      };
      setDragging(true);
      e.currentTarget.setPointerCapture(e.pointerId);
    },
    [pos.x, pos.y],
  );

  const onHeaderPointerMove = useCallback((e: React.PointerEvent<HTMLElement>) => {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== e.pointerId) return;
    const el = panelRef.current;
    const width = el?.offsetWidth ?? PANEL_WIDTH;
    const height = el?.offsetHeight ?? 120;
    setPos(
      clampPosition(
        drag.originX + (e.clientX - drag.startX),
        drag.originY + (e.clientY - drag.startY),
        width,
        height,
      ),
    );
  }, []);

  const onHeaderPointerUp = useCallback((e: React.PointerEvent<HTMLElement>) => {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== e.pointerId) return;
    dragRef.current = null;
    setDragging(false);
    e.currentTarget.releasePointerCapture(e.pointerId);
  }, []);

  if (!ready) return null;

  return (
    <div
      ref={panelRef}
      className={`usage-guide-panel${expanded ? ' is-expanded' : ' is-collapsed'}${dragging ? ' is-dragging' : ''}`}
      style={{ left: pos.x, top: pos.y, width: PANEL_WIDTH }}
      role="dialog"
      aria-label={L.title}
      aria-modal="false"
    >
      <header
        className="usage-guide-header"
        onPointerDown={onHeaderPointerDown}
        onPointerMove={onHeaderPointerMove}
        onPointerUp={onHeaderPointerUp}
        onPointerCancel={onHeaderPointerUp}
      >
        <div className="usage-guide-header-text">
          <span className="usage-guide-drag-icon" aria-hidden>
            ☰
          </span>
          <div className="usage-guide-header-titles">
            <strong>{L.title}</strong>
            <span className="usage-guide-header-sub">{L.subtitle}</span>
          </div>
          <span className="usage-guide-drag-hint">{L.dragHint}</span>
        </div>
        <button
          type="button"
          className="usage-guide-toggle"
          aria-label={expanded ? L.collapse : L.expand}
          aria-expanded={expanded}
          onClick={() => setExpanded((open) => !open)}
        >
          {expanded ? '▼' : '▲'}
        </button>
      </header>

      {expanded ? (
        <div className="usage-guide-body">
          <div className="usage-guide-hero">
            <p className="usage-guide-hero-kicker">MediCall AI Platform</p>
            <h2 className="usage-guide-hero-title">{L.heroTitle}</h2>
            <p className="usage-guide-hero-lead">{L.heroLead}</p>
            <div className="usage-guide-stack" aria-label={L.stackLabel}>
              {techStack.map((tag) => (
                <span key={tag} className="usage-guide-stack-pill">
                  {tag}
                </span>
              ))}
            </div>
          </div>

          <FeaturedSection block={architectureFeatured} />

          <figure className="usage-guide-diagram" aria-label={L.diagramLabel}>
            <figcaption>{L.diagramLabel}</figcaption>
            <pre>{archDiagram}</pre>
          </figure>

          <FeaturedSection block={callFlowFeatured} />
          <FeaturedSection block={autoResponseFeatured} />
          <FeaturedSection block={appointmentFeatured} />
          <FeaturedSection block={emergencyFeatured} />
          <FeaturedSection block={adminFeatured} />
          <FeaturedSection block={connectFeatured} />
          <FeaturedSection block={securityFeatured} />

          <p className="usage-guide-scroll-hint">{L.scrollHint}</p>
          <h3 className="usage-guide-workflow-title">{L.workflowLabel}</h3>
          {guideSections.map((section) => (
            <div key={section.label} className="usage-guide-section">
              <p className="usage-guide-section-label">{section.label}</p>
              <ol className="usage-guide-steps">
                {section.steps.map((step) => (
                  <li key={step.title}>
                    <strong>{step.title}</strong>
                    <p>{step.body}</p>
                    {step.items?.length ? (
                      <ul className="usage-guide-items">
                        {step.items.map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    ) : null}
                  </li>
                ))}
              </ol>
            </div>
          ))}
          <p className="usage-guide-footer">{L.footer}</p>
        </div>
      ) : null}
    </div>
  );
}
