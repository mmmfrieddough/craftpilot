<p align="center">
  <img src="src/main/resources/assets/craftpilot/icon.png" alt="Craftpilot Logo" width="200">
</p>

# Craftpilot

[![Release](https://img.shields.io/github/v/release/mmmfrieddough/craftpilot?include_prereleases)](https://github.com/mmmfrieddough/craftpilot/releases)
[![Build Status](https://github.com/mmmfrieddough/craftpilot/actions/workflows/release.yml/badge.svg)](https://github.com/mmmfrieddough/craftpilot/actions)
[![Modrinth](https://img.shields.io/modrinth/dt/craftpilot?logo=modrinth)](https://modrinth.com/mod/craftpilot)
[![CurseForge](https://img.shields.io/curseforge/dt/craftpilot?logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/craftpilot)
[![License](https://img.shields.io/github/license/mmmfrieddough/craftpilot)](LICENSE)

Craftpilot is your personal building assistant in Minecraft. As you build, it provides relevant suggestions through an easy-to-use overlay. Whether you're seeking inspiration for new structures or struggling with complex details, Craftpilot helps bring your creative vision to life. The assistant supports a wide variety of building styles and block palettes, making it a versatile tool for any Minecraft builder.

<p align="center">
  <img src="showcase1.gif" alt="Craftpilot Main Demo" width="600">
</p>

## Setup

Craftpilot requires a companion program that runs the ML model. Here's how to get started:

### Quick Start (Recommended)

1. Download the latest release from the [releases page](https://github.com/mmmfrieddough/minecraft-schematic-generator/releases)
2. Run the companion program
   > ⚠️ **Note**: The ML model requires a modern GPU (NVIDIA recommended) for optimal performance. Without one, generation times may be significantly slower.
3. Start Minecraft - Craftpilot will automatically connect to the local instance

### Alternative Setup Options

- **For Developers**: You can run the companion program directly from source code. Clone the [repo](https://github.com/mmmfrieddough/minecraft-schematic-generator), install Python dependencies, and run it locally. This is useful for customization or troubleshooting.
- **Advanced Configuration**: The companion program can run on a remote server. Configure the connection URL in the mod settings to point to your desired server address.

## Usage

1. **Start Building**

   - Place any block to begin a new build or continue an existing one
   - Craftpilot will display suggested blocks as a transparent overlay

<p align="center">
  <img src="showcase2.gif" alt="Craftpilot Main Demo" width="600">
</p>

2. **Working with Suggestions**

   - Accept suggestions by placing blocks where they're shown
   - As you build, new suggestions will automatically appear
   - Use the pick block function (middle mouse) on suggestions to quickly select blocks
   - Press C to clear all current suggestions (key configurable in controls)
   - Remove individual suggestions by left-clicking (punching) them

3. **Building Flexibility**
   - Feel free to deviate from suggestions - the system will adapt
   - A grace period allows for placing a few different blocks before regenerating suggestions
   - This helps accommodate support blocks and minor adjustments

## Customization

Settings can be edited in the config file "craftpilot.json" or on the configuration screen accessed through [Mod Menu](https://github.com/TerraformersMC/ModMenu). Available settings include visual options (like overlay appearance), building behavior, and ML model parameters.
