# TFC Icy’s Better Horses

TerraFirmaCraft integration for Icy’s Better Horses, developed for Gregnautics.

**Minecraft 1.21.1 · NeoForge · Version 0.1.6**

[Русское описание и установка](README-RU.md) · [Validation notes](RESULTS.md)

## Features

- TFC familiarity, feeding, calendar-based aging, pregnancy and breeding.
- Climate-based natural spawning for 15 horse breeds.
- Manual gaits and persistent, server-controlled stamina.
- Compatibility work for horse equipment, carts and Gregnautics.
- Compact stamina HUD with ten segments above the TFC hunger display.

## Downloads and installation

Download the JAR and optional pack files from [Downloads — 0.1.6](downloads/0.1.6).
Install it in `mods` on **both the server and every client**. Remove previous versions
of this addon before updating. Keep the original TFC and Icy’s Better Horses mods.

The optional Gregnautics ZIP contains this addon and pack-specific configuration
and KubeJS scripts. Review those files before merging them into another modpack.
Third-party dependency JARs are not included.

Tested dependency versions: TerraFirmaCraft 4.2.13 and Icy’s Better Horses 2.1.8,
with their required dependencies. See `build.gradle` for the development classpath.

## Building

Use JDK 21 and the included Gradle wrapper. Supply your installed pack’s `mods`
directory explicitly, because the original development default is machine-specific:

```sh
./gradlew build -PpackMods="/absolute/path/to/Gregnautics/mods"
```

On Windows use `gradlew.bat` instead of `./gradlew`.
The resulting JAR is written to `build/libs`.

The overlay and QA helper scripts retain the original development setup and paths.
Review and adjust those paths before using the helpers. `prepare_pack_overlay.py
--apply` changes pack files and creates backups. It is not required to build the JAR.

## Release 0.1.6

This release refines the stamina HUD: ten vanilla-style segments, adaptive positioning
above TFC hunger rows, low-stamina colours, and hiding with F1 or open menus.
See `RESULTS.md` for the existing validation record and its limitations.

This is an independent compatibility addon, not an official release of TFC or
Icy’s Better Horses. The addon metadata declares the MIT license; dependencies
remain subject to their respective licenses.
