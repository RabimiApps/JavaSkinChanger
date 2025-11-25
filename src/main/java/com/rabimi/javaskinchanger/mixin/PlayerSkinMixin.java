package com.rabimi.javaskinchanger.mixin;

import com.rabimi.javaskinchanger.SkinLibrary;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * NOTE:
 * - 本Mixinは target メソッド名が mappings によって異なるため、
 *   実際に使うメソッド名(getSkinTexture / getLocationSkin / getSkin)は
 *   あなたの yarn mappings に合わせて変更する必要があるかもしれません.
 *
 * - もしビルドエラー出たら、エラーのメソッド名を教えてください。即修正版を出します。
 */
@Mixin(AbstractClientPlayerEntity.class)
public class PlayerSkinMixin {

    // Redirect the method that returns player's skin Identifier.
    // The target below is an example; you might need to change method name & descriptor.
    @Redirect(
        method = "method_XXXX", // <-- replace with actual method name that calls AbstractClientPlayerEntity.getSkinTexture() or similar
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTexture()Lnet/minecraft/util/Identifier;"
        )
    )
    private Identifier redirectGetSkin(AbstractClientPlayerEntity instance) {
        // First give user-selected custom skin if exists
        String active = SkinLibrary.getActive();
        if (active != null && !active.isEmpty()) {
            try {
                // active stored as Identifier#toString earlier; try parse
                if (active.contains(":")) {
                    String[] parts = active.split("/", 2);
                    // attempt to parse creative ways
                }
                // quick attempt: try to create Identifier by splitting at namespace/path
                String s = active;
                if (s.startsWith("Identifier(")) {
                    // fallback parsing if weird
                }
                // easiest: last fragment after '/' is name used earlier: javaskinchanger/skin_...
                // Build Identifier we used when registering texture:
                if (active.contains("javaskinchanger")) {
                    String name = active;
                    if (name.contains("/")) name = name.substring(name.lastIndexOf('/')+1);
                    if (name.endsWith(".png")) name = name.substring(0, name.length()-4);
                    return new Identifier("javaskinchanger", name);
                }
            } catch (Exception ignored) {}
        }
        // otherwise fallback to original call (return original player's skin)
        return instance.getSkinTexture(); // may be the original method
    }
}