import { api, type AnalyzeResponse } from '../api/client';
import { panelHeader, severityBadge, escapeHtml } from '../components/ui';

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
            <label class="text-xs font-mono uppercase tracking-wide text-ink-soft">Symptoms (comma separated)</label>
            <textarea name="symptoms" rows="4" placeholder="fever, cough, fatigue"
              class="mt-1 w-full border border-line bg-white px-3 py-2 text-sm focus:outline-none focus:border-brand">fever, cough, fatigue</textarea>
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

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorBox.classList.add('hidden');

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

function buildResults(response: AnalyzeResponse): string {
  const a = response.analysis;
  const escalationNote = a.escalation_needed
    ? `<div class="border border-severity-high text-severity-high text-sm px-3 py-2" style="background: color-mix(in srgb, var(--color-severity-high) 8%, transparent)">This case may require escalation based on current symptoms.</div>`
    : `<div class="border border-line text-ink-soft text-sm px-3 py-2 bg-paper">No immediate escalation required based on current assessment.</div>`;

  return `
    <div class="space-y-5">
      <div>
        <div class="text-[11px] uppercase tracking-wide text-ink-soft font-mono mb-1">Assessment Results</div>
        <div class="flex items-center gap-4 flex-wrap">
          ${severityBadge(a.severity)}
          <span class="font-mono text-sm">Risk Score: <strong class="text-base">${a.risk_score}</strong> / 100</span>
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
