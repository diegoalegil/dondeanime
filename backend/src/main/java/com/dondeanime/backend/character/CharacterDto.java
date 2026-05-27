package com.dondeanime.backend.character;

public record CharacterDto(
        Long anilistId,
        String name,
        String image,
        String role
) {
    public static CharacterDto from(AnimeCharacterRole appearance) {
        AnimeCharacter character = appearance.getCharacter();
        return new CharacterDto(
                character.getAnilistId(),
                character.getName(),
                character.getImage(),
                appearance.getRole());
    }
}
