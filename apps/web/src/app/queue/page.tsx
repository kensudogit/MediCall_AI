import { LiveQueue } from '@/components/LiveQueue';

export default function QueuePage() {
  return (
    <div>
      <h2>ライブキュー</h2>
      <p className="page-meta" style={{ marginBottom: '1rem' }}>
        進行中の通話をリアルタイムで監視します。緊急・職員転送は優先表示されます。
      </p>
      <LiveQueue />
    </div>
  );
}
