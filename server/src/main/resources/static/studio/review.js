const studio = { schoolId: null, reviews: [], review: null, confirmed: new Set(), csrf: null,
  classrooms: [], assignments: [], preparation: new Map(), pendingAssignments: new Map(),
  classroomSession: null, roster: [] };
const byId = id => document.getElementById(id);
const sceneNames = {
  COMIC: "Quadrinho", PUZZLE: "Quebra-cabeça", WORD_BUILDER: "Formar a palavra",
  GROUP_HANDOFF: "Conversa em dupla", END: "Encerramento"
};
const speakerNames = { LEIA_TEACHER: "LÉIA", NARRATOR: "Narrador", CHILD_CHARACTER: "Criança", DOG: "Cachorro" };
const roleNames = { PHONE: "Celular", TABLET: "Tablet", THUMBNAIL: "Miniatura", AUDIO: "Áudio" };

function element(tag, value, className) {
  const node = document.createElement(tag);
  if (value != null) node.textContent = value;
  if (className) node.className = className;
  return node;
}

function feedback(message, isError = false) {
  const target = byId("feedback");
  target.textContent = message;
  target.style.background = isError ? "#fff0e5" : "#eaf0ff";
}

async function api(path, options = {}) {
  const response = await fetch(path, { credentials: "same-origin", cache: "no-store", ...options });
  if (!response.ok) {
    let message = "Não foi possível concluir. Atualize a revisão e tente novamente.";
    try { message = (await response.json()).safeMessage || message; } catch (_) { /* no body */ }
    throw new Error(message);
  }
  if (response.status === 204) return null;
  return response.json();
}

function route(item = studio.review) {
  return `/studio/api/schools/${studio.schoolId}/stories/${item.storyId}/versions/${item.version}`;
}

function post(path, body, idempotencyKey = crypto.randomUUID()) {
  return api(path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": studio.csrf,
      "Idempotency-Key": idempotencyKey
    },
    body: body == null ? undefined : JSON.stringify(body)
  });
}

function put(path, body) {
  return api(path, {
    method: "PUT",
    headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": studio.csrf },
    body: JSON.stringify(body)
  });
}

function classroomBase(classroomId = byId("roster-classroom").value) {
  return `/studio/api/schools/${studio.schoolId}/classrooms/${classroomId}`;
}

function sessionStorageKey(classroomId = byId("roster-classroom").value) {
  return `studio-classroom-session:${studio.schoolId}:${classroomId}`;
}

function renderSeatBoard(seats = studio.roster.map(item => ({ ...item, connected: false }))) {
  const board = byId("session-board");
  board.replaceChildren();
  if (!seats.length) {
    board.append(element("p", "A lista desta turma ainda está vazia.", "muted"));
    return;
  }
  const summary = element("p", `${seats.filter(item => item.connected).length} de ${seats.length} tablets conectados.`, "muted");
  const grid = element("div", null, "seat-grid");
  for (const seat of seats) {
    const item = element("div", null, `seat${seat.connected ? " connected" : ""}`);
    item.append(element("strong", `${seat.seatNumber}. ${seat.displayName}`));
    item.append(element("small", seat.connected ? "Tablet conectado" : "Aguardando tablet"));
    grid.append(item);
  }
  board.append(summary, grid);
}

async function loadRoster() {
  const classroomId = byId("roster-classroom").value;
  if (!classroomId) return;
  const roster = await api(`${classroomBase(classroomId)}/roster`);
  studio.roster = roster.learners;
  byId("roster-names").value = studio.roster.map(item => item.displayName).join("\n");
  renderSeatBoard();
  studio.classroomSession = null;
  byId("classroom-session-result").hidden = true;
  let storedSession = null;
  try { storedSession = sessionStorage.getItem(sessionStorageKey(classroomId)); } catch (_) { /* unavailable */ }
  if (storedSession) {
    try {
      studio.classroomSession = await api(`/studio/api/schools/${studio.schoolId}/classroom-sessions/${storedSession}`);
      if (studio.classroomSession.status === "ACTIVE") {
        byId("classroom-session-code").textContent = "AULA EM ANDAMENTO";
        byId("classroom-session-expiry").textContent = `Aberta até ${new Date(studio.classroomSession.expiresAt).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}.`;
        byId("classroom-session-result").hidden = false;
        renderSeatBoard(studio.classroomSession.seats);
      }
    } catch (_) {
      try { sessionStorage.removeItem(sessionStorageKey(classroomId)); } catch (_) { /* unavailable */ }
    }
  }
}

async function refreshClassroomSession() {
  if (!studio.classroomSession?.sessionId) return;
  studio.classroomSession = await api(
    `/studio/api/schools/${studio.schoolId}/classroom-sessions/${studio.classroomSession.sessionId}`);
  renderSeatBoard(studio.classroomSession.seats);
}

function renderList() {
  const list = byId("story-list");
  list.replaceChildren();
  for (const item of studio.reviews) {
    const card = element("article", null, "card");
    const state = item.state === "DRAFT" ? "Aguardando revisão"
      : item.state === "APPROVED" ? "Aprovada, não publicada" : "Publicada";
    card.append(element("span", state, "status wait"));
    card.append(element("h3", item.title));
    card.append(element("p", `Versão ${item.version} · revisão ${item.revision}`));
    const open = element("button", "Revisar história", "button quiet");
    open.type = "button";
    open.addEventListener("click", () => loadReview(item));
    card.append(open);
    list.append(card);
  }
  feedback(studio.reviews.length ? "Selecione uma história para conferir as cenas e mídias." :
    "Nenhum rascunho pronto para revisão nesta escola. A geração continua desligada até aprovação das fontes pedagógicas.");
}

async function loadList() {
  byId("review-section").hidden = true;
  byId("list-section").hidden = false;
  studio.reviews = await api(`/studio/api/schools/${studio.schoolId}/reviews`);
  renderList();
}

function renderScenes(pack) {
  const target = byId("story-scenes");
  target.replaceChildren();
  for (const [index, scene] of pack.nodes.entries()) {
    const card = element("article");
    card.append(element("strong", `${index + 1}. ${sceneNames[scene.type] || "Etapa"}`));
    if (scene.altText) card.append(element("p", `Imagem: ${scene.altText}`));
    if (scene.prompt) card.append(element("p", `Pergunta: ${scene.prompt}`));
    for (const line of scene.dialogue || []) card.append(element("p", `${speakerNames[line.speaker] || "Personagem"}: “${line.text}”`));
    if (scene.instruction) card.append(element("p", `Instrução: ${scene.instruction}`));
    if (scene.targetWord) card.append(element("p", `Palavra: ${scene.targetWord}`));
    if (scene.completionSpeech) card.append(element("p", `Conclusão: ${scene.completionSpeech}`));
    if (scene.closingSpeech) card.append(element("p", `Encerramento: ${scene.closingSpeech}`));
    target.append(card);
  }
  const sources = byId("story-sources");
  sources.replaceChildren(element("strong", "Procedência pedagógica"));
  const refs = pack.provenance?.sourceRefs || [];
  sources.append(element("p", refs.length
    ? refs.map(ref => `${ref.sourceId} · ${ref.sourceVersion}`).join("; ")
    : pack.provenance?.createdBy === "ASSISTED"
      ? "Nenhuma fonte declarada para conteúdo assistido. Não aprove sem esclarecer a origem."
      : "Conteúdo preparado pela professora; sem fonte externa declarada."));
}

function updateApproval() {
  const review = studio.review;
  const pending = review.assets.filter(item => !studio.confirmed.has(`${item.assetId}:${item.role}:${item.sha256}`));
  byId("approve").hidden = review.state !== "DRAFT";
  byId("approve").disabled = review.state !== "DRAFT" || pending.length > 0;
  byId("publish").hidden = review.state !== "APPROVED";
  byId("review-instruction").textContent = review.state === "DRAFT"
    ? pending.length ? `Confira e confirme ${pending.length} recurso(s) antes de aprovar.` : "Todas as variantes foram conferidas. A aprovação congela esta versão."
    : review.state === "APPROVED" ? "Aprovada. Publicar não envia automaticamente à turma."
      : "Publicada. Escolha uma turma; o preparo nos aparelhos será confirmado separadamente.";
  renderJourney();
}

function renderJourney() {
  if (!studio.review) return;
  const published = studio.review.state === "PUBLISHED";
  const assigned = studio.assignments.length > 0;
  const confirmed = studio.assignments.reduce((total, assignment) =>
    total + (studio.preparation.get(assignment.assignmentId)?.recentlyConfirmedDevices || 0), 0);
  const states = {
    review: studio.review.state === "DRAFT" ? ["active", "Em revisão"] : ["ready", "Revisada"],
    publish: published ? ["ready", "Publicada"]
      : studio.review.state === "APPROVED" ? ["active", "Pronta para publicar"] : ["", "Aguardando revisão"],
    send: assigned ? ["ready", "Enviada"] : published ? ["active", "Escolher turma"] : ["", "Aguardando publicação"],
    prepare: confirmed > 0 ? ["ready", `${confirmed} aparelho(s) pronto(s)`]
      : assigned ? ["active", "Confirmar aparelhos"] : ["", "Aguardando envio"]
  };
  for (const [name, [tone, status]] of Object.entries(states)) {
    const step = document.querySelector(`[data-journey="${name}"]`);
    step.className = `journey-step ${tone}`.trim();
    step.querySelector("small").textContent = status;
  }
}

function renderAssignments() {
  const selector = byId("classroom");
  const selected = selector.value;
  selector.replaceChildren();
  for (const classroom of studio.classrooms) {
    const option = document.createElement("option");
    option.value = classroom.classroomId;
    option.textContent = classroom.name;
    selector.append(option);
  }
  if (studio.classrooms.some(item => item.classroomId === selected)) selector.value = selected;
  const list = byId("assignment-list");
  list.replaceChildren();
  if (!studio.classrooms.length) {
    list.append(element("p", "Nenhuma turma vinculada está disponível nesta escola."));
  } else if (!studio.assignments.length) {
    list.append(element("p", "Ainda não disponibilizada a nenhuma turma acessível."));
  } else {
    for (const assignment of studio.assignments) {
      const name = studio.classrooms.find(item => item.classroomId === assignment.target.id)?.name || "Turma";
      const summary = studio.preparation.get(assignment.assignmentId);
      const detail = !summary ? "consultando aparelhos…"
        : summary.error ? "não foi possível verificar o preparo; tente atualizar."
        : summary.pairedCompatibleDevices === 0 ? "nenhum aparelho compatível pareado."
        : `${summary.recentlyConfirmedDevices} de ${summary.pairedCompatibleDevices} aparelhos ` +
          `confirmaram cache nas últimas ${summary.freshnessHours} horas.`;
      const row = element("div", null, "assignment-row");
      row.append(element("p", `${name}: disponível para baixar; ${detail}`));
      const withdraw = element("button", `Retirar da ${name}`, "button quiet withdraw-assignment");
      withdraw.type = "button";
      withdraw.addEventListener("click", async () => {
        if (!window.confirm(`Retirar esta história da ${name}? Após a reconexão, os aparelhos não poderão reabri-la.`)) return;
        withdraw.disabled = true;
        try {
          await post(`/studio/api/schools/${studio.schoolId}/assignments/${assignment.assignmentId}/revoke`);
          await loadAssignments();
          feedback(`História retirada da ${name}. Aparelhos offline serão atualizados na próxima conexão.`);
        } catch (error) {
          withdraw.disabled = false;
          feedback(error.message, true);
        }
      });
      row.append(withdraw);
      list.append(row);
    }
  }
  byId("refresh-preparation").hidden = studio.assignments.length === 0;
  if (studio.assignments.length) {
    const confirmed = studio.assignments.some(assignment =>
      (studio.preparation.get(assignment.assignmentId)?.recentlyConfirmedDevices || 0) > 0);
    byId("review-instruction").textContent = confirmed
      ? "Há confirmações recentes de cache; confira a quantidade por turma. Isso não indica uso pela criança."
      : "Disponível para a turma. Confira abaixo se os aparelhos já confirmaram o preparo.";
  } else if (studio.review?.state === "PUBLISHED") {
    byId("review-instruction").textContent = "Publicada. Escolha uma turma; nenhum envio ativo está disponível agora.";
  }
  byId("assign").disabled = !selector.value || studio.assignments.some(
    item => item.target.id === selector.value);
  renderJourney();
}

function renderPairingClassrooms() {
  const selector = byId("pairing-classroom");
  const selected = selector.value;
  selector.replaceChildren();
  for (const classroom of studio.classrooms) {
    const option = document.createElement("option");
    option.value = classroom.classroomId;
    option.textContent = classroom.name;
    selector.append(option);
  }
  if (studio.classrooms.some(item => item.classroomId === selected)) selector.value = selected;
  byId("create-pairing-code").disabled = !selector.value;

  const rosterSelector = byId("roster-classroom");
  const rosterSelected = rosterSelector.value;
  rosterSelector.replaceChildren();
  for (const classroom of studio.classrooms) {
    const option = document.createElement("option");
    option.value = classroom.classroomId;
    option.textContent = classroom.name;
    rosterSelector.append(option);
  }
  if (studio.classrooms.some(item => item.classroomId === rosterSelected)) {
    rosterSelector.value = rosterSelected;
  }
  byId("import-roster").disabled = !rosterSelector.value;
  byId("open-classroom-session").disabled = !rosterSelector.value;
}

async function loadPairingClassrooms() {
  studio.classrooms = await api(`/studio/api/schools/${studio.schoolId}/classrooms`);
  byId("pairing-result").hidden = true;
  renderPairingClassrooms();
  await loadRoster();
}

async function refreshPreparation() {
  const schoolId = studio.schoolId;
  const result = await Promise.all(studio.assignments.map(async assignment => {
    const path = `/studio/api/schools/${schoolId}/assignments/${assignment.assignmentId}/preparation`;
    try { return [assignment.assignmentId, await api(path)]; }
    catch (_) { return [assignment.assignmentId, { error: true }]; }
  }));
  if (schoolId !== studio.schoolId) return;
  studio.preparation = new Map(result);
  renderAssignments();
}

async function loadAssignments() {
  const base = `/studio/api/schools/${studio.schoolId}`;
  const [classrooms, assignments] = await Promise.all([
    api(`${base}/classrooms`), api(`${route()}/assignments`)
  ]);
  studio.classrooms = classrooms;
  studio.assignments = assignments;
  studio.preparation.clear();
  byId("assignment-section").hidden = false;
  renderAssignments();
  await refreshPreparation();
}

function pendingAssignment(classroomId) {
  const key = `studio-assign:${studio.schoolId}:${studio.review.storyId}:${studio.review.version}:${classroomId}`;
  let pending = studio.pendingAssignments.get(key);
  try { pending ||= JSON.parse(sessionStorage.getItem(key)); } catch (_) { /* unavailable storage */ }
  if (!pending?.idempotencyKey || !pending?.availableFrom) {
    pending = { idempotencyKey: crypto.randomUUID(), availableFrom: new Date().toISOString() };
  }
  studio.pendingAssignments.set(key, pending);
  try { sessionStorage.setItem(key, JSON.stringify(pending)); } catch (_) { /* memory survives this page */ }
  return { key, pending };
}

function renderAssets(pack) {
  const target = byId("story-assets");
  target.replaceChildren();
  if (!studio.review.assets.length) target.append(element("p", "Esta versão não possui arquivos visuais."));
  for (const asset of studio.review.assets) {
    const card = element("article", null, "asset-card");
    card.append(element("strong", `${roleNames[asset.role] || "Recurso"} · ${asset.assetId.replaceAll("_", " ")}`));
    const alt = pack.nodes.find(node => node.visualAssetId === asset.assetId || node.imageAssetId === asset.assetId)?.altText
      || `Recurso ${asset.assetId} para ${asset.role}`;
    const media = document.createElement(asset.role === "AUDIO" ? "audio" : "img");
    if (media.tagName === "IMG") { media.alt = alt; media.loading = "lazy"; }
    else { media.controls = true; media.preload = "metadata"; }
    const label = element("label");
    const check = document.createElement("input");
    check.type = "checkbox";
    check.disabled = true;
    check.setAttribute("aria-label", `Confirme ${asset.assetId} para ${roleNames[asset.role] || asset.role}`);
    const key = `${asset.assetId}:${asset.role}:${asset.sha256}`;
    check.addEventListener("change", () => {
      if (check.checked) studio.confirmed.add(key); else studio.confirmed.delete(key);
      updateApproval();
    });
    media.addEventListener(media.tagName === "IMG" ? "load" : "loadedmetadata", () => {
      check.disabled = studio.review.state !== "DRAFT";
    });
    media.addEventListener("error", () => {
      card.append(element("p", "Arquivo indisponível. Não aprove esta versão.", "status wait"));
    });
    media.src = asset.previewUrl;
    label.append(check, element("span", "Vi e confirmo esta variante"));
    label.hidden = studio.review.state !== "DRAFT";
    card.append(media, element("small", `Arquivo vinculado: ${asset.sha256.slice(0, 12)}…`), label);
    target.append(card);
  }
}

async function loadReview(item) {
  feedback("Carregando prévia privada…");
  studio.review = await api(`${route(item)}/review`);
  studio.confirmed.clear();
  const pack = JSON.parse(studio.review.packJson);
  byId("review-title").textContent = pack.title;
  const stateName = { DRAFT: "Rascunho", APPROVED: "Aprovada", PUBLISHED: "Publicada" }[studio.review.state] || studio.review.state;
  byId("review-state").textContent = `Versão ${studio.review.version} · ${stateName} · arquivo ${studio.review.packSha256.slice(0, 12)}…`;
  renderScenes(pack);
  renderAssets(pack);
  updateApproval();
  byId("assignment-section").hidden = true;
  if (studio.review.state === "PUBLISHED") await loadAssignments();
  byId("list-section").hidden = true;
  byId("review-section").hidden = false;
  feedback("Prévia editorial carregada. Confira os arquivos reais abaixo.");
}

async function start() {
  try {
    studio.csrf = (await api("/studio/api/csrf")).token;
    const context = await api("/studio/api/me");
    const selector = byId("school");
    for (const school of context.schools) {
      const option = document.createElement("option");
      option.value = school.schoolId;
      option.textContent = school.name;
      selector.append(option);
    }
    studio.schoolId = selector.value;
    selector.addEventListener("change", async () => {
      studio.schoolId = selector.value;
      try { await Promise.all([loadList(), loadPairingClassrooms()]); }
      catch (error) { feedback(error.message, true); }
    });
    await Promise.all([loadList(), loadPairingClassrooms()]);
  } catch (error) { feedback(error.message, true); }
}

byId("back").addEventListener("click", () => loadList().catch(error => feedback(error.message, true)));
byId("approve").addEventListener("click", async () => {
  const review = studio.review;
  if (review.state !== "DRAFT" || byId("approve").disabled) return;
  try {
    byId("approve").disabled = true;
    await post(`${route()}/approve`, {
      expectedRevision: review.revision,
      expectedPackSha256: review.packSha256,
      confirmedAssets: review.assets.map(({ assetId, role, sha256 }) => ({ assetId, role, sha256 })),
      confirmedWarningIds: []
    });
    await loadReview(review);
    feedback("Versão aprovada. A publicação é uma etapa separada.");
  } catch (error) { feedback(error.message, true); updateApproval(); }
});
byId("publish").addEventListener("click", async () => {
  if (studio.review.state !== "APPROVED" || !window.confirm("Publicar esta versão? Ela ainda não será enviada à turma.")) return;
  try {
    const review = studio.review;
    await post(`${route()}/publish`);
    await loadReview(review);
    feedback("Versão publicada. Escolha uma turma abaixo; o preparo dos aparelhos será uma etapa separada.");
  } catch (error) { feedback(error.message, true); }
});
byId("classroom").addEventListener("change", renderAssignments);
byId("refresh-preparation").addEventListener("click", () =>
  refreshPreparation().then(() => feedback("Preparo atualizado. Confira a quantidade de aparelhos por turma."))
    .catch(error => feedback(error.message, true)));
byId("roster-classroom").addEventListener("change", () =>
  loadRoster().catch(error => feedback(error.message, true)));
byId("import-roster").addEventListener("click", async () => {
  const names = byId("roster-names").value.split(/\r?\n/)
    .map(value => value.trim().replace(/\s+/g, " ")).filter(Boolean);
  if (!names.length || names.length > 40) {
    feedback("Informe de 1 a 40 alunos, com um nome por linha.", true);
    return;
  }
  const button = byId("import-roster");
  button.disabled = true;
  try {
    const roster = await put(`${classroomBase()}/roster`, { names });
    studio.roster = roster.learners;
    renderSeatBoard();
    feedback(`${names.length} aluno(s) salvos. Agora você pode abrir a aula.`);
  } catch (error) { feedback(error.message, true); }
  finally { button.disabled = false; }
});
byId("open-classroom-session").addEventListener("click", async () => {
  const button = byId("open-classroom-session");
  button.disabled = true;
  try {
    const opened = await post(`${classroomBase()}/sessions`);
    studio.classroomSession = { ...opened, seats: studio.roster.map(item => ({ ...item, connected: false })) };
    try { sessionStorage.setItem(sessionStorageKey(), opened.sessionId); } catch (_) { /* unavailable */ }
    byId("classroom-session-code").textContent = opened.joinCode;
    byId("classroom-session-expiry").textContent = `Válido até ${new Date(opened.expiresAt).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}. Digite no preparo de cada tablet.`;
    byId("classroom-session-result").hidden = false;
    renderSeatBoard(studio.classroomSession.seats);
    feedback(`Aula aberta para ${opened.learnerCount} aluno(s). Use o código nos tablets.`);
  } catch (error) { feedback(error.message, true); }
  finally { button.disabled = false; }
});
byId("refresh-classroom-session").addEventListener("click", () =>
  refreshClassroomSession()
    .then(() => feedback("Mapa de carteiras atualizado."))
    .catch(error => feedback(error.message, true)));
byId("close-classroom-session").addEventListener("click", async () => {
  if (!studio.classroomSession?.sessionId || !window.confirm("Encerrar esta aula e liberar os tablets para outra turma?")) return;
  try {
    await post(`/studio/api/schools/${studio.schoolId}/classroom-sessions/${studio.classroomSession.sessionId}/close`);
    try { sessionStorage.removeItem(sessionStorageKey()); } catch (_) { /* unavailable */ }
    studio.classroomSession = null;
    byId("classroom-session-result").hidden = true;
    renderSeatBoard();
    feedback("Aula encerrada. Os tablets podem entrar em outra turma.");
  } catch (error) { feedback(error.message, true); }
});
byId("create-pairing-code").addEventListener("click", async () => {
  const classroomId = byId("pairing-classroom").value;
  if (!classroomId) return;
  const button = byId("create-pairing-code");
  button.disabled = true;
  try {
    const result = await post(`/studio/api/schools/${studio.schoolId}/device-pairing-codes`,
      { classroomId });
    byId("pairing-code").textContent = result.code;
    byId("pairing-expiry").textContent = `Válido até ${new Date(result.expiresAt).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}. Não compartilhe fora da sala.`;
    byId("pairing-result").hidden = false;
    feedback("Código criado. Digite-o agora na área adulta do tablet.");
  } catch (error) {
    feedback(error.message, true);
  } finally {
    button.disabled = !byId("pairing-classroom").value;
  }
});
byId("assign").addEventListener("click", async () => {
  const classroomId = byId("classroom").value;
  const classroom = studio.classrooms.find(item => item.classroomId === classroomId);
  if (studio.review?.state !== "PUBLISHED" || !classroom || byId("assign").disabled
      || !window.confirm(`Enviar esta história para ${classroom.name}?`)) return;
  const { key, pending } = pendingAssignment(classroomId);
  try {
    byId("assign").disabled = true;
    await post(`${route()}/assignments`,
      { classroomId, availableFrom: pending.availableFrom }, pending.idempotencyKey);
    studio.pendingAssignments.delete(key);
    try { sessionStorage.removeItem(key); } catch (_) { /* unavailable storage */ }
    await loadAssignments();
    feedback(`História disponível para ${classroom.name}. Aguarde a confirmação de preparo dos aparelhos.`);
  } catch (error) {
    feedback(error.message, true);
    try { await loadAssignments(); } catch (_) { renderAssignments(); }
  }
});
byId("logout").addEventListener("click", async () => {
  await fetch("/studio/logout", { method: "POST", credentials: "same-origin",
    headers: { "X-XSRF-TOKEN": studio.csrf } });
  window.location.assign("/login");
});
start();
