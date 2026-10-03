package dev.kaenguruu.nomodleftbehind.configuration;

import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ConfigurationValidator {
    private static final Pattern IPV4_LITERAL = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Set<String> BUILT_IN_ALLOWED_URL_HOSTS = Set.of(
        "modrinth.com",
        "curseforge.com",
        "forgecdn.net",
        "github.com",
        "githubusercontent.com"
    );
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

        var allowedUrlHosts = getAllowedUrlHosts(configuration.trustedDomains(), issues);
        validateModList("clientMods", configuration.clientMods(), issues, allowedUrlHosts);
        validateModList("serverMods", configuration.serverMods(), issues, allowedUrlHosts);

        return List.copyOf(issues);
    }

    private static void validateModList(
        String listPath,
        List<DownloadableModConfiguration> mods,
        List<ConfigurationIssue> issues,
        Set<String> allowedUrlHosts
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
            validateName(modPath + ".name", mod.name(), issues);
            validateUrl(urlPath, mod.url(), issues, allowedUrlHosts);
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

    private static void validateName(
        String path,
        String value,
        List<ConfigurationIssue> issues
    ) {
        if (value == null || value.isBlank()) {
            issues.add(new ConfigurationIssue(
                path,
                ConfigurationError.MISSING_NAME,
                "Name must be provided."
            ));
        }
    }

    private static void validateUrl(
        String path,
        String value,
        List<ConfigurationIssue> issues,
        Set<String> allowedUrlHosts
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
            var isIpLiteral = isHostPresent && isIpLiteral(host);

            if (!isHttps || !isHostPresent || isIpLiteral) {
                issues.add(new ConfigurationIssue(
                    path,
                    ConfigurationError.INVALID_URL,
                    "URL must use HTTPS and contain a DNS host name, not an IP address."
                ));
            } else if (!isAllowedHost(host, allowedUrlHosts)) {
                issues.add(new ConfigurationIssue(
                    path,
                    ConfigurationError.INVALID_URL,
                    "URL host is not an approved mod source."
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

    private static Set<String> getAllowedUrlHosts(
        List<String> trustedDomains,
        List<ConfigurationIssue> issues
    ) {
        var allowedUrlHosts = new HashSet<>(BUILT_IN_ALLOWED_URL_HOSTS);
        if (trustedDomains == null) {
            return Set.copyOf(allowedUrlHosts);
        }

        for (var index = 0; index < trustedDomains.size(); index++) {
            var trustedDomain = trustedDomains.get(index);
            if (!isValidTrustedDomain(trustedDomain)) {
                issues.add(new ConfigurationIssue(
                    "trustedDomains[" + index + "]",
                    ConfigurationError.INVALID_TRUSTED_DOMAIN,
                    "Trusted domain must be a domain name without a scheme, path, port, or wildcard."
                ));
                continue;
            }

            allowedUrlHosts.add(trustedDomain.trim().toLowerCase(Locale.ROOT));
        }

        return Set.copyOf(allowedUrlHosts);
    }

    private static boolean isValidTrustedDomain(String trustedDomain) {
        if (trustedDomain == null || trustedDomain.isBlank()) {
            return false;
        }

        var normalizedDomain = trustedDomain.trim().toLowerCase(Locale.ROOT);
        if (normalizedDomain.length() > 253
            || normalizedDomain.indexOf('.') < 0
            || isIpLiteral(normalizedDomain)) {
            return false;
        }

        var labelStart = 0;
        for (var index = 0; index <= normalizedDomain.length(); index++) {
            if (index < normalizedDomain.length() && normalizedDomain.charAt(index) != '.') {
                continue;
            }

            if (!isValidDomainLabel(normalizedDomain, labelStart, index)) {
                return false;
            }
            labelStart = index + 1;
        }

        return true;
    }

    private static boolean isValidDomainLabel(String domain, int start, int end) {
        var length = end - start;
        if (length < 1 || length > 63
            || isNotAsciiLetterOrDigit(domain.charAt(start))
            || isNotAsciiLetterOrDigit(domain.charAt(end - 1))) {
            return false;
        }

        for (var index = start + 1; index < end - 1; index++) {
            var character = domain.charAt(index);
            if (isNotAsciiLetterOrDigit(character) && character != '-') {
                return false;
            }
        }

        return true;
    }

    private static boolean isNotAsciiLetterOrDigit(char character) {
        return (character < 'a' || character > 'z')
            && (character < '0' || character > '9');
    }

    private static boolean isIpLiteral(String host) {
        return host.contains(":") || IPV4_LITERAL.matcher(host).matches();
    }

    private static boolean isAllowedHost(String host, Set<String> allowedUrlHosts) {
        var normalizedHost = host.toLowerCase(Locale.ROOT);
        return allowedUrlHosts.stream().anyMatch(allowedHost ->
            normalizedHost.equals(allowedHost) || normalizedHost.endsWith("." + allowedHost)
        );
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
