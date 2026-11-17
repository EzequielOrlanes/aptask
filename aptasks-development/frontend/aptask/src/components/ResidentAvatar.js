import React, { useEffect, useRef } from "react";

// Paletas fixas: a mesma pessoa sempre recebe a mesma aparência, escolhida pelo nome.
const PALETTES = [
  { skin: "#f3c9a5", hair: "#3b2a22", shirt: "#3cc7b6" },
  { skin: "#e8b48f", hair: "#7a3e2b", shirt: "#f4a06b" },
  { skin: "#d9a07a", hair: "#1f1b18", shirt: "#8f8ae0" },
  { skin: "#f6d6bd", hair: "#c9893f", shirt: "#f7d46a" },
];

function hashName(text) {
  let hash = 0;
  for (const char of text) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  return hash;
}

const MAX_EYE_OFFSET = 1.6; // em unidades do viewBox (48x48)

function AnimatedFigure({ name }) {
  const ref = useRef(null);
  const hash = hashName(name);
  const palette = PALETTES[hash % PALETTES.length];
  // Pequena defasagem entre as figuras, para não se moverem em sincronia.
  const delay = `${(hash % 7) * 0.4}s`;

  // Os olhos seguem o ponteiro: a direção é normalizada e limitada a um pequeno deslocamento.
  useEffect(() => {
    const svg = ref.current;
    if (!svg) return undefined;
    const handlePointer = event => {
      const rect = svg.getBoundingClientRect();
      const dx = event.clientX - (rect.left + rect.width / 2);
      const dy = event.clientY - (rect.top + rect.height / 2);
      const distance = Math.hypot(dx, dy) || 1;
      const strength = Math.min(distance / (rect.width * 3), 1) * MAX_EYE_OFFSET;
      svg.style.setProperty("--eye-x", `${(dx / distance) * strength}px`);
      svg.style.setProperty("--eye-y", `${(dy / distance) * strength}px`);
    };
    window.addEventListener("pointermove", handlePointer, { passive: true });
    return () => window.removeEventListener("pointermove", handlePointer);
  }, []);

  return (
    <svg ref={ref} className="avatar-figure" viewBox="0 0 48 48" role="img" aria-hidden="true" style={{ animationDelay: delay }}>
      <path d="M8 46c0-9 7-14 16-14s16 5 16 14z" fill={palette.shirt} />
      <rect x="20.5" y="27" width="7" height="6" rx="2" fill={palette.skin} />
      <circle cx="24" cy="20" r="9.5" fill={palette.skin} />
      <path d="M14.2 19c-.6-6.4 3.6-10.6 9.9-10.6 5.2 0 9.4 3.4 9.7 9.1-2.6-2.6-5.9-3.9-9.6-3.9-3.6 0-7.2 1.2-10 5.4z" fill={palette.hair} />
      <g className="avatar-gaze" style={{ transform: "translate(var(--eye-x, 0px), var(--eye-y, 0px))" }}>
        <ellipse className="avatar-eye" cx="20.6" cy="21" rx="1.2" ry="1.5" fill="#2d2f33" />
        <ellipse className="avatar-eye" cx="27.4" cy="21" rx="1.2" ry="1.5" fill="#2d2f33" />
      </g>
      <circle cx="18" cy="24.4" r="1.4" fill="#f08a8a" opacity=".45" />
      <circle cx="30" cy="24.4" r="1.4" fill="#f08a8a" opacity=".45" />
      <path d="M21.4 25.2c1.4 1.4 3.8 1.4 5.2 0" fill="none" stroke="#2d2f33" strokeWidth="1.2" strokeLinecap="round" />
    </svg>
  );
}

function ResidentAvatar({ resident, className = "", size = "normal" }) {
  const name = resident?.name || resident?.userName || "Morador";
  const image = resident?.avatarUrl || resident?.avatar_url;

  return (
    <span className={`resident-avatar ${size === "large" ? "resident-avatar-large" : size === "small" ? "resident-avatar-small" : ""} ${className}`} aria-label={`Avatar de ${name}`}>
      {image
        ? <img src={image} alt="" onError={event => { event.currentTarget.remove(); }} />
        : <AnimatedFigure name={name} />}
    </span>
  );
}

export default ResidentAvatar;
