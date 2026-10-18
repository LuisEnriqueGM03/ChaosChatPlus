# Validación de Chaos ChatPlus 1.1.1 — Minecraft 26.2

Fecha: 18 de octubre de 2026. Publicación estable para Java 25.

| Comprobación | Fabric | Forge | NeoForge |
| --- | --- | --- | --- |
| Compilación final | Correcta | Correcta | Correcta |
| Pruebas automáticas | 31 sin fallos | 31 sin fallos | 31 sin fallos |
| Comprobaciones dentro del juego | 50 correctas | 50 correctas | 50 correctas |
| Jugadores conectados a servidor dedicado | 3 | 3 | 3 |
| Arranque y cierre guardando el mundo | Correcto | Correcto | Correcto |
| Revisión visual de capturas | Correcta | Correcta | Correcta |

Las pruebas usan tres procesos gráficos reales (Luis, Pepe y Audit) y un servidor dedicado independiente por cargador, con Java 25 y NVIDIA RTX 3060. Todos se ejecutan en el mismo equipo, conectados por TCP local. No se usaron mundos ni configuraciones del usuario. No se declara compatibilidad de red entre cargadores.

## Cobertura dentro del juego

- Conexión, permisos y sincronización de los 197 emojis y fuentes del servidor.
- Chat público, menciones, glifos de emoji, autocompletado con Tab y escritura Unicode.
- Compartir un ítem real, recibir su tooltip y abrir su vista previa mediante un clic de ratón sobre el enlace.
- Búsqueda de GIFs propios, decodificación en múltiples texturas y recepción pública y privada.
- Cámara, zoom, vista previa, envío de captura real del renderizador y apertura del visor de la imagen recibida mediante un clic sobre el enlace.
- Modo privado enviado desde ChatScreen con Enter: texto, emoji, ítem, GIF y captura recibidos por el destinatario.
- Tercer jugador: ausencia de mensajes y enlaces privados; denegación de una solicitud de captura privada con ID conocido y de un reporte no autorizado.
- Subida de PNG por la red, aprobación de un OP, sincronización de textura y recarga automática de otro PNG añadido al directorio.
- Reporte legítimo registrado, borrado por moderador e invalidación de las cachés de los tres jugadores.
- Rechazo de imagen inválida e ID inexistente, sin desconectar al remitente; conservación de acceso del propietario a su captura privada.

Las 50 comprobaciones por cargador suman 150 verificaciones de ejecución. Las pruebas JUnit suman otras 93 ejecuciones sin fallos. RUNTIME_RESULTS.json contiene los resultados y los mensajes de comprobación. runtime-screenshots.zip contiene las capturas de cada cargador.

Además se ejecutan tres comprobaciones específicas de cámara por cargador: campo de visión realmente usado en la proyección, ocultación de HUD y restauración de la vista previa. Son 9 verificaciones adicionales; se conservan los registros de la prueba multijugador antes de estas sesiones.

## Adaptación a Minecraft 26.2

La gestión de pantallas usa Gui.screen()/setScreen(), el chat y la interfaz visible usan Gui.hud y la captura toma el renderizador desde GameRenderer.mainRenderTarget(). El modo cámara oculta Hud.extractRenderState() sin cancelar el gestor de pantallas. Las pantallas usan GuiGraphicsExtractor y las texturas RenderPipelines.GUI_TEXTURED. Se adaptaron los eventos de teclado y ratón, perfiles, permisos y enlaces de objetos y capturas. La captura del renderizador se recibe mediante callback y abre su vista previa en el hilo del cliente. El zoom actúa sobre Camera.calculateFov() sin alterar la mano. La cámara oculta también los avisos emergentes.

Las fuentes dinámicas usan GlyphStitcher; se invalidan las cachés al sustituirlas y se liberan sus proveedores al recargar o desconectar. NativeImage conserva ARGB: cada cliente comprueba rojo, un color mixto y alfa parcial. Dos comprobaciones adicionales hacen clic en los enlaces reales del chat y verifican que se abran los visores de ítem y captura.

Se probaron Fabric Loader 0.19.5 / Fabric API 0.161.0+26.2, Forge 65.1.5 y NeoForge 26.2.0.93. Forge usa JAVA_21 en la declaración de compatibilidad de Mixin porque su enum no admite JAVA_25; las clases y el juego requieren Java 25 en los tres cargadores.

## Reproducción en Windows

Requiere JDK 25 y Python 3. JDK 25 ejecuta Gradle y los juegos de prueba; las clases generadas usan Java 25. Sustituye las rutas de ejemplo por tus instalaciones.

```text
python tools/runtime-prepare.py fabric --jdk25 "C:/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"
python tools/runtime-tests.py fabric
```

Repite ambos comandos con forge y neoforge. El preparador exporta los lanzamientos y usa javaw.exe, que en este equipo tiene la preferencia de gráfica de alto rendimiento ya configurada. Las pruebas se guardan en .verification/runtime/<cargador>; los servidores escuchan únicamente en 127.0.0.1, puertos 26031–26033, sin autenticación para estos perfiles locales. Cierra una sesión de prueba antes de iniciar la siguiente. Al repetirla, se conserva la configuración de la ejecución anterior en un directorio de respaldo dentro de la carpeta de pruebas.

Los clientes se inician y conectan uno por uno, después de completar la carga gráfica y consultar el estado del servidor. Los JAR se compilan con ./gradlew build, ./gradlew -p platforms/forge build y ./gradlew -p platforms/neoforge build. El archivo -slim.jar de Forge no es el instalable; distribuye el JAR sin clasificador.

## Alcance

La validación cubre los tres clientes con el mod y el servidor de su mismo cargador. No se probaron aquí servidores remotos con latencia, otros mods, ni la integración opcional con Chaos Ranks, KLIPY o Sightengine con cuentas externas. Estas integraciones mantienen sus requisitos de configuración. La moderación probada en esta sesión es la de reportes y borrado por OP.
