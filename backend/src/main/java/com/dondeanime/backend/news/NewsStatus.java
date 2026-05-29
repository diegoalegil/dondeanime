package com.dondeanime.backend.news;

/**
 * Ciclo de vida de una noticia.
 * DRAFT: recién ingerida del RSS, aún sin procesar/traducir.
 * PENDING_REVIEW: procesada por el LLM, esperando aprobación de Diego.
 * PUBLISHED: visible en el sitio.
 * ARCHIVED: retirada.
 */
public enum NewsStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    ARCHIVED
}
