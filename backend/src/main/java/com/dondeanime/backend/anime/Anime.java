package com.dondeanime.backend.anime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "anime")
public class Anime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anilist_id", unique = true, nullable = false)
    private Long anilistId;

    @Column(name = "title_romaji")
    private String titleRomaji;

    @Column(name = "title_english")
    private String titleEnglish;

    @Column(unique = true)
    private String slug;

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

}
