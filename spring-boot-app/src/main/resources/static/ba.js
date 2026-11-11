const result = document.querySelector("#result");
const sentence = document.querySelector("#sentence");

const state = {
  text: "",
  channel: null,
  channels: null,
  capability: null,
  clarifyUsed: false,
  intent: null
};

document.querySelector("#analyze").addEventListener("click", () => {
  state.text = sentence.value;
  state.channel = null;
  state.channels = null;
  state.capability = null;
  state.clarifyUsed = false;
  state.intent = null;
  interpret();
});

document.querySelectorAll("[data-example]").forEach((button) => {
  button.addEventListener("click", () => {
    sentence.value = button.dataset.example;
    document.querySelector("#analyze").click();
  });
});

async function interpret() {
  const response = await post("/api/ba/interpret", {
    text: state.text,
    channel: state.channel,
    channels: state.channels,
    capability: state.capability,
    clarifyUsed: state.clarifyUsed
  });
  if (!response.ok) {
    showError(response.body);
    return;
  }
  renderInterpret(response.body);
}

function renderInterpret(payload) {
  clear(result);
  if (payload.type === "clarify") {
    result.append(questionPanel(payload));
    return;
  }
  if (payload.type === "none") {
    result.append(paragraph(payload.message, "empty"));
    return;
  }
  state.intent = payload.intent;
  result.append(shortlistPanel(payload.journeys));
}

function questionPanel(payload) {
  const section = panel();
  section.append(heading(payload.question));
  const row = document.createElement("div");
  row.className = "row";
  if (payload.field === "channel") {
    payload.options.forEach((option) => {
      const label = document.createElement("label");
      const box = document.createElement("input");
      box.type = "checkbox";
      box.value = option.id;
      label.append(box, document.createTextNode(" " + option.label));
      row.append(label);
    });
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = "Continue";
    button.addEventListener("click", () => {
      state.channels = [...row.querySelectorAll("input:checked")].map((box) => box.value);
      state.clarifyUsed = true;
      interpret();
    });
    section.append(row);
    section.append(button);
    return section;
  }
  payload.options.forEach((option) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "choice";
    button.textContent = option.label;
    button.addEventListener("click", () => {
      state.capability = option.id;
      state.clarifyUsed = true;
      interpret();
    });
    row.append(button);
  });
  section.append(row);
  return section;
}

function shortlistPanel(journeys) {
  const section = panel();
  section.append(heading("Confirm the journeys"));
  journeys.forEach((journey) => {
    const label = document.createElement("label");
    const box = document.createElement("input");
    box.type = "checkbox";
    box.name = "journey";
    box.value = journey.id;
    box.checked = true;
    label.append(box, document.createTextNode(" " + journey.name));
    section.append(label);
  });
  const row = document.createElement("div");
  row.className = "row";
  const button = document.createElement("button");
  button.type = "button";
  button.textContent = "Show chains";
  button.addEventListener("click", () => confirm(section));
  row.append(button);
  section.append(row);
  return section;
}

async function confirm(section) {
  const journeyIds = [...section.querySelectorAll("input[name=journey]:checked")].map((box) => box.value);
  const response = await post("/api/ba/confirm", {
    journeyIds,
    changeType: state.intent.changeType,
    subject: state.intent.subject
  });
  if (!response.ok) {
    showError(response.body);
    return;
  }
  clear(result);
  result.append(teamsPanel(response.body.teams));
  response.body.chains.forEach((chain) => result.append(chainPanel(chain)));
}

function teamsPanel(teams) {
  const section = panel();
  section.append(heading("Teams to consult"));
  const list = document.createElement("ul");
  list.className = "teams";
  teams.forEach((team) => {
    const item = document.createElement("li");
    item.textContent = team;
    list.append(item);
  });
  section.append(list);
  return section;
}

function chainPanel(chain) {
  const section = document.createElement("article");
  section.className = "chain";
  section.append(heading(chain.name));
  const list = document.createElement("ol");
  list.className = "hops";
  chain.hops.forEach((hop) => list.append(hopItem(hop)));
  section.append(list);
  if (chain.sideFacts && chain.sideFacts.length) {
    const side = document.createElement("div");
    side.className = "side";
    side.append(subheading("Declared, not on this journey"));
    chain.sideFacts.forEach((fact) => {
      const line = document.createElement("p");
      line.textContent = fact.fromApplicationName + " → " + fact.toApplicationName;
      side.append(line);
      side.append(labels([fact], fact.seenInTrace));
    });
    section.append(side);
  }
  if (chain.traceIds && chain.traceIds.length) {
    const row = document.createElement("div");
    row.className = "row";
    section.append(row);
    chain.traceIds.forEach((traceId) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "secondary";
      button.textContent = "Open " + traceId;
      const slot = document.createElement("div");
      button.addEventListener("click", () => openTrace(traceId, slot));
      row.append(button);
      section.append(slot);
    });
  }
  return section;
}

function hopItem(hop) {
  const item = document.createElement("li");
  item.className = hop.highlighted || hop.proposed ? "hop highlight" : "hop";
  const seq = document.createElement("div");
  seq.className = "seq";
  seq.textContent = String(hop.sequence);
  const body = document.createElement("div");
  const title = document.createElement("div");
  title.className = "hop-title";
  title.textContent = hop.applicationName;
  const meta = document.createElement("div");
  meta.className = "meta";
  meta.textContent = [hop.team || "No owning team", hop.operationName].filter(Boolean).join(" · ");
  body.append(title, meta, labels(hop.evidence, hop.seenInTrace));
  if (hop.highlighted && !hop.proposed) {
    const note = document.createElement("div");
    note.className = "note";
    note.textContent = "Changed hop";
    body.append(note);
  }
  item.append(seq, body);
  return item;
}

function labels(evidence, seenInTrace) {
  const row = document.createElement("div");
  row.className = "labels";
  evidence.forEach((item) => {
    const chip = document.createElement("span");
    chip.className = "label " + item.label;
    chip.textContent = item.source ? item.label + " · " + item.source : item.label;
    row.append(chip);
  });
  const declared = evidence.some((item) => item.label === "declared");
  if (declared && !seenInTrace) {
    const note = document.createElement("span");
    note.className = "note";
    note.textContent = "Not seen in traces";
    row.append(note);
  }
  return row;
}

async function openTrace(traceId, slot) {
  const response = await get("/api/traces/" + encodeURIComponent(traceId));
  if (!response.ok) {
    showError(response.body);
    return;
  }
  clear(slot);
  const table = document.createElement("table");
  const head = document.createElement("tr");
  ["#", "Application", "Operation", "Time", "Status"].forEach((name) => {
    const cell = document.createElement("th");
    cell.textContent = name;
    head.append(cell);
  });
  table.append(head);
  response.body.hops.forEach((hop) => {
    const row = document.createElement("tr");
    [hop.sequence, hop.applicationName, hop.operationName, hop.time, hop.status].forEach((value) => {
      const cell = document.createElement("td");
      cell.textContent = value;
      row.append(cell);
    });
    table.append(row);
  });
  const wrap = document.createElement("div");
  wrap.className = "trace";
  wrap.append(table);
  slot.append(wrap);
}

function panel() {
  const section = document.createElement("section");
  section.className = "panel";
  return section;
}

function heading(text) {
  const title = document.createElement("h2");
  title.textContent = text;
  return title;
}

function subheading(text) {
  const title = document.createElement("h3");
  title.textContent = text;
  return title;
}

function paragraph(text, className) {
  const element = document.createElement("p");
  element.className = className;
  element.textContent = text;
  return element;
}

function showError(body) {
  clear(result);
  result.append(paragraph(body && body.message ? body.message : "The request failed.", "error"));
}

function clear(node) {
  while (node.firstChild) {
    node.removeChild(node.firstChild);
  }
}

async function post(url, payload) {
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return read(response);
}

async function get(url) {
  const response = await fetch(url);
  return read(response);
}

async function read(response) {
  const body = await response.json().catch(() => null);
  return { ok: response.ok, body };
}
