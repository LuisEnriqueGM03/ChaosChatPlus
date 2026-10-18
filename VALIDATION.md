# Validación de Chaos ChatPlus 1.1.1 — Minecraft 1.21.1

Fecha: 18 de octubre de 2026. Publicación estable para Java 21.

| Comprobación | Fabric | Forge | NeoForge |
| --- | --- | --- | --- |
| Compilación final | Correcta | Correcta | Correcta |
| Pruebas automáticas | 31 sin fallos | 31 sin fallos | 31 sin fallos |
| Comprobaciones dentro del juego | 45 correctas | 45 correctas | 45 correctas |
| Jugadores conectados a servidor dedicado | 3 | 3 | 3 |
| Arranque y cierre guardando el mundo | Correcto | Correcto | Correcto |
| Revisión visual de capturas | Correcta | Correcta | Correcta |

Las pruebas usan tres procesos gráficos reales (Luis, Pepe y Audit) y un servidor dedicado independiente por cargador, con Java 21 y NVIDIA RTX 3060. Todos se ejecutan en el mismo equipo, conectados por TCP local. No se usaron mundos ni configuraciones del usuario. No se declara compatibilidad de red entre cargadores.

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

Las 45 comprobaciones por cargador suman 135 verificaciones de ejecución. Las pruebas JUnit suman otras 93 ejecuciones sin fallos. RUNTIME_RESULTS.json contiene los resultados y los mensajes de comprobación. runtime-screenshots.zip contiene las capturas de cada cargador.

## Correcciones encontradas durante la validación

Forge y NeoForge decoraban el chat público mediante sus eventos, pero estos no cubrían /msg, /tell y /w. Se añadió la decoración del contenido privado, preservando el mensaje firmado y limitando los avisos de menciones al destinatario. La prueba exige ahora recibir un glifo de emoji dentro del mensaje privado; antes de la corrección Forge mostraba :corazon: literalmente.

La compilación nativa registra también la versión como entrada de processResources, evitando conservar una versión antigua dentro del JAR cuando cambia gradle.properties/build.gradle. Se verificaron los metadatos finales 1.1.1, Java 21, ausencia de entradas ZIP duplicadas y dependencia MixinExtras incluida en Forge.

## Reproducción en Windows

Requiere JDK 21, JDK 25 y Python 3. JDK 25 ejecuta Gradle/Loom; los juegos de prueba y el código generado usan Java 21. Sustituye las rutas de ejemplo por tus instalaciones.

```text
python tools/runtime-prepare.py fabric --jdk21 "C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot" --jdk25 "C:/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"
python tools/runtime-tests.py fabric
```

Repite ambos comandos con forge y neoforge. El preparador exporta los lanzamientos y usa javaw.exe, que en este equipo tiene la preferencia de gráfica de alto rendimiento ya configurada. Las pruebas se guardan en .verification/runtime/<cargador>; los servidores escuchan únicamente en 127.0.0.1, puertos 25631–25633, sin autenticación para estos perfiles locales. Cierra una sesión de prueba antes de iniciar la siguiente. Al repetirla, se conserva la configuración de la ejecución anterior en un directorio de respaldo dentro de la carpeta de pruebas.

Los JAR se compilan con ./gradlew build, ./gradlew -p platforms/forge build y ./gradlew -p platforms/neoforge build. El archivo -slim.jar de Forge no es el instalable; distribuye el JAR sin clasificador.

## Alcance

La validación cubre los tres clientes con el mod y el servidor de su mismo cargador. No se probaron aquí servidores remotos con latencia, otros mods, ni la integración opcional con Chaos Ranks, KLIPY o Sightengine con cuentas externas. Estas integraciones mantienen sus requisitos de configuración. La moderación probada en esta sesión es la de reportes y borrado por OP.
