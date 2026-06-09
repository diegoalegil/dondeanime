package com.dondeanime.backend.anime;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AnimeTag {

    @Column(name = "tag_name", length = 120, nullable = false)
    private String name;

    @Column(name = "rank")
    private Integer rank;

    public AnimeTag() {
    }

    public AnimeTag(String name, Integer rank) {
        this.name = name;
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public Integer getRank() {
        return rank;
    }

    // rank entra en equals/hashCode: Hibernate compara los elementos del
    // @ElementCollection con equals al hacer dirty-check, y sin rank un cambio
    // de rank en AniList nunca se persistía en re-syncs.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnimeTag animeTag)) return false;
        return Objects.equals(name, animeTag.name) && Objects.equals(rank, animeTag.rank);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rank);
    }
}
