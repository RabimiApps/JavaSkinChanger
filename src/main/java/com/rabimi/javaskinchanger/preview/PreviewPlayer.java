package com.rabimi.javaskinchanger.preview;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

public class PreviewPlayer extends ClientPlayerEntity {

    private Identifier skinId;

    public PreviewPlayer(ClientWorld world, Identifier skin) {
        super(MinecraftClient.getInstance(), world,
              MinecraftClient.getInstance().getNetworkHandler().getProfile(),
              null);
        this.skinId = skin;
    }

    @Override
    public Identifier getSkinTexture() {
        return skinId;
    }

    public void setSkin(Identifier id) {
        this.skinId = id;
    }
}