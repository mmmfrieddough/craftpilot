package mmmfrieddough.craftpilot.network.payloads;

import mmmfrieddough.craftpilot.Reference;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.network.codec.PacketCodec;
import io.netty.buffer.ByteBuf;

public record ModHandshakePayload() implements CustomPayload {
    public static final Identifier MOD_HANDSHAKE_ID = Identifier.of(Reference.MOD_ID, "handshake");
    public static final CustomPayload.Id<ModHandshakePayload> ID = new CustomPayload.Id<>(MOD_HANDSHAKE_ID);

    public static final PacketCodec<ByteBuf, ModHandshakePayload> CODEC = PacketCodec.ofStatic((buf, value) -> {
    }, buf -> new ModHandshakePayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}