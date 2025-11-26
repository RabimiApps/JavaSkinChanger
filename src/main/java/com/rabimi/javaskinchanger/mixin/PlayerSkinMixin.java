package com.rabimi.javaskinchanger.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractClientPlayerEntity.class)
public class PlayerSkinMixin {

    @Redirect(
        method = "getSkinTexture", // Yarn mapping に合わせる
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTexture()Lnet/minecraft/util/Identifier;"
        )
    )
    private Identifier redirectSkin(AbstractClientPlayerEntity instance) {
        // 独自スキンがあれば返す
        Identifier custom = SkinManager.getCustomSkin(instance.getUuid());
        if (custom != null) return custom;

        // それ以外は元メソッド呼ぶ
        return instance.getClass().getMethod("getSkin", null) != null ?
                (Identifier) instance.getClass().getMethod("getSkin").invoke(instance)
                : null;
    }
}