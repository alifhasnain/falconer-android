# R8 generally rewrites ServiceLoader.load into a direct constructor call, so
# this is belt-and-suspenders: if a build ever can't find the engine, keep the
# no-arg constructor of any FalconerEngine implementation (RELEASE_STRIPPING.md §3.6).
-keep class * implements dev.alifhasnain.falconer.engine.FalconerEngine { <init>(); }
