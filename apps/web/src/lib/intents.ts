export const intentLabels: Record<string, string> = {
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
  HUMAN_TRANSFER: '職員転送',
};

export function intentLabel(intent?: string | null): string {
  if (!intent) return '-';
  return intentLabels[intent] || intent;
}
