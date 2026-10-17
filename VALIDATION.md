# Validación de Chaos ChatPlus 1.1.0 — Minecraft 1.21.1

Fecha: 17 de octubre de 2026.

| Comprobación | Fabric | Forge | NeoForge |
| --- | --- | --- | --- |
| Compilación y empaquetado | Correcto | Correcto | Correcto |
| Pruebas automáticas | 31, sin fallos | 31, sin fallos | 31, sin fallos |
| Inspección de JAR y metadatos | Correcto | Correcto | Correcto |
| Arranque y cierre dedicado con esta adaptación | No ejecutado | Correcto | Correcto |
| Cliente gráfico y multijugador real con estos cambios | Pendiente | Pendiente | Pendiente |

Los servidores nativos cargaron los 197 emojis y 360 GIFs integrados y cerraron guardando sus mundos de prueba. Los JAR nativos contienen sus adaptadores y metadatos de cargador y no incluyen fabric.mod.json. Forge incluye la dependencia MixinExtras. Los seis JAR de distribución no contienen entradas ZIP duplicadas; el código compilado requiere Java 21.

## Reproducción

Usa JDK 25 para ejecutar Gradle/Loom y ten instalado JDK 21 para las herramientas de Forge y NeoForge; el mod compilado funciona sobre Java 21.

```text
./gradlew build
./gradlew -p platforms/forge build
./gradlew -p platforms/neoforge build
```

Los resultados JUnit quedan bajo `build/test-results/test` de cada proyecto. Las pruebas cubren imágenes y GIFs inválidos, límites de subidas, configuración, ensamblado de red, archivos de fuentes, permisos, capturas privadas, cachés e integridad de recursos. No sustituyen una prueba de instalación con jugadores.

La prueba dedicada de cada cargador nativo se realizó en un directorio aislado, comprobando el mensaje de servidor listo, la carga de recursos y el guardado al parar. Los mundos del usuario no se usaron en estas pruebas. La prueba de Forge usa la unión de conjuntos de fuentes configurada en gradle.properties.

## Límites de esta publicación

Esta es una beta. No se ha confirmado con los cambios actuales la representación gráfica, la comunicación entre jugadores ni la integración opcional con Chaos Ranks en cada cargador. Las tres ediciones necesitan servidor y clientes del mismo cargador; no se declara compatibilidad de red entre cargadores.
