# UniGUI
UniGUI is a UI framework built on top of my library for the Unigine engine. It is designed not to be directly tied to 
the Minecraft renderer, making it portable without requiring a complete core rewrite. <br>

The main goal of this library is to deliver maximum UI performance and follow the "write once, run anywhere" principle. <br> <br>
We use a dedicated DrawCommands system that processes tasks and allows them to be grouped into batch buckets for maximum performance. 
Since the library is fully integrated into Minecraft, there should be no context issues.

## Stonecutter builds

The project is wired for Minecraft 1.20.1 and 1.21.1 through Stonecutter. Current platform modules are `common`,
`fabric`, `forge`, and the Fabric `TestMod`; NeoForge is enabled for 1.21.1.

Useful commands:

```powershell
.\gradlew.bat :1.20.1:fabric:build
.\gradlew.bat :1.20.1:forge:build
.\gradlew.bat :1.21.1:fabric:build
.\gradlew.bat :1.21.1:forge:build
.\gradlew.bat :1.21.1:neoforge:build
```

To verify everything currently supported:

```powershell
.\gradlew.bat :1.20.1:common:build :1.20.1:fabric:build :1.20.1:forge:build :1.20.1:TestMod:build :1.21.1:common:build :1.21.1:fabric:build :1.21.1:forge:build :1.21.1:neoforge:build :1.21.1:TestMod:build --continue
```
