# Chaos ChatPlus 1.1.1 — Minecraft 26.3

Fabric, Forge y NeoForge para Minecraft 26.3. Requiere Java 25 y el mismo cargador en servidor y clientes. Fabric también necesita Fabric API.

- Adaptados los controles a SDL: clics izquierdo y derecho, favoritos, autocompletado, búsquedas, cierre de selectores y cámara. Conservados colores, transparencia y animaciones.
- Adaptada la carga de fuentes del servidor a ResourceManager.Selector y VanillaPackResources.fullResources().
- Captura de pantalla asíncrona, vista previa, visor, zoom y ocultación de interfaz y avisos emergentes.
- Fuentes dinámicas adaptadas al nuevo gestor, con invalidación de cachés y liberación de imágenes al recargar o desconectar.
- Adaptados los permisos del servidor, perfiles, mensajes interactivos, objetos y registro nativo de eventos y red de los tres cargadores.
- Conservados el chat público y privado, GIF, emojis propios, moderación, recargas y denegación de acceso a imágenes privadas.

Validación: 31 pruebas automáticas y 54 comprobaciones multijugador por cargador, con tres clientes gráficos y servidor dedicado. Incluye clics reales sobre enlaces de ítems y capturas, colores ARGB y transparencia. Otras tres comprobaciones por cargador verifican el campo de visión real de la cámara, la ocultación de HUD y la vista previa. Consulta VALIDATION.md para cobertura y alcance.

Versiones probadas: Fabric Loader 0.19.5 y Fabric API 0.163.0+26.3; Forge 66.0.13; NeoForge 26.3.0.92. Los archivos de Chaos ChatPlus se publican como Release.

Ramas: fabric-26.3, forge-26.3 y neoforge-26.3. Instala solo el JAR de tu cargador; -sources.jar contiene las fuentes.
