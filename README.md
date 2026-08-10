# skript-minestom
**skript-minestom** is a server implementation using Minestom, where users write exactly what they want the server to do
using Skript syntax. Skript is baked within the server jar, and it supports some existing Skript addons (skript-reflect 2.6.3 and oopsk 1.0-beta2),
but most addons are written specifically for skript-minestom.

This GitHub fork of Skript is based on SkriptLang's fork of Mirreski's improvements of Skript which was built on Njol's original Skript.
The Skript version this was originally based on was 2.9, so there may still be remnants of that, but lots of internal systems have been
upgraded to match more recent Skript versions.

For those wanting to use Skript for other platforms, the `common` module has been modified, but left in a state where you should
just be able to fork and use that as a base for your Skript implementation elsewhere!

> [!WARNING]
> skript-minestom is **not** intended for beginner Skript users due to the complexity of Minestom.

## Why Minestom?
You can view the advantages and disadvantages Minestom has [here](https://github.com/skript-minestom/skript-minestom/wiki).

Skript offers an unmatched development velocity for those who know it already. Combine that with the performance benefits
Minestom offers due to its inherent lack of vanilla features, and we've found it's a recipe for success (depending on what
kind of server you're trying to make). With that, it's still important to keep in mind the performance drawbacks that an interpreted
language like Skript has during use.

It's not for everyone, but it's up for you to decide if it fits your use-case and workflow desires.

> [!IMPORTANT]
> skript-minestom is in **Alpha** and is bound to have lots of changes and new features added over time. With this, it's important
> to be cautious when choosing to use it for a production server.

## Requirements
skript-minestom requires a Java 25 runtime environment in order for it to work properly.

## Download
You can find the downloads for each version with their release notes in the [releases page](https://github.com/skript-minestom/skript-minestom/releases).

## Documentation
Documentation is available [here](https://smdocs.hapily.me/) for the latest version of skript-minestom and addons.

## Getting Started
First see [the wiki](https://github.com/skript-minestom/skript-minestom/wiki) and [documentation with examples](https://smdocs.hapily.me/),
but if that doesn't solve your problem or you have further questions you can join and ask for help in [the discord](https://discord.gg/NAzscWaFRg)!

## Addons
We don't offer support for addons on this repository. If there is an issue with an addon, take it to that addon's repository.
If you are an addon developer and need help developing an addon, you can take a look at the [in-house addons in this organization](https://github.com/orgs/skript-minestom/repositories)
or ask for help in [the discord](https://discord.gg/NAzscWaFRg).

## Official Tool Roadmap
- [x] **[ADDON]** [skript-reflect (non-fork) 2.6.3 support](https://github.com/SkriptLang/skript-reflect/releases/tag/v2.6.3)
- [x] **[ADDON]** [oopsk (non-fork) 1.0-beta2 support](https://github.com/sovdeeth/oopsk/releases/tag/1.0-beta2)
- [x] **[ADDON]** [skript-bdengine](https://github.com/skript-minestom/skript-bdengine)
- [x] **[ADDON]** [skript-gui-minestom](https://github.com/skript-minestom/skript-gui-minestom)
- [x] **[ADDON]** [SkriptHubDocsTool (fork for skript-minestom)](https://github.com/skript-minestom/SkriptHubDocsTool)
- [x] **[ADDON]** [SKNoise (fork for skript-minestom)](https://github.com/skript-minestom/SKNoise)
- [x] **[ADDON]** [SkCheese-minestom](https://github.com/skript-minestom/SkCheese-minestom)
- [ ] **[ADDON]** skript-blocks (vanilla placement rules, fluid/farming crop mechanics, etc.)
- [ ] **[ADDON]** skript-tebex (Tebex integration for skript-minestom)
- [ ] **[SNIPPET]** Minimal Discord bot integration through skript-reflect

## Contributing
We are open to pull requests! There are a lot of features we still want to add to skript-minestom, so if there's anything
you see is missing and want to add it, or improve existing features, feel free to [open a pull request](https://github.com/skript-minestom/skript-minestom/compare)! 
If you use AI in your PR, please state exactly how it was used.

If you find a bug or want a feature in skript-minestom, feel free to make [an issue](https://github.com/skript-minestom/skript-minestom/issues/new/choose).
If it's a bug your reporting, make sure you post a minimal code sample to reproduce the bug.

## Maven Repository
skript-minestom is available to import into your java project [here](https://maven.hapily.me/#/snapshots/com/github/hapily04/skript-minestom).

## Developers
Special credits to [lilrosalyn](https://github.com/lilrosalyn) for the original fork [here](https://github.com/lilrosalyn/CommonSkript)

You can find all contributors [here](https://github.com/skript-minestom/skript-minestom/graphs/contributors) and [here for SkriptLang/Skript contributors](https://github.com/SkriptLang/Skript/graphs/contributors).

All code is owned by its writer, licensed for others under GPLv3 (see LICENSE)
unless otherwise specified.
