# Chaos ChatPlus 1.1.1 — Minecraft 26.1

Fabric, Forge y NeoForge para Minecraft 26.1. Requiere Java 25 y el mismo cargador en servidor y clientes. Fabric también necesita Fabric API.

- Adaptadas las pantallas y texturas a GuiGraphicsExtractor y RenderPipeline, conservando colores, transparencia y animaciones.
- Adaptados el teclado, ratón, texto Unicode y clics sobre ítems y capturas al nuevo sistema de entrada.
- Captura de pantalla asíncrona, vista previa, visor, zoom y ocultación de interfaz y avisos emergentes.
- Fuentes dinámicas adaptadas al nuevo gestor, con invalidación de cachés y liberación de imágenes al recargar o desconectar.
- Adaptados los permisos del servidor, perfiles, mensajes interactivos, objetos y registro nativo de eventos y red de los tres cargadores.
- Conservados el chat público y privado, GIF, emojis propios, moderación, recargas y denegación de acceso a imágenes privadas.

Validación: 31 pruebas automáticas y 50 comprobaciones multijugador por cargador, con tres clientes gráficos y servidor dedicado. Incluye clics reales sobre enlaces de ítems y capturas, colores ARGB y transparencia. Consulta VALIDATION.md para cobertura y alcance.

Versiones probadas: Fabric Loader 0.19.5 y Fabric API 0.145.1+26.1; Forge 62.0.9; NeoForge 26.1.0.19-beta. La dependencia NeoForge conserva el sufijo asignado por su propio proyecto; los archivos de Chaos ChatPlus se publican como Release.

Ramas: fabric-26.1, forge-26.1 y neoforge-26.1. Instala solo el JAR de tu cargador; -sources.jar contiene las fuentes.
