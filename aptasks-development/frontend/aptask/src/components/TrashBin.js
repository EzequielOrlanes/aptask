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
const BODY_HEIGHT = 106;

function TrashBin({ level = "EMPTY" }) {
  const percent = FILL_PERCENT[level] ?? 0;
  // O líquido sobe de forma contínua: 0% desloca tudo para baixo do corpo, 100% deixa tudo visível.
  const offset = BODY_HEIGHT * (1 - percent / 100);

  return (
    <figure className={`trash-bin trash-bin--${level.toLowerCase()}`}>
      <svg viewBox="0 0 120 150" role="img" aria-label={`Lixeira ${FILL_LABELS[level].toLowerCase()}`}>
        <defs>
          <clipPath id="trash-bin-body">
            <path d="M22 40 L98 40 L90 140 Q89 146 83 146 L37 146 Q31 146 30 140 Z" />
          </clipPath>
        </defs>

        <g clipPath="url(#trash-bin-body)">
          <g className="trash-bin__liquid" style={{ transform: `translateY(${offset}px)` }}>
            <rect className="trash-bin__fill" x="0" y={BODY_TOP} width="120" height={BODY_HEIGHT + 10} />
            <path className="trash-bin__wave" d="M-30 40 Q-15 30 0 40 T30 40 T60 40 T90 40 T120 40 T150 40 V150 H-30 Z" />
          </g>
        </g>

        <path className="trash-bin__outline" d="M22 40 L98 40 L90 140 Q89 146 83 146 L37 146 Q31 146 30 140 Z" />
        <rect className="trash-bin__lid" x="14" y="26" width="92" height="12" rx="4" />
        <path className="trash-bin__handle" d="M50 26 Q60 12 70 26" />
      </svg>
      <figcaption>{FILL_LABELS[level]}</figcaption>
    </figure>
  );
}

export default TrashBin;
