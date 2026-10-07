# Chaos ChatPlus (Fabric 1.21.1)

Chat moderno para servidores: emojis (también para clientes vanilla), emojis propios, mostrar ítems,
menciones `@nombre` y capturas del juego moderadas. Se instala en el servidor; con el mod en el cliente se
desbloquean el diseño nuevo, el selector de emojis, los botones y el visor de capturas.

**Chaos Ranks es opcional.** Si está instalado: tu etiqueta de rango sale en la barra de escribir, los permisos
se pueden dar por rango, existe `@rango`, y las fuentes de las etiquetas viajan en el pack de emojis para que
también las vean los jugadores vanilla.

## Funciones

| | Sin mod (vanilla) | Con mod |
|---|---|---|
| `:fuego:`, `😂` → emoji | imagen (con el pack) o `:fuego:` | imagen |
| Emojis propios (`config/chaoschatplus/emojis/*.png`) | imagen con el pack | imagen + selector |
| Subir emoji soltando un PNG en el chat | – | sí (lo aprueba un OP) |
| `@nombre` | sonido, aviso y nombre resaltado | además el mensaje entero con barra dorada |
| Mostrar ítem | escribiendo `[item]` / `[i]` | botón 🗡 → eliges del inventario |
| Capturas | ve el aviso | botón 📷, miniatura, visor, guardar y reportar |
| Chat rediseñado | – | panel redondeado, animación, barra T con prefijo de rango |

### Anti-NSFW en las capturas
- Solo se pueden mandar **capturas del propio juego**: el mod oculta la interfaz, lee el frame y lo enseña antes de enviar.
  No existe forma de elegir un archivo del PC.
- El servidor vuelve a codificar cada imagen (fuera metadatos y datos raros), con un tope de tamaño, cooldown y permisos.
- Reportes de jugadores → aviso a los OPs + `config/chaoschatplus/reports.log`; con N reportes se oculta sola.
- `/chatplus shot delete <id>` la borra para todos. Opción `blurUntilClick`.
- Filtro automático opcional: `screenshots.moderation.provider = "sightengine"` con tu `apiUser` y `apiSecret`.

## Comandos
- `/emoji` – lista · `/emoji pending|approve|deny|remove|enable|alias|ranks` (OP)
- `/chatplus reload` · `/chatplus pack export` · `/chatplus shot list|delete <id>` (OP)

## Pack de emojis
Por defecto el servidor lo sirve él mismo por HTTP en el puerto `25599` (`resourcePack.port`), usando la misma dirección
con la que entró cada jugador (o `resourcePack.publicHost`). Si tu hosting no deja abrir otro puerto:
`/chatplus pack export`, sube el zip y usa `"mode": "external"` con `externalUrl`.

## Desarrollo
- `python tools/gen_emojis.py --preview` · `python tools/gen_lang.py` · `python tools/gen_icon.py`
- `./gradlew runServer` (carpeta `run-server`), `./gradlew runJoinServer` (Luis, con mod),
  `./gradlew runJoinVanilla` (Pepe, como cliente sin mod). Con `-Pselftest=true` siguen un guion y dejan
  capturas en `run*/screenshots/selftest_*.png`.

Los emojis de serie son pixel art propio (`tools/gen_emojis.py`), licencia MIT como el resto del mod.
