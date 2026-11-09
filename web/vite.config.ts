import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [
		sveltekit({
			compilerOptions: {
				// Force runes mode for the project, except for libraries. Can be removed in svelte 6.
				runes: ({ filename }) =>
					filename.split(/[/\\]/).includes('node_modules') ? undefined : true
			},
			adapter: adapter(),
			paths: {
				// Empty for a root domain (Cloudflare, custom domain). Set BASE_PATH=/indexlauncher
				// when serving from a GitHub Pages project URL such as idan.github.io/indexlauncher.
				base: (process.env.BASE_PATH ?? '') as '' | `/${string}`
			}
		})
	]
});
