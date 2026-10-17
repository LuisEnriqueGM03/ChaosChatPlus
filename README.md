# Edición Fabric — Minecraft 1.21.1

Rama `fabric-1.21.1`, Chaos ChatPlus 1.1.0 beta. El repositorio mantiene el código común y los adaptadores de los tres cargadores.

Compila esta edición desde la raíz con `./gradlew build`. JAR instalable: `build/libs/chaoschatplus-1.1.0.jar`. Para ejecutar el mod necesitas Java 21; para compilar consulta las versiones de JDK en VALIDATION.md.

[Descargar esta versión y sus fuentes](https://github.com/LuisEnriqueGM03/ChaosChatPlus/releases/tag/v1.1.0-1.21.1) · [Resultados y límites de pruebas](VALIDATION.md).

---

# Chaos ChatPlus (Minecraft 1.21.1)

Chat moderno para servidores: emojis, emojis propios, mostrar ítems,
menciones `@nombre` y capturas del juego moderadas. Se instala en el servidor y en cada cliente; incluye el diseño nuevo, el selector de emojis, los botones y el visor de capturas.

**Chaos Ranks es opcional.** Si está instalado: tu etiqueta de rango sale en la barra de escribir, los permisos
se pueden dar por rango, existe `@rango`, y las fuentes de las etiquetas llegan a los jugadores por el mod.

## Funciones

**El mod es obligatorio en el servidor y en cada jugador**: quien entre sin él es expulsado con un aviso
para instalarlo. No hay paquete de recursos: los emojis que añade el servidor y las etiquetas de Chaos Ranks
llegan por el propio mod.

### Moderación de capturas
- La interfaz permite tomar y enviar **capturas del propio juego**: el mod oculta la interfaz, lee el frame y lo enseña antes de enviar.
  La interfaz no ofrece archivos arbitrarios, pero un cliente modificado puede enviar otras imágenes.
  Recodificar una imagen no detecta contenido inapropiado. El filtro externo está desactivado por defecto.
- El servidor vuelve a codificar cada imagen (fuera metadatos y datos raros), con un tope de tamaño, cooldown y permisos.
- Reportes de jugadores → aviso a los OPs + `config/chaoschatplus/reports.log`; con N reportes se oculta sola.
- `/chatplus shot delete <id>` la borra para todos. Opción `blurUntilClick`.
- Filtro automático opcional: `screenshots.moderation.provider = "sightengine"` con tu `apiUser` y `apiSecret`.

## Comandos
- `/emoji` – lista · `/emoji pending|approve|deny|remove|enable|alias|ranks` (OP)
- `/chatplus reload` · `/chatplus shot list|delete <id>` (OP)


## Desarrollo
- `python tools/gen_emojis.py --preview` · `python tools/gen_lang.py` · `python tools/gen_icon.py`
- `./gradlew runServer` (carpeta `run-server`), `./gradlew runJoinServer` (Luis, con mod),
  `./gradlew runJoinVanilla` (Pepe sin mod: comprueba que lo expulsa). Con `-Pselftest=true` siguen un guion y dejan
  capturas en `run*/screenshots/selftest_*.png`.

Los emojis de serie son pixel art propio (`tools/gen_emojis.py`), licencia MIT como el resto del mod.

## GIFs

- **Los del mod**: 360 propios (texto, efectos, troll, emojis y anime chibi), en
  `config/chaoschatplus/gifs/default/<categoría>/`. Suelta más GIFs en `config/chaoschatplus/gifs/<categoría>/` y
  salen solos (se reducen a ≤128 px).
- **Custom (Klipy)**: pon tu clave de https://partner.klipy.com/api-keys en `gifs.klipyKey`. Cumple los requisitos de
  integración de Klipy: cada jugador busca y carga los GIFs desde su juego, directo a Klipy (el servidor solo le pasa la
  clave); los GIFs no se guardan en disco; los resultados se muestran tal cual en su propia pestaña; el buscador dice
  «Search KLIPY» y sale «Powered by KLIPY». El filtro de contenido se configura en el Partner Panel de Klipy. Sin clave,
  la pestaña dice que no está configurado.
- **Favoritos**: clic derecho en un GIF propio del selector (los de Klipy no, para no mezclarlos). Se guardan en el PC de cada jugador.

## Estructura y comprobaciones

- `src/main/java`: servidor, protocolo, permisos y lógica compartida.
- `src/client/java`: interfaz, renderizado y estado exclusivo del cliente. Loom comprueba la separación al compilar.
- `src/test/java`: pruebas automáticas de imágenes, configuración, paquetes, permisos, acceso privado y cachés.
- `./gradlew build`: compila ambos entornos, ejecuta las pruebas y genera el mod en `build/libs/`.
- `SelfTest` sigue siendo un guion visual complementario; no sustituye las pruebas ni una sesión multijugador.

La revisión periódica de las carpetas y la conversión de GIFs se realizan en un trabajador en segundo plano.
Los registros publican mapas inmutables y la sincronización con los jugadores vuelve al hilo del servidor.
Las recargas manuales esperan a completar su operación antes de confirmar el resultado.

## Límites y configuración

- `emojis.maxPending`: máximo global de subidas pendientes (100 por defecto).
- `emojis.maxPendingPerPlayer`: máximo pendiente por jugador (5 por defecto).
- `emojis.uploadCooldownSeconds`: espera entre intentos de subida (30 segundos por defecto).
- Los pendientes reservan espacio dentro de `emojis.maxCustom`; aprobarlos no puede superar ese límite.
- Las capturas se limitan a 900 KB como máximo configurable para permanecer dentro del presupuesto de un paquete.
- GIFs en memoria: máximo 64 entradas y 96 MiB de píxeles; capturas: 24 entradas y 64 MiB.
- Si `config.json` es inválido, una recarga conserva el archivo y la configuración activa. Al iniciar el servidor,
  se solicita corregirlo en vez de arrancar con permisos o moderación distintos de los configurados.
- Los cambios válidos se guardan mediante un archivo temporal y sustitución atómica cuando el sistema lo permite.

## Fabric, Forge y NeoForge

Las tres ediciones comparten `src/main/java`, `src/client/java`, recursos y pruebas.
Los adaptadores registran red y eventos de forma nativa; las versiones Forge y NeoForge no necesitan Fabric API.
Instala el archivo correspondiente al cargador de tu servidor y de tus jugadores. No mezcles las ediciones en una
misma instalación. Chaos Ranks continúa siendo opcional y necesita una edición compatible con ese cargador.

| Edición | Compilación desde la raíz | Archivo instalable |
| --- | --- | --- |
| Fabric | `./gradlew build` | `build/libs/chaoschatplus-1.1.0.jar` |
| Forge 52.1.20+ | `./gradlew -p platforms/forge build` | `platforms/forge/build/libs/chaoschatplus-forge-1.21.1-1.1.0.jar` |
| NeoForge 21.1.259+ | `./gradlew -p platforms/neoforge build` | `platforms/neoforge/build/libs/chaoschatplus-neoforge-1.21.1-1.1.0.jar` |

Para compilar, usa JDK 25 para ejecutar Gradle: la versión actual de Loom lo requiere. Forge y NeoForge compilan
con una toolchain JDK 21; instala ambos JDK o configura su detección mediante Gradle. Los tres mods generan
bytecode Java 21 para Minecraft 1.21.1. La edición Forge incluye MixinExtras dentro del archivo instalable;
el archivo con sufijo `slim` es intermedio y no es el que debes distribuir.

- `src/fabric/java` y `src/fabricClient/java`: conexiones y eventos exclusivos de Fabric.
- `platforms/forge`: compilación y adaptadores nativos Forge.
- `platforms/neoforge`: compilación y adaptadores nativos NeoForge.
- `platform/ServerNetwork` y `client/ClientNetwork`: frontera entre las funciones del chat y el transporte del cargador.
- `net/PayloadCatalog`: catálogo único de mensajes, codecs y direcciones utilizado por los tres transportes.
