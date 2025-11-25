package com.rabimi.javaskinchanger.mixin;

import com.rabimi.javaskinchanger.SkinCache;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntityRenderer.class)
public class PlayerSkinMixin {

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTexture()Lnet/minecraft/util/Identifier;"
            )
    )
    private Identifier overrideSkin(AbstractClientPlayerEntity player) {
        return (SkinCache.customSkin != null)
                ? SkinCache.customSkin
                : player.getSkinTexture();
    }
}