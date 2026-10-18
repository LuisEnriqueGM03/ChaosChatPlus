# Chaos ChatPlus 1.1.1 — Minecraft 26.2

Fabric, Forge y NeoForge para Minecraft 26.2. Requiere Java 25 y el mismo cargador en servidor y clientes. Fabric también necesita Fabric API.

- Adaptadas las pantallas al gestor Gui de 26.2, el chat a Gui.hud y la cámara al nuevo renderizador. Conservados colores, transparencia y animaciones.
- Adaptados el teclado, ratón, texto Unicode y clics sobre ítems y capturas al nuevo sistema de entrada.
- Captura de pantalla asíncrona, vista previa, visor, zoom y ocultación de interfaz y avisos emergentes.
- Fuentes dinámicas adaptadas al nuevo gestor, con invalidación de cachés y liberación de imágenes al recargar o desconectar.
- Adaptados los permisos del servidor, perfiles, mensajes interactivos, objetos y registro nativo de eventos y red de los tres cargadores.
- Conservados el chat público y privado, GIF, emojis propios, moderación, recargas y denegación de acceso a imágenes privadas.

Validación: 31 pruebas automáticas y 50 comprobaciones multijugador por cargador, con tres clientes gráficos y servidor dedicado. Incluye clics reales sobre enlaces de ítems y capturas, colores ARGB y transparencia. Otras tres comprobaciones por cargador verifican el campo de visión real de la cámara, la ocultación de HUD y la vista previa. Consulta VALIDATION.md para cobertura y alcance.

Versiones probadas: Fabric Loader 0.19.5 y Fabric API 0.161.0+26.2; Forge 65.1.5; NeoForge 26.2.0.93. Los archivos de Chaos ChatPlus se publican como Release.

Ramas: fabric-26.2, forge-26.2 y neoforge-26.2. Instala solo el JAR de tu cargador; -sources.jar contiene las fuentes.
