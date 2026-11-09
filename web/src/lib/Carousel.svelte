<script lang="ts" module>
	export type Reason = { ending: string; copy: string };
</script>

<script lang="ts">
	import { onMount } from 'svelte';

	let { reasons, interval = 4000 }: { reasons: Reason[]; interval?: number } = $props();

	let index = $state(0);
	let previous = $state<number | null>(null);
	let paused = $state(false);
	let ready = $state(false);

	// The progress bar's CSS animation is the timer, so pausing it pauses the carousel and
	// resuming picks up mid-dash. Before hydration, the first reason stays put.
	const running = $derived(ready && !paused);

	onMount(() => {
		ready = true;
	});

	function show(i: number) {
		if (i === index) return;
		previous = index;
		index = i;
	}
</script>

<div
	class="carousel"
	style:--interval="{interval}ms"
	role="group"
	aria-roledescription="carousel"
	aria-label="What people appreciate"
>
	<div class="slides">
		{#each reasons as reason, i (reason.ending)}
			<div
				class="slide"
				class:current={i === index}
				class:previous={i === previous}
				aria-hidden={i !== index}
			>
				<p class="ending">{reason.ending}.</p>
				<p class="copy">{reason.copy}</p>
			</div>
		{/each}
	</div>

	<div class="controls">
		<div class="dashes" class:running role="group" aria-label="Reasons">
			{#each reasons as reason, i (reason.ending)}
				<button
					type="button"
					class:current={i === index}
					aria-label={reason.ending}
					aria-current={i === index ? 'true' : undefined}
					onclick={() => show(i)}
				>
					<span class="track">
						<span
							class="fill"
							onanimationend={() => {
								if (i === index) show((index + 1) % reasons.length);
							}}
						></span>
					</span>
				</button>
			{/each}
		</div>

		<button
			type="button"
			class="toggle"
			aria-label={paused ? 'Play' : 'Pause'}
			onclick={() => (paused = !paused)}
		>
			<svg viewBox="0 0 12 12" aria-hidden="true">
				{#if paused}
					<path
						d="M3.5 2.25v7.5L10 6z"
						stroke="currentColor"
						stroke-width="1.5"
						stroke-linejoin="round"
					/>
				{:else}
					<rect x="2.25" y="1.5" width="2.5" height="9" rx="0.75" />
					<rect x="7.25" y="1.5" width="2.5" height="9" rx="0.75" />
				{/if}
			</svg>
		</button>
	</div>
</div>

<style>
	.carousel {
		/* The selected dash's track; the play/pause icon shares it. */
		--selected: color-mix(in srgb, var(--accent) 40%, transparent);
	}

	.slides {
		display: grid;
	}

	/* Every slide shares one grid cell, so the block is always as tall as the longest
	   reason and nothing below it shifts as they change. */
	.slide {
		grid-area: 1 / 1;
		pointer-events: none;
		user-select: none;
	}

	.slide.current {
		pointer-events: auto;
		user-select: auto;
	}

	.ending,
	.copy {
		opacity: 0;
		transition:
			opacity 0.35s ease-in,
			transform 0.35s ease-in,
			filter 0.35s ease-in;
	}

	.ending {
		font-size: var(--display);
		font-weight: 800;
		line-height: 1.08;
		letter-spacing: -0.025em;
		color: var(--accent);
		text-wrap: balance;
		transform: translateY(0.3em);
		filter: blur(0.08em);
	}

	.copy {
		max-width: 51ch;
		margin-top: clamp(1rem, 2.5vw, 1.75rem);
		font-size: clamp(1.125rem, 1rem + 0.5vw, 1.375rem);
		line-height: 1.45;
		color: var(--muted);
		text-wrap: pretty;
		transform: translateY(0.6em);
		filter: blur(4px);
	}

	.previous .ending,
	.previous .copy {
		transform: translateY(-0.2em);
	}

	.current .ending,
	.current .copy {
		opacity: 1;
		transform: none;
		filter: none;
		transition:
			opacity 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.15s,
			transform 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.15s,
			filter 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.15s;
	}

	.current .copy {
		transition-delay: 0.27s;
	}

	.controls {
		display: flex;
		align-items: center;
		gap: 0.25rem;
		margin-top: clamp(1.5rem, 4vw, 2.25rem);
	}

	.dashes {
		display: flex;
		flex-wrap: wrap;
		gap: 0.375rem;
	}

	button {
		appearance: none;
		margin: 0;
		padding: 0.75rem 0;
		border: 0;
		background: none;
		cursor: pointer;
	}

	.track {
		display: block;
		width: 0.875rem;
		height: 3px;
		overflow: hidden;
		border-radius: 3px;
		background: var(--faint);
		transition:
			width 0.4s cubic-bezier(0.16, 1, 0.3, 1),
			background-color 0.2s;
	}

	.toggle {
		display: grid;
		place-items: center;
		width: 1.75rem;
		height: 1.75rem;
		padding: 0;
		border-radius: 50%;
		color: var(--selected);
		transition: color 0.2s;
	}

	.toggle svg {
		width: 0.75rem;
		height: 0.75rem;
		fill: currentColor;
	}

	/* Touch screens leave the last tapped element in :hover, so only real pointers get it. */
	@media (hover: hover) {
		button:hover .track {
			background: var(--muted);
		}

		.toggle:hover {
			color: var(--accent);
		}
	}

	/* Tinted so the selected dash reads as selected even when paused before its progress
	   fill has started. */
	.current .track,
	.current:hover .track {
		width: 2.25rem;
		background: var(--selected);
	}

	.fill {
		display: block;
		height: 100%;
		background: var(--accent);
		transform: scaleX(0);
		transform-origin: left;
	}

	.current .fill {
		animation: progress var(--interval) linear forwards paused;
	}

	.running .current .fill {
		animation-play-state: running;
	}

	@keyframes progress {
		to {
			transform: scaleX(1);
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.ending,
		.copy,
		.previous .ending,
		.previous .copy {
			transform: none;
			filter: none;
		}
	}
</style>
