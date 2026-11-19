const icon = (name: string) => {
  const paths: Record<string, string> = {
    arrow: '<path d="M5 12h14M13 6l6 6-6 6"/>',
    arrowUpRight: '<path d="M7 17 17 7M8 7h9v9"/>',
    people: '<path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="10" cy="7" r="4"/><path d="M20 21v-2a4 4 0 0 0-3-3.9M16 3.2a4 4 0 0 1 0 7.6"/>',
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
          <span class="public-brand-copy"><strong>AegisCare</strong><small>Care coordination</small></span>
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
            <p class="public-eyebrow"><span class="eyebrow-rule"></span> AegisCare / Care coordination</p>
            <h1 id="hero-title">When care gets complicated, <em>people still need to feel seen.</em></h1>
            <p class="public-hero-lede">AegisCare is being built to help care teams keep assessments, urgent concerns and facility coordination connected — so people don't get lost between the steps.</p>
            <div class="public-hero-actions">
              <a class="public-button-primary" href="#our-approach">Why this matters ${icon('arrow')}</a>
              <a class="public-text-link" href="#dashboard">I'm part of a care team <span>↗</span></a>
            </div>
            <p class="public-hero-note"><span class="note-line"></span> Built around a simple belief: the person comes before the process.</p>
          </div>

          <figure class="public-hero-figure">
            <div class="public-photo-wrap">
              <img
                class="public-hero-photo"
                src="https://images.unsplash.com/photo-1758691462954-e6fa5005474b?auto=format&fit=crop&w=1800&q=85"
                alt="A doctor and patient seated across a desk during a consultation"
                fetchpriority="high"
              />
              <span class="photo-index">A CARE CONVERSATION / 01</span>
            </div>
            <figcaption>
              <span class="photo-caption-main">Before there is a plan, there is a person to listen to.</span>
              <span class="photo-credit">Photo by <a href="https://unsplash.com/@vitalygariev" target="_blank" rel="noreferrer">Vitaly Gariev</a> on <a href="https://unsplash.com/photos/doctor-consulting-with-patient-at-desk-GBcLdqOBbXE" target="_blank" rel="noreferrer">Unsplash</a></span>
            </figcaption>
          </figure>
        </section>

        <section class="public-belief" id="our-approach" aria-labelledby="belief-title">
          <div class="belief-label"><span>01</span><span>Why we're building AegisCare</span></div>
          <div class="belief-main">
            <h2 id="belief-title">A person doesn't experience care in departments. They experience the <em>moments between them.</em></h2>
            <div class="belief-copy">
              <p>One conversation becomes an assessment. An assessment may need an urgent review. A review may lead to another team or another facility. Every handoff carries a little more of someone's story.</p>
              <p>When that context is hard to find, people can be left repeating themselves and care teams can be left piecing together what happened before. AegisCare is being built to make those connections easier to follow.</p>
            </div>
          </div>
          <div class="belief-foot"><span class="belief-foot-mark">A</span><span>Not another layer between people and care.<br><strong>A clearer way for the people providing it to work together.</strong></span></div>
        </section>

        <section class="public-how" id="how-it-works" aria-labelledby="how-title">
          <div class="public-section-intro">
            <p class="public-eyebrow"><span class="eyebrow-rule"></span> The care journey</p>
            <h2 id="how-title">A clearer next step can make <em>a difficult day feel more manageable.</em></h2>
            <p class="section-intro-copy">The platform is designed around the practical work that helps care teams keep a person's situation in view.</p>
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
            <h2 id="workspace-title">A shared picture for the people doing <em>the work.</em></h2>
            <p>AegisCare brings key parts of care coordination into one workspace. Not to make healthcare feel more technical, but to make the day-to-day work easier to follow.</p>
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
        <a class="public-brand" href="#home"><span class="public-brand-mark">A</span><span class="public-brand-copy"><strong>AegisCare</strong><small>Care coordination</small></span></a>
        <span class="footer-thought">Keep the person in view.</span>
        <a href="#dashboard">Care team access ${icon('arrowUpRight')}</a>
      </footer>
    </div>
  `;
}
