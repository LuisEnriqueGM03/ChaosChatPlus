# Chaos ChatPlus 1.1.1 — Minecraft 1.21.4

Fabric, Forge y NeoForge para Minecraft 1.21.4. Requiere Java 21 y el mismo cargador en servidor y clientes. Fabric también necesita Fabric API.

- Adaptadas las texturas a RenderType y conservadas sus medidas, coordenadas y transparencias.
- Adaptadas las imágenes a ARGB, preservando colores y transparencia de capturas, GIF y emojis.
- Adaptados el zoom de cámara, las fuentes, la vista previa y los eventos del juego a 1.21.4.
- La cámara oculta interfaz y avisos emergentes al tomar la imagen.
- Conservados chat público y privado, ítems, capturas privadas, permisos, recargas y moderación.

Validación: 31 pruebas automáticas y 48 comprobaciones multijugador por cargador, con tres clientes gráficos y servidor dedicado. Incluye una regresión explícita de colores y transparencia. Consulta VALIDATION.md para cobertura y alcance.

Fabric Loader 0.16.0 o posterior y Fabric API; Forge 54.1.14 o posterior; NeoForge 21.4.158 o posterior.

Ramas: fabric-1.21.4, forge-1.21.4 y neoforge-1.21.4. Instala solo el JAR de tu cargador; -sources.jar contiene las fuentes.
