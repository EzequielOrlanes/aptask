import React from "react";

export const FILL_LEVELS = ["EMPTY", "HALF", "FULL"];

export const FILL_LABELS = {
  EMPTY: "Vazia",
  HALF: "Meio cheia",
  FULL: "Cheia",
};

// Altura do conteúdo em relação ao corpo da lixeira (0 = vazia, 100 = cheia).
const FILL_PERCENT = { EMPTY: 0, HALF: 50, FULL: 100 };

// Lê o nível do status vindo do backend. Se o campo novo não existir, usa o booleano antigo.
export function getFillLevel(trashStatus) {
  if (FILL_LEVELS.includes(trashStatus?.fillLevel)) return trashStatus.fillLevel;
  return trashStatus?.isFull ? "FULL" : "EMPTY";
}

const BODY_TOP = 40;
const BODY_BOTTOM = 146;
const BODY_HEIGHT = BODY_BOTTOM - BODY_TOP;
// Corpo afunilado: mais largo em cima, mais estreito embaixo.
const BODY_PATH = "M20 40 H100 L92 138 Q91 146 83 146 H37 Q29 146 28 138 Z";

// Topo do conteúdo com bordas amassadas, como papel e sacolas.
function crumpledTop() {
  let d = "M-30 44";
  for (let x = -30; x < 150; x += 14) d += ` q7 -9 14 0`;
  return `${d} V160 H-30 Z`;
}
const CRUMPLED_TOP = crumpledTop();

// Objetos dentro da lixeira. "stage" indica o nível em que aparecem: HALF (meio cheia) ou FULL (cheia).
const ITEMS = [
  { x: 36, y: 132, r: -8, kind: "bottle", stage: "HALF" },
  { x: 54, y: 137, r: 12, kind: "can", stage: "HALF" },
  { x: 72, y: 129, r: 6, kind: "bag", stage: "HALF" },
  { x: 86, y: 139, r: 20, kind: "ball", stage: "HALF" },
  { x: 62, y: 114, r: -4, kind: "ball", stage: "HALF" },
  { x: 44, y: 116, r: 8, kind: "can", stage: "HALF" },
  { x: 40, y: 100, r: -10, kind: "bottle", stage: "FULL" },
  { x: 60, y: 92, r: 14, kind: "can", stage: "FULL" },
  { x: 78, y: 104, r: 0, kind: "ball", stage: "FULL" },
  { x: 34, y: 84, r: -18, kind: "can", stage: "FULL" },
  { x: 56, y: 74, r: 4, kind: "bag", stage: "FULL" },
  { x: 76, y: 80, r: -6, kind: "bottle", stage: "FULL" },
  { x: 48, y: 66, r: 10, kind: "ball", stage: "FULL" },
  { x: 70, y: 64, r: -8, kind: "can", stage: "FULL" },
];

function ItemShape({ kind }) {
  switch (kind) {
    case "ball":
      return (
        <g>
          <circle r="6" fill="#f6f3ec" stroke="#aaa291" strokeWidth="1" />
          <path d="M-4 -2l3 2 2-3 3 3M-3 3l3-2 3 3" fill="none" stroke="#aaa291" strokeWidth=".8" strokeLinecap="round" />
        </g>
      );
    case "can":
      return (
        <g>
          <rect x="-5" y="-9" width="10" height="18" rx="2" fill="#a9c3d1" stroke="#6f8f9f" strokeWidth=".8" />
          <rect x="-5" y="-9" width="10" height="3" fill="#7fa0b2" />
        </g>
      );
    case "bottle":
      return (
        <path d="M-3-12h6v5q3 3 3 7v9q0 2-2 2h-8q-2 0-2-2V0q0-4 3-7z" fill="#b8e0c8" stroke="#6fa88a" strokeWidth=".8" />
      );
    case "bag":
      return (
        <path d="M-8-6h16l2 16q0 2-2 2h-14q-2 0-2-2z" fill="#e6d3a3" stroke="#b39a5e" strokeWidth=".8" />
      );
    default:
      return null;
  }
}

function WasteItems({ level }) {
  const showHalf = level === "HALF" || level === "FULL";
  const showFull = level === "FULL";
  return ITEMS
    .filter(item => (item.stage === "HALF" ? showHalf : showFull))
    .map((item, index) => (
      <g key={`${item.x}-${item.y}`} transform={`translate(${item.x} ${item.y}) rotate(${item.r})`}>
        <g className="trash-item" style={{ animationDelay: `${index * 0.06}s` }}>
          <ItemShape kind={item.kind} />
        </g>
      </g>
    ));
}

// Moscas: posição (x, y) no viewBox e atraso da animação. Meio cheia usa as 3 primeiras; cheia, as 7.
const FLIES = [
  [96, 28, 0],
  [14, 70, 0.6],
  [106, 98, 1.1],
  [18, 120, 0.4],
  [102, 58, 1.6],
  [6, 40, 0.9],
  [66, 6, 1.3],
];
const FLY_COUNT = { EMPTY: 0, HALF: 3, FULL: 7 };

function Flies({ count }) {
  return FLIES.slice(0, count).map(([x, y, delay], index) => (
    <g key={index} transform={`translate(${x} ${y})`}>
      <g className="trash-fly" style={{ animationDelay: `${delay}s` }}>
        <ellipse className="trash-fly__wing" cx="-2.6" cy="-2" rx="2.4" ry="1.3" />
        <ellipse className="trash-fly__wing trash-fly__wing--right" cx="2.6" cy="-2" rx="2.4" ry="1.3" />
        <ellipse className="trash-fly__body" cx="0" cy="0" rx="2.2" ry="1.6" />
        <circle className="trash-fly__head" cx="2.2" cy="-0.4" r="1" />
      </g>
    </g>
  ));
}

function TrashBin({ level = "EMPTY" }) {
  const percent = FILL_PERCENT[level] ?? 0;
  // O conteúdo sobe de forma contínua: 0% fica abaixo do corpo, 100% preenche tudo.
  const offset = BODY_HEIGHT * (1 - percent / 100);

  return (
    <figure className={`trash-bin trash-bin--${level.toLowerCase()}`}>
      <svg viewBox="0 0 120 160" role="img" aria-label={`Lixeira ${FILL_LABELS[level].toLowerCase()}`}>
        <defs>
          <clipPath id="trash-bin-body">
            <path d={BODY_PATH} />
          </clipPath>
        </defs>

        {/* Conteúdo dentro do corpo */}
        <g clipPath="url(#trash-bin-body)">
          <g className="trash-bin__content" style={{ transform: `translateY(${offset}px)` }}>
            <rect className="trash-bin__waste" x="0" y={BODY_TOP} width="120" height={BODY_HEIGHT + 12} />
            <path className="trash-bin__crumple" d={CRUMPLED_TOP} />
          </g>
          <WasteItems level={level} />
        </g>

        {/* Nervuras e frisos do corpo */}
        <g className="trash-bin__ribs" clipPath="url(#trash-bin-body)">
          <path d="M18 62 H102 M18 84 H102 M18 106 H102 M18 128 H102" />
          <path d="M44 46 V146 M60 46 V146 M76 46 V146" />
        </g>

        {/* Contorno do corpo */}
        <path className="trash-bin__outline" d={BODY_PATH} />

        {/* Aro e tampa */}
        <rect className="trash-bin__rim" x="16" y="34" width="88" height="6" rx="3" />
        <rect className="trash-bin__lid" x="12" y="24" width="96" height="12" rx="6" />
        <rect className="trash-bin__knob" x="50" y="14" width="20" height="10" rx="5" />

        {/* Moscas voando em volta da lixeira */}
        <Flies count={FLY_COUNT[level] ?? 0} />
      </svg>
      <figcaption>{FILL_LABELS[level]}</figcaption>
    </figure>
  );
}

export default TrashBin;
