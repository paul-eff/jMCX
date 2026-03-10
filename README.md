# jMCX

<div align="center">

![Version](https://img.shields.io/badge/version-1.1.0-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.11(Java)-green)
![Java](https://img.shields.io/badge/java-21-red)
![License](https://img.shields.io/badge/license-MIT-brightgreen)

*A modern Java library for reading, editing, and writing Minecraft Anvil region files*

</div>

## Features

**jMCX** provides a comprehensive solution for Minecraft world file manipulation with clean, efficient APIs:

- **Complete Anvil Region Format Support** - Read, edit, and write .mca region files
- **Fluent Builder API** - Modern builder pattern for easy structure creation
- **Smart Compression** - Automatic detection and support for GZIP, ZLIB, and uncompressed files

## Installation

You can either import the [latest relase](https://github.com/paul-eff/jMCX/releases/latest) `.jar` file directly as a dependency via your IDE.

Or use a build system of your choosing (e.g. Maven):
```xml
<dependencies>
    <dependency>
        <groupId>de.pauleff</groupId>
        <artifactId>jmcx</artifactId>
        <version>1.1.0</version>
    </dependency>
<dependencies>
```

## Quick Start

**Read region file:**
```java
AnvilReader reader = new AnvilReader();
Region region = reader.readRegion(new File("r.0.0.mca"));
List<Chunk> chunks = region.getChunks();
```

**Edit chunk blocks:**
```java
Optional<IChunk> chunkOpt = region.getChunk(0, 0);
if (chunkOpt.isPresent()) {
    IChunk chunk = chunkOpt.get();
    ICompoundTag nbt = chunk.getNBTData();
    IListTag sections = nbt.getList("sections");
    
    // Replace dirt with diamond in block palette
    for (int i = 0; i < sections.size(); i++) {
        ICompoundTag section = (ICompoundTag) sections.get(i);
        ICompoundTag blockStates = section.getCompound("block_states");
        IListTag palette = blockStates.getList("palette");
        
        for (int j = 0; j < palette.size(); j++) {
            ICompoundTag block = (ICompoundTag) palette.get(j);
            if ("minecraft:dirt".equals(block.getString("Name"))) {
                block.setString("Name", "minecraft:diamond_block");
            }
        }
    }
}
```

**Copy chunk data:**
```java
Optional<IChunk> targetChunk = region.getChunk(1, 1);
if (targetChunk.isPresent()) {
    targetChunk.get().setNBTData(chunk.getNBTData());
}
```

**Write modified region:**
```java
AnvilWriter writer = new AnvilWriter();
writer.writeRegion(region, new File("r.0.0-updated.mca"));
```

**Find chunks with entities:**
```java
List<Chunk> ownableChunks = region.getChunksWithOwnables();
for (Chunk chunk : ownableChunks) {
    System.out.println("Chunk at " + chunk.getChunkX() + ", " + chunk.getChunkZ());
}
```

## Status

### Supported
- Complete CRUD operations (Create, Read, Update, Delete)
- Compression formats: **GZIP**, **ZLIB**, **None**
- Chunk Management: Coordinate extraction, payload handling, ...
- Many convenience methods (chunkHasOwnableEntities, getChunkByCoordinates, etc.)

### Future Plans
- Enhanced editing operations
- LZ4 compression support
- Bedrock Region format support
- McRegion (Alpha) Level format support
- Graphical interface (maybe!)

## Documentation

Explore the `examples/` folder for comprehensive usage patterns.

## Building
```bash
mvn clean install
```

## Important Notice

**Always backup your world files before modification.** While thoroughly tested, data corruption is always possible with world file manipulation tools.

## Related Projects

This library will be used by:
- [MinecraftOfflineOnlineConverter](https://github.com/paul-eff/MinecraftOfflineOnlineConverter) - Player data migration with UUID conversion

Built with:
- [jNBT](https://github.com/paul-eff/jNBT) - NBT file manipulation

## References

- [Region File Format](https://minecraft.wiki/w/Region_file_format)
- [Chunk Format](https://minecraft.wiki/w/Chunk_format)

## Remark
Minecraft is a registered trademark of Mojang AB.

