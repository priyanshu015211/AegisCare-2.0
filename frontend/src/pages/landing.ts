const icon = (name: string) => {
  const paths: Record<string, string> = {
    arrow: '<path d="M5 12h14M13 6l6 6-6 6"/>',
    arrowUpRight: '<path d="M7 17 17 7M8 7h9v9"/>',
    clipboard: '<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4.5h6M9 10h6M9 14h6M9 18h3"/>',
    pulse: '<path d="M3 12h4l3-8 4 16 3-8h4"/>',
    route: '<circle cx="6" cy="18" r="2"/><circle cx="18" cy="6" r="2"/><path d="M8 18h3a4 4 0 0 0 4-4V10a4 4 0 0 1 4-4"/>',
    shield: '<path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z"/><path d="M9 12h6"/>',
    heart: '<path d="M20.8 8.7c0 5.2-8.8 10-8.8 10s-8.8-4.8-8.8-10A4.7 4.7 0 0 1 12 6.4a4.7 4.7 0 0 1 8.8 2.3Z"/>',
  };
  return `<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">${paths[name] ?? ''}</svg>`;
};

export function renderLanding(root: HTMLElement) {
  root.innerHTML = `
    <div class="public-site">
      <a class="public-skip-link" href="#main-content">Skip to content</a>
      <header class="public-header">
        <a class="public-brand" href="#home" aria-label="AegisCare home">
          <span class="public-brand-mark">A</span>
          <span class="public-brand-copy"><strong>AegisCare</strong><small>Care, connected.</small></span>
        </a>
        <nav class="public-nav" aria-label="Main navigation">
          <a href="#our-approach">Why AegisCare</a>
          <a href="#how-it-works">How it works</a>
          <a href="#for-care-teams">For care teams</a>
        </nav>
        <a class="public-header-cta" href="#dashboard">Care team workspace ${icon('arrowUpRight')}</a>
      </header>

      <main id="main-content">
        <section class="public-hero" id="home" aria-labelledby="hero-title">
          <div class="public-hero-copy">
            <p class="public-eyebrow"><span class="eyebrow-rule"></span> A little more continuity in care</p>
            <h1 id="hero-title">You shouldn't have to tell your story <em>twice.</em></h1>
            <p class="public-hero-lede">When care moves between people and places, the details that matter can get left behind. AegisCare is being built to help care teams keep assessments, urgent reviews and next steps connected — with the person at the centre.</p>
            <div class="public-hero-actions">
              <a class="public-button-primary" href="#how-it-works">See the care journey ${icon('arrow')}</a>
              <a class="public-text-link" href="#dashboard">For care teams <span>↗</span></a>
            </div>
            <div class="public-hero-assurance">
              <span class="assurance-mark">${icon('heart')}</span>
              <span><strong>One person. One connected story.</strong><small>Designed around the moments between visits.</small></span>
            </div>
          </div>

          <div class="public-hero-visual care-story-visual" role="img" aria-label="An original illustration of one continuous care thread connecting listening, review and the next step.">
            <div class="care-visual-topline"><span>A CARE STORY, HELD TOGETHER</span><span>AEGISCARE / 01</span></div>
            <div class="care-story-art">
              <div class="story-coral-sun"></div>
              <div class="story-soft-shape"></div>
              <svg class="story-thread" viewBox="0 0 640 500" fill="none" aria-hidden="true">
                <path d="M70 344C156 344 167 274 245 278C320 282 315 206 386 206C466 206 485 171 580 174" stroke="#F4ECDF" stroke-width="20" stroke-linecap="round" opacity=".28"/>
                <path d="M70 344C156 344 167 274 245 278C320 282 315 206 386 206C466 206 485 171 580 174" stroke="#F4ECDF" stroke-width="5" stroke-linecap="round"/>
                <path d="M70 344C156 344 167 274 245 278C320 282 315 206 386 206C466 206 485 171 580 174" stroke="#F88A79" stroke-width="2" stroke-linecap="round" stroke-dasharray="1 13"/>
                <circle cx="70" cy="344" r="25" fill="#F4ECDF" stroke="#F88A79" stroke-width="3"/>
                <circle cx="70" cy="344" r="7" fill="#3D574A"/>
                <circle cx="386" cy="206" r="30" fill="#F4ECDF" stroke="#F88A79" stroke-width="3"/>
                <path d="M386 216s-13-8-13-16a7 7 0 0 1 13-4 7 7 0 0 1 13 4c0 8-13 16-13 16Z" stroke="#3D574A" stroke-width="2" stroke-linejoin="round"/>
                <circle cx="580" cy="174" r="25" fill="#F4ECDF" stroke="#F88A79" stroke-width="3"/>
                <circle cx="580" cy="174" r="7" fill="#3D574A"/>
              </svg>
              <div class="story-overline">THE DETAILS THAT MATTER</div>
              <div class="story-statement">Shouldn't have to<br><em>start over.</em></div>
              <div class="story-step story-step-one"><span>01</span><strong>Be heard</strong><small>Start with the person</small></div>
              <div class="story-step story-step-two"><span>02</span><strong>Be understood</strong><small>Keep the context close</small></div>
              <div class="story-step story-step-three"><span>03</span><strong>Move forward</strong><small>Make the next step clearer</small></div>
              <div class="story-handwritten">care, carried forward</div>
            </div>
            <div class="care-visual-footer"><span>Listen carefully. Keep the story connected.</span><span>OUR GUIDING IDEA</span></div>
          </div>
        </section>

        <section class="public-belief" id="our-approach" aria-labelledby="belief-title">
          <div class="belief-label"><span>01</span><span>Why we're building AegisCare</span></div>
          <div class="belief-main">
            <h2 id="belief-title">A person remembers the conversation. A system remembers the record. <em>Care needs both.</em></h2>
            <div class="belief-copy">
              <p>A person may explain what is happening, answer the same questions again, wait for a review and then meet a new team. From their side, it is one experience — even when the work behind it crosses departments and facilities.</p>
              <p>AegisCare is being built around that gap. It gives care teams a clearer way to follow assessments, urgent reviews and coordination activity, so the context behind a decision is easier to find when the next person needs it.</p>
            </div>
          </div>
          <div class="belief-foot"><span class="belief-foot-mark">A</span><span>The record matters.<br><strong>The person matters more.</strong></span></div>
        </section>

        <section class="public-how" id="how-it-works" aria-labelledby="how-title">
          <div class="public-section-intro">
            <p class="public-eyebrow"><span class="eyebrow-rule"></span> The care journey</p>
            <h2 id="how-title">Small moments of clarity. <em>One more connected experience.</em></h2>
            <p class="section-intro-copy">The platform is designed around practical work that helps care teams keep a person's situation in view.</p>
          </div>
          <div class="care-journey">
            <article class="journey-step">
              <div class="journey-step-top"><span class="journey-number">01</span><span class="journey-icon">${icon('clipboard')}</span></div>
              <h3>Listen & assess</h3>
              <p>Bring reported concerns and relevant details into a structured assessment that a care professional can review.</p>
            </article>
            <article class="journey-step">
              <div class="journey-step-top"><span class="journey-number">02</span><span class="journey-icon">${icon('pulse')}</span></div>
              <h3>Notice what needs attention</h3>
              <p>Make risk indicators and cases requiring timely review easier for the team to see.</p>
            </article>
            <article class="journey-step">
              <div class="journey-step-top"><span class="journey-number">03</span><span class="journey-icon">${icon('route')}</span></div>
              <h3>Coordinate what comes next</h3>
              <p>Keep facility context and coordination activity visible as the team works through the next step.</p>
            </article>
          </div>
          <p class="journey-disclaimer">This is an illustrative workflow. Clinical assessment and decisions remain with qualified healthcare professionals.</p>
        </section>

        <section class="public-workspace" id="for-care-teams" aria-labelledby="workspace-title">
          <div class="workspace-copy">
            <p class="public-eyebrow"><span class="eyebrow-rule"></span> For the people providing care</p>
            <h2 id="workspace-title">The tools should make room for <em>the human work.</em></h2>
            <p>Behind every clear patient conversation is a care team doing careful, often invisible work. AegisCare is being built to make that work easier to see, share and follow through.</p>
            <a class="public-button-primary" href="#dashboard">Explore the care team workspace ${icon('arrow')}</a>
          </div>
          <div class="workspace-list" aria-label="Care team workspace areas">
            <div class="workspace-row"><span class="workspace-row-icon">${icon('clipboard')}</span><span><strong>Patient triage</strong><small>Assessment details and risk review</small></span><span class="workspace-row-number">01</span></div>
            <div class="workspace-row"><span class="workspace-row-icon">${icon('pulse')}</span><span><strong>Emergency centre</strong><small>Urgent cases and escalation visibility</small></span><span class="workspace-row-number">02</span></div>
            <div class="workspace-row"><span class="workspace-row-icon">${icon('route')}</span><span><strong>Care coordination</strong><small>Facility context and connected next steps</small></span><span class="workspace-row-number">03</span></div>
          </div>
        </section>

        <section class="public-ai" aria-labelledby="ai-title">
          <div class="ai-symbol">${icon('shield')}</div>
          <div class="ai-copy">
            <p class="public-eyebrow">A principle, not a promise</p>
            <h2 id="ai-title">Technology can support care. <em>It should never stand in for it.</em></h2>
            <p>AI-assisted tools should help organise information and bring relevant context forward for review. They should not diagnose a person or make care decisions on their own. Clinical judgement, accountability and the human conversation stay with qualified professionals.</p>
          </div>
        </section>

        <section class="public-patient-note" aria-labelledby="patient-note-title">
          <div class="patient-note-label">A note for patients & families</div>
          <div>
            <h2 id="patient-note-title">Looking for medical help right now?</h2>
            <p>AegisCare is being developed as a coordination tool for care teams. It is not a medical advice service or an emergency response service. For personal medical concerns, contact a qualified healthcare professional. If someone may be experiencing an emergency, contact local emergency services or go to the nearest emergency department.</p>
          </div>
        </section>
      </main>

      <footer class="public-footer">
        <a class="public-brand" href="#home"><span class="public-brand-mark">A</span><span class="public-brand-copy"><strong>AegisCare</strong><small>Care, connected.</small></span></a>
        <span class="footer-thought">Care, carried forward.</span>
        <a href="#dashboard">Care team access ${icon('arrowUpRight')}</a>
      </footer>
    </div>
  `;
}
