package com.dondeanime.backend.trakt;

public record TraktUserSettingsResponse(TraktUser user) {

    String externalUserId() {
        if (user == null) {
            return null;
        }
        if (user.ids() != null && user.ids().slug() != null && !user.ids().slug().isBlank()) {
            return user.ids().slug().trim();
        }
        if (user.username() != null && !user.username().isBlank()) {
            return user.username().trim();
        }
        return null;
    }
}
