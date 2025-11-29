package com.rabimi.javaskinchanger.mixin;

import com.rabimi.javaskinchanger.SkinChangeScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractClientPlayer.class)
public class PlayerSkinMixin {

    @Redirect(method = "getSkinTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/PlayerRenderer;bindTexture(Lnet/minecraft/resources/ResourceLocation;)V"))
    private void redirectSkin(PlayerRenderer renderer, ResourceLocation original) {
        // SkinChangeScreenで選んだスキンがあれば返す
        ResourceLocation custom = SkinChangeScreen.getCustomSkin();
        if (custom != null) {
            renderer.bindTexture(custom);
        } else {
            renderer.bindTexture(original);
        }
    }
}