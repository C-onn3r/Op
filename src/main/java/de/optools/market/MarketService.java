package de.optools.market;

import de.optools.OpTools;
import de.optools.market.api.HistoryPoint;
import de.optools.market.api.MarketCategory;
import de.optools.market.api.MarketItem;
import de.optools.market.api.MerchantRate;
import de.optools.market.api.OpsuchtApiClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Caches API results (the API itself caches for 60 s, so polling faster is pointless). All fields are replaced
 * atomically, so the UI can read them from the render thread without locking.
 */
public final class MarketService {
	private static final long HISTORY_TTL_MS = 5 * 60_000;

	private final OpsuchtApiClient api;
	private final Supplier<Integer> refreshSeconds;

	private volatile List<MarketItem> prices = List.of();
	private volatile List<MarketCategory> categories = List.of();
	private volatile List<MerchantRate> rates = List.of();
	private volatile long pricesUpdated;
	private volatile long ratesUpdated;
	private volatile String error;
	private volatile boolean loading;
	private final Map<String, CachedHistory> history = new ConcurrentHashMap<>();

	public MarketService(OpsuchtApiClient api, Supplier<Integer> refreshSeconds) {
		this.api = api;
		this.refreshSeconds = refreshSeconds;
	}

	/** Refreshes prices, categories and merchant rates if the cache is stale (or {@code force}). */
	public void refresh(boolean force) {
		long now = System.currentTimeMillis();
		long ttl = Math.max(30, refreshSeconds.get()) * 1000L;
		if (loading || (!force && now - pricesUpdated < ttl && now - ratesUpdated < ttl)) return;
		loading = true;
		CompletableFuture<Void> p = api.fetchPrices().thenAccept(list -> {
			prices = List.copyOf(list);
			pricesUpdated = System.currentTimeMillis();
		});
		CompletableFuture<Void> c = categories.isEmpty()
				? api.fetchCategories().thenAccept(list -> categories = List.copyOf(list))
				: CompletableFuture.completedFuture(null);
		CompletableFuture<Void> r = api.fetchMerchantRates().thenAccept(list -> {
			rates = List.copyOf(list);
			ratesUpdated = System.currentTimeMillis();
		});
		CompletableFuture.allOf(p, c, r).whenComplete((v, t) -> {
			loading = false;
			if (t != null) {
				error = "API nicht erreichbar: " + rootMessage(t);
				OpTools.LOG.warn("OPSUCHT-API: {}", rootMessage(t));
			} else {
				error = null;
			}
		});
	}

	/** Returns cached history (possibly null while loading) and triggers a fetch if needed. */
	public Map<String, List<HistoryPoint>> history(String material) {
		CachedHistory cached = history.get(material);
		long now = System.currentTimeMillis();
		if (cached == null || (!cached.loading && now - cached.fetched > HISTORY_TTL_MS)) {
			CachedHistory next = new CachedHistory(cached == null ? null : cached.data);
			next.loading = true;
			history.put(material, next);
			api.fetchHistory(material).whenComplete((data, t) -> {
				next.loading = false;
				next.fetched = System.currentTimeMillis();
				if (t == null) next.data = data;
				else next.error = rootMessage(t);
			});
			return next.data;
		}
		return cached.data;
	}

	public String historyError(String material) {
		CachedHistory c = history.get(material);
		return c == null ? null : c.error;
	}

	private static String rootMessage(Throwable t) {
		while (t.getCause() != null) t = t.getCause();
		return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
	}

	public List<MarketItem> prices() {
		return prices;
	}

	public List<MarketCategory> categories() {
		return categories;
	}

	public List<MerchantRate> rates() {
		return rates;
	}

	public long pricesUpdated() {
		return pricesUpdated;
	}

	public String error() {
		return error;
	}

	public boolean loading() {
		return loading;
	}

	private static final class CachedHistory {
		volatile Map<String, List<HistoryPoint>> data;
		volatile boolean loading;
		volatile long fetched;
		volatile String error;

		CachedHistory(Map<String, List<HistoryPoint>> data) {
			this.data = data;
		}
	}
}
