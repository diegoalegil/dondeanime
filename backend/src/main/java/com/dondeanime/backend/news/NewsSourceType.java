package com.dondeanime.backend.news;

/**
 * Origen de una fuente de noticias.
 * RSS: feed XML (ANN, Crunchyroll). REDDIT/YOUTUBE: vía API. MANUAL: enviada por Diego (Telegram/admin).
 */
public enum NewsSourceType {
    RSS,
    REDDIT,
    YOUTUBE,
    MANUAL
}
