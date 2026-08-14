# Third-party components

Jabe embeds the following GPL-3.0 components as a nested Fabric runtime:

- ViaFabricPlus 4.5.5, https://github.com/ViaVersion/ViaFabricPlus
- ViaBedrock update/1.26.40 base at commit c72fd6fd, with Jabe's protocol-2168
  patches at commit 53cfeb96, https://github.com/nisesimadao/Jabe-ViaBedrock

ViaBedrock is built from the vendored source tree. Its output replaces the
older ViaBedrock library inside the ViaFabricPlus 4.5.5 runtime. The original
copyright notices remain in the embedded archives.

Jabe is distributed under GPL-3.0-or-later to preserve the applicable terms.
