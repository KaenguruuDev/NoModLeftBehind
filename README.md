# No Mod Left Behind

No Mod Left Behind helps Minecraft modpacks identify missing externally hosted mod files before startup continues. It
compares the files in the instance `mods` directory with the filename patterns in its configuration. On the client, a
startup window shows missing mods and provides links that open in the default browser. On a dedicated server, missing
required mods stop startup and missing optional mods are reported as warnings.

No Mod Left Behind does not download or install the files automatically. The configured URL is used as the source page
or download link that the player or server administrator can open.

## Download

Download No Mod Left Behind from [Modrinth](https://modrinth.com/mod/no-mod-left-behind). It supports Minecraft
versions 1.21 through 1.21.8 with NeoForge.

## Configuration file

The configuration file is created automatically at `config/nomodleftbehind/nomodleftbehind.json` relative to the
Minecraft instance directory. An empty file contains three empty lists:

```json
{
  "clientMods": [],
  "serverMods": [],
  "trustedDomains": []
}
```

`clientMods` contains mod definitions needed by the client. `serverMods` contains mod definitions needed by a dedicated
server. A definition may appear in both lists when the same mod is required in both environments. `trustedDomains` can
contain additional website domains that the modpack author trusts. Each configured domain also covers its subdomains.

Each entry in either list has a `name`, a `url`, a `filePattern`, and an `isOptional` value. The `name` identifies the
mod in the startup window and server log. The `url` should be a secure HTTPS web link to the mod. It must use
`modrinth.com`, `curseforge.com`, `forgecdn.net`, `github.com`, or `githubusercontent.com`, including their subdomains.
Links that use an IP address instead of a website name are not accepted. The validator does not contact the URL or
follow redirects during startup. The `filePattern` is a Java regular expression matched against the
complete filename in the `mods` directory, so an expression such as `example-mod-[0-9]+\\.jar` can match versioned JAR
files. You can use
[regex101.com](https://regex101.com/?regex=%5Ecreate-1%5C.21%5C.1-6%5C.0%5C.10%5C.jar&testString=create-1.21.1-6.0.10.jar%0Anotactuallycreate-1.21.1-6.0.10.jar&flags=gm&flavor=java&delimiter=%22)
to test and refine patterns; select the Java flavor when
testing. The `isOptional` value controls whether a missing file is required for startup. It defaults to `false` when
omitted.

For example:

```json
{
  "clientMods": [
    {
      "name": "Example Client Mod",
      "url": "https://downloads.example.com/example-client-mod.jar",
      "filePattern": "example-client-mod-[0-9]+\\.jar",
      "isOptional": false
    },
    {
      "name": "Example Cosmetic Mod",
      "url": "https://downloads.example.com/example-cosmetic-mod.jar",
      "filePattern": "example-cosmetic-mod-[0-9]+\\.jar",
      "isOptional": true
    }
  ],
  "serverMods": [
    {
      "name": "Example Server Mod",
      "url": "https://downloads.example.com/example-server-mod.jar",
      "filePattern": "example-server-mod-[0-9]+\\.jar",
      "isOptional": false
    }
  ],
  "trustedDomains": [
    "example.com"
  ]
}
```

Both mod lists must be present, and every entry must provide a non-empty name, URL, and filename pattern. URLs must be
unique within each list. The same URL may be used once in `clientMods` and once in `serverMods`. The `trustedDomains`
list is optional, and each entry must be a bare domain name without a scheme, path, port, or wildcard.
