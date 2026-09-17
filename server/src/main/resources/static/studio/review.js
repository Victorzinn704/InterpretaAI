const studio = { schoolId: null, reviews: [], review: null, confirmed: new Set(), csrf: null };
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
  return response.json();
}

function route(item = studio.review) {
  return `/studio/api/schools/${studio.schoolId}/stories/${item.storyId}/versions/${item.version}`;
}

function post(path, body) {
  return api(path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": studio.csrf,
      "Idempotency-Key": crypto.randomUUID()
    },
    body: body == null ? undefined : JSON.stringify(body)
  });
}

function renderList() {
  const list = byId("story-list");
  list.replaceChildren();
  for (const item of studio.reviews) {
    const card = element("article", null, "card");
    card.append(element("span", item.state === "DRAFT" ? "Aguardando revisão" : "Aprovada, não publicada", "status wait"));
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
  byId("approve").disabled = review.state !== "DRAFT" || pending.length > 0;
  byId("publish").hidden = review.state !== "APPROVED";
  byId("review-instruction").textContent = review.state === "DRAFT"
    ? pending.length ? `Confira e confirme ${pending.length} recurso(s) antes de aprovar.` : "Todas as variantes foram conferidas. A aprovação congela esta versão."
    : review.state === "APPROVED" ? "Aprovada. Publicar não envia automaticamente à turma."
      : "Publicada. Falta atribuir esta versão à turma e aguardar o cache dos aparelhos.";
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
      try { await loadList(); } catch (error) { feedback(error.message, true); }
    });
    await loadList();
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
    feedback("Versão publicada. Atribuição à turma e preparo dos aparelhos ainda são etapas separadas.");
  } catch (error) { feedback(error.message, true); }
});
byId("logout").addEventListener("click", async () => {
  await fetch("/studio/logout", { method: "POST", credentials: "same-origin",
    headers: { "X-XSRF-TOKEN": studio.csrf } });
  window.location.assign("/login");
});
start();
