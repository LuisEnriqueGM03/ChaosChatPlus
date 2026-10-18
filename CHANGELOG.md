# Chaos ChatPlus 1.1.1 — Minecraft 1.20.1

Fabric, Forge y NeoForge para Minecraft 1.20.1. Requiere Java 17 y el mismo cargador en servidor y clientes. Fabric necesita Fabric API.

- Adaptados los quince tipos de mensajes a FriendlyByteBuf, con límites de tamaño y pruebas de codificación y lectura.
- Adaptadas las conexiones nativas de Forge y NeoForge al transporte de 1.20.1 y los eventos de Fabric a su API correspondiente.
- Adaptados chat, menciones, fuentes, cámara, ítems y pantallas a Minecraft 1.20.1.
- La cámara también oculta la interfaz de ForgeGui en Forge y NeoForge.
- Conservados mensajes privados con emojis, GIF, capturas privadas, permisos, recarga y moderación.
- Pruebas reproducibles con Java 17, tres clientes gráficos y un servidor dedicado por cargador.

Fabric Loader 0.16.0 o posterior con Fabric API; Forge 47.4.26 o posterior; NeoForge 47.1.106 o posterior. Instala solo el JAR de tu cargador. Consulta VALIDATION.md para cobertura y alcance.

Ramas: fabric-1.20.1, forge-1.20.1 y neoforge-1.20.1.

**Validación:** 34 pruebas automáticas y 45 comprobaciones dentro del juego por cargador, con tres clientes y servidor dedicado.
