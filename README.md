# Chaos ChatPlus (Fabric 1.21.1)

Chat moderno para servidores: emojis, emojis propios, mostrar ítems,
menciones `@nombre` y capturas del juego moderadas. Se instala en el servidor; con el mod en el cliente se
desbloquean el diseño nuevo, el selector de emojis, los botones y el visor de capturas.

**Chaos Ranks es opcional.** Si está instalado: tu etiqueta de rango sale en la barra de escribir, los permisos
se pueden dar por rango, existe `@rango`, y las fuentes de las etiquetas llegan a los jugadores por el mod.

## Funciones

**El mod es obligatorio en el servidor y en cada jugador**: quien entre sin él es expulsado con un aviso
para instalarlo. No hay paquete de recursos: los emojis que añade el servidor y las etiquetas de Chaos Ranks
llegan por el propio mod.

### Anti-NSFW en las capturas
- Solo se pueden mandar **capturas del propio juego**: el mod oculta la interfaz, lee el frame y lo enseña antes de enviar.
  No existe forma de elegir un archivo del PC.
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
