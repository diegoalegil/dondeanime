package com.dondeanime.backend.config;

import com.dondeanime.animetitlematcher.api.AnimeTitleMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone el {@link AnimeTitleMatcher} como bean. Es inmutable y thread-safe,
 * así que un único singleton con la configuración por defecto basta.
 */
@Configuration
public class MatchingConfig {

    @Bean
    public AnimeTitleMatcher animeTitleMatcher() {
        return AnimeTitleMatcher.createDefault();
    }
}
