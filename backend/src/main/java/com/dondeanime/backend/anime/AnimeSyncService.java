package com.dondeanime.backend.anime;

import java.text.Normalizer;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dondeanime.backend.anime.anilist.AniListClient;
import com.dondeanime.backend.anime.anilist.AniListCharacter;
import com.dondeanime.backend.anime.anilist.AniListCharacterConnection;
import com.dondeanime.backend.anime.anilist.AniListCharacterEdge;
import com.dondeanime.backend.anime.anilist.AniListCharacterImage;
import com.dondeanime.backend.anime.anilist.AniListCharacterName;
import com.dondeanime.backend.anime.anilist.AniListCoverImage;
import com.dondeanime.backend.anime.anilist.AniListFuzzyDate;
import com.dondeanime.backend.anime.anilist.AniListMedia;
import com.dondeanime.backend.anime.anilist.AniListStudio;
import com.dondeanime.backend.anime.anilist.AniListStudioConnection;
import com.dondeanime.backend.anime.anilist.AniListTag;
import com.dondeanime.backend.anime.anilist.AniListTitle;
import com.dondeanime.backend.character.AnimeCharacter;
import com.dondeanime.backend.character.AnimeCharacterRepository;
import com.dondeanime.backend.character.AnimeCharacterRole;
import com.dondeanime.backend.studio.Studio;
import com.dondeanime.backend.studio.StudioRepository;

/**
 * Orquesta el sync desde AniList hacia la BD local.
 *
 * Flujo por anime:
 *   1. findByAnilistId → si existe, actualiza esa entidad; si no, crea una nueva.
 *   2. Copia campos del DTO a la entidad (defensivo ante nulls anidados).
 *   3. Genera slug a partir del título inglés (o romaji), con dedupe por anilistId.
 *   4. Marca syncedAt y guarda.
 *
 * Sin @Transactional: cada save es su propia transacción auto-commit.
 * Así un anime que falle no arrastra a los demás.
 */
@Service
public class AnimeSyncService {

    public static final int MAX_POPULAR_SYNC_COUNT = 1000;

    private static final Logger log = LoggerFactory.getLogger(AnimeSyncService.class);

    private final AniListClient client;
    private final AnimeRepository repository;
    private final StudioRepository studioRepository;
    private final AnimeCharacterRepository characterRepository;

    public AnimeSyncService(
            AniListClient client,
            AnimeRepository repository,
            StudioRepository studioRepository,
            AnimeCharacterRepository characterRepository) {
        this.client = client;
        this.repository = repository;
        this.studioRepository = studioRepository;
        this.characterRepository = characterRepository;
    }

    public int syncPopular(int count) {
        if (count < 1 || count > MAX_POPULAR_SYNC_COUNT) {
            throw new IllegalArgumentException("count debe estar entre 1 y " + MAX_POPULAR_SYNC_COUNT);
        }

        log.info("Sync AniList iniciado: pidiendo top {} por popularidad", count);
        List<AniListMedia> medias = client.fetchPopular(count);
        log.info("AniList devolvió {} anime", medias.size());

        int ok = 0;
        int failed = 0;
        for (AniListMedia media : medias) {
            if (media.id() == null) {
                failed++;
                continue;
            }
            try {
                saveOrUpdate(media);
                ok++;
            } catch (Exception e) {
                log.error("Error procesando anilistId={}: {}", media.id(), e.getMessage());
                failed++;
            }
        }
        log.info("Sync completado: {} guardados, {} fallos", ok, failed);
        return ok;
    }

    private void saveOrUpdate(AniListMedia media) {
        Anime anime = repository.findByAnilistIdWithCharacters(media.id())
                .orElseGet(Anime::new);

        anime.setAnilistId(media.id());

        AniListTitle title = media.title();
        if (title != null) {
            anime.setTitleRomaji(title.romaji());
            anime.setTitleEnglish(title.english());
        }

        anime.setSlug(buildSlug(media));
        anime.setDescription(media.description());
        anime.setFormat(media.format());
        anime.setStatus(media.status());
        anime.setEpisodes(media.episodes());
        anime.setEpisodeDuration(media.duration());
        anime.setStudio(mainStudioName(media.studios()));
        anime.setAverageScore(media.averageScore());
        anime.setPopularity(media.popularity());

        AniListFuzzyDate start = media.startDate();
        if (start != null) {
            anime.setStartYear(start.year());
            anime.setStartMonth(start.month());
            anime.setStartDay(start.day());
        }

        AniListFuzzyDate end = media.endDate();
        if (end != null) {
            anime.setEndYear(end.year());
            anime.setEndMonth(end.month());
            anime.setEndDay(end.day());
        }

        AniListCoverImage cover = media.coverImage();
        if (cover != null) {
            anime.setCoverImage(cover.large());
        }
        anime.setBannerImage(media.bannerImage());

        anime.setSeason(media.season());
        anime.setSeasonYear(media.seasonYear());

        // Reasignamos el Set entero para que Hibernate borre los géneros
        // viejos y meta los nuevos en la tabla anime_genre.
        if (media.genres() != null) {
            anime.setGenres(new HashSet<>(media.genres()));
        } else {
            anime.setGenres(new HashSet<>());
        }

        anime.setStudios(mapStudios(media.studios()));

        if (media.tags() != null) {
            HashSet<AnimeTag> tags = new HashSet<>();
            for (AniListTag tag : media.tags()) {
                if (tag.name() != null && !tag.name().isBlank()) {
                    tags.add(new AnimeTag(tag.name(), tag.rank()));
                }
            }
            anime.setTags(tags);
        } else {
            anime.setTags(new HashSet<>());
        }

        anime.setSyncedAt(Instant.now());
        anime.replaceCharacterRoles(mapCharacters(media.characters()));

        repository.save(anime);
    }

    private Set<Studio> mapStudios(AniListStudioConnection connection) {
        if (connection == null || connection.nodes() == null) {
            return new HashSet<>();
        }

        Set<Studio> studios = new HashSet<>();
        for (AniListStudio node : connection.nodes()) {
            if (node == null || node.id() == null || isBlank(node.name())) {
                continue;
            }

            Studio studio = studioRepository.findByAnilistId(node.id())
                    .orElseGet(Studio::new);
            studio.setAnilistId(node.id());
            studio.setName(node.name());
            studio.setSlug(buildStudioSlug(node));
            studio.setAnimationStudio(Boolean.TRUE.equals(node.isAnimationStudio()));
            studios.add(studioRepository.save(studio));
        }
        return studios;
    }

    private String buildStudioSlug(AniListStudio studio) {
        String slug = Studio.slugify(studio.name());
        if (slug.isEmpty()) {
            slug = "studio-" + studio.id();
        }

        Optional<Studio> existing = studioRepository.findBySlug(slug);
        if (existing.isPresent() && !Objects.equals(existing.get().getAnilistId(), studio.id())) {
            slug = slug + "-" + studio.id();
        }
        return slug;
    }

    private List<AnimeCharacterRole> mapCharacters(AniListCharacterConnection connection) {
        if (connection == null || connection.edges() == null) {
            return List.of();
        }

        return connection.edges().stream()
                .limit(6)
                .map(this::mapCharacterRole)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<AnimeCharacterRole> mapCharacterRole(AniListCharacterEdge edge) {
        if (edge == null || edge.node() == null || edge.node().id() == null) {
            return Optional.empty();
        }

        AniListCharacter node = edge.node();
        AnimeCharacter character = characterRepository.findByAnilistId(node.id())
                .orElseGet(AnimeCharacter::new);
        character.setAnilistId(node.id());
        character.setName(characterName(node));
        character.setImage(characterImage(node.image()));

        AnimeCharacterRole role = new AnimeCharacterRole();
        role.setCharacter(characterRepository.save(character));
        role.setRole(isBlank(edge.role()) ? "MAIN" : edge.role());
        return Optional.of(role);
    }

    private String characterName(AniListCharacter character) {
        AniListCharacterName name = character.name();
        if (name == null) {
            return "Personaje " + character.id();
        }
        if (!isBlank(name.full())) {
            return name.full();
        }
        if (!isBlank(name.nativeName())) {
            return name.nativeName();
        }
        return "Personaje " + character.id();
    }

    private String characterImage(AniListCharacterImage image) {
        if (image == null) {
            return null;
        }
        return !isBlank(image.large()) ? image.large() : image.medium();
    }

    private String buildSlug(AniListMedia media) {
        String base = null;
        if (media.title() != null) {
            base = media.title().english();
            if (isBlank(base)) {
                base = media.title().romaji();
            }
        }
        if (isBlank(base)) {
            base = "anime-" + media.id();
        }

        String slug = normalize(base);
        if (slug.isEmpty()) {
            slug = "anime-" + media.id();
        }

        // Dedupe: si el slug existe ya en otro anime distinto, sufija con anilistId.
        // Objects.equals porque getAnilistId() puede ser null (NPE-safe, igual
        // que buildStudioSlug más arriba).
        Optional<Anime> existing = repository.findBySlug(slug);
        if (existing.isPresent() && !Objects.equals(existing.get().getAnilistId(), media.id())) {
            slug = slug + "-" + media.id();
        }
        return slug;
    }

    private static String normalize(String input) {
        String n = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        if (n.startsWith("-")) n = n.substring(1);
        if (n.endsWith("-")) n = n.substring(0, n.length() - 1);
        return n;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String mainStudioName(AniListStudioConnection studios) {
        if (studios == null || studios.nodes() == null) {
            return null;
        }

        return studios.nodes().stream()
                .map(AniListStudio::name)
                .filter(name -> !isBlank(name))
                .findFirst()
                .orElse(null);
    }
}
