#!/usr/bin/env bash
set -euo pipefail

: "${GITHUB_RUN_NUMBER:?Missing workflow run number}"
: "${GITHUB_SHA:?Missing commit SHA}"
apk=${1:?Usage: publish-dev-release.sh path/to/app-debug.apk}
tag="dev-${GITHUB_RUN_NUMBER}"
asset_name=$(basename "$apk")

if [[ ! -s "$apk" ]]; then
  echo "::error::APK does not exist or is empty: $apk"
  exit 1
fi

if release=$(gh release view "$tag" --json isDraft,targetCommitish,assets); then
  if ! jq -e --arg sha "$GITHUB_SHA" '.targetCommitish == $sha' <<< "$release" >/dev/null; then
    echo "::error::Release $tag already exists for a different commit."
    exit 1
  fi
  if jq -e '.isDraft == false' <<< "$release" >/dev/null; then
    if ! jq -e --arg name "$asset_name" \
      'any(.assets[]; .name == $name and .size > 0)' <<< "$release" >/dev/null; then
      echo "::error::Published release $tag is missing its APK; refusing to modify it."
      exit 1
    fi
    # In particular, do not overwrite its APK or demote a manually promoted release.
    echo "Release $tag is already published; leaving it unchanged."
    exit 0
  fi
else
  # Keep incomplete uploads private. A retry can resume this draft safely.
  gh release create "$tag" \
    --draft \
    --prerelease \
    --latest=false \
    --title "Development build ${GITHUB_RUN_NUMBER}" \
    --target "$GITHUB_SHA" \
    --notes "Development debug APK built from commit ${GITHUB_SHA}.

Android version code: ${GITHUB_RUN_NUMBER}.
Package: app.indexlauncher.debug. Signed with the persistent development key.

Install app-debug.apk to update an existing development-key-signed build in place.
Older runner-signed or locally signed builds require uninstalling first, which deletes app data.

Promoting this prerelease changes its GitHub release status only; it remains a debug APK."
fi

# Only unpublished drafts may have their asset replaced on a retry.
gh release upload "$tag" "$apk" --clobber
gh release edit "$tag" --draft=false --prerelease --latest=false
