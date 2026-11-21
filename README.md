# BastionMCRewards 1.0.0 — BASTION MC

Plugin independiente de recompensas para **Purpur 1.21.11 / Java 21**.

## Funciones
- `/recompensas` abre el menú principal con recompensas principales y progresivas.
- `/recompensas principales` y `/recompensas progresivas` abren directamente cada sección.
- `/recompensas reload` (OP o permiso `bastionrewards.admin`) recarga `config.yml` y `progressive.yml`.
- Las 7 categorías diarias independientes corresponden en orden a `default`, `magma`, `piglin`, `carmesi`, `basalto`, `vikingo`, `bastion`.
- Progresivas 1 a 100: un día por cada 24 h, en orden; si pasan más de 48 h desde el último reclamo sin reclamar, la racha se reinicia al abrirla/reclamar. Estos tiempos son configurables.
- Todas las recompensas empiezan **vacías**: no se pueden reclamar hasta agregarles premios.
- Premios configurables: comandos de consola e ítems simples (material/cantidad). Si incluyes comandos externos, el plugin no puede garantizar que su ejecución tenga éxito. Prueba en servidor de prueba.
- Datos de reclamación persistentes en `players.yml`. **Respalda ese archivo**.
- Configura menús, slots, materiales, nombres, lore, sonidos, mensajes y colores en `config.yml`, y premios de los 100 días en `progressive.yml`.
- Formatos: `&a`, `&l`, `&#RRGGBB`, gradientes construidos carácter por carácter y `<gradient:#FF0000:#FFFF00>Texto</gradient>`.

## Importante: permisos VIP
Los rangos se habilitan por permisos Bukkit/LuckPerms, no solo por el nombre del grupo. Para dar acceso a MAGMA:
```
lp group magma permission set bastionrewards.rank.magma true
```
Haz lo mismo con `piglin`, `carmesi`, `basalto`, `vikingo` y `bastion`. DEFAULT siempre es accesible. Si los grupos más altos deben heredar recompensas inferiores, configura la herencia en LuckPerms o concede sus respectivos permisos.

## Ejemplo de premio diario del rango MAGMA
En `config.yml`, en `ranks.magma`, cambia:
```yaml
commands:
  - 'eco give %player% 150'
items:
  - material: DIAMOND
    amount: 2
```

## Ejemplo día 30 progresivo
En `progressive.yml`:
```yaml
days:
  '30':
    material: GOLD_BLOCK
    name: '&#FFD15B&lDía #30'
    lore: ['&7Premio por tu constancia', '%status%']
    commands:
      - 'eco give %player% 1000'
    items:
      - material: DIAMOND
        amount: 4
```

## Compilar en GitHub Actions
Sube **el contenido** de este ZIP a la raíz de un repositorio GitHub nuevo. En la pestaña Actions abre `Build BastionMCRewards`, `Run workflow` y descarga el artefacto `BastionMCRewards-JAR`. Instala el `.jar` en `plugins/` y reinicia. No requiere dependencia dura; LuckPerms sí para manejar permisos cómodamente.

## Notas
- Los cambios a menús y recompensas mediante `/recompensas reload` no eliminan el historial de reclamos.
- Si se apaga el servidor durante un comando externo, la ejecución de comandos no está transaccionada; haz una copia de seguridad de `players.yml`.
- La entrega de ítems simples comprueba espacio en inventario antes de otorgar premios. Para ítems especiales o personalizados, usa comandos de consola configurables de los plugins que los suministran.
- No se ha probado en tu servidor real. Prueba primero en un entorno de desarrollo antes de habilitar recompensas valiosas.

## 1.0.1 — Protección de los menús, brillo y cabezas
- Todos los inventarios de /recompensas usan un InventoryHolder exclusivo.
- Clics, shift-click, teclas numéricas, intercambio con offhand y arrastres se cancelan en los inventarios del plugin, para usuarios normales y OP.
- El brillo aparece automáticamente en los premios reclamables que tengan recompensas configuradas (`glow-when-claimable: true`). Puedes forzar brillo con `glow: true` para ítems de menú.
- Sustituye cualquier icono con `material: PLAYER_HEAD` y `head-owner: NombreDelJugador` o `head-uuid: UUID`. Sirve en `config.yml` y días individuales de `progressive.yml`.
- Si ya tienes `config.yml` personalizado, no lo borres: las nuevas opciones son opcionales y tienen valores predeterminados.
