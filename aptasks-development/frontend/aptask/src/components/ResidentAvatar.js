import React from "react";

function ResidentAvatar({ resident, className = "", size = "normal" }) {
  const name = resident?.name || resident?.userName || "Morador";
  const initials = name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join("").toUpperCase();
  const image = resident?.avatarUrl || resident?.avatar_url;

  return (
    <span className={`resident-avatar ${size === "large" ? "resident-avatar-large" : size === "small" ? "resident-avatar-small" : ""} ${className}`} aria-label={`Avatar de ${name}`}>
      <span className="resident-avatar-initials">{initials}</span>
      {image && <img src={image} alt="" onError={event => { event.currentTarget.remove(); }} />}
    </span>
  );
}

export default ResidentAvatar;
