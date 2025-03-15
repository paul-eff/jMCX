# jMCX v0.2
<p align="center">
  <img src="https://img.shields.io/badge/version-0.2-blue">
  <img src="https://img.shields.io/badge/minecraft-1.21.4 (Java)-green">
  <img src="https://img.shields.io/badge/java-21-red">
</p>
Currently there aren't any sophisticated libraries to interact with Minecraft Anvil (more to come) files in Java (read, edit and write). 
And those that are around haven't been updated in a while or are directly integrated to projects like mcaselector by Querz. 

With this library I want to provide an up to date and efficient (and also overengineered) way to interact with Anvil (more to come) files.

jMCX will in the near future be part of [MinecraftOfflineOnlineConverter](https://github.com/paul-eff/MinecraftOfflineOnlineConverter) to enable UUID conversion down to entity level.

### Supports
- Operations: Reading and writing (.mca from/to file)
- Compression types: LZIP, GZIP and NONE
### WIP
- Operations: Editing
- Compression types: LZ4
### Future
- Bedrock Region format
- GUI (BIG maybe!)

# Usage

- Obviously download the jar
- Add it as a dependency to your project (dependent on your IDE)
- Take a look into the `Main.java` file.

# Sources
- https://minecraft.wiki/w/Region_file_format
- Dependencies: [jNBT](https://github.com/paul-eff/jNBT)

# Disclaimer
Please always make a backup of your files before using this tool.
Whilst it was thoroughly tested, there is always the chance that a bug might occur!

If you need support for a specific version or a custom feature, please leave me a message or issue :)!

# Remark
Minecraft is a registered trademark of Mojang AB.

