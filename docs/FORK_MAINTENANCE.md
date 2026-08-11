# Fork maintenance

This document describes the small, reviewable maintenance loop for the
WatermelonDS Enhanced fork. It does not change Kotlin, C++, or enhancement
payloads.

## Reference model

- `origin`: `https://github.com/joeblack2k/WatermelonDS-Enhanced.git`
- `upstream`: `https://github.com/SapphireRhodonite/melonDS-android.git`
- `base`: `Fet_OfflineChevos@1e0a463d785448ab47f78a12e2a88241df288cf9`
- `head`: `codex/watermelon-enhanced-pr@a5d59602c7f48c985c78e85b91a7ed9ba30bcebe`
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
