const state = {
  page: "today",
  wizardStep: 1,
  objective: "Reconhecer palavra e imagem",
  sourceReady: false,
  word: "MAÇÃ",
  preview: "phone",
  published: false,
  observationSaved: false
};

const pages = [
  ["today", "⌂", "Hoje"],
  ["stories", "▣", "Histórias"],
  ["classes", "◉", "Turmas"],
  ["reports", "↗", "Acompanhamento"]
];

const titles = {
  today: ["Visão da aula", "Hoje"],
  stories: ["Preparar experiências", "Histórias"],
  wizard: ["Autoria guiada", "Criar história"],
  review: ["Revisão docente", "Prévia da história"],
  classes: ["Aplicar com segurança", "Turmas"],
  reports: ["Observar e replanejar", "Acompanhamento"]
};

function navMarkup() {
  return pages.map(([id, icon, label]) => `
    <button class="nav-button" type="button" data-page="${id}" ${state.page === id ? 'aria-current="page"' : ""}>
      <span aria-hidden="true">${icon}</span><span>${label}</span>
    </button>`).join("");
}

function status(label, type = "") {
  return `<span class="status ${type}">${label}</span>`;
}

function button(label, action, style = "") {
  return `<button class="button ${style}" type="button" data-action="${action}">${label}</button>`;
}

function todayView() {
  return `
    <section class="hero">
      <div>
        ${status("Aula de hoje", "ready")}
        <h2>A história está pronta para a turma</h2>
        <p><strong>A maçã do lanche da LÉIA</strong> combina quadrinho, puzzle 2×2, formação da palavra e conversa em dupla.</p>
        <div class="inline-actions">${button("Iniciar com a turma", "go-classes", "primary")} ${button("Ver como criança", "go-review", "quiet")}</div>
      </div>
      <div class="hero-figure" aria-label="Maçã da história">🍎</div>
    </section>
    <div class="section-head"><div><h2>Preparação dos aparelhos</h2><p>Estado confirmado pelos próprios dispositivos.</p></div>${button("Ver detalhes", "go-classes", "quiet")}</div>
    <section class="grid">
      <article class="card"><p>Prontos</p><div class="metric">18</div>${status("Pode iniciar", "ready")}</article>
      <article class="card"><p>Preparando</p><div class="metric">4</div>${status("Baixando recursos", "wait")}</article>
      <article class="card"><p>Sem conexão</p><div class="metric">2</div>${status("Acervo local disponível", "offline")}</article>
    </section>
    <div class="section-head"><div><h2>Próxima ação</h2><p>Uma orientação direta, sem configurações técnicas.</p></div></div>
    <article class="card callout"><strong>Enquanto quatro aparelhos terminam de preparar:</strong> organize duplas e use os dois aparelhos offline com a história-base já instalada.</article>`;
}

function storiesView() {
  return `
    <div class="section-head">
      <div><h2>Suas histórias</h2><p>Crie, revise e publique experiências dentro do InterpretaAI.</p></div>
      ${button("+ Criar história", "new-story", "primary")}
    </div>
    <div class="filters"><button class="choice selected" type="button">Acervo</button><button class="choice" type="button">Rascunhos</button><button class="choice" type="button">Publicadas</button></div>
    <section class="grid" style="margin-top:16px">
      <article class="card"><div class="story-thumb">🍎</div>${status("Publicada v1", "ready")}<h3>A maçã do lanche da LÉIA</h3><p>Compreensão, palavra e explicação em dupla.</p><div class="story-meta"><span class="status">8 min</span><span class="status">1º ano</span></div>${button("Abrir história", "go-review", "quiet")}</article>
      <article class="card"><div class="story-thumb">⚽</div>${status("Acervo") }<h3>Onde está a bola?</h3><p>Pistas do cotidiano, puzzle e formação de palavra.</p><div class="story-meta"><span class="status">7 min</span><span class="status">1º–2º</span></div>${button("Usar como modelo", "new-story", "quiet")}</article>
      <article class="card"><div class="story-thumb">🌳</div>${status("Rascunho", "wait")}<h3>A sombra da árvore</h3><p>Aguardando confirmação do objeto principal.</p><div class="story-meta"><span class="status">Salvo agora</span></div>${button("Continuar revisão", "new-story", "quiet")}</article>
    </section>`;
}

function wizardSteps() {
  const labels = ["Objetivo", "Contexto", "Confirmar", "Proposta", "Revisar", "Publicar"];
  return labels.map((label, index) => {
    const number = index + 1;
    const cls = number === state.wizardStep ? "active" : number < state.wizardStep ? "done" : "";
    return `<div class="step ${cls}"><span>${number < state.wizardStep ? "✓" : number}</span><b>${label}</b></div>`;
  }).join("");
}

function wizardBody() {
  if (state.wizardStep === 1) return `
    <h2>O que a turma vai explorar?</h2><p class="muted">Escolha um objetivo. A IA não decide o objetivo pela professora.</p>
    <div class="form-stack"><div class="choices">
      ${["Reconhecer palavra e imagem", "Interpretar uma pista", "Relacionar fonema e letra", "Explicar para uma dupla"].map(item => `<button class="choice ${state.objective === item ? "selected" : ""}" type="button" data-objective="${item}">${item}</button>`).join("")}
    </div><label class="field">Faixa organizacional<select><option>1º ano</option><option>2º ano</option><option>Turma multiano</option></select><small>Serve para organizar, não para diagnosticar.</small></label>
    <div class="inline-actions">${button("Continuar", "wizard-next", "primary")}</div></div>`;
  if (state.wizardStep === 2) return `
    <h2>Qual imagem ou tema inicia a história?</h2><p class="muted">Use material próprio, biblioteca aprovada ou apenas um tema.</p>
    <div class="form-stack"><div class="dropzone">
      ${state.sourceReady ? '<div><div class="demo-apple">🍎</div><strong>Imagem de demonstração selecionada</strong><p>Objeto provável: maçã</p></div>' : '<div><strong>Arraste uma imagem aqui</strong><p>ou use um exemplo seguro para percorrer o protótipo.</p><button class="button quiet" type="button" data-action="demo-image">Usar maçã de demonstração</button></div>'}
    </div><div class="inline-actions">${button("Voltar", "wizard-back", "quiet")} ${button("Continuar", "wizard-next", "primary")}</div></div>`;
  if (state.wizardStep === 3) return `
    <h2>Confirme antes de gerar</h2><p class="muted">A professora resolve ambiguidades; o sistema não escolhe silenciosamente.</p>
    <div class="form-stack">
      <label class="field">Palavra principal<input id="target-word" value="${state.word}" maxlength="24"><small>Será usada na história, no puzzle e na formação da palavra.</small></label>
      <label class="field">Duração<select><option>8 minutos</option><option>5 minutos</option><option>12 minutos</option></select></label>
      <label class="field">Participação<select><option>Em dupla</option><option>Individual</option><option>Em grupo</option></select></label>
      <div class="callout"><strong>Será criado:</strong> quadrinho → puzzle 2×2 → quadrinho → formação da palavra → conversa em dupla.</div>
      <div class="inline-actions">${button("Voltar", "wizard-back", "quiet")} ${button("Criar proposta", "save-word", "primary")}</div>
    </div>`;
  if (state.wizardStep === 4) return `
    <h2>Proposta pronta para revisão</h2><p class="muted">No produto real esta tarefa será assíncrona e poderá continuar em segundo plano.</p>
    <div class="job-steps">
      ${["Imagem sanitizada e analisada", "Referências pedagógicas recuperadas", "Roteiro estruturado", "Imagens e atividades preparadas", "Contrato e acessibilidade validados"].map((item, i) => `<div class="job-row"><strong>✓</strong><span>${item}</span><small>${i === 4 ? "sem bloqueios" : "concluído"}</small></div>`).join("")}
    </div><div class="inline-actions" style="margin-top:18px">${button("Revisar história", "wizard-next", "primary")} ${button("Ver fontes usadas", "show-sources", "quiet")}</div>`;
  if (state.wizardStep === 5) return reviewView(true);
  return publishView();
}

function wizardView() {
  return `<button class="button quiet" type="button" data-action="exit-wizard">← Voltar às histórias</button>
    <div class="section-head"><div><p class="eyebrow">Novo rascunho</p><h2>A maçã do lanche da LÉIA</h2></div>${status("Salvo automaticamente", "ready")}</div>
    <section class="wizard"><aside class="steps" aria-label="Etapas da criação">${wizardSteps()}</aside><article class="card">${wizardBody()}</article></section>`;
}

function reviewView(inWizard = false) {
  return `<div class="review-layout">
    <section class="card">
      <div class="section-head"><div><h2>Prévia infantil</h2><p>A mesma composição que o aparelho executará.</p></div><div class="inline-actions"><button class="choice ${state.preview === "phone" ? "selected" : ""}" type="button" data-preview="phone">Celular</button><button class="choice ${state.preview === "tablet" ? "selected" : ""}" type="button" data-preview="tablet">Tablet</button></div></div>
      <div class="device-preview"><div class="device ${state.preview === "tablet" ? "tablet" : ""}"><div class="comic-frame">👩🏽‍🏫 🐕 🍎</div><div class="speech">“Meu lanche sumiu. Você me ajuda a descobrir qual fruta estava aqui?”</div><button class="button primary" type="button">▶ Ouvir e continuar</button></div></div>
    </section>
    <aside class="card"><p class="eyebrow">Cena 1 de 6</p><h3>A cesta vazia</h3><div class="scene-list"><button class="scene-button selected" type="button">1. Quadrinho — pista</button><button class="scene-button" type="button">2. Puzzle 2×2</button><button class="scene-button" type="button">3. Descoberta</button><button class="scene-button" type="button">4. Formar ${state.word}</button><button class="scene-button" type="button">5. Conversa em dupla</button><button class="scene-button" type="button">6. Encerramento</button></div>
      <hr><label class="field">Palavra da história<input id="review-word" value="${state.word}"></label><div class="inline-actions" style="margin-top:12px">${button("Salvar palavra", "review-word", "quiet")} ${button("Refazer só esta imagem", "regenerate-scene", "quiet")}</div>
      <p class="evidence-ref">Fontes: método LEIA v2026-09 · acessibilidade v1</p>
    </aside>
  </div><div class="inline-actions" style="margin-top:18px">${button(inWizard ? "Voltar" : "Histórias", inWizard ? "wizard-back" : "exit-wizard", "quiet")} ${button(inWizard ? "Continuar para publicar" : "Enviar para turma", inWizard ? "wizard-next" : "publish-direct", "primary")}</div>`;
}

function publishView() {
  return `<h2>Publicar a versão revisada</h2><p class="muted">A publicação envia a versão exata; os aparelhos preparam o cache automaticamente.</p>
    <div class="form-stack"><div class="callout"><strong>Sem bloqueios.</strong> 2 avisos revisados: imagem assistida e uso em dupla.</div>
      <label class="field">Destino<select><option>1º ano A — todos os aparelhos</option><option>Grupo Azul — 6 aparelhos</option></select></label>
      <label class="field">Quando fica disponível?<select><option>Agora</option><option>Na próxima aula</option></select></label>
      <article class="card"><strong>A maçã do lanche da LÉIA — versão 1</strong><p>6 cenas · 8 minutos · 3 imagens · 1 puzzle · 1 palavra · 1 conversa em dupla</p></article>
      <div class="inline-actions">${button("Voltar", "wizard-back", "quiet")} ${button(state.published ? "Publicada ✓" : "Publicar e preparar aparelhos", "publish", state.published ? "quiet" : "primary")}</div>
    </div>`;
}

function classesView() {
  return `<div class="section-head"><div><h2>1º ano A</h2><p>24 aparelhos pareados · atividade atual: A maçã do lanche da LÉIA v1</p></div>${button("Atribuir história", "go-stories", "primary")}</div>
    <section class="grid"><article class="card"><p>Prontos</p><div class="metric">18</div>${status("Confirmado pelo aparelho", "ready")}</article><article class="card"><p>Preparando</p><div class="metric">4</div>${status("62% em média", "wait")}</article><article class="card"><p>Sem conexão</p><div class="metric">2</div>${status("Acervo-base disponível", "offline")}</article></section>
    <div class="section-head"><div><h2>Aparelhos</h2><p>“Publicada” e “pronta” são estados diferentes.</p></div></div>
    <section class="card">
      <div class="device-row"><div><strong>Tablet 01 · Grupo Sol</strong><p class="muted">Último contato agora</p></div>${status("Pronta", "ready")}<div class="progress"><span style="width:100%"></span></div></div>
      <div class="device-row"><div><strong>Tablet 02 · Grupo Lua</strong><p class="muted">Baixando primeira sequência</p></div>${status("Preparando", "wait")}<div class="progress"><span style="width:68%"></span></div></div>
      <div class="device-row"><div><strong>Celular 03 · Grupo Rio</strong><p class="muted">Sem contato há 12 minutos</p></div>${status("Sem conexão", "offline")}<div class="progress"><span style="width:0%"></span></div></div>
      <div class="device-row"><div><strong>Tablet 04 · Grupo Mata</strong><p class="muted">Pacote e hashes verificados</p></div>${status("Pronta", "ready")}<div class="progress"><span style="width:100%"></span></div></div>
    </section>`;
}

function reportsView() {
  return `<div class="section-head"><div><h2>Evidências da turma</h2><p>Participação contextualizada, sem nota, ranking ou diagnóstico.</p></div><div class="filters"><select><option>Últimos 7 dias</option></select><select><option>Todas as histórias</option></select></div></div>
    <section class="grid"><article class="card"><p>Sessões iniciadas</p><div class="metric">22</div><span class="evidence-ref">Aplicativo · 1º ano A</span></article><article class="card"><p>Pedidos de apoio</p><div class="metric">9</div><span class="evidence-ref">em 5 de 22 sessões</span></article><article class="card"><p>Conversas em dupla</p><div class="metric">18</div><span class="evidence-ref">passagens registradas</span></article></section>
    <div class="section-head"><div><h2>O que observar</h2><p>Cada síntese mostra origem, amostra e limite.</p></div></div>
    <section class="grid two">
      <article class="card evidence"><span class="evidence-ref">APLICATIVO · HISTÓRIA DA MAÇÃ</span><h3>Apoio visual apareceu em 5 sessões</h3><p>Em quatro delas, a interação foi concluída depois da pista visual. Isso descreve a amostra e não conclui dificuldade.</p><button class="button quiet" type="button">Ver 5 referências</button></article>
      <article class="card evidence"><span class="evidence-ref">ASSISTENTE · REVISÃO NECESSÁRIA</span><h3>Possível retomada</h3><p>Experimentar uma nova história curta com pista oral antes da pista visual e observar a participação em dupla.</p><button class="button quiet" type="button" data-action="accept-suggestion">Usar no planejamento</button></article>
    </section>
    <div class="section-head"><div><h2>Registrar observação</h2><p>Seu texto permanece humano e tem histórico de edição.</p></div></div>
    <form class="card observation-form" id="observation-form"><label class="field">Observação da professora<textarea id="observation" placeholder="O que você observou durante a atividade?">${state.observationSaved ? "A turma usou a pista da árvore para justificar a escolha da maçã em duplas." : ""}</textarea></label><div class="inline-actions" style="margin-top:12px"><button class="button primary" type="submit">Salvar observação</button></div></form>`;
}

function render() {
  document.querySelector("#desktop-nav").innerHTML = navMarkup();
  document.querySelector("#mobile-nav").innerHTML = navMarkup();
  document.querySelector("#page-eyebrow").textContent = titles[state.page][0];
  document.querySelector("#page-title").textContent = titles[state.page][1];
  const view = document.querySelector("#view");
  if (state.page === "today") view.innerHTML = todayView();
  if (state.page === "stories") view.innerHTML = storiesView();
  if (state.page === "wizard") view.innerHTML = wizardView();
  if (state.page === "review") view.innerHTML = reviewView(false);
  if (state.page === "classes") view.innerHTML = classesView();
  if (state.page === "reports") view.innerHTML = reportsView();
  bindForms();
}

function showToast(message) {
  const toast = document.querySelector("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  window.setTimeout(() => toast.classList.remove("show"), 2200);
}

function bindForms() {
  const form = document.querySelector("#observation-form");
  if (form) form.addEventListener("submit", event => {
    event.preventDefault();
    state.observationSaved = true;
    showToast("Observação salva com histórico.");
  });
}

document.addEventListener("click", event => {
  const pageButton = event.target.closest("[data-page]");
  if (pageButton) {
    state.page = pageButton.dataset.page;
    render();
    return;
  }
  const objective = event.target.closest("[data-objective]");
  if (objective) {
    state.objective = objective.dataset.objective;
    render();
    return;
  }
  const preview = event.target.closest("[data-preview]");
  if (preview) {
    state.preview = preview.dataset.preview;
    render();
    return;
  }
  const action = event.target.closest("[data-action]")?.dataset.action;
  if (!action) return;
  if (action === "new-story") { state.page = "wizard"; state.wizardStep = 1; }
  if (action === "exit-wizard" || action === "go-stories") state.page = "stories";
  if (action === "go-classes") state.page = "classes";
  if (action === "go-review") state.page = "review";
  if (action === "publish-direct") { state.page = "wizard"; state.wizardStep = 6; }
  if (action === "demo-image") state.sourceReady = true;
  if (action === "wizard-back") state.wizardStep = Math.max(1, state.wizardStep - 1);
  if (action === "wizard-next") {
    if (state.wizardStep === 2 && !state.sourceReady) return showToast("Escolha uma imagem ou tema para continuar.");
    state.wizardStep = Math.min(6, state.wizardStep + 1);
  }
  if (action === "save-word") {
    const value = document.querySelector("#target-word")?.value.trim().toUpperCase();
    if (!value) return showToast("Confirme a palavra principal.");
    state.word = value;
    state.wizardStep = 4;
  }
  if (action === "review-word") {
    const value = document.querySelector("#review-word")?.value.trim().toUpperCase();
    if (!value) return showToast("A palavra não pode ficar vazia.");
    state.word = value;
    showToast("Palavra atualizada em todas as cenas relacionadas.");
  }
  if (action === "regenerate-scene") showToast("Nova variante solicitada apenas para esta imagem.");
  if (action === "show-sources") showToast("2 fontes versionadas · nenhuma fonte infantil.");
  if (action === "publish") { state.published = true; showToast("Versão publicada. Preparação automática iniciada."); }
  if (action === "accept-suggestion") showToast("Sugestão adicionada como rascunho de planejamento.");
  if (action === "profile") showToast("Perfil e Administração ficam separados do trabalho diário.");
  render();
});

render();
