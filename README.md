# Jabe

Jabe is a client-side Fabric mod that aims to make Minecraft Java Edition act as
a Bedrock-compatible client and LAN host only while Bedrock mode is active.

## Current milestone

The first working vertical slice targets **Minecraft Java Edition 26.1.2**:

- adds a **Bedrock LAN** button to Java's multiplayer screen;
- broadcasts RakNet unconnected pings on UDP port 19132;
- parses Bedrock unconnected pong advertisements;
- shows the world name, player count, game version, and address;
- connects a selected world through the embedded ViaFabricPlus/ViaBedrock runtime;
- keeps Java and Bedrock compatibility modes as explicit state;
- has been runtime-tested through discovery, login, spawn, terrain loading,
  movement/swimming, item sync, block breaking, and block placement against
  Windows BDS **1.26.43.1**.

The Jabe jar embeds a patched ViaFabricPlus **4.5.5** runtime and a vendored
ViaBedrock protocol-2168 build based on upstream's `update/1.26.40` branch. No
separate ViaFabricPlus installation is required. Jabe carries the v2168 packet
layout fixes needed by BDS 1.26.43.1, including resource-pack negotiation,
entity data, authenticated input, compression, and chunk/subchunk transport.
It also retains a small binary-compatibility alias for ViaFabricPlus 4.5.5
after the v2168 authenticated-input enum rename.

The Drop 3 of 2026 experimental palette is covered without missing-state
placeholders: Poplar variants preserve the corresponding Pale Oak state,
Straw Bed preserves facing/part/occupied state as a Yellow Bed, and dynamic
Wool shapes and vegetation use color-preserving vanilla fallbacks.

## Build

JDK 25 is required.

```powershell
.\gradlew.bat build
```

The distributable mod is `build/libs/jabe-0.1.0.jar`.

When the sibling `../MineAgent/build/libs/mineagent-0.2.0.jar` exists, Loom adds
it to development runs as a `runtimeOnly` dependency. It is not included in the
Jabe artifact. `.mcp.json` points Codex at MineAgent's local MCP bridge so the
running client UI can be inspected and driven without desktop automation.

`vendor/runtime/ViaFabricPlus-4.5.5.jar` and `vendor/ViaBedrock` are build
inputs. `prepareBundledBedrockRuntime` replaces ViaFabricPlus's older nested
ViaBedrock jar and nests the patched runtime in the Jabe artifact. See
`THIRD_PARTY_NOTICES.md` for sources and GPL licensing.

## Architecture

- `network/BedrockLanDiscovery`: isolated RakNet LAN discovery transport
- `network/BedrockLanWorld`: normalized Bedrock advertisement state
- `network/BedrockSessionConnector`: optional ViaFabricPlus/ViaBedrock adapter
- `compat/CompatibilityState`: explicit Java/Bedrock mode boundary
- `screen/BedrockLanScreen`: discovery and connection UI

The locally owned Google Play APK under `E:\Coding\MCBERecomp` may be used for
behavior comparison. No Minecraft binary, asset, authentication code, DRM
material, or extracted copyrighted content is copied into this repository.

## Next milestones

1. Improve item, model, texture, and collision-shape fidelity for experimental
   1.26.43 content that currently uses vanilla visual fallbacks.
2. Add a real Bedrock LAN host bridge for an integrated Java server; never
   advertise a fake LAN world before the RakNet/Bedrock server path is ready.
3. Verify against the owned Bedrock 1.26.40.5 Android build and a Nintendo
   Switch on the same LAN.
