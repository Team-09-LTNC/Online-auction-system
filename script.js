const data = window.PORTFOLIO_DATA;
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
  "bai-1": 12,
  "bai-2": 11,
  "bai-3": 6,
  "bai-4": 3,
  "bai-5": 5,
  "bai-6": 4,
};

const tags = {
  "bai-1": ["File Explorer", "Quản lý dữ liệu", "An toàn tập tin"],
  "bai-2": ["Google Scholar", "IEEE", "Đánh giá nguồn"],
  "bai-3": ["Prompt", "AI học thuật", "So sánh đầu ra"],
  "bai-4": ["Trello", "Google Drive", "Discord"],
  "bai-5": ["Gemini", "DALL-E 3", "Canva"],
  "bai-6": ["Minh bạch", "70/30", "Kiểm chứng"],
};

const topicNames = {
  "bai-1": "Quản lý tập tin",
  "bai-2": "Tìm kiếm học thuật",
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
const asset = (slug) => `assets/project-${Number(slug.replace("bai-", ""))}.png`;

function stepEvidence(page, index) {
  const count = evidenceCounts[page.slug] || 0;
  if (index > count) return "";

  return `
    <figure class="step-evidence">
      <img src="assets/evidence-steps/${page.slug}/step-${index}.png?v=sharp-evidence" alt="Minh chung ${page.section} buoc ${index}" loading="lazy">
      <figcaption>Minh chứng bước ${index} từ Google Site gốc</figcaption>
    </figure>
  `;
}

function classifyLine(line, page, state) {
  const text = line.trim();
  if (!text) return "";

  if (/^(I|II|III|IV|V|VI|VII|VIII|IX|X)\.\s/.test(text)) {
    return `<h3 class="dialog-section-heading">${text}</h3>`;
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
      text,
    )
  ) {
    return `<p class="dialog-callout">${text}</p>`;
  }

  if (/:/.test(text) && text.length < 130) {
    return `<p class="dialog-keyline">${text}</p>`;
  }

  return `<p>${text}</p>`;
}

function renderContent(page) {
  const state = { step: 0 };
  return page.text.map((line) => classifyLine(line, page, state)).join("");
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
      const title = cleanTitle(page.title);
      const introLine =
        page.text.find((line) => line.includes("Bài tập này")) ||
        page.description ||
        page.text[0] ||
        title;

      return `
        <article class="project-card reveal" data-category="${categories[page.slug] || "digital"}" data-slug="${page.slug}" data-tilt>
          <img class="project-image" src="${asset(page.slug)}" alt="${topicNames[page.slug] || title}" loading="lazy">
          <div class="project-body">
            <div class="project-number">${String(index + 1).padStart(2, "0")}</div>
            <h3>${title}</h3>
            <p>${short(introLine, 270)}</p>
            <div class="tag-row">${(tags[page.slug] || []).map((tag) => `<span>${tag}</span>`).join("")}</div>
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
          <img src="${asset(page.slug)}" alt="${topicNames[page.slug]}" loading="lazy">
          <h3>${String(index + 1).padStart(2, "0")} · ${topicNames[page.slug]}</h3>
        </article>
      `,
    )
    .join("");
}

function renderReflection() {
  const lines = summary?.text || [];
  qs("#reflectionLead").textContent = summary?.description || lines[0] || "";
  qs("#reflectionContent").innerHTML = lines.slice(0, 34).map((line) => `<p>${line}</p>`).join("");
}

function openProject(slug) {
  const page = projects.find((project) => project.slug === slug);
  if (!page) return;

  qs("#dialogContent").innerHTML = `
    <img class="dialog-cover" src="${asset(slug)}" alt="${topicNames[slug]}">
    <h2>${cleanTitle(page.title)}</h2>
    <div class="dialog-meta">
      <span>${page.section}</span>
      <span>${page.text.length} mục nội dung</span>
      <span>${topicNames[slug]}</span>
    </div>
    <div class="dialog-content">${renderContent(page)}</div>
  `;
  qs("#projectDialog").showModal();
}

function initInteractions() {
  document.addEventListener("click", (event) => {
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
