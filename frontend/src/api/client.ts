const BASE = '/api/v1';

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(`${res.status} ${text}`);
  }
  return res.json() as Promise<T>;
}

async function postAudio(path: string, audio: Blob): Promise<{ transcript: string }> {
  const form = new FormData();
  form.append('audio', audio, 'recording.webm');
  const res = await fetch(`${BASE}${path}`, { method: 'POST', body: form });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(`${res.status} ${text}`);
  }
  return res.json();
}

async function postForAudio(path: string, body: unknown): Promise<Blob> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(`${res.status} ${text}`);
  }
  return res.blob();
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  /** Uploads a recorded audio clip and returns its transcript via Google Speech-to-Text. */
  transcribeAudio: (audio: Blob) => postAudio('/voice/transcribe', audio),
  /** Sends text to Google Text-to-Speech and returns the spoken response as playable audio. */
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
