# Belt-and-suspenders for the ServiceLoader-discovered engine in the minified
# release build (same rule :falconer-impl ships as a consumer rule). R8 usually
# rewrites ServiceLoader.load to a direct constructor call, but keep the no-arg
# ctor of any implementation in case it doesn't.
-keep class * implements dev.alifhasnain.falconer.engine.FalconerEngine { <init>(); }
