'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { api, CallSession } from '@/lib/api';
import { intentLabel } from '@/lib/intents';

const POLL_MS = 10_000;

function queuePriority(c: CallSession): number {
  if (c.emergencyFlag) return 0;
  if (c.transferred) return 1;
  return 2;
}

function sortQueue(calls: CallSession[]): CallSession[] {
  return [...calls].sort((a, b) => {
    const p = queuePriority(a) - queuePriority(b);
    if (p !== 0) return p;
    return new Date(a.startedAt).getTime() - new Date(b.startedAt).getTime();
  });
}

function elapsed(startedAt: string): string {
  const sec = Math.floor((Date.now() - new Date(startedAt).getTime()) / 1000);
  if (sec < 60) return `${sec}秒`;
  const min = Math.floor(sec / 60);
  return `${min}分${sec % 60}秒`;
}

export function LiveQueue() {
  const [calls, setCalls] = useState<CallSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);
  const [notifyEnabled, setNotifyEnabled] = useState(false);
  const seenRef = useRef<Set<string>>(new Set());

  const refresh = useCallback(async () => {
    try {
      const data = await api<CallSession[]>('/api/admin/calls/queue');
      const sorted = sortQueue(data);

      if (notifyEnabled && typeof Notification !== 'undefined') {
        for (const c of sorted) {
          if ((c.emergencyFlag || c.transferred) && !seenRef.current.has(c.id)) {
            seenRef.current.add(c.id);
            const title = c.emergencyFlag ? '緊急通話' : '職員転送';
            new Notification(`MediCall AI: ${title}`, {
              body: `${c.callerPhone || '不明'} — ${intentLabel(c.intent)}`,
              tag: c.id,
            });
          }
        }
      }

      setCalls(sorted);
      setLastUpdate(new Date());
    } catch {
      /* API offline */
    } finally {
      setLoading(false);
    }
  }, [notifyEnabled]);

  useEffect(() => {
    refresh();
    const id = setInterval(refresh, POLL_MS);
    return () => clearInterval(id);
  }, [refresh]);

  const enableNotify = async () => {
    if (typeof Notification === 'undefined') return;
    const perm = await Notification.requestPermission();
    setNotifyEnabled(perm === 'granted');
  };

  const urgent = calls.filter(c => c.emergencyFlag || c.transferred);
  const waiting = calls.filter(c => !c.emergencyFlag && !c.transferred);

  return (
    <div>
      <div className="queue-toolbar">
        <p className="page-meta">
          {loading ? '読み込み中…' : `${calls.length} 件の進行中通話`}
          {lastUpdate && ` · 最終更新 ${lastUpdate.toLocaleTimeString('ja-JP')}`}
          {' · '}{POLL_MS / 1000}秒ごとに自動更新
        </p>
        <div className="queue-actions">
          <button type="button" onClick={refresh}>今すぐ更新</button>
          {!notifyEnabled && (
            <button type="button" className="secondary" onClick={enableNotify}>
              ブラウザ通知を有効化
            </button>
          )}
          {notifyEnabled && <span className="notify-on">通知 ON</span>}
        </div>
      </div>

      {urgent.length > 0 && (
        <>
          <h3 className="queue-section-title queue-section-title--urgent">
            要対応 ({urgent.length})
          </h3>
          <div className="queue-cards">
            {urgent.map(c => (
              <QueueCard key={c.id} call={c} urgent />
            ))}
          </div>
        </>
      )}

      <h3 className="queue-section-title">
        進行中の通話 ({waiting.length})
      </h3>
      {waiting.length === 0 && urgent.length === 0 ? (
        <div className="card queue-empty">
          現在、進行中の通話はありません。
          <p style={{ marginTop: '0.5rem', fontSize: '0.85rem', color: '#666' }}>
            <Link href="/call">通話デモ</Link>でテスト通話を開始できます。
          </p>
        </div>
      ) : (
        <div className="queue-cards">
          {waiting.map(c => (
            <QueueCard key={c.id} call={c} />
          ))}
        </div>
      )}
    </div>
  );
}

function QueueCard({ call, urgent }: { call: CallSession; urgent?: boolean }) {
  return (
    <div className={`queue-card${urgent ? ' queue-card--urgent' : ''}`}>
      <div className="queue-card-head">
        <strong>{call.callerPhone || '電話番号不明'}</strong>
        <span className="queue-elapsed">{elapsed(call.startedAt)}</span>
      </div>
      <div className="queue-card-badges">
        {call.emergencyFlag && <span className="badge emergency">緊急</span>}
        {call.transferred && <span className="badge transfer">転送</span>}
        <span className="badge active">進行中</span>
        {call.verified && <span className="badge confirmed">本人確認済</span>}
      </div>
      <p className="queue-card-meta">
        意図: {intentLabel(call.intent)}
        {call.transferReason && ` · ${call.transferReason}`}
      </p>
      <p className="queue-card-time">
        開始: {new Date(call.startedAt).toLocaleString('ja-JP')}
      </p>
      <Link href={`/calls/${call.id}`} className="queue-card-link">通話詳細を開く →</Link>
    </div>
  );
}
