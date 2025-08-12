package de.beqare.quicktimer.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import com.mojang.brigadier.CommandDispatcher;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class Quicktimer implements ClientModInitializer {

    private enum Mode { OFF, UP, DOWN, PAUSED }

    private static Mode mode = Mode.OFF;
    private static int ticks = 0;
    private static int remainingTicks = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(this::tick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
    }

    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("timer")
                .then(literal("up").executes(ctx -> {
                    startUp();
                    return 1;
                }))
                .then(literal("down").then(argument("seconds", integer(1)).executes(ctx -> {
                    int sec = ctx.getArgument("seconds", Integer.class);
                    startDown(sec);
                    return 1;
                })))
                .then(literal("pause").executes(ctx -> {
                    pause();
                    return 1;
                }))
                .then(literal("stop").executes(ctx -> {
                    stop();
                    return 1;
                }))
        );
    }

    private void tick(MinecraftClient client) {
        if (mode == Mode.UP) {
            ticks++;
            sendActionBar(client, "§d§l" + getFormattedTime(ticks / 20));
        } else if (mode == Mode.DOWN) {
            remainingTicks--;
            sendActionBar(client, "§d§l" + getFormattedTime(remainingTicks / 20));
            if (remainingTicks <= 0) {
                stop();
                sendActionBar(client, "§c§lTimer expired!");
            }
        }
    }

    private void startUp() {
        mode = Mode.UP;
        ticks = 0;
    }

    private void startDown(int seconds) {
        mode = Mode.DOWN;
        remainingTicks = seconds * 20;
    }

    private void pause() {
        if (mode != Mode.OFF) {
            mode = Mode.PAUSED;
        }
    }

    private void stop() {
        mode = Mode.OFF;
        ticks = 0;
        remainingTicks = 0;
    }

    private void sendActionBar(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }

    private String getFormattedTime(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
