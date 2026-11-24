import React from "react";

/**
 * Placeholder animado exibido enquanto não há dados confiáveis do backend.
 * Nunca mostra conteúdo inventado: só formas pulsando.
 * variant: "row" (linha de lista), "card" (cartão de morador) ou "block" (área livre).
 */
function Skeleton({ variant = "row", count = 1, label = "Carregando…", className = "" }) {
  return (
    <div className={`skeleton-group ${className}`.trim()} role="status" aria-busy="true" aria-live="polite">
      <span className="skeleton-sr">{label}</span>
      {Array.from({ length: count }, (_, index) => (
        <span key={index} className={`skeleton skeleton--${variant}`} aria-hidden="true" />
      ))}
    </div>
  );
}

export default Skeleton;
