# Validación de Chaos ChatPlus 1.1.1 — Minecraft 1.20.1

Fecha: 18 de octubre de 2026. Publicación estable para Java 17.

| Comprobación | Fabric | Forge | NeoForge |
| --- | --- | --- | --- |
| Compilación final | Correcta | Correcta | Correcta |
| Pruebas automáticas | 34 sin fallos | 34 sin fallos | 34 sin fallos |
| Comprobaciones dentro del juego | 45 correctas | 45 correctas | 45 correctas |
| Jugadores conectados a servidor dedicado | 3 | 3 | 3 |
| Arranque y cierre guardando el mundo | Correcto | Correcto | Correcto |
| Revisión visual de capturas | Correcta | Correcta | Correcta |
| Regresión adicional de cámara | 3 correctas | 3 correctas | 3 correctas |

Las pruebas usan tres procesos gráficos reales (Luis, Pepe y Audit) y un servidor dedicado independiente por cargador, con Java 17 y NVIDIA RTX 3060. Todos se ejecutan en el mismo equipo, conectados por TCP local. No se usaron mundos ni configuraciones del usuario. No se declara compatibilidad de red entre cargadores.

## Cobertura dentro del juego

- Conexión, permisos y sincronización de los 197 emojis y fuentes del servidor.
- Chat público, menciones, glifos de emoji, autocompletado con Tab y escritura Unicode.
- Compartir un ítem real y recibir el enlace y su contenido de tooltip.
- Búsqueda de GIFs propios, decodificación en múltiples texturas y recepción pública y privada.
- Cámara, zoom, vista previa, envío de captura real del renderizador y visor de la imagen recibida.
- Modo privado enviado desde ChatScreen con Enter: texto, emoji, ítem, GIF y captura recibidos por el destinatario.
- Tercer jugador: ausencia de mensajes y enlaces privados; denegación de una solicitud de captura privada con ID conocido y de un reporte no autorizado.
- Subida de PNG por la red, aprobación de un OP, sincronización de textura y recarga automática de otro PNG añadido al directorio.
- Reporte legítimo registrado, borrado por moderador e invalidación de las cachés de los tres jugadores.
- Rechazo de imagen inválida e ID inexistente, sin desconectar al remitente; conservación de acceso del propietario a su captura privada.

Las 45 comprobaciones por cargador suman 135 verificaciones de ejecución. La regresión posterior de cámara añade 3 comprobaciones por cargador; se conservan los resultados multijugador completos por separado. Las pruebas JUnit suman otras 102 ejecuciones sin fallos. RUNTIME_RESULTS.json contiene los resultados y los mensajes de comprobación. runtime-screenshots.zip contiene las capturas de cada cargador.

## Correcciones encontradas durante la validación

La adaptación reemplaza los paquetes de Minecraft 1.21.1 por codecs limitados de FriendlyByteBuf y SimpleChannel en los cargadores nativos. Se añadieron tres pruebas: ida y vuelta de los quince mensajes, límites de arrays antes de reservar memoria e IDs truncados o demasiado largos.

La revisión visual detectó que ForgeGui reemplaza Gui y evitaba el mixin que oculta la interfaz durante la cámara. Se añadió la cancelación de RenderGuiEvent.Pre en ambos cargadores nativos y una prueba dirigida de cámara, supresión del HUD y restauración de la vista previa. También se ocultan las notificaciones emergentes durante la cámara, porque se dibujan fuera del HUD.

El arranque real detectó diferencias en mouseScrolled, en el renderizado del chat y en la creación de sus mensajes. Se adaptaron las firmas y el etiquetado de menciones al flujo de 1.20.1. Las fuentes usan los proveedores y FontSet.reload de esta versión; la cámara y las pantallas usan su API de renderizado.

Se verificaron los metadatos 1.1.1 para Minecraft 1.20.1, bytecode Java 17, ausencia de entradas ZIP duplicadas y MixinExtras incluida en ambos cargadores nativos. NeoForge 1.20.1 usa mods.toml y el identificador forge de esa generación, con requisito 47.1.106.

## Reproducción en Windows

Requiere JDK 17, JDK 21, JDK 25 y Python 3. JDK 25 ejecuta Gradle/Loom; los juegos de prueba y el código generado usan Java 17. Sustituye las rutas de ejemplo por tus instalaciones.

```text
python tools/runtime-prepare.py fabric --jdk17 "C:/Users/luise/.jdks/temurin-17" --jdk25 "C:/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"
python tools/runtime-tests.py fabric
```

Repite ambos comandos con forge y neoforge. Para repetir solo la regresión adicional de cámara: python tools/runtime-camera.py forge o neoforge, después de preparar los lanzamientos. El guion inicia y conecta los clientes uno por uno y comprueba el chat cuando los tres están conectados. Consulta el estado del servidor antes de conectar, como la lista multijugador, con hasta tres intentos acotados durante el arranque. En los perfiles de Forge y NeoForge se desactiva earlyWindowControl para evitar conflictos entre tres ventanas preliminares simultáneas. El preparador exporta los lanzamientos y usa javaw.exe, que en este equipo tiene la preferencia de gráfica de alto rendimiento ya configurada. Las pruebas se guardan en .verification/runtime/<cargador>; los servidores escuchan únicamente en 127.0.0.1, puertos 25731–25733, sin autenticación para estos perfiles locales. Cierra una sesión de prueba antes de iniciar la siguiente. Al repetirla, se conserva la configuración de la ejecución anterior en un directorio de respaldo dentro de la carpeta de pruebas.

Los JAR se compilan con ./gradlew build, ./gradlew -p platforms/forge build y ./gradlew -p platforms/neoforge build. Distribuye el JAR sin clasificador; -sources.jar contiene las fuentes.

## Alcance

La validación cubre los tres clientes con el mod y el servidor de su mismo cargador. No se probaron aquí servidores remotos con latencia, otros mods, ni la integración opcional con Chaos Ranks, KLIPY o Sightengine con cuentas externas. Estas integraciones mantienen sus requisitos de configuración. La moderación probada en esta sesión es la de reportes y borrado por OP.
