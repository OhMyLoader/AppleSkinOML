# AppleSkinOML

The AppleSkin mod (by squeek502, Unlicense) ported to
[OhMyLoader](https://github.com/OhMyLoader/OhMyLoader) — the food/hunger HUD overlays and the
server→client sync of saturation, exhaustion, and the natural-regeneration rule.

```sh
gradlew runClient        # a normal OML dev client; build with `gradlew build`
```

The project consumes `org.ohmyloader:*` through the OML Gradle plugin like any third-party mod
project; publish the loader to the local Maven repository first when its API changed (see
[CONTRIBUTING.md](CONTRIBUTING.md)).

## License

Upstream AppleSkin is Unlicense. This repository has no separate license file yet.
