package com.isums.common.i18n;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers {@link TranslationLanguageResolver} automatically for any service
 * that depends on proto-common. Services can supply their own bean to override.
 */
@AutoConfiguration
public class TranslationI18nAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TranslationLanguageResolver translationLanguageResolver() {
        return new TranslationLanguageResolver();
    }
}
