package com.dondeanime.backend.curated;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(
        name = "curated_list_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_curated_list_item_anime",
                        columnNames = {"curated_list_id", "anime_slug"}),
                @UniqueConstraint(
                        name = "uk_curated_list_item_position",
                        columnNames = {"curated_list_id", "position"})
        },
        indexes = {
                @Index(name = "idx_curated_list_item_list_position", columnList = "curated_list_id,position"),
                @Index(name = "idx_curated_list_item_anime_slug", columnList = "anime_slug")
        })
public class CuratedListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curated_list_id", nullable = false)
    private CuratedList curatedList;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
    @Size(max = 180)
    @Column(name = "anime_slug", nullable = false, length = 180)
    private String animeSlug;

    @Min(1)
    @Column(nullable = false)
    private int position;

    @Size(max = 600)
    @Column(columnDefinition = "TEXT")
    private String note;

    public CuratedListItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CuratedList getCuratedList() {
        return curatedList;
    }

    public void setCuratedList(CuratedList curatedList) {
        this.curatedList = curatedList;
    }

    public String getAnimeSlug() {
        return animeSlug;
    }

    public void setAnimeSlug(String animeSlug) {
        this.animeSlug = animeSlug;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
