package com.chaos.chatplus.neoforge;
import com.chaos.chatplus.platform.Platform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Path;
import java.util.Optional;
public final class NeoEnvironment implements Platform.Environment {
    public Path configDir() { return FMLPaths.CONFIGDIR.get(); }
    public boolean loaded(String id) { return ModList.get().isLoaded(id); }
    public Optional<Path> resource(String id, String path) {
        var file = ModList.get().getModFileById(id);
        return file == null ? Optional.empty() : file.getFile().getContents().findFile(path).map(Path::of);
    }
    public String version() { return ModList.get().getModContainerById("chaoschatplus").orElseThrow().getModInfo().getVersion().toString(); }
}
