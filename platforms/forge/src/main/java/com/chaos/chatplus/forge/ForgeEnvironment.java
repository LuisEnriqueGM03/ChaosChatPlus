package com.chaos.chatplus.forge;
import com.chaos.chatplus.platform.Platform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;
import java.util.Optional;
public final class ForgeEnvironment implements Platform.Environment {
    public Path configDir() { return FMLPaths.CONFIGDIR.get(); }
    public boolean loaded(String id) { return ModList.isLoaded(id); }
    public Optional<Path> resource(String id, String path) {
        var file = ModList.getModFileById(id);
        return file == null ? Optional.empty() : Optional.of(file.getFile().findResource(path));
    }
    public String version() { return ModList.getModContainerById("chaoschatplus").orElseThrow().getModInfo().getVersion().toString(); }
}
