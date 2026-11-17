import React, { useEffect, useId, useRef } from "react";

// Paletas fixas: a mesma pessoa sempre recebe a mesma aparência, escolhida pelo nome.
const PALETTES = [
  { skinLight: "#ffd9b8", skin: "#f2b48c", skinShade: "#d98b62", hair: "#2b211c", hairLight: "#4a3a31", shirt: "#7ee08a", shirtDark: "#4fb35f", collar: "#c7a6e8" },
  { skinLight: "#f5c39a", skin: "#e0a076", skinShade: "#b97650", hair: "#6b3a25", hairLight: "#8f5538", shirt: "#ffb27a", shirtDark: "#e07f4c", collar: "#4fb6c9" },
  { skinLight: "#d9a27c", skin: "#b9764f", skinShade: "#8a5234", hair: "#15110f", hairLight: "#2c2521", shirt: "#8f8ae0", shirtDark: "#6a66bd", collar: "#f5d36b" },
  { skinLight: "#fde3cf", skin: "#f5c7a4", skinShade: "#d9a07c", hair: "#c58a3f", hairLight: "#e0a95c", shirt: "#f7d46a", shirtDark: "#d9b24a", collar: "#3cc7b6" },
];

// Dois penteados: volume alto e topete lateral.
const HAIR_PATHS = [
  "M24 46C20 26 32 12 50 12c18 0 29 13 25 34-2-8-7-14-14-17-9 4-24 5-32 1-4 4-6 9-5 16z",
  "M24 48C21 30 30 14 46 13c12-1 22 5 27 16 2 4 3 9 3 15-4-9-10-13-17-14-8 5-22 6-32 0-3 4-4 9-3 18z",
];

function hashName(text) {
  let hash = 0;
  for (const char of text) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  return hash;
}

const MAX_EYE_OFFSET = 1.8; // em unidades do viewBox (100x100)

function AnimatedHead({ name }) {
  const ref = useRef(null);
  const uid = useId().replace(/:/g, "");
  const hash = hashName(name);
  const palette = PALETTES[hash % PALETTES.length];
  const hairPath = HAIR_PATHS[(hash >> 3) % HAIR_PATHS.length];
  // Pequena defasagem entre as figuras, para não piscarem em sincronia.
  const delay = `${(hash % 7) * 0.4}s`;

  // Os olhos seguem o ponteiro: direção normalizada e deslocamento limitado.
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

  const skinId = `skin-${uid}`;
  const shirtId = `shirt-${uid}`;
  const hairId = `hair-${uid}`;
  const blushId = `blush-${uid}`;

  return (
    <svg ref={ref} className="avatar-figure" viewBox="0 0 100 100" role="img" aria-hidden="true" style={{ animationDelay: delay }}>
      <defs>
        <radialGradient id={skinId} cx="40%" cy="32%" r="75%">
          <stop offset="0%" stopColor={palette.skinLight} />
          <stop offset="62%" stopColor={palette.skin} />
          <stop offset="100%" stopColor={palette.skinShade} />
        </radialGradient>
        <linearGradient id={shirtId} x1="0" y1="0" x2="0.4" y2="1">
          <stop offset="0%" stopColor={palette.shirt} />
          <stop offset="100%" stopColor={palette.shirtDark} />
        </linearGradient>
        <linearGradient id={hairId} x1="0" y1="0" x2="0.3" y2="1">
          <stop offset="0%" stopColor={palette.hairLight} />
          <stop offset="100%" stopColor={palette.hair} />
        </linearGradient>
        <radialGradient id={blushId}>
          <stop offset="0%" stopColor="#ff6f6f" stopOpacity="0.45" />
          <stop offset="100%" stopColor="#ff6f6f" stopOpacity="0" />
        </radialGradient>
      </defs>

      {/* Busto: camiseta com gola */}
      <path d="M14 100c0-15 11-22 26-23h20c15 1 26 8 26 23z" fill={`url(#${shirtId})`} />
      <path d="M38 77q12 10 24 0" fill="none" stroke={palette.collar} strokeWidth="3" strokeLinecap="round" />

      {/* Pescoço com sombra */}
      <path d="M41 62h18v16c-3 3-15 3-18 0z" fill={palette.skinShade} />

      {/* Orelhas */}
      <ellipse cx="25.5" cy="50" rx="4.2" ry="6.4" fill={palette.skin} />
      <ellipse cx="74.5" cy="50" rx="4.2" ry="6.4" fill={palette.skin} />

      {/* Rosto */}
      <path d="M50 18c-15 0-24 10-24 26 0 13 8 24 24 26 16-2 24-13 24-26 0-16-9-26-24-26z" fill={`url(#${skinId})`} />

      {/* Cabelo com volume */}
      <path d={hairPath} fill={`url(#${hairId})`} />

      {/* Sobrancelhas */}
      <path d="M35 39q5-3 10-1" fill="none" stroke={palette.hair} strokeWidth="2.6" strokeLinecap="round" />
      <path d="M55 38q5-2 10 1" fill="none" stroke={palette.hair} strokeWidth="2.6" strokeLinecap="round" />

      {/* Olhos: seguem o ponteiro; piscam pelo classe avatar-eye */}
      <g className="avatar-gaze" style={{ transform: "translate(var(--eye-x, 0px), var(--eye-y, 0px))" }}>
        <ellipse className="avatar-eye" cx="40" cy="47" rx="3.2" ry="4" fill="#1f2328" />
        <ellipse className="avatar-eye" cx="60" cy="47" rx="3.2" ry="4" fill="#1f2328" />
        <circle cx="41.2" cy="45.4" r="1.1" fill="#ffffff" />
        <circle cx="61.2" cy="45.4" r="1.1" fill="#ffffff" />
      </g>

      {/* Nariz e bochechas */}
      <path d="M50 52q-2 5 1 7q2 1 3 0" fill="none" stroke={palette.skinShade} strokeWidth="1.6" strokeLinecap="round" />
      <circle cx="33" cy="58" r="6" fill={`url(#${blushId})`} />
      <circle cx="67" cy="58" r="6" fill={`url(#${blushId})`} />

      {/* Sorriso */}
      <path d="M42 66q8 7 16 0q-8 2-16 0z" fill="#7a3b33" stroke="#7a3b33" strokeWidth="1.4" strokeLinejoin="round" />
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
        : <AnimatedHead name={name} />}
    </span>
  );
}

export default ResidentAvatar;
