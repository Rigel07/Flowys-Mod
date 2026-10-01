# Flowys (Forge 1.20.1)

Pequeñas flores con cara que vuelan por los campos. Domestícalas con un cubo de agua.

## Compilar con GitHub (sin instalar nada)
1. Crea un repositorio en GitHub y sube **todo el contenido** de este zip a la raíz (incluida la carpeta `.github`).
2. Ve a la pestaña **Actions**: el workflow *Build Flowys* se ejecuta solo en cada push (o dale a *Run workflow*).
3. Cuando termine (unos 5-10 min la primera vez), entra en la ejecución y descarga el artefacto **flowys-jar**: dentro está el `.jar`.
4. Para publicar una release con el .jar: crea una etiqueta, por ejemplo `v1.0.0`, y se adjunta automáticamente en *Releases*.

Copia el `.jar` a la carpeta `mods` de tu Minecraft 1.20.1 con Forge 47.x.

## Controles
- Cubo de agua sobre una Flowy salvaje -> la domestica.
- Clic derecho (domesticada): seguir / quedarse quieta.
- Agachado + clic derecho: abrir su inventario.
- Girasol o flor pequeña a dos domesticadas -> cría.
- `GIFT_INTERVAL` en `FlowyEntity.java` cambia el tiempo entre drops (5 min por defecto).
- Regenerar texturas: `python3 make_textures.py` (requiere Pillow).
