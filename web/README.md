# Index Launcher website

A single static landing page built with [SvelteKit](https://svelte.dev/docs/kit) and prerendered by `@sveltejs/adapter-static`. The site, its dependencies, and its config all live in `web/`.

| What | Where |
| --- | --- |
| Copy: carousel reasons and the “Also” list | `src/routes/+page.svelte` |
| The rotating masthead (4-second interval) | `src/lib/Carousel.svelte` |
| Colors, type scale, and spacing tokens | `src/app.css` |
| Site config, including the base path | `vite.config.ts` |

Nunito is the only typeface. It is self-hosted from `@fontsource-variable/nunito` (the Google Fonts files, OFL-licensed), so visitors' browsers never contact Google.

## Develop

Requires Node 22.17 or later; `.nvmrc` pins 24.

```sh
cd web
npm install
npm run dev      # http://localhost:5173
npm run check    # type and Svelte checks
npm run build    # static site in web/build
npm run preview  # serve the build
```

## Deploy

The output in `build/` is plain HTML, CSS, JS, and fonts. It runs from a domain root by default. To serve it from a subpath, set `BASE_PATH` (for example, `/indexlauncher`) at build time.

### Cloudflare Pages

Connect the repository in **Workers & Pages → Create → Pages**, then use:

- **Root directory:** `web`
- **Build command:** `npm run build`
- **Build output directory:** `build`
- **Environment variable:** `NODE_VERSION=24`, if the build image doesn't pick up `.nvmrc`

To upload from your machine instead: `npm run build && npx wrangler pages deploy build --project-name index-launcher`.

### GitHub Pages

Under **Settings → Pages**, set the source to **GitHub Actions**. GitHub only runs workflows from the repository root, so the workflow is the one file that can't live here. Save the following as `.github/workflows/deploy-web.yml`:

```yaml
name: Deploy website

on:
  push:
    branches: [main]
    paths: ['web/**']
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: pages
  cancel-in-progress: false

jobs:
  build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: web
    steps:
      - uses: actions/checkout@v7
        with:
          persist-credentials: false
      - uses: actions/setup-node@v7
        with:
          node-version-file: web/.nvmrc
          cache: npm
          cache-dependency-path: web/package-lock.json
      - id: pages
        uses: actions/configure-pages@v6
      - run: npm ci
      - run: npm run build
        env:
          # "/indexlauncher" on idan.github.io; empty with a custom domain.
          BASE_PATH: ${{ steps.pages.outputs.base_path }}
      - uses: actions/upload-pages-artifact@v5
        with:
          path: web/build

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - id: deployment
        uses: actions/deploy-pages@v5
```

For a custom domain, add a `static/CNAME` file containing the domain. `static/.nojekyll` already stops GitHub from hiding the `_app` directory if you ever publish from a branch.
