package net.iann.vanillareenchanted.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.iann.vanillareenchanted.enchantment.PlayerKnowledgeData;
import net.iann.vanillareenchanted.network.KnowledgeSync;
import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class VRCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("vr")
                        .then(Commands.literal("knowledge")
                                .executes(context -> knowledge(context.getSource()))
                        )
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(context -> unlock(
                                                        context.getSource(),
                                                        ResourceLocationArgument.getId(context, "enchantment"),
                                                        IntegerArgumentType.getInteger(context, "level")
                                                ))
                                        )
                                )
                        )
        );
    }

    private static int knowledge(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);

        if (knowledge.getAll().isEmpty()) {
            source.sendSuccess(
                    () -> net.minecraft.network.chat.Component.literal("No enchantment knowledge unlocked."),
                    false
            );

            return 1;
        }

        knowledge.getAll().forEach((id, level) -> {
            source.sendSuccess(
                    () -> net.minecraft.network.chat.Component.literal(id + " -> " + level),
                    false
            );
        });

        return 1;
    }

    private static int unlock(
            CommandSourceStack source,
            ResourceLocation enchantmentId,
            int level
    ) {
        ServerPlayer player = source.getPlayer();

        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        knowledge.setLevel(enchantmentId, level);

        // Important:
        // The command changes the SERVER copy of the attachment.
        // This sends the updated data to the CLIENT so the GUI can display it.
        KnowledgeSync.sendToClient(player);

        source.sendSuccess(
                () -> net.minecraft.network.chat.Component.literal(
                        "Unlocked " + enchantmentId + " level " + level
                ),
                false
        );

        return 1;
    }
}