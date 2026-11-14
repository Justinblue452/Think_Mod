package com.fun.client;

import com.fun.client.render.EntityWarningRenderer;
import com.fun.client.render.InfoHudRenderer;
import com.fun.client.render.NavigationArrowRenderer;
import com.fun.client.render.SessionHudRenderer;
import com.fun.client.util.*;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ThinkClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ConfigManager.load();
		KeyBindings.register();

		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR,
				Identifier.fromNamespaceAndPath("think", "nav_arrow"),
				(g, delta) -> { if (HudConfig.hudVisible && HudConfig.showArrow) NavigationArrowRenderer.render(g, delta); });

		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR,
				Identifier.fromNamespaceAndPath("think", "info_panel"),
				(g, delta) -> { if (HudConfig.hudVisible && HudConfig.showInfo) InfoHudRenderer.render(g, delta); });

		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR,
				Identifier.fromNamespaceAndPath("think", "session_hud"),
				(g, delta) -> { if (HudConfig.hudVisible) SessionHudRenderer.render(g, delta); });

		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR,
				Identifier.fromNamespaceAndPath("think", "entity_warning"),
				(g, delta) -> { if (HudConfig.hudVisible) EntityWarningRenderer.render(g, delta); });

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			KeyBindings.tick();
			StrongholdLocator.onClientTick();
			HudCache.update();
			EntityWarningDetector.update();
		});

		registerCommands();
	}

	private void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> {
			dispatcher.register(ClientCommands.literal("think")
					.then(ClientCommands.literal("reset")
							.executes(c -> { StrongholdLocator.reset(); c.getSource().sendFeedback(Component.translatable("think.command.reset")); return 1; }))
					.then(ClientCommands.literal("status")
							.executes(c -> {
								var src = c.getSource();
								if (StrongholdLocator.hasValidTarget())
									src.sendFeedback(Component.translatable("think.command.status.valid",
											String.format("%.1f", StrongholdLocator.getTargetX()),
											String.format("%.1f", StrongholdLocator.getTargetZ()),
											StrongholdLocator.getRayCount()));
								else
									src.sendFeedback(Component.translatable("think.command.status.invalid",
											StrongholdLocator.getLastEyeCount(), StrongholdLocator.getRayCount()));
								return 1;
							}))
					.then(ClientCommands.literal("hud")
							.then(ClientCommands.literal("toggle")
									.executes(c -> {
										HudConfig.hudVisible = !HudConfig.hudVisible;
										ConfigManager.save();
										c.getSource().sendFeedback(Component.translatable("think.command.hud",
												HudConfig.hudVisible ? Component.translatable("think.state.on") : Component.translatable("think.state.off")));
										return 1;
									})))
					.then(ClientCommands.literal("warn")
							.then(ClientCommands.literal("add")
									.then(ClientCommands.argument("entityId", StringArgumentType.word())
											.executes(c -> {
												String id = StringArgumentType.getString(c, "entityId").toLowerCase();
												if (!id.contains(":")) id = "minecraft:" + id;
												if (HudConfig.warningEntityTypes.add(id)) {
													ConfigManager.save();
													c.getSource().sendFeedback(Component.literal("§a[Think] 已添加警示实体: " + id));
												} else {
													c.getSource().sendFeedback(Component.literal("§e[Think] 该实体已在警示列表中。"));
												}
												return 1;
											})))
							.then(ClientCommands.literal("remove")
									.then(ClientCommands.argument("entityId", StringArgumentType.word())
											.executes(c -> {
												String id = StringArgumentType.getString(c, "entityId").toLowerCase();
												if (!id.contains(":")) id = "minecraft:" + id;
												if (HudConfig.warningEntityTypes.remove(id)) {
													ConfigManager.save();
													c.getSource().sendFeedback(Component.literal("§a[Think] 已移除警示实体: " + id));
												} else {
													c.getSource().sendFeedback(Component.literal("§e[Think] 未找到该实体。"));
												}
												return 1;
											})))
							.then(ClientCommands.literal("list")
									.executes(c -> {
										c.getSource().sendFeedback(Component.literal("§a[Think] 当前警示实体: " + String.join(", ", HudConfig.warningEntityTypes)));
										return 1;
									}))
							.then(ClientCommands.literal("range")
									.then(ClientCommands.argument("detect", DoubleArgumentType.doubleArg(1))
											.then(ClientCommands.argument("warning", DoubleArgumentType.doubleArg(1))
													.then(ClientCommands.argument("danger", DoubleArgumentType.doubleArg(1))
															.executes(c -> {
																double detect = DoubleArgumentType.getDouble(c, "detect");
																double warning = DoubleArgumentType.getDouble(c, "warning");
																double danger = DoubleArgumentType.getDouble(c, "danger");
																if (warning >= detect || danger >= warning) {
																	c.getSource().sendFeedback(Component.literal("§c[Think] 范围必须递减：侦测 > 警告 > 危险"));
																	return 0;
																}
																HudConfig.detectRange = detect;
																HudConfig.warningRange = warning;
																HudConfig.dangerRange = danger;
																ConfigManager.save();
																c.getSource().sendFeedback(Component.literal(String.format("§a[Think] 范围已更新: 侦测 %.1f, 警告 %.1f, 危险 %.1f", detect, warning, danger)));
																return 1;
															}))))))
					.then(ClientCommands.literal("coord")
							.then(ClientCommands.literal("toNether")
									.executes(c -> {
										var s = c.getSource(); BlockPos p = BlockPos.containing(s.getPosition());
										var r = CoordinateUtil.overworldToNether(p);
										s.sendFeedback(Component.translatable("think.command.coord.nether",
												String.format("%d, %d, %d", r.getX(), r.getY(), r.getZ())));
										return 1;
									}))
							.then(ClientCommands.literal("toOverworld")
									.executes(c -> {
										var s = c.getSource(); BlockPos p = BlockPos.containing(s.getPosition());
										var r = CoordinateUtil.netherToOverworld(p);
										s.sendFeedback(Component.translatable("think.command.coord.overworld",
												String.format("%d, %d, %d", r.getX(), r.getY(), r.getZ())));
										return 1;
									})))
					.then(ClientCommands.literal("tick")
							.then(ClientCommands.argument("ticks", IntegerArgumentType.integer())
									.executes(c -> {
										int t = IntegerArgumentType.getInteger(c, "ticks");
										c.getSource().sendFeedback(Component.translatable("think.command.tick",
												t, String.format("%.2f", CoordinateUtil.ticksToSeconds(t))));
										return 1;
									})))
					.then(ClientCommands.literal("max")
							.then(ClientCommands.argument("count", IntegerArgumentType.integer(1, 64))
									.executes(c -> {
										int n = IntegerArgumentType.getInteger(c, "count");
										HudConfig.maxWarningEntities = n;
										ConfigManager.save();
										c.getSource().sendFeedback(Component.literal("§a[Think] 同时显示警告实体的上限: " + n));
										return 1;
									})))
			);
		});
	}
}