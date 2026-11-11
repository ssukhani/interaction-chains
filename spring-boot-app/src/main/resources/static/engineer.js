const query = document.querySelector("#query");
const matches = document.querySelector("#matches");
const result = document.querySelector("#result");

query.addEventListener("input", () => loadMatches(query.value));
loadMatches("");

async function loadMatches(text) {
  const response = await fetch("/api/catalog/operations?q=" + encodeURIComponent(text));
  const choices = await response.json();
  clear(matches);
  choices.forEach((choice) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "secondary match";
    button.textContent = choice.applicationName + " · " + choice.operationName;
    button.addEventListener("click", () => select(choice));
    matches.append(button);
  });
}

async function select(choice) {
  query.value = choice.applicationName + " · " + choice.operationName;
  clear(matches);
  const response = await fetch(
    "/api/analysis/impact?applicationId=" + encodeURIComponent(choice.applicationId)
      + "&operationId=" + encodeURIComponent(choice.operationId)
  );
  const body = await response.json();
  if (!response.ok) {
    clear(result);
    const error = document.createElement("p");
    error.className = "error";
    error.textContent = body.message || "The request failed.";
    result.append(error);
    return;
  }
  renderImpact(body);
}

function renderImpact(impact) {
  clear(result);
  const intro = document.createElement("p");
  intro.textContent = impact.journeys.length + " journeys include " + impact.applicationName + " · " + impact.operationName;
  result.append(intro);
  impact.journeys.forEach((chain) => result.append(chainPanel(chain)));
}

function chainPanel(chain) {
  const section = document.createElement("article");
  section.className = "chain";
  const title = document.createElement("h2");
  title.textContent = chain.name;
  section.append(title);
  const list = document.createElement("ol");
  list.className = "hops";
  chain.hops.forEach((hop) => list.append(hopItem(hop)));
  section.append(list);
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
  item.className = hop.highlighted ? "hop highlight" : "hop";
  const seq = document.createElement("div");
  seq.className = "seq";
  seq.textContent = String(hop.sequence);
  const body = document.createElement("div");
  const title = document.createElement("div");
  title.className = "hop-title";
  title.textContent = hop.applicationName;
  const meta = document.createElement("div");
  meta.className = "meta";
  meta.textContent = hop.team + " · " + hop.operationName;
  body.append(title, meta, labels(hop));
  if (hop.highlighted) {
    const note = document.createElement("div");
    note.className = "note";
    note.textContent = "Changed hop";
    body.append(note);
  }
  item.append(seq, body);
  return item;
}

function labels(hop) {
  const row = document.createElement("div");
  row.className = "labels";
  hop.evidence.forEach((item) => {
    const chip = document.createElement("span");
    chip.className = "label " + item.label;
    chip.textContent = item.source ? item.label + " · " + item.source : item.label;
    row.append(chip);
  });
  const declared = hop.evidence.some((item) => item.label === "declared");
  if (declared && !hop.seenInTrace) {
    const note = document.createElement("span");
    note.className = "note";
    note.textContent = "Not seen in traces";
    row.append(note);
  }
  return row;
}

async function openTrace(traceId, slot) {
  const response = await fetch("/api/traces/" + encodeURIComponent(traceId));
  const body = await response.json();
  clear(slot);
  const table = document.createElement("table");
  const head = document.createElement("tr");
  ["#", "Application", "Operation", "Time", "Status"].forEach((name) => {
    const cell = document.createElement("th");
    cell.textContent = name;
    head.append(cell);
  });
  table.append(head);
  body.hops.forEach((hop) => {
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

function clear(node) {
  while (node.firstChild) {
    node.removeChild(node.firstChild);
  }
}
