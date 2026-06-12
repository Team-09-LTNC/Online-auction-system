const data = window.PORTFOLIO_DATA;
const reportData = window.REPORT_DATA || {};
const pages = data.pages || [];
const projects = pages.filter((page) => /^bai-/.test(page.slug));
const intro = pages.find((page) => page.slug === "intro");
const summary = pages.find((page) => page.slug === "summary");

const categories = {
  "bai-1": "digital",
  "bai-2": "digital",
  "bai-3": "ai",
  "bai-4": "collab",
  "bai-5": "ai",
  "bai-6": "ai",
};

const evidenceCounts = {
  "bai-1": 13,
};

const evidenceStepMap = {
  "bai-1": {
    1: 1,
    2: 2,
    3: 3,
    4: 4,
    5: 5,
    6: 6,
    7: 7,
    8: 8,
    11: 9,
    15: 10,
    17: 11,
    21: 12,
    25: 13,
  },
};

const evidenceDisplayWidths = {
  "bai-1": [466, 624, 613, 624, 624, 624, 624, 624, 624, 624, 624, 624, 624],
};

const tags = {
  "bai-1": ["File Explorer", "Quản lý dữ liệu", "An toàn tập tin"],
  "bai-2": ["Elicit", "Graphene", "Pin Li-S"],
  "bai-3": ["Prompt", "AI học thuật", "So sánh đầu ra"],
  "bai-4": ["Trello", "Google Drive", "Discord"],
  "bai-5": ["Gemini", "DALL-E 3", "Canva"],
  "bai-6": ["Minh bạch", "70/30", "Kiểm chứng"],
};

const topicNames = {
  "bai-1": "Quản lý tập tin",
  "bai-2": "Tổng hợp khoa học",
  "bai-3": "Prompt hiệu quả",
  "bai-4": "Hợp tác trực tuyến",
  "bai-5": "Sáng tạo với AI",
  "bai-6": "AI có trách nhiệm",
};

const qs = (selector) => document.querySelector(selector);
const qsa = (selector) => [...document.querySelectorAll(selector)];
const short = (text, limit = 240) =>
  text && text.length > limit ? `${text.slice(0, limit).trim()}...` : text;
const cleanTitle = (text) => (text || "").replace(/^Portfolio\s*-\s*/i, "");
const asset = (slug) =>
  slug === "bai-2" ? "assets/project-2.svg" : `assets/project-${Number(slug.replace("bai-", ""))}.png`;
const projectReport = (page) => reportData[page.slug] || {};
const sectionId = (slug, index) => `${slug}-section-${index + 1}`;
const publicHeadings = {
  "Giới thiệu bài tập": "Giới thiệu",
  "Thông tin bài tập": "Tổng quan dự án",
  "Mục tiêu và kỹ năng đạt được": "Năng lực phát triển",
};
const polishPublicLine = (text) =>
  text
    .replace(/^Bài báo cáo cũng so sánh/i, "Phần nghiên cứu cũng so sánh")
    .replace(/hoàn thành báo cáo nghiên cứu/gi, "hoàn thành sản phẩm nghiên cứu")
    .replace(/trong bài báo cáo/gi, "trong dự án");

function stepEvidence(page, index) {
  const count = evidenceCounts[page.slug] || 0;
  const mappedSteps = evidenceStepMap[page.slug];
  const imageIndex = mappedSteps ? mappedSteps[index] : index <= count ? index : null;
  if (!imageIndex) return "";
  const evidenceVersion = page.slug === "bai-1" ? "clarity-hires-v3" : "clarity-original";
  const imageSrc = `assets/evidence-steps/${page.slug}/step-${imageIndex}.png?v=${evidenceVersion}`;
  const displayWidth = evidenceDisplayWidths[page.slug]?.[imageIndex - 1];
  const figureStyle = displayWidth ? ` style="--evidence-width:${displayWidth}px"` : "";

  return `
    <figure class="step-evidence ${page.slug}"${figureStyle}>
      <a class="step-evidence-link" href="${imageSrc}" data-full-image="${imageSrc}" title="Mở ảnh toàn màn hình">
        <img src="${imageSrc}" alt="Hình ảnh quy trình ${page.section} bước ${index}" loading="lazy" decoding="async">
      </a>
    </figure>
  `;
}

function classifyLine(line, page, state) {
  const text = line.trim();
  if (!text) return "";
  const publicText = polishPublicLine(publicHeadings[text] || text);

  if (/^(I|II|III|IV|V|VI|VII|VIII|IX|X)\.\s/.test(publicText)) {
    const id = sectionId(page.slug, state.section);
    state.section += 1;
    return `<h3 class="dialog-section-heading" id="${id}">${publicText}</h3>`;
  }

  if (/^\d+\.\s?/.test(text)) {
    state.step += 1;
    return `
      <p class="dialog-step">
        <span>${text.match(/^\d+/)[0]}</span>
        ${text.replace(/^\d+\.\s?/, "")}
      </p>
      ${stepEvidence(page, state.step)}
    `;
  }

  if (/^•\s?/.test(text)) {
    return `<p class="dialog-bullet">${text.replace(/^•\s?/, "")}</p>`;
  }

  if (
    /^(Bài học cốt lõi|Minh chứng|Kết luận|Điểm mạnh|Điểm cần cải thiện|Mức độ hoàn thành|Delete:|Shift \+ Delete:|Thông tin bài tập|Mục tiêu và kỹ năng đạt được|Giới thiệu bài tập|Tổng kết Portfolio học tập)/.test(
      publicText,
    )
  ) {
    return `<p class="dialog-callout">${publicText}</p>`;
  }

  if (/:/.test(text) && text.length < 130) {
    return `<p class="dialog-keyline">${text}</p>`;
  }

  return `<p>${publicText}</p>`;
}

function renderContent(page) {
  const state = { step: 0, section: 0 };
  return page.text.map((line) => classifyLine(line, page, state)).join("");
}

function renderReportTable(table) {
  return `
    <figure class="report-table-block">
      ${table.caption ? `<figcaption>${table.caption}</figcaption>` : ""}
      <div class="report-table-wrap">
        <table class="report-table${table.columns.length > 4 ? " report-table-wide" : ""}">
          <thead><tr>${table.columns.map((column) => `<th>${column}</th>`).join("")}</tr></thead>
          <tbody>
            ${table.rows
              .map((row) => `<tr>${row.map((cell) => `<td>${cell}</td>`).join("")}</tr>`)
              .join("")}
          </tbody>
        </table>
      </div>
    </figure>
  `;
}

function renderStructuredContent(page, lines, tables) {
  const state = { step: 0, section: 0 };
  const renderedTables = new Set();
  let currentHeading = "";
  let html = "";

  const appendTablesForHeading = (heading) => {
    tables.forEach((table, index) => {
      if (
        !renderedTables.has(index) &&
        table.targetHeading &&
        heading.toLowerCase().includes(table.targetHeading.toLowerCase())
      ) {
        html += renderReportTable(table);
        renderedTables.add(index);
      }
    });
  };

  lines.forEach((line) => {
    const text = (line || "").trim();
    if (/^(I|II|III|IV|V|VI|VII|VIII|IX|X)\.\s/.test(text)) {
      appendTablesForHeading(currentHeading);
      currentHeading = text.replace(/^(I|II|III|IV|V|VI|VII|VIII|IX|X)\.\s*/, "");
    }
    html += classifyLine(line, page, state);
  });

  appendTablesForHeading(currentHeading);
  tables.forEach((table, index) => {
    if (!renderedTables.has(index)) html += renderReportTable(table);
  });
  return html;
}

function renderOutline(report, page) {
  const sourceLines = report.content || page.text || [];
  const headings = sourceLines.filter((line) =>
    /^(I|II|III|IV|V|VI|VII|VIII|IX|X)\.\s/.test((line || "").trim()),
  );
  const fallback = (report.sections || []).map((section) => section.title);
  const items = headings.length ? headings : fallback;

  return `
    <nav class="project-outline" aria-label="Mục lục ${page.section}">
      <div class="outline-intro">
        <span class="outline-icon">⌁</span>
        <div>
          <p>Lộ trình khám phá</p>
          <h3>Mục lục bài làm</h3>
          <span>Nhìn nhanh toàn bộ nội dung trước khi đi vào chi tiết.</span>
        </div>
      </div>
      <ol>
        ${items
          .map(
            (heading, index) => `
              <li>
                <a href="#${sectionId(page.slug, index)}">
                  <span>${String(index + 1).padStart(2, "0")}</span>
                  <strong>${heading.replace(/^(I|II|III|IV|V|VI|VII|VIII|IX|X)\.\s*/, "")}</strong>
                </a>
              </li>
            `,
          )
          .join("")}
        ${
          report.reportFile
            ? `
              <li>
                <a href="#${page.slug}-full-report">
                  <span>${String(items.length + 1).padStart(2, "0")}</span>
                  <strong>Báo cáo chi tiết</strong>
                </a>
              </li>
            `
            : ""
        }
      </ol>
    </nav>
  `;
}

function renderFullReport(report, page) {
  if (!report.reportFile) return "";

  return `
    <section class="full-report-section" id="${page.slug}-full-report">
      <div class="full-report-heading">
        <div>
          <p>Tài liệu gốc</p>
          <h3>Báo cáo chi tiết</h3>
          <span>${report.reportName || `Báo cáo ${page.section}`} · ${report.reportPages || "Nhiều"} trang PDF</span>
        </div>
        <div class="full-report-actions">
          <a href="${report.reportFile}" target="_blank" rel="noopener noreferrer">Mở toàn màn hình</a>
          <a href="${report.reportFile}" download>Tải báo cáo PDF</a>
        </div>
      </div>
      <details class="pdf-preview">
        <summary>Xem trực tiếp toàn bộ báo cáo</summary>
        <iframe
          src="${report.reportFile}#view=FitH"
          title="${report.reportName || `Báo cáo chi tiết ${page.section}`}"
          loading="lazy"
        ></iframe>
      </details>
    </section>
  `;
}

function renderReport(report, page) {
  const metrics = (report.metrics || [])
    .map(
      ([label, value]) => `
        <article>
          <span>${label}</span>
          <strong>${value}</strong>
        </article>
      `,
    )
    .join("");

  const sourceLines = report.content || page.text || [];
  const detailedLines = sourceLines.filter((line, index) => {
    if (!line?.trim()) return false;
    if (index === 0 && /^Bài \d+:/.test(line.trim())) return false;
    return !/^Minh chứng\s*:/i.test(line.trim());
  });
  const tables = [...(report.tables || []), ...(report.table ? [report.table] : [])];
  const structuredContent = renderStructuredContent(page, detailedLines, tables);

  const findings = (report.findings || [])
    .map(
      (finding, index) => `
        <article class="report-finding">
          <span>${String(index + 1).padStart(2, "0")}</span>
          <h4>${finding.title}</h4>
          <p>${finding.text}</p>
        </article>
      `,
    )
    .join("");

  return `
    <p class="report-lead">${report.summary}</p>
    ${metrics ? `<div class="report-metrics">${metrics}</div>` : ""}
    <div class="report-google-structure">${structuredContent}</div>
    ${findings ? `<section class="report-section report-recap"><h3>Điểm nổi bật</h3><div class="report-findings">${findings}</div></section>` : ""}
    ${
      report.reflection
        ? `<blockquote class="report-reflection"><strong>Bài học cá nhân</strong><p>${report.reflection}</p></blockquote>`
        : ""
    }
  `;
}

function renderAbout() {
  const text = intro?.text || [];
  qs("#aboutLead").textContent =
    text.find((line) => line.includes("Xin chào")) || intro?.description || "";

  const facts = text.filter(
    (line) =>
      line.startsWith("Họ và tên:") ||
      line.startsWith("Ngành học:") ||
      line.startsWith("Lớp:") ||
      line.startsWith("Trường:") ||
      line.startsWith("Học phần:"),
  );
  facts.splice(1, 0, "Mã sinh viên: 25020163");

  qs("#profileFacts").innerHTML = facts.map((line) => `<p>${line}</p>`).join("");

  const skillTitles = ["Kỹ năng số", "Trí tuệ nhân tạo", "Làm việc nhóm"];
  qs("#skillIntro").innerHTML = skillTitles
    .map((title, index) => {
      const titleIndex = text.findIndex((line) => line === title);
      const body = titleIndex >= 0 ? text[titleIndex + 1] : "";
      return `
        <article class="skill-card">
          <strong>${String(index + 1).padStart(2, "0")}</strong>
          <div>
            <h3>${title}</h3>
            <p>${body}</p>
          </div>
        </article>
      `;
    })
    .join("");
}

function renderProjects() {
  qs("#projectGrid").innerHTML = projects
    .map((page, index) => {
      const report = projectReport(page);
      const title = report.title || cleanTitle(page.title);
      const introLine =
        report.summary ||
        page.text.find((line) => line.includes("Bài tập này")) ||
        page.description ||
        page.text[0] ||
        title;

      return `
        <article class="project-card reveal" data-category="${categories[page.slug] || "digital"}" data-slug="${page.slug}" data-tilt>
          <div class="project-watermark" aria-hidden="true">${String(index + 1).padStart(2, "0")}</div>
          <div class="project-body">
            <div class="project-number">${String(index + 1).padStart(2, "0")}</div>
            <h3>${title}</h3>
            <p>${short(introLine, 270)}</p>
            <div class="tag-row">${(report.tags || tags[page.slug] || []).map((tag) => `<span>${tag}</span>`).join("")}</div>
            <button class="open-detail" data-slug="${page.slug}">Xem nội dung chi tiết</button>
          </div>
        </article>
      `;
    })
    .join("");
}

function renderTopics() {
  qs("#topicGallery").innerHTML = projects
    .map(
      (page, index) => `
        <article class="topic-card">
          <span class="topic-bubble">${String(index + 1).padStart(2, "0")}</span>
          <div><h3>${topicNames[page.slug]}</h3><p>${(projectReport(page).tags || tags[page.slug] || []).join(" · ")}</p></div>
        </article>
      `,
    )
    .join("");
}

function renderReflection() {
  const lines = [
    "Portfolio ghi lại quá trình em phát triển năng lực số theo hướng chủ động, có hệ thống và gắn với những tình huống thực tế.",
    "Qua sáu dự án, em không chỉ học cách sử dụng công cụ mà còn rèn luyện khả năng xác định vấn đề, tổ chức quy trình, đánh giá đầu ra và cải thiện sản phẩm.",
    "Kỹ năng quan trọng nhất em đạt được là biết kết hợp tư duy cá nhân với công nghệ: dùng AI để mở rộng lựa chọn nhưng luôn tự kiểm chứng và chịu trách nhiệm với quyết định cuối cùng.",
    "Trong thời gian tới, em muốn tiếp tục nâng cao kỹ năng nghiên cứu, thiết kế sản phẩm số và xây dựng các dự án có chiều sâu hơn, hữu ích hơn cho người dùng.",
  ];
  qs("#reflectionLead").textContent =
    "Nhìn lại hành trình học tập, mỗi dự án đều góp phần hình thành một cách làm việc rõ ràng, sáng tạo và có trách nhiệm hơn.";
  qs("#reflectionContent").innerHTML = lines.map((line) => `<p>${line}</p>`).join("");
}

function openProject(slug) {
  const page = projects.find((project) => project.slug === slug);
  if (!page) return;
  const report = projectReport(page);
  const isEvidenceLesson = slug === "bai-1";

  qs("#dialogContent").innerHTML = `
    <header class="dialog-hero">
      <span class="dialog-whale" aria-hidden="true">◜◡◝</span>
      <p>${page.section}</p>
      <h2 id="dialogTitle">${report.title || cleanTitle(page.title)}</h2>
      <div class="dialog-meta">
        <span>${isEvidenceLesson ? "Quy trình trực quan" : "Nội dung chuyên đề"}</span>
        <span>${topicNames[slug]}</span>
        <span>${(report.tags || tags[slug] || []).length} trọng tâm</span>
      </div>
    </header>
    <div class="dialog-content">
      ${renderOutline(report, page)}
      ${
        isEvidenceLesson
          ? renderStructuredContent(page, page.text, [
              ...(report.tables || []),
              ...(report.table ? [report.table] : []),
            ])
          : renderReport(report, page)
      }
      ${renderFullReport(report, page)}
    </div>
  `;
  qs("#projectDialog").showModal();
}

function initInteractions() {
  const viewer = qs("#imageViewer");
  const viewerImage = qs("#imageViewer img");
  let viewerZoom = 1;

  const updateViewerZoom = () => {
    viewerImage.style.width = viewerZoom === 0 ? "min(100%, 1400px)" : `${viewerImage.naturalWidth * viewerZoom}px`;
  };

  const openImageViewer = (src, alt) => {
    viewerImage.alt = alt;
    viewerZoom = 0;
    viewer.hidden = false;
    document.body.classList.add("viewer-open");
    viewerImage.onload = updateViewerZoom;
    viewerImage.src = src;
    updateViewerZoom();
  };

  const closeImageViewer = () => {
    viewer.hidden = true;
    viewerImage.removeAttribute("src");
    document.body.classList.remove("viewer-open");
  };

  document.addEventListener("click", (event) => {
    const evidenceLink = event.target.closest("[data-full-image]");
    if (evidenceLink) {
      event.preventDefault();
      openImageViewer(evidenceLink.dataset.fullImage, evidenceLink.querySelector("img")?.alt || "Hình ảnh quy trình");
      return;
    }

    const zoomControl = event.target.closest("[data-zoom]");
    if (zoomControl) {
      const action = zoomControl.dataset.zoom;
      if (action === "reset") viewerZoom = 0;
      if (action === "actual") viewerZoom = 1;
      if (action === "in") viewerZoom = Math.min(3, (viewerZoom || 0.5) + 0.25);
      if (action === "out") viewerZoom = Math.max(0.25, (viewerZoom || 1) - 0.25);
      updateViewerZoom();
      return;
    }

    if (event.target.closest("[data-viewer-close]") || event.target === viewer) {
      closeImageViewer();
      return;
    }

    const detail = event.target.closest(".open-detail");
    if (detail) openProject(detail.dataset.slug);

    const filter = event.target.closest(".filter");
    if (filter) {
      qsa(".filter").forEach((button) => button.classList.toggle("active", button === filter));
      qsa(".project-card").forEach((card) => {
        const visible = filter.dataset.filter === "all" || card.dataset.category === filter.dataset.filter;
        card.classList.toggle("hidden", !visible);
      });
    }
  });

  qs(".close-dialog").addEventListener("click", () => qs("#projectDialog").close());
  qs("#projectDialog").addEventListener("click", (event) => {
    if (event.target.id === "projectDialog") qs("#projectDialog").close();
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !viewer.hidden) closeImageViewer();
  });

  qsa("[data-tilt]").forEach((card) => {
    card.addEventListener("pointermove", (event) => {
      if (matchMedia("(prefers-reduced-motion: reduce)").matches) return;
      const rect = card.getBoundingClientRect();
      const x = (event.clientX - rect.left) / rect.width - 0.5;
      const y = (event.clientY - rect.top) / rect.height - 0.5;
      card.style.transform = `perspective(900px) rotateX(${y * -4}deg) rotateY(${x * 6}deg) translateY(-3px)`;
    });
    card.addEventListener("pointerleave", () => {
      card.style.transform = "";
    });
  });
}

function initReveal() {
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("visible");
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.12 },
  );
  qsa(".reveal").forEach((item) => observer.observe(item));
}

function initNav() {
  const links = qsa(".site-header nav a");
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        links.forEach((link) => link.classList.toggle("active", link.getAttribute("href") === `#${entry.target.id}`));
      });
    },
    { rootMargin: "-42% 0px -48% 0px" },
  );
  qsa("section[id]").forEach((section) => observer.observe(section));
}

function initProgress() {
  const bar = qs(".progress");
  const update = () => {
    const height = document.documentElement.scrollHeight - innerHeight;
    bar.style.width = `${height > 0 ? (scrollY / height) * 100 : 0}%`;
  };
  addEventListener("scroll", update, { passive: true });
  update();
}

function initCanvas() {
  const canvas = qs("#motionCanvas");
  if (matchMedia("(prefers-reduced-motion: reduce)").matches) {
    canvas.remove();
    return;
  }
  const ctx = canvas.getContext("2d");
  let dots = [];

  const resize = () => {
    const ratio = devicePixelRatio || 1;
    canvas.width = innerWidth * ratio;
    canvas.height = innerHeight * ratio;
    canvas.style.width = `${innerWidth}px`;
    canvas.style.height = `${innerHeight}px`;
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
    dots = Array.from({ length: Math.min(90, Math.floor(innerWidth / 14)) }, (_, index) => ({
      x: Math.random() * innerWidth,
      y: Math.random() * innerHeight,
      vx: (Math.random() - 0.5) * 0.22,
      vy: (Math.random() - 0.5) * 0.22,
      color: index % 3 === 0 ? "#00b8d9" : index % 3 === 1 ? "#ff6b57" : "#b8f45b",
    }));
  };

  const draw = () => {
    ctx.clearRect(0, 0, innerWidth, innerHeight);
    dots.forEach((dot, index) => {
      dot.x += dot.vx;
      dot.y += dot.vy;
      if (dot.x < -10) dot.x = innerWidth + 10;
      if (dot.x > innerWidth + 10) dot.x = -10;
      if (dot.y < -10) dot.y = innerHeight + 10;
      if (dot.y > innerHeight + 10) dot.y = -10;

      ctx.globalAlpha = 0.34;
      ctx.beginPath();
      ctx.arc(dot.x, dot.y, 2, 0, Math.PI * 2);
      ctx.fillStyle = dot.color;
      ctx.fill();

      for (let next = index + 1; next < dots.length; next += 1) {
        const other = dots[next];
        const distance = Math.hypot(dot.x - other.x, dot.y - other.y);
        if (distance < 118) {
          ctx.globalAlpha = 0.1;
          ctx.beginPath();
          ctx.moveTo(dot.x, dot.y);
          ctx.lineTo(other.x, other.y);
          ctx.strokeStyle = "#00b8d9";
          ctx.stroke();
        }
      }
    });
    ctx.globalAlpha = 1;
    requestAnimationFrame(draw);
  };

  resize();
  draw();
  addEventListener("resize", resize);
}

renderAbout();
renderProjects();
renderTopics();
renderReflection();
initInteractions();
initReveal();
initNav();
initProgress();
initCanvas();
