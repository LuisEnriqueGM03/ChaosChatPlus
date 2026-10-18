# Chaos ChatPlus 1.1.1 — Minecraft 1.21.1

Versión estable para Fabric, Forge y NeoForge. Requiere Java 21 y el mismo cargador en servidor y clientes; Fabric requiere además Fabric API.

- Corregidos los emojis y tokens de ítems en mensajes privados de Forge y NeoForge (/msg, /tell y /w).
- Las menciones decoradas en privados de los cargadores nativos avisan solo a sus destinatarios.
- Corregida la actualización de la versión interna en los JAR de Forge y NeoForge.
- Añadidas pruebas reproducibles con tres clientes gráficos y un servidor dedicado por cargador.

**Validación:** 31 pruebas automáticas y 45 comprobaciones dentro del juego por cargador, sin fallos. Se probaron chat público y privado, emojis, ítems, GIFs, cámara y capturas, permisos, aislamiento de terceros, recargas y moderación. Los tres servidores cerraron guardando sus mundos; las capturas de interfaz se revisaron visualmente.

Fabric Loader 0.16.0 o posterior con Fabric API; Forge 52.1.20 o posterior; NeoForge 21.1.259 o posterior. Instala solo el JAR de tu cargador; -sources.jar contiene código para desarrollo. Consulta VALIDATION.md para cobertura y alcance, RUNTIME_RESULTS.json para resultados y SHA256SUMS.txt para verificar los archivos.

Ramas: fabric-1.21.1, forge-1.21.1 y neoforge-1.21.1. Esta publicación sustituye a la beta 1.1.0.
