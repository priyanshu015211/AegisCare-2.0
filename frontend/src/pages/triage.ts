import { api, type AnalyzeResponse } from '../api/client';
import { panelHeader, severityBadge, escapeHtml } from '../components/ui';

let activeRecorder: MediaRecorder | null = null;
let activeAudio: HTMLAudioElement | null = null;

export async function renderTriage(container: HTMLElement) {
  container.innerHTML = `
    <div class="grid grid-cols-1 lg:grid-cols-5 gap-6">
      <div class="lg:col-span-2 panel p-5 h-fit">
        ${panelHeader('Patient Triage', 'Rule-based clinical risk assessment')}
        <form id="triage-form" class="space-y-4">
          <div>
            <label class="text-xs font-mono uppercase tracking-wide text-ink-soft">Patient ID</label>
            <input name="patientId" type="text" value="P-1043"
              class="mt-1 w-full border border-line bg-white px-3 py-2 text-sm font-mono focus:outline-none focus:border-brand" />
          </div>
          <div>
            <label class="text-xs font-mono uppercase tracking-wide text-ink-soft">Duration</label>
            <input name="duration" type="text" placeholder="e.g., 2 days"
              class="mt-1 w-full border border-line bg-white px-3 py-2 text-sm focus:outline-none focus:border-brand" />
          </div>
          <div>
            <div class="flex items-center justify-between">
              <label class="text-xs font-mono uppercase tracking-wide text-ink-soft">Symptoms (comma separated)</label>
              <button type="button" id="mic-btn"
                class="flex items-center gap-1.5 text-xs font-mono uppercase tracking-wide border border-line px-2 py-1 hover:border-brand transition-colors"
                title="Speak your symptoms">
                <span id="mic-icon">&#127908;</span><span id="mic-label">Speak</span>
              </button>
            </div>
            <textarea name="symptoms" rows="4" placeholder="fever, cough, fatigue"
              class="mt-1 w-full border border-line bg-white px-3 py-2 text-sm focus:outline-none focus:border-brand">fever, cough, fatigue</textarea>
            <div id="voice-status" class="text-xs text-ink-soft mt-1 hidden"></div>
          </div>
          <button type="submit" id="triage-submit"
            class="w-full bg-brand text-white text-sm font-medium py-2.5 hover:bg-brand-strong transition-colors">
            Analyze Patient
          </button>
          <div id="triage-error" class="text-severity-critical text-sm hidden"></div>
        </form>
      </div>

      <div class="lg:col-span-3 panel p-5" id="triage-results">
        <div class="text-sm text-ink-soft h-full flex items-center justify-center py-16">
          Submit a patient's symptoms to see the assessment.
        </div>
      </div>
    </div>
  `;

  const form = container.querySelector('#triage-form') as HTMLFormElement;
  const errorBox = container.querySelector('#triage-error') as HTMLElement;
  const submitBtn = container.querySelector('#triage-submit') as HTMLButtonElement;
  const results = container.querySelector('#triage-results') as HTMLElement;
  const symptomsField = container.querySelector('textarea[name="symptoms"]') as HTMLTextAreaElement;
  const micBtn = container.querySelector('#mic-btn') as HTMLButtonElement;
  const micLabel = container.querySelector('#mic-label') as HTMLElement;
  const voiceStatus = container.querySelector('#voice-status') as HTMLElement;

  setupMic(micBtn, micLabel, voiceStatus, symptomsField);

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorBox.classList.add('hidden');
    stopActivePlayback();

    const fd = new FormData(form);
    const patientId = String(fd.get('patientId') ?? '').trim();
    const duration = String(fd.get('duration') ?? '').trim();
    const symptoms = String(fd.get('symptoms') ?? '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    if (symptoms.length === 0) {
      errorBox.textContent = 'Please enter at least one symptom.';
      errorBox.classList.remove('hidden');
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Analyzing\u2026';
    results.innerHTML = `<div class="text-sm text-ink-soft h-full flex items-center justify-center py-16">Analyzing symptoms&hellip;</div>`;

    try {
      const response = await api.post<AnalyzeResponse>('/ai/analyze', { patientId, symptoms, duration });
      results.innerHTML = buildResults(response);
      void speakResult(results, response.analysis);
    } catch (err) {
      errorBox.textContent = `Failed to reach backend: ${(err as Error).message}`;
      errorBox.classList.remove('hidden');
      results.innerHTML = `<div class="text-sm text-ink-soft h-full flex items-center justify-center py-16">No assessment yet.</div>`;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Analyze Patient';
    }
  });
}

function setupMic(
  micBtn: HTMLButtonElement,
  micLabel: HTMLElement,
  voiceStatus: HTMLElement,
  symptomsField: HTMLTextAreaElement,
) {
  let recording = false;
  let chunks: BlobPart[] = [];

  micBtn.addEventListener('click', async () => {
    if (recording) {
      activeRecorder?.stop();
      return;
    }

    if (!navigator.mediaDevices?.getUserMedia) {
      voiceStatus.textContent = 'Voice input is not supported in this browser.';
      voiceStatus.classList.remove('hidden');
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      chunks = [];
      const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : 'audio/webm';
      const recorder = new MediaRecorder(stream, { mimeType });
      activeRecorder = recorder;

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunks.push(e.data);
      };

      recorder.onstop = async () => {
        recording = false;
        micLabel.textContent = 'Speak';
        micBtn.classList.remove('border-severity-critical', 'text-severity-critical');
        stream.getTracks().forEach((t) => t.stop());
        activeRecorder = null;

        const blob = new Blob(chunks, { type: mimeType });
        voiceStatus.textContent = 'Transcribing\u2026';
        voiceStatus.classList.remove('hidden');
        micBtn.disabled = true;

        try {
          const { transcript } = await api.transcribeAudio(blob);
          if (transcript) {
            const current = symptomsField.value.trim();
            symptomsField.value = current ? `${current}, ${transcript}` : transcript;
            voiceStatus.textContent = `Heard: "${transcript}"`;
          } else {
            voiceStatus.textContent = "Didn't catch any speech \u2014 try again.";
          }
        } catch (err) {
          voiceStatus.textContent = `Voice input unavailable: ${(err as Error).message}`;
        } finally {
          micBtn.disabled = false;
        }
      };

      recorder.start();
      recording = true;
      micLabel.textContent = 'Stop';
      micBtn.classList.add('border-severity-critical', 'text-severity-critical');
      voiceStatus.textContent = 'Listening\u2026 tap Stop when done.';
      voiceStatus.classList.remove('hidden');
    } catch {
      voiceStatus.textContent = 'Microphone access was denied or unavailable.';
      voiceStatus.classList.remove('hidden');
    }
  });
}

function stopActivePlayback() {
  if (activeAudio) {
    activeAudio.pause();
    activeAudio = null;
  }
}

async function speakResult(resultsContainer: HTMLElement, analysis: AnalyzeResponse['analysis']) {
  const speakStatus = resultsContainer.querySelector('#speak-status') as HTMLElement | null;
  const stopBtn = resultsContainer.querySelector('#speak-stop') as HTMLButtonElement | null;
  if (!speakStatus) return;

  const text = `${analysis.reasoning} ${analysis.follow_up_question}`;
  try {
    const audioBlob = await api.synthesizeSpeech(text);
    const url = URL.createObjectURL(audioBlob);
    const audio = new Audio(url);
    activeAudio = audio;

    speakStatus.textContent = '\u{1F50A} Speaking response\u2026';
    speakStatus.classList.remove('hidden');
    stopBtn?.classList.remove('hidden');

    stopBtn?.addEventListener('click', () => stopActivePlayback());

    audio.addEventListener('ended', () => {
      speakStatus.classList.add('hidden');
      stopBtn?.classList.add('hidden');
      URL.revokeObjectURL(url);
      if (activeAudio === audio) activeAudio = null;
    });

    await audio.play();
  } catch {
    // Voice may not be configured (no API key) or the request failed \u2014
    // fail silently since the text results are already shown.
    speakStatus.classList.add('hidden');
  }
}

function buildResults(response: AnalyzeResponse): string {
  const a = response.analysis;
  const escalationNote = a.escalation_needed
    ? `<div class="border border-severity-high text-severity-high text-sm px-3 py-2" style="background: color-mix(in srgb, var(--color-severity-high) 8%, transparent)">This case may require escalation based on current symptoms.</div>`
    : `<div class="border border-line text-ink-soft text-sm px-3 py-2 bg-paper">No immediate escalation required based on current assessment.</div>`;

  return `
    <div class="space-y-5">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <div>
          <div class="text-[11px] uppercase tracking-wide text-ink-soft font-mono mb-1">Assessment Results</div>
          <div class="flex items-center gap-4 flex-wrap">
            ${severityBadge(a.severity)}
            <span class="font-mono text-sm">Risk Score: <strong class="text-base">${a.risk_score}</strong> / 100</span>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <span id="speak-status" class="text-xs text-ink-soft hidden"></span>
          <button type="button" id="speak-stop"
            class="hidden text-xs font-mono uppercase tracking-wide border border-line px-2 py-1 hover:border-brand transition-colors">
            Stop
          </button>
        </div>
      </div>

      <div>
        <div class="text-xs font-mono uppercase tracking-wide text-ink-soft mb-1">Clinical Reasoning</div>
        <p class="text-sm leading-relaxed">${escapeHtml(a.reasoning)}</p>
      </div>

      <div>
        <div class="text-xs font-mono uppercase tracking-wide text-ink-soft mb-1">Suggested Follow-up Question</div>
        <p class="text-sm leading-relaxed italic text-ink-soft">${escapeHtml(a.follow_up_question)}</p>
      </div>

      ${escalationNote}

      <details class="text-xs">
        <summary class="cursor-pointer font-mono uppercase tracking-wide text-ink-soft">Technical Details</summary>
        <pre class="mt-2 p-3 bg-console text-paper-raised overflow-x-auto font-mono text-[11px]">${escapeHtml(JSON.stringify(response.current_state, null, 2))}</pre>
      </details>
    </div>
  `;
}
