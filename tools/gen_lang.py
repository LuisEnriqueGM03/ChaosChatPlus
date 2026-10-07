"""
Genera assets/chaoschatplus/lang/*.json (español de España y de Latinoamérica, e inglés).
Uso: python tools/gen_lang.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "chaoschatplus", "lang")

# clave: (español, inglés)
TEXTS = {
    "button.cancel": ("Cancelar", "Cancel"),
    "button.close": ("Cerrar", "Close"),
    "cooldown": ("Espera %s s antes de volver a hacerlo.", "Wait %s s before doing that again."),
    "reloaded": ("Chaos ChatPlus recargado (%s emojis).", "Chaos ChatPlus reloaded (%s emojis)."),

    "bar.placeholder": ("Escribe un mensaje…", "Type a message…"),
    "bar.emojis": ("Emojis", "Emojis"),
    "bar.item": ("Mostrar un ítem", "Show an item"),
    "bar.shot": ("Enviar una captura del juego", "Send a game screenshot"),

    "picker.recent": ("Recientes", "Recent"),
    "picker.favorites": ("Favoritos", "Favorites"),
    "picker.no_recent": ("Aún no usaste ninguno", "None used yet"),
    "picker.no_favorites": ("Clic derecho = favorito", "Right click = favorite"),
    "picker.fav_hint": ("clic der. ★", "right click ★"),
    "category.caras": ("Caras", "Faces"),
    "category.corazones": ("Corazones", "Hearts"),
    "category.gestos": ("Gestos", "Gestures"),
    "category.simbolos": ("Símbolos", "Symbols"),
    "category.objetos": ("Objetos", "Objects"),
    "category.minecraft": ("Minecraft", "Minecraft"),
    "category.servidor": ("Del servidor", "Server"),
    "category.animales": ("Animales", "Animals"),
    "category.comida": ("Comida", "Food"),
    "category.naturaleza": ("Naturaleza", "Nature"),
    "bar.send": ("Enviar", "Send"),
    "bar.limit": ("Llegaste al límite de %s caracteres", "You reached the %s character limit"),
    "camera.hint": ("Clic o Enter: capturar  ·  Esc: cancelar", "Click or Enter: capture  ·  Esc: cancel"),
    "camera.title": ("Modo cámara", "Camera mode"),
    "shot.review_title": ("Captura reportada", "Reported screenshot"),
    "shot.delete": ("Borrar", "Delete"),
    "shot.delete_button": ("[Borrar]", "[Delete]"),
    "shot.review_text": ("Captura %s de %s · %s reportes", "Screenshot %s by %s · %s reports"),

    "mention.hover": ("Mención: %s", "Mention: %s"),
    "mention.actionbar": ("%s te mencionó", "%s mentioned you"),
    "mention.tag": ("Te mencionaron", "You were mentioned"),

    "item.shared": ("%s%s muestra %s", "%s%s shows %s"),
    "item.no_permission": ("No puedes mostrar ítems en este servidor.", "You can't show items on this server."),
    "item.picker_title": ("Mostrar un ítem en el chat", "Show an item in chat"),
    "item.picker_hint": ("Clic en un ítem para mostrarlo · Esc para volver", "Click an item to show it · Esc to go back"),

    "emoji.list": ("Emojis (%s) · pasa el ratón para ver el código:", "Emojis (%s) · hover to see the code:"),
    "emoji.name": ("Nombre", "Name"),
    "emoji.upload": ("Subir", "Upload"),
    "emoji.upload_title": ("Subir emoji", "Upload emoji"),
    "emoji.upload_note": ("Un OP lo revisará antes de que todos puedan usarlo.",
                          "An OP will review it before everyone can use it."),
    "emoji.no_permission": ("No puedes subir emojis en este servidor.", "You can't upload emojis on this server."),
    "emoji.bad_image": ("La imagen debe ser un PNG de como mucho %s px.", "The image must be a PNG of at most %s px."),
    "emoji.added": ("Emoji %s añadido.", "Emoji %s added."),
    "emoji.pending": ("Emoji enviado: un OP lo revisará pronto.", "Emoji sent: an OP will review it soon."),
    "emoji.pending_staff": ("%s subió el emoji :%s: · /emoji pending para revisarlo",
                            "%s uploaded the emoji :%s: · /emoji pending to review it"),
    "emoji.pending_list": ("Emojis pendientes (%s):", "Pending emojis (%s):"),
    "emoji.approve_button": ("[Aprobar]", "[Approve]"),
    "emoji.deny_button": ("[Rechazar]", "[Deny]"),
    "emoji.approved": ("Emoji %s de %s aprobado.", "Emoji %s by %s approved."),
    "emoji.denied": ("Emoji %s rechazado.", "Emoji %s denied."),
    "emoji.your_approved": ("¡Tu emoji %s ya está disponible!", "Your emoji %s is now available!"),
    "emoji.your_denied": ("Tu emoji %s no fue aprobado.", "Your emoji %s was not approved."),
    "emoji.removed": ("Emoji quitado.", "Emoji removed."),
    "emoji.enabled": ("Emoji activado de nuevo.", "Emoji enabled again."),
    "emoji.alias_added": ("Alias añadido.", "Alias added."),
    "emoji.ranks_set": ("Rangos del emoji actualizados.", "Emoji ranks updated."),
    "emoji.error.ok": ("Hecho.", "Done."),
    "emoji.error.pending": ("Pendiente de aprobar.", "Pending approval."),
    "emoji.error.bad_name": ("Nombre no válido: usa a-z, 0-9 y _ (de 2 a 32).",
                             "Invalid name: use a-z, 0-9 and _ (2 to 32)."),
    "emoji.error.taken": ("Ese nombre ya está en uso.", "That name is already taken."),
    "emoji.error.bad_image": ("No es una imagen PNG válida (máx. %s px).", "Not a valid PNG image (max %s px)."),
    "emoji.error.too_big": ("La imagen pesa demasiado (máx. %s px y 30 KB).", "The image is too large (max %s px and 30 KB)."),
    "emoji.error.limit": ("Se alcanzó el máximo de emojis del servidor.", "The server emoji limit was reached."),
    "emoji.error.not_found": ("Ese emoji no existe.", "That emoji doesn't exist."),

    "pack.required": ("Este servidor necesita el pack de emojis del chat.", "This server requires the chat emoji pack."),
    "pack.exported": ("Pack guardado en %s (sha1 %s). Súbelo y pon su URL en resourcePack.externalUrl.",
                      "Pack saved to %s (sha1 %s). Upload it and set its URL in resourcePack.externalUrl."),

    "shot.sent": ("%s%s envió una captura", "%s%s sent a screenshot"),
    "shot.view": ("[Ver]", "[View]"),
    "shot.hover": ("Clic para verla", "Click to view it"),
    "shot.hover_vanilla": ("Instala Chaos ChatPlus para ver capturas", "Install Chaos ChatPlus to view screenshots"),
    "shot.need_mod": ("Para ver capturas necesitas el mod Chaos ChatPlus.",
                      "You need the Chaos ChatPlus mod to view screenshots."),
    "shot.no_permission": ("No puedes enviar capturas en este servidor.", "You can't send screenshots on this server."),
    "shot.too_big": ("La captura pesa demasiado (máx. %s KB).", "The screenshot is too large (max %s KB)."),
    "shot.invalid": ("La captura no es válida.", "The screenshot is not valid."),
    "shot.unverified": ("No se pudo verificar la captura; inténtalo más tarde.",
                        "The screenshot couldn't be verified; try again later."),
    "shot.rejected": ("La captura fue bloqueada por el filtro de contenido.",
                      "The screenshot was blocked by the content filter."),
    "shot.sending": ("Enviando captura…", "Sending screenshot…"),
    "shot.send_title": ("¿Enviar esta captura al chat?", "Send this screenshot to chat?"),
    "shot.send_note": ("Solo se pueden enviar capturas del juego. Los reportes llegan a los OPs.",
                       "Only game screenshots can be sent. Reports go to the OPs."),
    "shot.send": ("Enviar", "Send"),
    "shot.send_to_chat": ("[Enviar al chat]", "[Send to chat]"),
    "shot.send_hover": ("Mandar esta captura al chat del servidor", "Send this screenshot to the server chat"),
    "shot.viewer_title": ("Captura", "Screenshot"),
    "shot.by": ("Captura de %s", "Screenshot by %s"),
    "shot.loading": ("Cargando…", "Loading…"),
    "shot.gone": ("Esta captura ya no está disponible.", "This screenshot is no longer available."),
    "shot.reveal": ("Clic para mostrar", "Click to show"),
    "shot.save": ("Guardar", "Save"),
    "shot.saved": ("Captura guardada: %s", "Screenshot saved: %s"),
    "shot.report": ("Reportar", "Report"),
    "shot.reported": ("Gracias, los OPs revisarán la captura.", "Thanks, the OPs will review the screenshot."),
    "shot.report_staff": ("⚑ %s reportó una captura de %s (%s) · clic para revisarla",
                          "⚑ %s reported a screenshot by %s (%s) · click to review it"),
    "shot.deleted": ("Captura %s borrada.", "Screenshot %s deleted."),
    "shot.not_found": ("No hay ninguna captura %s.", "There is no screenshot %s."),
    "shot.list": ("Capturas activas (%s):", "Active screenshots (%s):"),
}

SPANISH = ["es_es", "es_mx", "es_ar", "es_cl", "es_ec", "es_uy", "es_ve"]


def write(code, index):
    data = {"chaoschatplus." + k: v[index] for k, v in TEXTS.items()}
    with open(os.path.join(LANG, code + ".json"), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent="\t")
        f.write("\n")


def main():
    os.makedirs(LANG, exist_ok=True)
    for code in SPANISH:
        write(code, 0)
    write("en_us", 1)
    print(f"{len(TEXTS)} textos en {len(SPANISH) + 1} idiomas")


if __name__ == "__main__":
    main()
