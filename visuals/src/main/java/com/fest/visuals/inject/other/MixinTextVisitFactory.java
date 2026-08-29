package com.fest.visuals.inject.other;

import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.fest.visuals.api.system.backend.SharedClass;
import com.fest.visuals.api.utils.other.ReplaceUtil;
import com.fest.visuals.client.features.modules.utility.NameProtectModule;

@Mixin(StringDecomposer.class)
public class MixinTextVisitFactory {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", ordinal = 0), method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", index = 0)
    private static String visitFormatted(String string) {
        if (!NameProtectModule.getInstance().isEnabled() || SharedClass.player() == null) {
            return string;
        }

        return ReplaceUtil.protectedString(string);
    }
}
