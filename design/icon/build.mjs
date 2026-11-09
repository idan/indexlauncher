// Generates the Index Launcher icon for Android and the website, plus a preview sheet.
//
//   cd design/icon && npm install && npm run build
//
// Outputs:
//   app/src/main/res/drawable/ic_launcher_background.xml
//   app/src/main/res/drawable/ic_launcher_foreground.xml  (also the themed-icon monochrome layer)
//   web/src/lib/assets/icon.svg                           (favicon and header logo)
//   design/icon/preview.html                              (not committed; open in a browser)

import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import * as fontkit from 'fontkit';
import { androidBackground, androidForeground, layout, previewHtml, webIcon } from './lib.mjs';

/** Background gradient, top-left to bottom-right. */
const GRADIENT = ['#8B5CF6', '#3730A3'];

/**
 * The shipped icon: a downward arrow beside a stacked "A" over "Z".
 * Lengths are dp on the 108dp adaptive-icon canvas; ratios are shares of markHeight.
 */
export const ICON = {
	weight: 800, // Nunito ExtraBold
	markHeight: 42, // top of the A to the Z's baseline, before the zLift/centerLetters/peek tweaks
	capRatio: 0.39, // cap height of each letter
	headRatio: 0.126, // how far each arrowhead arm reaches sideways (and up) from the tip
	gapRatio: 0.125, // space between the arrowhead and the letters
	zLift: 3.5, // move the Z this far toward the A
	centerLetters: true, // then centre the A/Z pair vertically on the canvas
	peek: 3.5, // the arrow tip ends this far below the Z's baseline
	arrow: {
		fromEdge: true, // the shaft runs off the top of the icon instead of starting at the A
		fadeStartY: 18, // where the fade starts when fromEdge: the top of the visible 72dp area
		from: 0.15, // opacity at the top
		to: 0.9, // opacity at the tip
	},
};

/**
 * Alternatives to compare in preview.html. Spread ICON and override what you're exploring;
 * the shipped files only ever use ICON.
 */
const CANDIDATES = [
	// { name: 'Arrow from the letters', ...ICON, zLift: 0, centerLetters: false, peek: 0,
	//   arrow: { fromEdge: false, from: 0.3, to: 0.8 } },
];

// Nunito variable font, pinned to a google/fonts commit so the outlines never shift.
const FONT_URL =
	'https://raw.githubusercontent.com/google/fonts/604936664fd62c14271209b51f98e7f495dd1a3e/ofl/nunito/Nunito%5Bwght%5D.ttf';
const FONT_SHA256 = 'bb55a5ca5c2042335b3991af27c4d0705d0ef41cac6164ac737fd8f2a1e85207';

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '../..');

async function loadFont() {
	const path = join(here, '.cache/Nunito[wght].ttf');
	if (!existsSync(path)) {
		const response = await fetch(FONT_URL);
		if (!response.ok) throw new Error(`Couldn't download Nunito: ${response.status}`);
		mkdirSync(dirname(path), { recursive: true });
		writeFileSync(path, Buffer.from(await response.arrayBuffer()));
	}
	const hash = createHash('sha256').update(readFileSync(path)).digest('hex');
	if (hash !== FONT_SHA256) throw new Error(`Unexpected Nunito font file (sha256 ${hash}). Delete ${path} and retry.`);
	return fontkit.openSync(path);
}

const font = await loadFont();
const shipped = layout(font, ICON);

const outputs = {
	'app/src/main/res/drawable/ic_launcher_background.xml': androidBackground(GRADIENT),
	'app/src/main/res/drawable/ic_launcher_foreground.xml': androidForeground(shipped),
	'web/src/lib/assets/icon.svg': webIcon(shipped, GRADIENT),
	'design/icon/preview.html': previewHtml(
		[{ name: 'Shipped', layout: shipped }, ...CANDIDATES.map((c) => ({ name: c.name, layout: layout(font, c) }))],
		GRADIENT,
	),
};
for (const [file, contents] of Object.entries(outputs)) {
	writeFileSync(join(root, file), contents);
	console.log(`wrote ${file}`);
}
