package de.optools;

import com.mojang.blaze3d.platform.InputConstants;
import de.optools.chat.AuctionLinkAction;
import de.optools.chat.ChatActionRegistry;
import de.optools.chat.PlayerNameAction;
import de.optools.commands.OpToolsCommands;
import de.optools.commands.ShortcutRegistry;
import de.optools.config.ConfigManager;
import de.optools.config.OpToolsConfig;
import de.optools.finance.FinanceBook;
import de.optools.gui.screen.FirstStartScreen;
import de.optools.gui.screen.MainScreen;
import de.optools.hud.HudManager;
import de.optools.jobs.JobTracker;
import de.optools.market.MarketService;
import de.optools.market.api.OpsuchtApiClient;
import de.optools.opsucht.OpsuchtPatterns;
import de.optools.opsucht.PatternRepository;
import de.optools.opsucht.parse.ChatLineParser;
import de.optools.opsucht.parse.JobActionbarParser;
import de.optools.opsucht.parse.PaymentParser;
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
import java.util.function.Supplier;

/**
 * OP Tools – client entrypoint. Builds the module graph and connects it to Fabric events. All OPSUCHT-specific
 * parsing lives in {@code de.optools.opsucht}; feature modules only consume parsed events.
 */
public final class OpTools implements ClientModInitializer {
	public static final String MOD_ID = "optools";
	public static final String VERSION = "0.1 Alpha";
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

		jobTracker = new JobTracker(() -> config().jobs, System::currentTimeMillis);
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
			// Actionbar messages are handled in GuiMixin (covers both packet types), see onActionbar().
			if (!overlay) onSystemMessage(message);
		});

		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
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

	/** Called for every actionbar text (from {@code GuiMixin}). */
	public void onActionbar(Component message) {
		if (!config().modules.jobTracker || !isActiveServer()) return;
		jobParser.parse(message.getString()).ifPresent(jobTracker::accept);
	}

	/** Non-overlay system messages: payments, and job messages if OPSUCHT shows them in chat. */
	private void onSystemMessage(Component message) {
		if (!isActiveServer()) return;
		String plain = TextUtil.stripFormatting(message.getString());
		if (chatLineParser.parse(plain).isPresent()) return; // written by a player – never trusted
		if (config().modules.finance) {
			paymentParser.parse(plain).ifPresent(financeBook::onPayment);
		}
		if (config().modules.jobTracker) {
			jobParser.parse(plain).ifPresent(jobTracker::accept);
		}
	}

	/** Applies chat actions (from {@code ChatComponentMixin}). */
	public Component decorateChat(Component message) {
		return chatActions.process(message);
	}

	/** Explains how a line would be parsed ({@code /optools debug <text>}). */
	public String debugParse(String text) {
		StringBuilder sb = new StringBuilder();
		jobParser.parse(text).ifPresentOrElse(g -> sb.append("Job: ").append(g.job()).append(" L").append(g.level())
						.append(" +").append(g.xp()).append(" XP +").append(g.money()).append(" $ ").append(g.progress()).append("% | "),
				() -> sb.append("kein Job-Treffer | "));
		paymentParser.parse(text).ifPresentOrElse(p -> sb.append(p.incoming() ? "Eingang " : "Ausgang ").append(p.amount())
				.append(" ").append(p.player()).append(" | "), () -> sb.append("keine Zahlung | "));
		chatLineParser.parse(text).ifPresentOrElse(c -> sb.append("Spieler-Chat von ").append(c.name()),
				() -> sb.append("kein Spieler-Chat"));
		return sb.toString();
	}

	public boolean isActiveServer() {
		if (!config().general.onlyOnOpsucht) return true;
		ServerData server = Minecraft.getInstance().getCurrentServer();
		return server != null && patternRepository.get().isOpsuchtAddress(server.ip);
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
