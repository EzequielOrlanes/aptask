import React, { useEffect, useRef } from "react";

// Paletas fixas: a mesma pessoa sempre recebe a mesma aparência, escolhida pelo nome.
const PALETTES = [
  { skin: "#f3c9a5", hair: "#3b2a22", shirt: "#3cc7b6", pants: "#3b3f4a" },
  { skin: "#e8b48f", hair: "#7a3e2b", shirt: "#f4a06b", pants: "#c9b89a" },
  { skin: "#d9a07a", hair: "#1f1b18", shirt: "#8f8ae0", pants: "#1f2430" },
  { skin: "#f6d6bd", hair: "#c9893f", shirt: "#f7d46a", pants: "#6b5a4a" },
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
    <svg ref={ref} className="avatar-figure" viewBox="0 0 48 88" role="img" aria-hidden="true" style={{ animationDelay: delay }}>
      {/* Pernas, sapatos, braços, tronco e cabeça: corpo inteiro em pé */}
      <rect x="14.5" y="52" width="9" height="28" rx="3.5" fill={palette.pants} />
      <rect x="24.5" y="52" width="9" height="28" rx="3.5" fill={palette.pants} />
      <ellipse cx="19" cy="82.5" rx="5.2" ry="2.6" fill="#2d2f33" />
      <ellipse cx="29" cy="82.5" rx="5.2" ry="2.6" fill="#2d2f33" />
      <rect x="8.8" y="28" width="4.8" height="22" rx="2.4" fill={palette.shirt} />
      <rect x="34.4" y="28" width="4.8" height="22" rx="2.4" fill={palette.shirt} />
      <circle cx="11.2" cy="51.5" r="2.5" fill={palette.skin} />
      <circle cx="36.8" cy="51.5" r="2.5" fill={palette.skin} />
      <path d="M13 31 Q13 26 18 26 H30 Q35 26 35 31 V53 H13 Z" fill={palette.shirt} />
      <rect x="21.5" y="21" width="5" height="6" rx="1.5" fill={palette.skin} />
      <circle cx="24" cy="14.5" r="8.5" fill={palette.skin} />
      <path d="M15.5 14c-.5-5.6 3.2-9.4 8.7-9.4 4.6 0 8.3 3 8.6 8-2.3-2.3-5.2-3.4-8.5-3.4-3.2 0-6.4 1.1-8.8 4.8z" fill={palette.hair} />
      <g className="avatar-gaze" style={{ transform: "translate(var(--eye-x, 0px), var(--eye-y, 0px))" }}>
        <ellipse className="avatar-eye" cx="20.8" cy="15.5" rx="1.1" ry="1.4" fill="#2d2f33" />
        <ellipse className="avatar-eye" cx="27.2" cy="15.5" rx="1.1" ry="1.4" fill="#2d2f33" />
      </g>
      <circle cx="18.3" cy="18.6" r="1.3" fill="#f08a8a" opacity=".45" />
      <circle cx="29.7" cy="18.6" r="1.3" fill="#f08a8a" opacity=".45" />
      <path d="M21.6 19.6c1.2 1.2 3.6 1.2 4.8 0" fill="none" stroke="#2d2f33" strokeWidth="1.1" strokeLinecap="round" />
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
