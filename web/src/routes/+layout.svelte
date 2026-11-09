<script lang="ts">
	import '@fontsource-variable/nunito';
	import nunito from '@fontsource-variable/nunito/files/nunito-latin-wght-normal.woff2?url';
	import icon from '#lib/assets/icon.svg';
	import '../app.css';
	import type { LayoutProps } from './$types';

	let { children }: LayoutProps = $props();
</script>

<svelte:head>
	<link rel="preload" href={nunito} as="font" type="font/woff2" crossorigin="anonymous" />
	<link rel="icon" href={icon} type="image/svg+xml" />
</svelte:head>

{@render children()}
