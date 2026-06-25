'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { api, CallEndResponse, CallResponse, CallStartResponse } from '@/lib/api';

type Turn = {
  role: 'caller' | 'assistant' | 'system';
  content: string;
  intent?: string;
  transfer?: boolean;
};

const INTENT_LABELS: Record<string, string> = {
  HOURS: '診療時間',
  HOLIDAY: '休診日',
  ACCESS: 'アクセス',
  BELONGINGS: '持ち物',
  APPOINTMENT_NEW: '新規予約',
  APPOINTMENT_CHANGE: '予約変更',
  APPOINTMENT_CANCEL: 'キャンセル',
  LAB: '検査',
  BILLING: '会計',
  PHARMACY: '薬',
  REFERRAL: '紹介状',
  EMERGENCY: '緊急',
  COMPLAINT: '苦情',
  FAQ: 'FAQ',
  HUMAN_TRANSFER: '職員転送',
};

function speak(text: string) {
  if (typeof window === 'undefined' || !window.speechSynthesis) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'ja-JP';
  utterance.rate = 1;
  window.speechSynthesis.speak(utterance);
}

export function CallDemo() {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [active, setActive] = useState(false);
  const [listening, setListening] = useState(false);
  const [turns, setTurns] = useState<Turn[]>([]);
  const [input, setInput] = useState('');
  const [summary, setSummary] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [voiceEnabled, setVoiceEnabled] = useState(true);
  const [speechSupported, setSpeechSupported] = useState(false);

  const [callerPhone, setCallerPhone] = useState('09012345678');
  const [fullName, setFullName] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');

  const recognitionRef = useRef<SpeechRecognition | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    setSpeechSupported(!!SR);
    if (SR) {
      const rec = new SR();
      rec.lang = 'ja-JP';
      rec.continuous = false;
      rec.interimResults = false;
      recognitionRef.current = rec;
    }
  }, []);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [turns]);

  const addTurn = useCallback((turn: Turn) => {
    setTurns((prev) => [...prev, turn]);
  }, []);

  const startCall = async () => {
    setError(null);
    setSummary(null);
    setLoading(true);
    try {
      const res = await api<CallStartResponse>('/api/connect/start', {
        method: 'POST',
        body: JSON.stringify({ callerPhone }),
      });
      setSessionId(res.sessionId);
      setActive(true);
      setTurns([{ role: 'assistant', content: res.greeting }]);
      if (voiceEnabled) speak(res.greeting);
    } catch {
      setError('通話を開始できませんでした。APIが起動しているか確認してください。');
    } finally {
      setLoading(false);
    }
  };

  const sendUtterance = async (utterance: string) => {
    if (!sessionId || !utterance.trim() || loading) return;
    setError(null);
    setLoading(true);
    addTurn({ role: 'caller', content: utterance.trim() });

    try {
      const body: Record<string, string> = {
        sessionId,
        utterance: utterance.trim(),
        callerPhone,
      };
      if (fullName) body.fullName = fullName;
      if (dateOfBirth) body.dateOfBirth = dateOfBirth;

      const res = await api<CallResponse>('/api/connect/utterance', {
        method: 'POST',
        body: JSON.stringify(body),
      });

      addTurn({
        role: 'assistant',
        content: res.text,
        intent: res.intent,
        transfer: res.transfer,
      });

      if (voiceEnabled) speak(res.text);

      if (res.transfer) {
        setActive(false);
        addTurn({
          role: 'system',
          content: `職員へ転送しました（${INTENT_LABELS[res.transferReason || ''] || res.transferReason}）`,
        });
      }
    } catch {
      setError('応答の取得に失敗しました。');
      setTurns((prev) => prev.slice(0, -1));
    } finally {
      setLoading(false);
      setInput('');
    }
  };

  const endCall = async () => {
    if (!sessionId) return;
    setLoading(true);
    setListening(false);
    recognitionRef.current?.abort();

    try {
      const res = await api<CallEndResponse>('/api/connect/end', {
        method: 'POST',
        body: JSON.stringify({ sessionId }),
      });
      setSummary(res.summary);
      setActive(false);
      addTurn({ role: 'system', content: '通話を終了しました。' });
    } catch {
      setError('通話終了処理に失敗しました。');
    } finally {
      setLoading(false);
    }
  };

  const toggleListen = () => {
    const rec = recognitionRef.current;
    if (!rec || !active || loading) return;

    if (listening) {
      rec.stop();
      setListening(false);
      return;
    }

    rec.onresult = (event: SpeechRecognitionEvent) => {
      const text = event.results[0]?.[0]?.transcript;
      if (text) sendUtterance(text);
    };
    rec.onerror = () => setListening(false);
    rec.onend = () => setListening(false);

    setListening(true);
    rec.start();
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    sendUtterance(input);
  };

  return (
    <div className="call-demo">
      <div className="call-demo-header">
        <div>
          <h2>通話デモ</h2>
          <p className="page-meta">
            ブラウザのマイクまたはテキストで AI 自動応答を体験できます（Amazon Connect 不要）
          </p>
        </div>
        <div className="call-status">
          <span className={`call-status-dot ${active ? 'live' : summary ? 'ended' : 'idle'}`} />
          {active ? '通話中' : summary ? '終了' : '待機中'}
        </div>
      </div>

      {error && <div className="call-error">{error}</div>}

      <div className="call-demo-grid">
        <div className="card call-settings">
          <h3>発信者情報</h3>
          <div className="form-group">
            <label>電話番号</label>
            <input
              value={callerPhone}
              onChange={(e) => setCallerPhone(e.target.value)}
              disabled={active}
              placeholder="09012345678"
            />
          </div>
          <div className="form-group">
            <label>氏名（予約時・任意）</label>
            <input
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              disabled={!active}
              placeholder="山田 太郎"
            />
          </div>
          <div className="form-group">
            <label>生年月日（予約時・任意）</label>
            <input
              value={dateOfBirth}
              onChange={(e) => setDateOfBirth(e.target.value)}
              disabled={!active}
              placeholder="1985-04-12"
            />
          </div>
          <label className="call-checkbox">
            <input
              type="checkbox"
              checked={voiceEnabled}
              onChange={(e) => setVoiceEnabled(e.target.checked)}
            />
            AI応答を音声で読み上げる
          </label>
          {!speechSupported && (
            <p className="call-hint">お使いのブラウザは音声認識非対応です。テキスト入力をご利用ください。</p>
          )}
          <div className="call-actions">
            {!active && !sessionId && (
              <button type="button" onClick={startCall} disabled={loading}>
                {loading ? '接続中…' : '通話開始'}
              </button>
            )}
            {active && (
              <>
                <button
                  type="button"
                  className={listening ? 'listening' : ''}
                  onClick={toggleListen}
                  disabled={loading || !speechSupported}
                >
                  {listening ? '聞き取り中…' : 'マイクで話す'}
                </button>
                <button type="button" className="danger" onClick={endCall} disabled={loading}>
                  通話終了
                </button>
              </>
            )}
            {!active && sessionId && (
              <button type="button" onClick={startCall} disabled={loading}>
                新しい通話を開始
              </button>
            )}
          </div>
          <div className="call-examples">
            <p className="call-hint">試すフレーズ:</p>
            <div className="call-example-chips">
              {['診療時間を教えてください', '予約したいです', '駐車場はありますか', '胸が痛いです'].map(
                (phrase) => (
                  <button
                    key={phrase}
                    type="button"
                    className="chip"
                    disabled={!active || loading}
                    onClick={() => sendUtterance(phrase)}
                  >
                    {phrase}
                  </button>
                ),
              )}
            </div>
          </div>
        </div>

        <div className="card call-conversation">
          <h3>会話</h3>
          <div className="call-log" ref={scrollRef}>
            {turns.length === 0 && (
              <p className="call-empty">「通話開始」を押すと AI が挨拶します。</p>
            )}
            {turns.map((t, i) => (
              <div key={i} className={`call-bubble call-bubble--${t.role}`}>
                <div className="call-bubble-meta">
                  {t.role === 'caller' ? 'あなた' : t.role === 'assistant' ? 'AI' : 'システム'}
                  {t.intent && (
                    <span className="badge ended">{INTENT_LABELS[t.intent] || t.intent}</span>
                  )}
                  {t.transfer && <span className="badge transfer">転送</span>}
                </div>
                <p>{t.content}</p>
              </div>
            ))}
            {loading && <p className="call-typing">AI が応答を生成しています…</p>}
          </div>

          <form onSubmit={handleSubmit} className="call-input-row">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={active ? 'メッセージを入力…' : '通話開始後に入力できます'}
              disabled={!active || loading}
            />
            <button type="submit" disabled={!active || loading || !input.trim()}>
              送信
            </button>
          </form>
        </div>
      </div>

      {summary && (
        <div className="card call-summary">
          <h3>通話要約</h3>
          <p>{summary}</p>
          {sessionId && (
            <Link href={`/calls/${sessionId}`} className="call-detail-link">
              通話履歴で詳細を見る →
            </Link>
          )}
        </div>
      )}
    </div>
  );
}
