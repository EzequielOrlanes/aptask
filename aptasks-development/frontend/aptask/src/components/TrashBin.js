import React from "react";

export const FILL_LEVELS = ["EMPTY", "HALF", "FULL"];

export const FILL_LABELS = {
  EMPTY: "Vazia",
  HALF: "Meio cheia",
  FULL: "Cheia",
};

// Altura do líquido em relação ao corpo da lixeira (0 = vazia, 100 = cheia).
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

// Pedaços de fruta boiando no líquido. As posições são locais ao líquido (y = 0 no topo do corpo
// quando cheia), então sobem junto com o nível. "stage" HALF aparece a partir de meio cheia.
const FRUITS = [
  { x: 44, y: 60, r: -15, kind: "orange", delay: 0, stage: "HALF" },
  { x: 72, y: 68, r: 20, kind: "apple", delay: 0.8, stage: "HALF" },
  { x: 58, y: 78, r: -5, kind: "banana", delay: 1.5, stage: "HALF" },
  { x: 36, y: 74, r: 10, kind: "apple", delay: 0.4, stage: "HALF" },
  { x: 80, y: 56, r: -30, kind: "orange", delay: 1.1, stage: "FULL" },
  { x: 50, y: 40, r: 12, kind: "banana", delay: 0.6, stage: "FULL" },
  { x: 70, y: 30, r: -10, kind: "apple", delay: 1.3, stage: "FULL" },
];

function FruitShape({ kind }) {
  switch (kind) {
    case "orange":
      return (
        <g>
          <circle r="7" fill="#ff9b3d" stroke="#d9702a" strokeWidth="1" />
          <path d="M0 0V-6M0 0L5 -3M0 0L5 3M0 0V6M0 0L-5 3M0 0L-5 -3" stroke="#ffd39a" strokeWidth=".9" />
        </g>
      );
    case "apple":
      return (
        <g>
          <path d="M-6 -2 Q-6 -7 0 -5 Q6 -7 6 -2 Q6 5 0 6 Q-6 5 -6 -2 Z" fill="#e8534f" stroke="#a83a36" strokeWidth=".9" />
          <path d="M0 -5 V-8" stroke="#6b4a2b" strokeWidth="1" strokeLinecap="round" />
        </g>
      );
    case "banana":
      return (
        <path d="M-7 -3 Q0 -8 7 -3 Q2 -4 -7 -3 Z" fill="#ffe066" stroke="#c9a52a" strokeWidth=".9" />
      );
    default:
      return null;
  }
}

function Fruits({ level }) {
  const showHalf = level === "HALF" || level === "FULL";
  const showFull = level === "FULL";
  return FRUITS
    .filter(fruit => (fruit.stage === "HALF" ? showHalf : showFull))
    .map(fruit => (
      <g key={`${fruit.x}-${fruit.y}`} transform={`translate(${fruit.x} ${fruit.y}) rotate(${fruit.r}) scale(1.5)`}>
        <g className="trash-fruit" style={{ animationDelay: `${fruit.delay}s` }}>
          <FruitShape kind={fruit.kind} />
        </g>
      </g>
    ));
}

// Pedaços que caem para fora pela borda quando a lixeira está cheia.
const SPILLS = [
  { delay: 0, kind: "orange" },
  { delay: 1.4, kind: "apple" },
  { delay: 2.7, kind: "banana" },
];

function Spills() {
  return SPILLS.map((spill, index) => (
    <g key={index} transform="translate(96 40) scale(1.4)">
      <g className="trash-spill" style={{ animationDelay: `${spill.delay}s` }}>
        <FruitShape kind={spill.kind} />
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
  // O líquido sobe de forma contínua: 0% fica abaixo do corpo, 100% preenche tudo.
  const offset = BODY_HEIGHT * (1 - percent / 100);

  return (
    <figure className={`trash-bin trash-bin--${level.toLowerCase()}`}>
      <svg viewBox="0 0 120 160" role="img" aria-label={`Lixeira ${FILL_LABELS[level].toLowerCase()}`}>
        <defs>
          <clipPath id="trash-bin-body">
            <path d={BODY_PATH} />
          </clipPath>
        </defs>

        {/* Líquido dentro do corpo, com ondas na superfície */}
        <g clipPath="url(#trash-bin-body)">
          <rect className="trash-bin__interior" x="0" y={BODY_TOP} width="120" height={BODY_HEIGHT + 10} />
          {/* transform por atributo: o Chrome ignora transform por CSS dentro de um clip-path */}
          <g className="trash-bin__liquid" transform={`translate(0 ${offset})`}>
            <rect className="trash-bin__fill" x="0" y={BODY_TOP} width="120" height={BODY_HEIGHT + 10} />
            <path className="trash-bin__wave" d="M-30 40 Q-15 30 0 40 T30 40 T60 40 T90 40 T120 40 T150 40 V150 H-30 Z" />
            <Fruits level={level} />
          </g>
        </g>

        {/* Contorno do corpo */}
        <path className="trash-bin__outline" d={BODY_PATH} />

        {/* Aro */}
        <rect className="trash-bin__rim" x="16" y="34" width="88" height="6" rx="3" />

        {/* Tampa e puxador: abrem quando a lixeira está cheia (dobradiça à esquerda) */}
        <g className="trash-bin__lidgroup">
          <rect className="trash-bin__lid" x="12" y="24" width="96" height="12" rx="6" />
          <rect className="trash-bin__knob" x="50" y="14" width="20" height="10" rx="5" />
        </g>

        {/* Pedaços caindo para fora, só quando cheia */}
        {level === "FULL" && <Spills />}

        {/* Moscas voando em volta da lixeira */}
        <Flies count={FLY_COUNT[level] ?? 0} />
      </svg>
      <figcaption>{FILL_LABELS[level]}</figcaption>
    </figure>
  );
}

export default TrashBin;
