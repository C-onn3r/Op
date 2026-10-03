package de.optools;

import com.mojang.blaze3d.platform.InputConstants;
import de.optools.chat.AuctionLinkAction;
import de.optools.chat.ChatActionRegistry;
import de.optools.chat.PlayerNameAction;
import de.optools.commands.OpToolsCommands;
import de.optools.commands.ShortcutRegistry;
import de.optools.config.ConfigManager;
import de.optools.config.OpToolsConfig;
import de.optools.util.Fmt;
import de.optools.finance.FinanceBook;
import de.optools.gui.screen.FirstStartScreen;
import de.optools.gui.screen.MainScreen;
import de.optools.hud.HudManager;
import de.optools.jobs.JobTracker;
import de.optools.market.MarketService;
import de.optools.market.api.OpsuchtApiClient;
import de.optools.market.ShardTooltip;
import de.optools.opsucht.IncomingText;
import de.optools.opsucht.OpsuchtPatterns;
import de.optools.opsucht.PatternRepository;
import de.optools.opsucht.parse.ChatLine;
import de.optools.opsucht.parse.ChatLineParser;
import de.optools.opsucht.parse.JobActionbarParser;
import de.optools.opsucht.parse.PaymentParser;
import de.optools.opsucht.parse.RtpParser;
import de.optools.rtp.RtpTracker;
import de.optools.opsucht.parse.TextUtil;
import de.optools.storage.DataProvider;
import de.optools.storage.DataStore;
import de.optools.storage.LocalDataProvider;
import de.optools.storage.model.JobSessionRecord;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * OP Tools – client entrypoint. Builds the module graph and connects it to Fabric events. All OPSUCHT-specific
 * parsing lives in {@code de.optools.opsucht}; feature modules only consume parsed events.
 */
public final class OpTools implements ClientModInitializer {
	public static final String MOD_ID = "optools";
	public static final String VERSION = "0.2 Alpha";
	public static final Logger LOG = LoggerFactory.getLogger("OP Tools");

	private static OpTools instance;

	private Path dir;
	private ConfigManager configManager;
	private PatternRepository patternRepository;
	private ShortcutRegistry shortcuts;
	private DataStore dataStore;
	private JobTracker jobTracker;
	private FinanceBook financeBook;
	private MarketService marketService;
	private ChatActionRegistry chatActions;
	private HudManager hud;
	private JobActionbarParser jobParser;
	private PaymentParser paymentParser;
	private ChatLineParser chatLineParser;
	private RtpParser rtpParser;
	private RtpTracker rtpTracker;
	private final Map<UUID, String> bossTexts = new HashMap<>();
	private KeyMapping menuKey;
	private Supplier<Screen> pendingScreen;
	private boolean firstStartShown;

	public static OpTools get() {
		return instance;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		dir = FabricLoader.getInstance().getConfigDir().resolve("optools");

		configManager = new ConfigManager(dir);
		configManager.load();
		patternRepository = new PatternRepository(dir);
		patternRepository.load();
		shortcuts = new ShortcutRegistry(dir);
		shortcuts.load();

		dataStore = new DataStore(createProvider());
		dataStore.load();

		Supplier<OpsuchtPatterns> patterns = patternRepository::get;
		jobParser = new JobActionbarParser(patterns);
		paymentParser = new PaymentParser(patterns);
		chatLineParser = new ChatLineParser(patterns);
		rtpParser = new RtpParser(patterns);
		rtpTracker = new RtpTracker(System::currentTimeMillis);

		jobTracker = new JobTracker(() -> config().jobs, System::currentTimeMillis);
		Fmt.fullNumbers(() -> config().general.numberStyle == OpToolsConfig.NumberStyle.FULL);
		financeBook = new FinanceBook(dataStore, this::config);
		jobTracker.addListener(financeBook);
		jobTracker.addListener(new JobTracker.Listener() {
			@Override
			public void onSessionFinished(JobSessionRecord session) {
				dataStore.addJobSession(session, config().jobs.maxHistorySessions);
				dataStore.saveAsync();
			}
		});

		marketService = new MarketService(new OpsuchtApiClient(() -> config().market.apiBaseUrl),
				() -> config().market.refreshSeconds);

		chatActions = new ChatActionRegistry(chatLineParser, () -> config().modules.chatActions && isActiveServer());
		chatActions.register(new PlayerNameAction(() -> config().chat));
		chatActions.register(new AuctionLinkAction(() -> config().chat, patterns));

		hud = new HudManager(this);
		HudElementRegistry.addLast(id("hud"), hud::render);

		ShardTooltip.register(this);

		KeyMapping.Category category = KeyMapping.Category.register(id("main"));
		menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.optools.menu", InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_O, category));

		registerEvents();
		LOG.info("OP Tools {} geladen (Daten: {})", VERSION, dataStore.provider().displayName());
	}

	/** 0.1 Alpha: always local. A remote provider can be returned here once it exists. */
	private DataProvider createProvider() {
		return new LocalDataProvider(dir.resolve("data"));
	}

	private void registerEvents() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> OpToolsCommands.register(dispatcher));

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			// Actionbar messages are handled in GuiMixin (covers both packet types).
			if (!overlay) onIncoming(IncomingText.Source.SYSTEM_CHAT, message);
		});

		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			bossTexts.clear();
			rtpTracker.reset();
			jobTracker.finishSession();
			financeBook.flushJobIncome();
			dataStore.saveAsync();
		}));

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			jobTracker.finishSession();
			financeBook.flushJobIncome();
			configManager.save();
			dataStore.shutdown();
		});
	}

	private void onTick(Minecraft mc) {
		jobTracker.tick();
		rtpTracker.tick();

		if (pendingScreen != null) {
			Supplier<Screen> next = pendingScreen;
			pendingScreen = null;
			mc.setScreen(next.get());
		}

		if (!config().setupDone && !firstStartShown && mc.screen instanceof TitleScreen title) {
			firstStartShown = true;
			mc.setScreen(new FirstStartScreen(title));
		}

		while (menuKey.consumeClick()) {
			if (mc.screen == null) mc.setScreen(new MainScreen(MainScreen.Tab.OVERVIEW));
		}
	}

	// ---- message entry points (called from Fabric events / mixins) ----

	/** Boss bars are re-sent on every update; only changed texts are processed. */
	public void onBossbar(UUID id, Component name) {
		if (name == null) return;
		String plain = name.getString();
		if (plain.equals(bossTexts.put(id, plain))) return;
		onIncoming(IncomingText.Source.BOSSBAR, name);
	}

	/**
	 * Central entry point for every server text the mod looks at (from {@code GuiMixin}, {@code BossHealthOverlayMixin}
	 * and the Fabric system-message event). Dispatches to job tracker, finance book and RTP tracker.
	 */
	public void onIncoming(IncomingText.Source source, Component message) {
		if (!isActiveServer()) return;
		String plain = TextUtil.stripFormatting(message.getString());
		boolean chat = source == IncomingText.Source.SYSTEM_CHAT;
		ChatLine chatLine = chat ? chatLineParser.parse(plain).orElse(null) : null;
		// "RTP » ..." looks like "Name » text" – it is only player chat if the sender really is a player
		boolean fromPlayer = chatLine != null && isPlayerName(chatLine.name());

		if (config().modules.jobTracker && !fromPlayer) {
			// chat lines are parsed strictly so ordinary messages with "$" are not counted as job income
			jobParser.parse(plain, chat).ifPresent(jobTracker::accept);
		}
		// payments: never from anything that looks like a chat line (fake protection, unchanged)
		if (chat && chatLine == null && config().modules.finance) {
			paymentParser.parse(plain).ifPresent(financeBook::onPayment);
		}
		if (config().modules.rtp && !fromPlayer) {
			rtpParser.parse(plain).ifPresent(rtpTracker::accept);
		}
	}

	/** Nicknames ("~Nick") or names of players in the tab list. */
	private static boolean isPlayerName(String name) {
		if (name.startsWith("~")) return true;
		var connection = Minecraft.getInstance().getConnection();
		return connection != null && connection.getPlayerInfo(name) != null;
	}

	/** Applies chat actions (from {@code ChatComponentMixin}). */
	public Component decorateChat(Component message) {
		return chatActions.process(message);
	}

	public boolean isActiveServer() {
		if (!config().general.onlyOnOpsucht) return true;
		ServerData server = Minecraft.getInstance().getCurrentServer();
		return server != null && (patternRepository.get().isOpsuchtAddress(server.ip)
				|| patternRepository.get().isOpsuchtAddress(server.name));
	}

	public void openScreenNextTick(Supplier<Screen> screen) {
		pendingScreen = screen;
	}

	public void reloadFiles() {
		patternRepository.load();
		shortcuts.load();
	}

	// ---- accessors ----

	public OpToolsConfig config() {
		return configManager.get();
	}

	public void saveConfig() {
		configManager.save();
	}

	public Path dir() {
		return dir;
	}

	public OpsuchtPatterns patterns() {
		return patternRepository.get();
	}

	public PatternRepository patternRepository() {
		return patternRepository;
	}

	public ShortcutRegistry shortcuts() {
		return shortcuts;
	}

	public DataStore dataStore() {
		return dataStore;
	}

	public JobTracker jobTracker() {
		return jobTracker;
	}

	public FinanceBook financeBook() {
		return financeBook;
	}

	public RtpTracker rtpTracker() {
		return rtpTracker;
	}

	public MarketService market() {
		return marketService;
	}

	public ChatActionRegistry chatActions() {
		return chatActions;
	}

	public HudManager hud() {
		return hud;
	}

	public KeyMapping menuKey() {
		return menuKey;
	}
}
