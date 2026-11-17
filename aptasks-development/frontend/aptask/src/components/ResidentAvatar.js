import React, { useEffect, useId, useRef } from "react";

// Aparências dos moradores, pelo primeiro nome (sem acento e em minúsculas).
// Tons de pele, estilo de cabelo e óculos foram definidos para cada pessoa.
const APPEARANCES = {
  andre: {
    skinLight: "#e0ad85", skin: "#cc9068", skinShade: "#9d6443",
    hair: "#1d1512", hairLight: "#3a2b24", hairStyle: "curly", glasses: false,
    shirt: "#6aa8f0", shirtDark: "#3f7fd0", collar: "#f5d36b",
  },
  gabriela: {
    skinLight: "#b57a55", skin: "#9a6240", skinShade: "#6e4026",
    hair: "#0e0b0a", hairLight: "#2a221e", hairStyle: "long", glasses: false,
    shirt: "#ff8fb1", shirtDark: "#e0628a", collar: "#fff0c2",
  },
  ezequiel: {
    skinLight: "#a06f4c", skin: "#82522f", skinShade: "#5b3520",
    hair: "#17110e", hairLight: "#2e231d", hairStyle: "fade", glasses: false,
    shirt: "#7ee08a", shirtDark: "#4fb35f", collar: "#c7a6e8",
  },
  guilherme: {
    skinLight: "#d4a07a", skin: "#bb8058", skinShade: "#8b5536",
    hair: "#120e0c", hairLight: "#2b221e", hairStyle: "fade", glasses: true,
    shirt: "#ffb27a", shirtDark: "#e07f4c", collar: "#4fb6c9",
  },
};

// Aparências de reserva, para nomes que não estão na lista acima.
const FALLBACKS = [
  { skinLight: "#d9a27c", skin: "#b9764f", skinShade: "#8a5234", hair: "#15110f", hairLight: "#2c2521", hairStyle: "fade", glasses: false, shirt: "#8f8ae0", shirtDark: "#6a66bd", collar: "#f5d36b" },
  { skinLight: "#c79270", skin: "#a86d4a", skinShade: "#7b4a2d", hair: "#1f1714", hairLight: "#3a2d26", hairStyle: "curly", glasses: false, shirt: "#f7d46a", shirtDark: "#d9b24a", collar: "#3cc7b6" },
];

function normalizeFirstName(name) {
  return name
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .trim()
    .split(/\s+/)[0]
    .toLowerCase();
}

function hashName(text) {
  let hash = 0;
  for (const char of text) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  return hash;
}

function appearanceFor(name) {
  return APPEARANCES[normalizeFirstName(name)] || FALLBACKS[hashName(name) % FALLBACKS.length];
}

const MAX_EYE_OFFSET = 1.8; // em unidades do viewBox (100x100)

function AnimatedHead({ name }) {
  const ref = useRef(null);
  const uid = useId().replace(/:/g, "");
  const look = appearanceFor(name);
  // Pequena defasagem entre as figuras, para não piscarem em sincronia.
  const delay = `${(hashName(name) % 7) * 0.4}s`;

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
          <stop offset="0%" stopColor={look.skinLight} />
          <stop offset="62%" stopColor={look.skin} />
          <stop offset="100%" stopColor={look.skinShade} />
        </radialGradient>
        <linearGradient id={shirtId} x1="0" y1="0" x2="0.4" y2="1">
          <stop offset="0%" stopColor={look.shirt} />
          <stop offset="100%" stopColor={look.shirtDark} />
        </linearGradient>
        <linearGradient id={hairId} x1="0" y1="0" x2="0.3" y2="1">
          <stop offset="0%" stopColor={look.hairLight} />
          <stop offset="100%" stopColor={look.hair} />
        </linearGradient>
        <radialGradient id={blushId}>
          <stop offset="0%" stopColor="#ff6f6f" stopOpacity="0.4" />
          <stop offset="100%" stopColor="#ff6f6f" stopOpacity="0" />
        </radialGradient>
      </defs>

      {/* Busto: camiseta com gola */}
      <path d="M14 100c0-15 11-22 26-23h20c15 1 26 8 26 23z" fill={`url(#${shirtId})`} />
      <path d="M38 77q12 10 24 0" fill="none" stroke={look.collar} strokeWidth="3" strokeLinecap="round" />

      {/* Cabelo longo: fica atrás dos ombros */}
      {look.hairStyle === "long" && (
        <path d="M23 60C19 34 30 16 50 16c20 0 31 18 27 44l2 26c-9-2-13-10-13-16H34c0 6-4 14-13 16z" fill={`url(#${hairId})`} />
      )}

      {/* Cabelo crespo: volume atrás da cabeça */}
      {look.hairStyle === "curly" && (
        <g fill={`url(#${hairId})`}>
          <circle cx="50" cy="26" r="17" />
          <circle cx="32" cy="32" r="12" />
          <circle cx="68" cy="32" r="12" />
          <circle cx="41" cy="16" r="11" />
          <circle cx="60" cy="16" r="11" />
        </g>
      )}

      {/* Pescoço com sombra */}
      <path d="M41 62h18v16c-3 3-15 3-18 0z" fill={look.skinShade} />

      {/* Orelhas */}
      <ellipse cx="25.5" cy="50" rx="4.2" ry="6.4" fill={look.skin} />
      <ellipse cx="74.5" cy="50" rx="4.2" ry="6.4" fill={look.skin} />

      {/* Rosto */}
      <path d="M50 18c-15 0-24 10-24 26 0 13 8 24 24 26 16-2 24-13 24-26 0-16-9-26-24-26z" fill={`url(#${skinId})`} />

      {/* Cabelo da frente */}
      {look.hairStyle === "fade" && (
        <path d="M28 40C27 26 36 17 50 17c14 0 23 9 22 23-4-6-8-7-12-7-4-3-16-3-20 0-4 0-8 1-12 7z" fill={`url(#${hairId})`} />
      )}
      {look.hairStyle === "long" && (
        <path d="M26 44C24 26 36 14 50 14c14 0 26 12 24 30-4-8-12-14-24-14s-20 6-24 14z" fill={`url(#${hairId})`} />
      )}

      {/* Sobrancelhas */}
      <path d="M35 39q5-3 10-1" fill="none" stroke={look.hair} strokeWidth="2.6" strokeLinecap="round" />
      <path d="M55 38q5-2 10 1" fill="none" stroke={look.hair} strokeWidth="2.6" strokeLinecap="round" />

      {/* Óculos */}
      {look.glasses && (
        <g fill="none" stroke="#1f2328" strokeWidth="2">
          <circle cx="40" cy="47" r="7" />
          <circle cx="60" cy="47" r="7" />
          <path d="M47 47h6" />
          <path d="M33 46L26 44M67 46l7-2" strokeWidth="1.6" />
        </g>
      )}

      {/* Olhos: seguem o ponteiro; piscam pela classe avatar-eye */}
      <g className="avatar-gaze" style={{ transform: "translate(var(--eye-x, 0px), var(--eye-y, 0px))" }}>
        <ellipse className="avatar-eye" cx="40" cy="47" rx="3.2" ry="4" fill="#1f2328" />
        <ellipse className="avatar-eye" cx="60" cy="47" rx="3.2" ry="4" fill="#1f2328" />
        <circle cx="41.2" cy="45.4" r="1.1" fill="#ffffff" />
        <circle cx="61.2" cy="45.4" r="1.1" fill="#ffffff" />
      </g>

      {/* Nariz e bochechas */}
      <path d="M50 52q-2 5 1 7q2 1 3 0" fill="none" stroke={look.skinShade} strokeWidth="1.6" strokeLinecap="round" />
      <circle cx="33" cy="58" r="6" fill={`url(#${blushId})`} />
      <circle cx="67" cy="58" r="6" fill={`url(#${blushId})`} />

      {/* Sorriso */}
      <path d="M42 66q8 7 16 0q-8 2-16 0z" fill="#4a2620" stroke="#4a2620" strokeWidth="1.4" strokeLinejoin="round" />
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
