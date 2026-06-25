import type { Metadata } from 'next';
import './globals.css';
import { UsageGuidePanel } from '@/components/UsageGuidePanel';
import { TenantProvider } from '@/components/TenantProvider';
import { TenantSwitcher } from '@/components/TenantSwitcher';

export const metadata: Metadata = {
  title: 'MediCall AI 管理画面',
  description: '医療コールセンター職員向け管理画面',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ja">
      <body>
        <TenantProvider>
          <div className="layout">
            <aside className="sidebar">
              <h1>MediCall AI</h1>
              <TenantSwitcher />
              <nav>
                <a href="/">ダッシュボード</a>
                <a href="/queue">ライブキュー</a>
                <a href="/call">通話デモ</a>
                <a href="/calls">通話履歴</a>
                <a href="/patients">患者検索</a>
                <a href="/faq">FAQ管理</a>
                <a href="/clinic">医院設定</a>
                <a href="/appointments">予約一覧</a>
                <a href="/tenants">テナント管理</a>
              </nav>
            </aside>
            <main className="content">{children}</main>
          </div>
          <UsageGuidePanel />
        </TenantProvider>
      </body>
    </html>
  );
}
