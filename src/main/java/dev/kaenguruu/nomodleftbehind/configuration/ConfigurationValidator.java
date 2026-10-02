package dev.kaenguruu.nomodleftbehind.configuration;

import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ConfigurationValidator {
    private static final Pattern IPV4_LITERAL = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

    private ConfigurationValidator() {
    }

    private static final Map<String, Pattern> CACHED_REGEX_PATTERNS = new HashMap<>();

    public static Map<String, Pattern> getCachedRegexPatterns() {
        return Collections.unmodifiableMap(CACHED_REGEX_PATTERNS);
    }

    public static List<ConfigurationIssue> validate(ConfigurationJsonRoot configuration) {
        var issues = new ArrayList<ConfigurationIssue>();

        if (configuration == null) {
            issues.add(new ConfigurationIssue(
                "configuration",
                ConfigurationError.NULL_CONFIGURATION,
                "Configuration is null."
            ));
            return List.copyOf(issues);
        }

        validateModList("clientMods", configuration.clientMods(), issues);
        validateModList("serverMods", configuration.serverMods(), issues);

        return List.copyOf(issues);
    }

    private static void validateModList(
        String listPath,
        List<DownloadableModConfiguration> mods,
        List<ConfigurationIssue> issues
    ) {
        if (mods == null) {
            issues.add(new ConfigurationIssue(
                listPath,
                ConfigurationError.NULL_MOD_LIST,
                "Mod list is missing."
            ));
            return;
        }

        Set<String> configuredUrls = new HashSet<>();
        for (var index = 0; index < mods.size(); index++) {
            var mod = mods.get(index);
            var modPath = listPath + "[" + index + "]";

            if (mod == null) {
                issues.add(new ConfigurationIssue(
                    modPath,
                    ConfigurationError.NULL_MOD_ENTRY,
                    "Mod configuration is null."
                ));
                continue;
            }

            var urlPath = modPath + ".url";
            validateUrl(urlPath, mod.url(), issues);
            if (mod.url() != null && !mod.url().isBlank() && !configuredUrls.add(mod.url())) {
                issues.add(new ConfigurationIssue(
                    urlPath,
                    ConfigurationError.DUPLICATE_URL,
                    "URL is already configured in this mod list."
                ));
            }
            validateFilePattern(modPath + ".filePattern", mod.filePattern(), issues);
        }
    }

    private static void validateUrl(
        String path,
        String value,
        List<ConfigurationIssue> issues
    ) {
        if (value == null || value.isBlank()) {
            issues.add(new ConfigurationIssue(
                path,
                ConfigurationError.MISSING_URL,
                "URL must be provided."
            ));
            return;
        }

        try {
            var uri = URI.create(value);
            var host = uri.getHost();
            var isHttps = "https".equalsIgnoreCase(uri.getScheme());
            var isHostPresent = host != null && !host.isBlank();
            var isIpLiteral = isHostPresent && (host.contains(":") || IPV4_LITERAL.matcher(host).matches());

            if (!isHttps || !isHostPresent || isIpLiteral) {
                issues.add(new ConfigurationIssue(
                    path,
                    ConfigurationError.INVALID_URL,
                    "URL must use HTTPS and contain a DNS host name, not an IP address."
                ));
            }
        } catch (IllegalArgumentException exception) {
            issues.add(new ConfigurationIssue(
                path,
                ConfigurationError.INVALID_URL,
                "URL is not valid URI syntax."
            ));
        }
    }

    private static void validateFilePattern(
        String path,
        String value,
        List<ConfigurationIssue> issues
    ) {
        if (value == null) {
            issues.add(new ConfigurationIssue(
                path,
                ConfigurationError.MISSING_FILE_PATTERN,
                "File pattern must be provided."
            ));
            return;
        }

        try {
            var pattern = Pattern.compile(value);
            CACHED_REGEX_PATTERNS.put(value, pattern);
        } catch (PatternSyntaxException exception) {
            issues.add(new ConfigurationIssue(
                path,
                ConfigurationError.INVALID_REGEX,
                exception.getDescription()
            ));
        }
    }
}
