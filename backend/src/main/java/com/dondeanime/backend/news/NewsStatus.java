package com.dondeanime.backend.news;

/**
 * Ciclo de vida de una noticia.
 * DRAFT: recién ingerida del RSS, aún sin procesar/traducir.
 * PENDING_REVIEW: redactada por el LLM, esperando aprobación manual desde el
 * bot de Telegram (Publicar/Descartar). Solo se usa con news.telegram.enabled.
 * PUBLISHED: visible en el sitio.
 * DISCARDED: descartada en revisión; no se publica ni se reprocesa.
 * ARCHIVED: retirada.
 */
public enum NewsStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    DISCARDED,
    ARCHIVED
}
