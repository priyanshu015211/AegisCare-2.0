const API_ORIGIN = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '');
const BASE = `${API_ORIGIN}/api/v1`;
const TOKEN_KEY = 'aegiscare.access_token';

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  role: 'ADMIN' | 'CLINICIAN' | 'COORDINATOR';
  hospitalId: string | null;
}

export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  user: AuthUser;
}

export function getAccessToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function clearSession(): void {
  sessionStorage.removeItem(TOKEN_KEY);
}

function handleUnauthorized(status: number, path: string, hadToken: boolean): void {
  if (status === 401 && !path.endsWith('/auth/login') && hadToken) {
    clearSession();
    window.dispatchEvent(new CustomEvent('aegiscare:auth-expired'));
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const token = getAccessToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const res = await fetch(`${BASE}${path}`, { ...options, headers });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    handleUnauthorized(res.status, path, Boolean(token));
    throw new Error(`${res.status} ${text || res.statusText}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

async function postAudio(path: string, audio: Blob): Promise<{ transcript: string }> {
  const form = new FormData();
  form.append('audio', audio, 'recording.webm');
  const headers = new Headers();
  const token = getAccessToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const res = await fetch(`${BASE}${path}`, { method: 'POST', headers, body: form });
  if (!res.ok) {
    handleUnauthorized(res.status, path, Boolean(token));
    throw new Error(`${res.status} ${await res.text().catch(() => res.statusText)}`);
  }
  return res.json();
}

async function postForAudio(path: string, body: unknown): Promise<Blob> {
  const headers = new Headers({ 'Content-Type': 'application/json' });
  const token = getAccessToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const res = await fetch(`${BASE}${path}`, { method: 'POST', headers, body: JSON.stringify(body) });
  if (!res.ok) {
    handleUnauthorized(res.status, path, Boolean(token));
    throw new Error(`${res.status} ${await res.text().catch(() => res.statusText)}`);
  }
  return res.blob();
}

async function login(email: string, password: string): Promise<AuthResponse> {
  const result = await request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  sessionStorage.setItem(TOKEN_KEY, result.accessToken);
  return result;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  login,
  logout: clearSession,
  transcribeAudio: (audio: Blob) => postAudio('/voice/transcribe', audio),
  synthesizeSpeech: (text: string) => postForAudio('/voice/speak', { text }),
};

export interface StorageStatus {
  mode: string;
  detail: string;
  connected: boolean;
}

export interface HealthResponse {
  status: string;
  storage: StorageStatus;
}

export interface DashboardSummary {
  hospital_load_pct: number;
  hospitals_tracked: number;
  active_emergencies: number;
  total_sessions: number;
  risk_distribution: Record<string, number>;
  recent_activity: Array<{
    patient_id: string;
    severity: string;
    risk_score: number;
    started_at: string;
  }>;
}

export interface AnalyzeResponse {
  analysis: {
    severity: string;
    risk_score: number;
    reasoning: string;
    follow_up_question: string;
    escalation_needed: boolean;
  };
  current_state: Record<string, unknown>;
}

export interface HighRiskPatient {
  patient_id: string;
  risk_score: number;
  severity: string;
  status: string;
  last_update: string;
}

export interface CoordinationSummary {
  active_emergencies: number;
  pending_appointments: number;
  doctors_available: number;
  doctors_total: number;
}

export interface QueueItem {
  patient_id: string;
  risk_score: number;
  action: string;
}

export interface Doctor {
  id: string;
  full_name: string;
  specialty: string;
  department: string;
  status: string;
}

export interface VolumePoint {
  hour: string;
  patients: number;
}
