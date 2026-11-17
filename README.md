**ATTENTION**: This mod is **no longer being developed nor maintained by McHorse**. If you wish to continue the mod, feel free to check out the [source code](https://github.com/mchorse/mappet).  

![Mappet](https://i.imgur.com/1pvpWDH.png)

[CurseForge page](https://www.curseforge.com/minecraft/mc-mods/mappet) – [GitHub](https://github.com/mchorse/mappet) – [Wiki](https://github.com/mchorse/mappet/wiki) 

**Mappet** is a Minecraft mod (for **1.12.2** exclusively) that is designed as a tool set for far superior adventure maps than possible with vanilla. It's also possible to use Mappet for scripted roleplays, and, to some extent, in Minecraft machinimas.

Special thanks to [Andruxioid](https://www.youtube.com/channel/UCnHOceBjwMyqCR5oYOoNqhQ) for the mod's name, Jvonlins, Centryfuga and [CustomNPCs Forever Vkontakte](https://vk.com/customnpcforever) group for testing, suggestions and directing!

## Install

Install [Minecraft Forge](http://files.minecraftforge.net/), download the latest stable version of jar file for available Minecraft version. Also install following mods: [McLib](https://www.curseforge.com/minecraft/mc-mods/mchorses-mclib), [Metamorph](https://www.curseforge.com/minecraft/mc-mods/metamorph), and [Blockbuster](https://www.curseforge.com/minecraft/mc-mods/blockbuster). Put it in minecraft's `mods` folder, and launch the game.

After that, Mappet mod should be installed and will appear in Minecraft's mods menu. If Mappet didn't appear in the mods menu, then something went wrong. 

## JDK setup for development

Use JDK 8 and set `JAVA_HOME` to the root of your JDK installation. Add its `bin` directory to `PATH` so the build also uses JDK 8's `javadoc`.

Windows (PowerShell)

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk8'
$env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
.\gradlew.bat --version
```

Linux and macOS

```sh
export JAVA_HOME=/path/to/jdk8
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --version
```

The `JVM` line should show Java `1.8`. To keep these settings across terminal sessions, save them in your user environment or shell profile.

## Resources

At the moment, the mod is still in development, however, here is a playlist of videos that cover some Mappet mod's features:

<a href="https://youtu.be/kJypWweCnKg?list=PLLnllO8nnzE__n6aLU8WUtfsGL1A62l0z"><img src="https://img.youtube.com/vi/kJypWweCnKg/0.jpg"></a> 

Alternatively, there is [an almost complete wiki](https://github.com/mchorse/mappet/wiki) describing all Mappet's mechanics. If you'll have questions about the mod, you can also join [Mappet's Discord server](https://discord.gg/QhvbgSWpYn).