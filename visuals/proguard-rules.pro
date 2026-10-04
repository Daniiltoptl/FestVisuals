# Obfuscation for the FestVisuals jar. Renames and repackages our own classes and strips all debug
# information so the shipped jar does not decompile into readable source. Everything the game loads
# by name — entrypoints, mixins, annotations read by reflection — is kept, and third-party code
# bundled into the jar is left completely untouched.

# Rename and repackage, but do not shrink or optimise: shrinking can drop a class only a mixin
# config names, and optimisation rewrites control flow in ways that upset Mixin's bytecode.
-dontshrink
-dontoptimize
-allowaccessmodification
-repackageclasses ''
-flattenpackagehierarchy ''
-overloadaggressively

# No source file names, no line tables, no parameter names: decompilers get nothing but structure.
-renamesourcefileattribute ''
-keepattributes !SourceFile,!SourceDir,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable,*Annotation*,Signature,InnerClasses,EnclosingMethod,PermittedSubclasses,Exceptions

# ProGuard lacks the full module graph for Minecraft/Fabric; references resolve at runtime via the
# mod loader, so missing-reference and duplicate-class noise from the library jars is not fatal.
-dontnote
-dontwarn
-ignorewarnings

# --- Everything that is not ours stays byte-for-byte (bundled Discord RPC, shine, atmospherics,
#     and the nested performance mods in META-INF/jars are left alone). ---
-keep class !com.fest.visuals.** { *; }
-keepclassmembers class !com.fest.visuals.** { *; }

# --- Fabric entry points (named in fabric.mod.json). ---
-keep class com.fest.visuals.FestVisuals { *; }
-keep class com.fest.visuals.FestPreLaunch { *; }

# --- Mixins: the config lists them by name and @Shadow/@Accessor members must keep the names that
#     line up with their targets, so freeze the whole inject package. ---
-keep class com.fest.visuals.inject.** { *; }

# --- Annotations our own code reads at runtime (module/command registration carry name + desc). ---
-keep @interface com.fest.visuals.** { *; }

# --- Enums may be looked up by name. ---
-keepclassmembers enum com.fest.visuals.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Duck-typed interfaces that mixins implement on Minecraft classes (the *Extractor / accessor
#     interfaces our code casts to). Keeping their member names keeps them matching the kept mixin
#     members; the classes themselves are still renamed. ---
-keepnames interface com.fest.visuals.** { *; }
-keepclassmembernames interface com.fest.visuals.** { *; }
-dontwarn **
-ignorewarnings
-dontwarn net.minecraft.network.chat.Component
