# Chaos ChatPlus 1.1.0 — Minecraft 1.21.1 (beta)

Tres ediciones independientes: Fabric, Forge y NeoForge. Requiere Java 21 y el mismo cargador en servidor y clientes. Instala solamente el JAR de tu cargador; los archivos `-sources.jar` son para desarrollo.

- Fabric requiere Fabric Loader 0.16.0 o posterior y Fabric API.
- Forge requiere 52.1.20 o posterior; incluye MixinExtras.
- NeoForge requiere 21.1.259 o posterior.
- Adaptadores de red y eventos nativos para Forge y NeoForge, con lógica y recursos compartidos.
- Mejores límites y validación de imágenes, ensamblado de paquetes, cachés y subidas de emojis.
- Guardado atómico de configuración y conservación del último estado válido al fallar una recarga.
- Recarga de recursos en segundo plano y comprobaciones de acceso a capturas privadas.
- Separación del código cliente y 31 pruebas automáticas por cargador.

Compilan las tres ediciones y pasan 93 ejecuciones de pruebas en total, sin fallos. Forge y NeoForge también pasaron el arranque y cierre de servidor dedicado. Falta validar los clientes gráficos y una sesión multijugador real con estos cambios; por eso esta publicación se distribuye como beta. Consulta VALIDATION.md.

Ramas: `fabric-1.21.1`, `forge-1.21.1` y `neoforge-1.21.1`. Chaos Ranks es opcional. Los SHA-256 de los seis JAR están en SHA256SUMS.txt.
