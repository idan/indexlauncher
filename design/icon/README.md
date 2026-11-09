# App icon

The Index Launcher icon is generated, not drawn by hand. `build.mjs` holds every parameter and writes:

| Output | Used for |
| --- | --- |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Adaptive icon background (purple gradient) |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Adaptive icon foreground, also the themed-icon monochrome layer |
| `web/src/lib/assets/icon.svg` | Website favicon and header logo |
| `design/icon/preview.html` | Comparison sheet (not committed) |

`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` wires the two layers together and isn't generated.

## Iterating

```sh
cd design/icon
npm install
npm run build    # rewrites the files above
open preview.html
```

To explore without changing the shipped icon, add entries to `CANDIDATES` in `build.mjs`. Each spreads `ICON` and overrides a few values, and appears as its own row in `preview.html`. Once you've picked one, copy its values into `ICON` and rebuild.

The first build downloads Nunito's variable font from a pinned `google/fonts` commit into `.cache/` and checks its hash, so the letter outlines stay identical between builds.

## The design

A downward arrow beside a stacked "A" over "Z": an alphabetical list, read top to bottom.

- **Letters:** Nunito ExtraBold (800), the website's typeface, converted to outlines so Android needs no font.
- **Arrow:** a round-capped stroke as thick as the letters' stems. It runs in from the top edge of the icon and fades from 15% opacity at the top of the visible area to 90% at its tip.
- **Spacing:** the Z sits 3.5dp closer to the A than a plain stack would put it, the letter pair is centred vertically, and the arrow tip extends 3.5dp below the Z's baseline.
- **Background:** the same `#8B5CF6` → `#3730A3` diagonal gradient as the original layered icon.
- **Fit:** everything except the deliberately cropped arrow shaft stays inside Android's 66dp safe zone, so every launcher mask, including the launcher's own circle, squircle, and rounded square, shows the whole mark.

### How it got here

1. Mark modelled on a "sort A→Z" glyph, at 800 rather than 700 or 900 weight.
2. Arrowhead arms shortened twice (to 60% of the first draft) and the gap to the letters tightened.
3. Arrow faded instead of solid. Variants starting at the top of the A (30% → 80%) and at the top edge (15% → 80%) were compared; the edge version won.
4. Z raised toward the A with the arrow peeking below it, then the letters centred and the arrow lengthened to keep the peek.

### Known trade-offs

- Below about 32px the faint top of the arrow almost disappears and the letters carry the icon. That's fine at launcher sizes (40dp and up) but makes the 16px favicon less distinctive.
- Themed icons keep the arrow's transparency, so it fades there too.
