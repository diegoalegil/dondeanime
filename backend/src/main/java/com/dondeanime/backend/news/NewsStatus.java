package com.dondeanime.backend.news;

/**
 * Ciclo de vida de una noticia.
 * DRAFT: recién ingerida del RSS, aún sin procesar/traducir.
 * PENDING_REVIEW: reservado para la fase LLM+Telegram (S2): procesada,
 * esperando aprobación manual. El flujo actual NO lo usa: el procesado
 * heurístico pasa DRAFT→PUBLISHED directamente si news.processing.publish=true.
 * PUBLISHED: visible en el sitio.
 * ARCHIVED: retirada.
 */
public enum NewsStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    ARCHIVED
}
