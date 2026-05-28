package com.dondeanime.backend.anime;

import java.util.List;
import java.util.Map;

import com.dondeanime.backend.provider.ProviderDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del endpoint GET /api/anime/{slug}.
 *
 * Envuelve un AnimeDetailDto (campos públicos del anime) con un
 * Map de providers agrupados por código de país.
 *
 * Ambos sub-DTOs ya esconden los campos internos (id interno,
 * syncedAt, tmdbId, updatedAt de providers, etc.).
 */
@Schema(description = "Detalle de anime con providers agrupados por codigo de pais")
public record AnimeDetailResponse(
        AnimeDetailDto anime,
        Map<String, List<ProviderDto>> watchProvidersByCountry
) {}
