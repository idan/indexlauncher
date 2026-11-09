<script lang="ts">
	import Carousel, { type Reason } from '#lib/Carousel.svelte';
	import icon from '#lib/assets/icon.svg';
	import obtainiumIcon from '#lib/assets/obtainium.svg';

	const repo = 'https://github.com/idan/indexlauncher';
	const releases = `${repo}/releases`;
	const obtainium =
		'https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%20%22app.indexlauncher.debug%22%2C%20%22url%22%3A%20%22https%3A%2F%2Fgithub.com%2Fidan%2Findexlauncher%22%2C%20%22author%22%3A%20%22Idan%20Gazit%22%2C%20%22name%22%3A%20%22Index%20Launcher%22%7D';

	const title = 'Index Launcher';
	const description =
		'A fast, minimal list launcher for Android: a few favorites, an alphabetical index, and optional widget pages.';

	const reasons: Reason[] = [
		{ ending: 'speed', copy: 'Open any app in your library without endless scrolling or folders.' },
		{
			ending: 'simplicity',
			copy: 'Just favorite apps and widgets. Alphabetical index on either side.'
		},
		{
			ending: 'a few widgets',
			copy: 'Add pages of widgets to the left.'
		},
		{ ending: 'privacy', copy: 'No analytics, no tracking, and no permissions to grant.' },
		{
			ending: 'foldables',
			copy: 'Unfold to see widget pages on the left. Apps stay on the right.'
		},
		{ ending: 'open-source', copy: 'Free and MIT-licensed on GitHub.' },
		{
			ending: 'one-handed usage',
			copy: 'The app list runs down the edge, right under your thumb. Works on the lefthand side, too!'
		},
		{
			ending: 'alphabets',
			copy: 'Scrub the A–Z rail to any letter. Best-effort handling of app names written in other scripts.'
		},
		{
			ending: 'a few favorite apps',
			copy: 'Keep the apps you use every day in your favorites, touch and hold to reorder them.'
		},
		{
			ending: 'lists, not app grids',
			copy: 'A jumble of icons was never fun to navigate.'
		}
	];

	const details = [
		{
			title: 'Work profiles',
			copy: 'Work apps sit beside personal ones, badged, with favorites of their own.'
		},
		{ title: 'Search on demand', copy: 'One tap away in all apps, and never on your home screen.' },
		{
			title: 'Your wallpaper',
			copy: 'Static or live, shown in full, with your choice of icon shape.'
		},
		{ title: 'A gentle tick', copy: 'A subtle haptic tap for each letter as your thumb scrubs.' }
	];
</script>

<svelte:head>
	<title>{title}: an Android launcher for people who appreciate speed</title>
	<meta name="description" content={description} />
	<meta property="og:type" content="website" />
	<meta property="og:title" content={title} />
	<meta property="og:description" content={description} />
	<meta name="twitter:card" content="summary" />
</svelte:head>

<div class="page">
	<header>
		<span class="brand"><img src={icon} alt="" width="28" height="28" />Index Launcher</span>
		<a href={repo}>GitHub</a>
	</header>

	<main>
		<section class="hero">
			<h1>An Android launcher for people who appreciate</h1>
			<Carousel {reasons} />

			<div class="actions">
				<a class="button" href={obtainium}>
					<img src={obtainiumIcon} alt="" width="22" height="22" />Get it on Obtainium
				</a>
				<a href={releases}>Releases on GitHub</a>
			</div>
			<p class="note">Free and open source. Requires Android 15 or later.</p>
		</section>

		<section class="details" aria-labelledby="details-title">
			<h2 id="details-title">Also</h2>
			<dl>
				{#each details as detail (detail.title)}
					<div>
						<dt>{detail.title}</dt>
						<dd>{detail.copy}</dd>
					</div>
				{/each}
			</dl>
		</section>
	</main>

	<footer>
		<p>Made by <a href="https://github.com/idan">Idan Gazit</a>. No analytics here, either.</p>
	</footer>
</div>

<style>
	.page {
		display: flex;
		flex-direction: column;
		max-width: var(--measure);
		min-height: 100svh;
		margin: 0 auto;
		padding: 0 var(--gutter);
	}

	header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding-block: clamp(1.5rem, 4vw, 2.5rem);
	}

	.brand {
		display: inline-flex;
		align-items: center;
		gap: 0.625rem;
		font-weight: 800;
		letter-spacing: -0.01em;
	}

	.brand img {
		border-radius: 7px;
	}

	header a {
		color: var(--muted);
		font-weight: 650;
		text-decoration: none;
		transition: color 0.2s;
	}

	header a:hover {
		color: var(--ink);
	}

	.hero {
		padding-block: clamp(3.5rem, 15vh, 10rem) clamp(4.5rem, 14vh, 9rem);
	}

	h1 {
		max-width: 15em;
		font-size: var(--display);
		font-weight: 300;
		line-height: 1.08;
		letter-spacing: -0.025em;
		text-wrap: balance;
	}

	.actions {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 1rem 1.75rem;
		margin-top: clamp(2.5rem, 7vh, 4rem);
		font-weight: 750;
	}

	.button {
		display: inline-flex;
		align-items: center;
		gap: 0.625em;
		padding: 0.5em 1.6em 0.5em 0.5em;
		border-radius: 999px;
		background: var(--ink);
		color: var(--bg);
		text-decoration: none;
		transition: background-color 0.2s;
	}

	.button:hover {
		background: var(--accent);
	}

	/* Obtainium's mark on white, like its launcher icon, so it reads on any button color. */
	.button img {
		width: 2.15em;
		height: 2.15em;
		padding: 0.45em;
		border-radius: 50%;
		background: #fff;
	}

	.actions a:not(.button) {
		text-decoration-color: var(--faint);
		transition: text-decoration-color 0.2s;
	}

	.actions a:not(.button):hover {
		text-decoration-color: currentColor;
	}

	.note {
		margin-top: 1.25rem;
		color: var(--muted);
		font-size: 0.9375rem;
		text-wrap: balance;
	}

	.details {
		padding-block: clamp(3rem, 8vw, 5.5rem);
		border-top: 1px solid var(--faint);
	}

	h2 {
		margin-bottom: clamp(1.75rem, 4vw, 2.75rem);
		color: var(--muted);
		font-size: 0.8125rem;
		font-weight: 800;
		letter-spacing: 0.14em;
		text-transform: uppercase;
	}

	dl {
		display: grid;
		gap: 2.25rem 2.5rem;
	}

	@media (min-width: 40rem) {
		dl {
			grid-template-columns: repeat(2, 1fr);
		}
	}

	@media (min-width: 64rem) {
		dl {
			grid-template-columns: repeat(4, 1fr);
		}
	}

	dt {
		margin-bottom: 0.375rem;
		font-size: 1.125rem;
		font-weight: 800;
	}

	dd {
		color: var(--muted);
		text-wrap: pretty;
	}

	footer {
		margin-top: auto;
		padding-block: 2.5rem;
		border-top: 1px solid var(--faint);
		color: var(--muted);
		font-size: 0.9375rem;
	}

	footer a {
		text-decoration-color: var(--faint);
	}

	footer a:hover {
		text-decoration-color: currentColor;
	}
</style>
