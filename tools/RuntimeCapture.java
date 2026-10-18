import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.util.*;

/** Captures the final launcher command before loading the game; only used by the test tooling. */
public final class RuntimeCapture {
    public static void premain(String ignored, Instrumentation instrumentation) throws Exception {
        Path output = Path.of(System.getProperty("chaoschatplus.exportPath"));
        Files.createDirectories(output.getParent());
        List<String> command = new ArrayList<>();
        command.add(ProcessHandle.current().info().command().orElseThrow());
        List<String> arguments = new ArrayList<>();
        for (String argument : java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (argument.contains("runtime-capture.jar") || argument.startsWith("-Dchaoschatplus.exportPath=")) continue;
            arguments.add(argument);
        }
        arguments.add("-classpath"); arguments.add(System.getProperty("java.class.path"));
        // Test program arguments are controlled by runtime-runs.gradle and contain no spaces.
        arguments.addAll(Arrays.asList(System.getProperty("sun.java.command").split(" +")));
        Path argfile = output.resolveSibling(output.getFileName().toString().replace(".json", "-args.txt"));
        Files.writeString(argfile, String.join("\n", arguments.stream().map(RuntimeCapture::quote).toList()));
        command.add("@" + argfile.toAbsolutePath());
        List<String> env = new ArrayList<>();
        System.getenv().forEach((k,v) -> {
            if (k.startsWith("MOD_CLASSES") || k.startsWith("MOD_DEV_") || k.startsWith("FORGE_") || k.startsWith("NEOFORGE_")) env.add(quote(k)+":"+quote(v));
        });
        Files.writeString(output,"{\"command\":["+String.join(",",command.stream().map(RuntimeCapture::quote).toList())+"],\"cwd\":"+quote(System.getProperty("user.dir"))+",\"environment\":{"+String.join(",",env)+"}}");
        System.exit(0);
    }
    private static String quote(String value) {
        return "\""+value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\"";
    }
}
