package com.dondeanime.backend.affiliate;

import java.time.LocalDate;

public record AffiliateDailyClicksDto(
        LocalDate date,
        Long clicks
) {}
