package dev.kaenguruu.nomodleftbehind.configuration;

import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationValidatorTest {
    @Test
    void rejectsNullConfiguration() {
        var issues = ConfigurationValidator.validate(null);

        assertEquals(1, issues.size());
        assertIssue(issues.getFirst(), "configuration", ConfigurationError.NULL_CONFIGURATION);
    }

    @Test
    void rejectsMissingListsAndEntries() {
        var nullEntryList = new ArrayList<DownloadableModConfiguration>();
        nullEntryList.add(null);
        var configuration = new ConfigurationJsonRoot(null, nullEntryList);

        var issues = ConfigurationValidator.validate(configuration);

        assertEquals(2, issues.size());
        assertIssue(issues.get(0), "clientMods", ConfigurationError.NULL_MOD_LIST);
        assertIssue(issues.get(1), "serverMods[0]", ConfigurationError.NULL_MOD_ENTRY);
    }

    @Test
    void rejectsMissingRequiredModFields() {
        var mod = new DownloadableModConfiguration("", null, "  ", false);

        var issues = ConfigurationValidator.validate(new ConfigurationJsonRoot(List.of(mod), List.of()));

        assertEquals(3, issues.size());
        assertIssue(issues.get(0), "clientMods[0].name", ConfigurationError.MISSING_NAME);
        assertIssue(issues.get(1), "clientMods[0].url", ConfigurationError.MISSING_URL);
        assertIssue(issues.get(2), "clientMods[0].filePattern", ConfigurationError.MISSING_FILE_PATTERN);
    }

    @Test
    void rejectsInsecureMalformedAndIpLiteralUrls() {
        var mods = List.of(
            mod("http://example.com/mod.jar", "http"),
            mod("https://192.0.2.10/mod.jar", "ipv4"),
            mod("https://[2001:db8::1]/mod.jar", "ipv6"),
            mod("https:///mod.jar", "missing-host"),
            mod("not a uri", "malformed")
        );

        var issues = ConfigurationValidator.validate(new ConfigurationJsonRoot(mods, List.of()));

        assertEquals(5, issues.size());
        assertIssue(issues.get(0), "clientMods[0].url", ConfigurationError.INVALID_URL);
        assertIssue(issues.get(1), "clientMods[1].url", ConfigurationError.INVALID_URL);
        assertIssue(issues.get(2), "clientMods[2].url", ConfigurationError.INVALID_URL);
        assertIssue(issues.get(3), "clientMods[3].url", ConfigurationError.INVALID_URL);
        assertIssue(issues.get(4), "clientMods[4].url", ConfigurationError.INVALID_URL);
    }

    @Test
    void rejectsInvalidRegex() {
        var mod = mod("https://example.com/mod.jar", "[");

        var issues = ConfigurationValidator.validate(new ConfigurationJsonRoot(List.of(mod), List.of()));

        assertEquals(1, issues.size());
        assertIssue(issues.getFirst(), "clientMods[0].filePattern", ConfigurationError.INVALID_REGEX);
    }

    @Test
    void rejectsDuplicateUrlsWithinClientModList() {
        var first = mod("https://example.com/mod.jar", "first");
        var second = mod("https://example.com/mod.jar", "second");

        var issues = ConfigurationValidator.validate(new ConfigurationJsonRoot(List.of(first, second), List.of()));

        assertEquals(1, issues.size());
        assertIssue(issues.getFirst(), "clientMods[1].url", ConfigurationError.DUPLICATE_URL);
    }

    @Test
    void rejectsDuplicateUrlsWithinServerModList() {
        var first = mod("https://example.com/mod.jar", "first");
        var second = mod("https://example.com/mod.jar", "second");

        var issues = ConfigurationValidator.validate(new ConfigurationJsonRoot(List.of(), List.of(first, second)));

        assertEquals(1, issues.size());
        assertIssue(issues.getFirst(), "serverMods[1].url", ConfigurationError.DUPLICATE_URL);
    }

    @Test
    void allowsTheSameUrlInClientAndServerModLists() {
        var clientMod = mod("https://example.com/mod.jar", "client");
        var serverMod = mod("https://example.com/mod.jar", "server");

        var issues = ConfigurationValidator.validate(new ConfigurationJsonRoot(List.of(clientMod), List.of(serverMod)));

        assertTrue(issues.isEmpty());
    }

    @Test
    void acceptsValidConfigurationAndCachesRegexPatterns() {
        var mod = mod("https://downloads.example.com/mod.jar", "example-mod-[0-9]+\\.jar");

        var issues = ConfigurationValidator.validate(new ConfigurationJsonRoot(List.of(mod), List.of(mod)));

        assertTrue(issues.isEmpty());
        assertFalse(ConfigurationValidator.getCachedRegexPatterns().isEmpty());
        assertTrue(ConfigurationValidator.getCachedRegexPatterns().containsKey(mod.filePattern()));
    }

    private static DownloadableModConfiguration mod(String url, String filePattern) {
        return new DownloadableModConfiguration(url, filePattern, "Test Mod", false);
    }

    private static void assertIssue(ConfigurationIssue issue, String path, ConfigurationError error) {
        assertEquals(path, issue.path());
        assertEquals(error, issue.error());
    }
}
