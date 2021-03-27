package com.bobmowzie.mowziesmobs.server.message;

import com.bobmowzie.mowziesmobs.client.sound.BlackPinkSound;
import com.bobmowzie.mowziesmobs.server.block.BlockGrottol;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class MessageBlackPinkInYourArea {
    private int entityID;

    public MessageBlackPinkInYourArea() {}

    public MessageBlackPinkInYourArea(AbstractMinecartEntity minecart) {
        this(minecart.getEntityId());
    }

    private MessageBlackPinkInYourArea(int entityId) {
        this.entityID = entityId;
    }

    public static void serialize(final MessageBlackPinkInYourArea message, final PacketBuffer buf) {
        buf.writeVarInt(message.entityID);
    }

    public static MessageBlackPinkInYourArea deserialize(final PacketBuffer buf) {
        final MessageBlackPinkInYourArea message = new MessageBlackPinkInYourArea();
        message.entityID = buf.readVarInt();
        return message;
    }

    public static class Handler implements BiConsumer<MessageBlackPinkInYourArea, Supplier<NetworkEvent.Context>> {
        @Override
        public void accept(final MessageBlackPinkInYourArea message, final Supplier<NetworkEvent.Context> contextSupplier) {
            final NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                World world = Minecraft.getInstance().world;
                Entity entity = world.getEntityByID(message.entityID);
                if (entity instanceof AbstractMinecartEntity) {
                    AbstractMinecartEntity minecart = (AbstractMinecartEntity) entity;
                    Minecraft.getInstance().getSoundHandler().play(new BlackPinkSound(minecart));
                    BlockState state = Blocks.STONE.getDefaultState()
                            .with(BlockGrottol.VARIANT, BlockGrottol.Variant.BLACK_PINK);
                    BlockPos pos = new BlockPos(minecart);
                    final float scale = 0.75F;
                    double x = minecart.getPosX(),
                            y = minecart.getPosY() + 0.375F + 0.5F + (minecart.getDefaultDisplayTileOffset() - 8) / 16.0F * scale,
                            z = minecart.getPosZ();
                    SoundType sound = state.getBlock().getSoundType(state, world, pos, minecart);
                    world.playSound(
                            x, y, z,
                            sound.getBreakSound(),
                            minecart.getSoundCategory(),
                            (sound.getVolume() + 1.0F) / 2.0F,
                            sound.getPitch() * 0.8F,
                            false
                    );
                    final int size = 3;
                    float offset =  -0.5F * scale;
                    for (int ix = 0; ix < size; ix++) {
                        for (int iy = 0; iy < size; iy++) {
                            for (int iz = 0; iz < size; iz++) {
                                double dx = (double) ix / size * scale;
                                double dy = (double) iy / size * scale;
                                double dz = (double) iz / size * scale;
                                Vec3d minecartMotion = minecart.getMotion();
                                Minecraft.getInstance().particles.addEffect(new DiggingParticle(
                                        world,
                                        x + dx + offset, y + dy + offset, z + dz + offset,
                                        dx + minecartMotion.getX(), dy + minecartMotion.getY(), dz + minecartMotion.getZ(),
                                        state
                                ) {}.setBlockPos(pos));
                            }
                        }
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }
}
