package com.dondeanime.backend.anime;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "anime")
public class Anime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anilist_id", unique = true, nullable = false)
    private Long anilistId;

    @Column(name = "tmdb_id")
    private Long tmdbId;

    @Column(name = "title_romaji")
    private String titleRomaji;

    @Column(name = "title_english")
    private String titleEnglish;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_es", columnDefinition = "TEXT")
    private String descriptionEs;

    @Column(length = 20)
    private String format; // TV, MOVIE, OVA, ONA, SPECIAL, MUSIC

    @Column(length = 30)
    private String status; // FINISHED, RELEASING, NOT_YET_RELEASED, CANCELLED, HIATUS

    private Integer episodes;

    @Column(name = "episode_duration")
    private Integer episodeDuration;

    @Column(length = 120)
    private String studio;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "start_month")
    private Integer startMonth;

    @Column(name = "start_day")
    private Integer startDay;

    @Column(name = "end_year")
    private Integer endYear;

    @Column(name = "end_month")
    private Integer endMonth;

    @Column(name = "end_day")
    private Integer endDay;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "banner_image", length = 500)
    private String bannerImage;

    @Column(name = "average_score")
    private Integer averageScore;

    private Integer popularity;

    @Column(name = "synced_at")
    private Instant syncedAt;

    /**
     * Géneros del anime ("Action", "Romance", "Slice of Life", ...).
     * Tabla aparte anime_genre con PK compuesto (anime_id, genre).
     * EAGER para que cuando devolvamos JSON estén ya cargados.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "anime_genre", joinColumns = @JoinColumn(name = "anime_id"))
    @Column(name = "genre", length = 50, nullable = false)
    private Set<String> genres = new HashSet<>();

    /** Temporada del estreno: WINTER, SPRING, SUMMER, FALL. */
    @Column(length = 10)
    private String season;

    @Column(name = "season_year")
    private Integer seasonYear;

    public Anime() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnilistId() {
        return anilistId;
    }

    public void setAnilistId(Long anilistId) {
        this.anilistId = anilistId;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Long tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getTitleRomaji() {
        return titleRomaji;
    }

    public void setTitleRomaji(String titleRomaji) {
        this.titleRomaji = titleRomaji;
    }

    public String getTitleEnglish() {
        return titleEnglish;
    }

    public void setTitleEnglish(String titleEnglish) {
        this.titleEnglish = titleEnglish;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionEs() {
        return descriptionEs;
    }

    public void setDescriptionEs(String descriptionEs) {
        this.descriptionEs = descriptionEs;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEpisodes() {
        return episodes;
    }

    public void setEpisodes(Integer episodes) {
        this.episodes = episodes;
    }

    public Integer getEpisodeDuration() {
        return episodeDuration;
    }

    public void setEpisodeDuration(Integer episodeDuration) {
        this.episodeDuration = episodeDuration;
    }

    public String getStudio() {
        return studio;
    }

    public void setStudio(String studio) {
        this.studio = studio;
    }

    public Integer getStartYear() {
        return startYear;
    }

    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    public Integer getStartMonth() {
        return startMonth;
    }

    public void setStartMonth(Integer startMonth) {
        this.startMonth = startMonth;
    }

    public Integer getStartDay() {
        return startDay;
    }

    public void setStartDay(Integer startDay) {
        this.startDay = startDay;
    }

    public Integer getEndYear() {
        return endYear;
    }

    public void setEndYear(Integer endYear) {
        this.endYear = endYear;
    }

    public Integer getEndMonth() {
        return endMonth;
    }

    public void setEndMonth(Integer endMonth) {
        this.endMonth = endMonth;
    }

    public Integer getEndDay() {
        return endDay;
    }

    public void setEndDay(Integer endDay) {
        this.endDay = endDay;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public void setBannerImage(String bannerImage) {
        this.bannerImage = bannerImage;
    }

    public Integer getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Integer averageScore) {
        this.averageScore = averageScore;
    }

    public Integer getPopularity() {
        return popularity;
    }

    public void setPopularity(Integer popularity) {
        this.popularity = popularity;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }

    public Set<String> getGenres() {
        return genres;
    }

    public void setGenres(Set<String> genres) {
        this.genres = genres;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public Integer getSeasonYear() {
        return seasonYear;
    }

    public void setSeasonYear(Integer seasonYear) {
        this.seasonYear = seasonYear;
    }

}
