# Fork maintenance

This document describes the small, reviewable maintenance loop for the
WatermelonDS Enhanced fork. It does not change Kotlin, C++, or enhancement
payloads.

## Reference model

- `origin`: `https://github.com/joeblack2k/WatermelonDS-Enhanced.git`
- `upstream`: `https://github.com/SapphireRhodonite/melonDS-android.git`
- `base`: `Fet_OfflineChevos@1e0a463d785448ab47f78a12e2a88241df288cf9`
- `origin/PR head`: `codex/watermelon-enhanced-pr@c68734fe226b70d60b20908b2af191293f084c7a`
- `local candidate tree`: `b9385a48402dbabc453c8b290ab5c73151b2b5a8` plus the complete uncommitted candidate source diff, including the two untracked source files
- `PR`: `#1`, open and draft
- `melonDS-android-lib` submodule URL:
  `https://github.com/joeblack2k/melonDS-android-lib.git`
- `melonDS-android-lib` submodule public branch:
  `codex/slot2-camera-contract`
- baseline `melonDS-android-lib` gitlink:
  `e4022d7e7ada535ff5ea9d087db389cc46e8da62`

The gitlink is the submodule commit recorded by the superproject. The public
repository is `joeblack2k/melonDS-android-lib`, and its public branch above
currently exposes `e4022d7e`; it is the branch configured in `.gitmodules`.
The branch setting is the intended source branch for a bounded update; it does not make
the superproject follow that branch automatically.

## Bounded update procedure

1. Start from a clean working tree and verify the repository root and current
   commit. Work only in the intended checkout.
2. Fetch `origin` and `upstream`, then inspect the proposed upstream range.
   Do not merge or rebase unrelated changes.
3. Update only the `melonDS-android-lib` submodule to a reviewed commit from
   `codex/slot2-camera-contract`. Record the old and new gitlinks before
   staging.
4. Inspect the submodule diff and confirm that the change is limited to the
   requested library revision. Do not edit Kotlin, C++, or payload files as
   part of this procedure.
5. Run the smallest applicable repository checks, including submodule
   consistency checks. Stop if the reviewed commit is unavailable, the
   submodule diff exceeds scope, or checks fail.
6. Review the superproject diff. The bounded update consists of the intended
   gitlink and its matching maintenance note only; leave all other work for a
   separate change.
7. Commit and push only when explicitly requested. This procedure itself does
   not authorize either operation.

Use repository-relative paths and normal Git remotes in notes and review
messages. Do not record user-specific host paths. A tracked `/private`
placeholder is repository text and does not identify a user's host.

The public branch and gitlink can be checked without changing repository state:

```sh
git ls-remote https://github.com/joeblack2k/melonDS-android-lib.git \
  refs/heads/codex/slot2-camera-contract
```

That command must return
`e4022d7e7ada535ff5ea9d087db389cc46e8da62`. A fresh-clone check is:

```sh
tmp="$(mktemp -d)"
git clone --branch codex/watermelon-enhanced-pr \
  https://github.com/joeblack2k/WatermelonDS-Enhanced.git "$tmp/repo"
git -C "$tmp/repo" submodule update --init --checkout melonDS-android-lib
test "$(git -C "$tmp/repo/melonDS-android-lib" rev-parse HEAD)" = \
  e4022d7e7ada535ff5ea9d087db389cc46e8da62
rm -rf "$tmp"
```

The other initialized and clean submodules are pinned at:

- `app/src/main/cpp/enet`: `2662c0de09e36f2a2030ccc2c528a3e4c9e8138a`
- `app/src/main/cpp/faad2`: `2653c918d788c5dc83826c97aaa4de4dd8931a8b`
- `app/src/main/cpp/oboe`: `a81bb9f87d4105b84b682685d3bfbb5beca371d1`

The M3 batch does not commit or push. APKs, build outputs, ROMs, credentials,
and captures remain outside Git. The complete uncommitted candidate source
diff, including the two untracked source files above, is part of the evidence
scope.

The current lint evidence rerun was `2026-08-12 12:38:21 +0200` using
`./gradlew lintGitHubProdDebug --no-daemon`: 635 errors, 574 warnings, and
1 hint. This is a checkout-wide candidate snapshot, not a comparison against
the base; the counts are not being classified against the earlier 630/573
result. The first failure is `DeviceLayoutDisplayMapper.kt:20`
(`WrongConstant` for `createWindowContext`). The seven enhancement
`MissingTranslation` annotations leave zero
enhancement-owned `MissingTranslation` findings. One enhancement-owned
`PluralsCandidate` warning remains for `enhancement_import_success`; it was
left unsuppressed because the resource is a clear pluralization candidate.
These are checkout-wide counts, not a claim that every finding is baseline.
The external log SHA-256 is
`11e3ba3fa7b1ca291a34c816d8a2204619d91ea1e74ec3064ee19756ebdf3900`.
The result does not close runtime or device gates. The candidate APK SHA-256
is `37b32ac828008e0c4fcc7b411f5fe06f8a268ec78ac4ff4cefa78155091e740c`.
