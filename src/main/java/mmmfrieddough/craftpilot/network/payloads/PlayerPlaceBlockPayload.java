package mmmfrieddough.craftpilot.network.payloads;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import io.netty.buffer.ByteBuf;

public record PlayerPlaceBlockPayload(Hand hand, BlockPos blockPos, BlockState blockState) implements CustomPayload {

    private static final Identifier PLACE_BLOCK_PACKET_ID = Identifier.of("craftpilot", "place_block_packet");
    public static final CustomPayload.Id<PlayerPlaceBlockPayload> ID = new CustomPayload.Id<>(PLACE_BLOCK_PACKET_ID);

    private static final PacketCodec<ByteBuf, Hand> HAND_CODEC = PacketCodec.ofStatic(
            (buf, hand) -> buf.writeByte(hand.ordinal()),
            buf -> Hand.values()[buf.readByte()]);

    private static final PacketCodec<ByteBuf, BlockState> BLOCKSTATE_CODEC = PacketCodec.ofStatic(
            (buf, state) -> {
                int stateId = Block.getRawIdFromState(state);
                buf.writeInt(stateId);
            },
            buf -> {
                int stateId = buf.readInt();
                return Block.getStateFromRawId(stateId);
            });

    public static final PacketCodec<ByteBuf, PlayerPlaceBlockPayload> CODEC = PacketCodec.tuple(
            HAND_CODEC, PlayerPlaceBlockPayload::hand,
            BlockPos.PACKET_CODEC, PlayerPlaceBlockPayload::blockPos,
            BLOCKSTATE_CODEC, PlayerPlaceBlockPayload::blockState,
            PlayerPlaceBlockPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
