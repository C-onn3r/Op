package de.optools.market.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Client for the official OPSUCHT API ({@code https://api.opsucht.net}). Only documented endpoints are used:
 *
 * <ul>
 *   <li>{@code GET /market/prices} – {@code {category: {MATERIAL: [{orderSide, activeOrders, price}]}}}</li>
 *   <li>{@code GET /market/categories} – {@code [{name, material, icon}]}</li>
 *   <li>{@code GET /market/history/{material}} – {@code {HOURLY|DAILY|WEEKLY|MONTHLY: [{avgPrice, minPrice,
 *   maxPrice, items, transactions, timestamp}]}}</li>
 *   <li>{@code GET /merchant/rates} – {@code [{source, target, base, exchangeRate}]} (Rohstoffhändler / Shards)</li>
 * </ul>
 *
 * <p>The auction house endpoints are deliberately not used.</p>
 */
public final class OpsuchtApiClient {
	private static final String USER_AGENT = "OP-Tools/0.1-Alpha (Fabric mod; +https://github.com/c-onn3r/op)";

	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	private final Supplier<String> baseUrl;

	public OpsuchtApiClient(Supplier<String> baseUrl) {
		this.baseUrl = baseUrl;
	}

	private CompletableFuture<JsonElement> get(String path) {
		String base = baseUrl.get();
		if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
		HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
				.timeout(Duration.ofSeconds(15))
				.header("Accept", "application/json")
				.header("User-Agent", USER_AGENT)
				.GET().build();
		return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenApply(response -> {
					if (response.statusCode() != 200) {
						throw new ApiException("HTTP " + response.statusCode() + " für " + path);
					}
					return JsonParser.parseString(response.body());
				});
	}

	public CompletableFuture<List<MarketItem>> fetchPrices() {
		return get("/market/prices").thenApply(json -> {
			List<MarketItem> items = new ArrayList<>();
			if (!json.isJsonObject()) return items;
			for (Map.Entry<String, JsonElement> category : json.getAsJsonObject().entrySet()) {
				if (!category.getValue().isJsonObject()) continue;
				for (Map.Entry<String, JsonElement> material : category.getValue().getAsJsonObject().entrySet()) {
					items.add(parseItem(category.getKey(), material.getKey(), material.getValue()));
				}
			}
			return items;
		});
	}

	private static MarketItem parseItem(String category, String material, JsonElement orders) {
		OrderInfo buy = null, sell = null;
		if (orders.isJsonArray()) {
			for (JsonElement el : orders.getAsJsonArray()) {
				if (!el.isJsonObject()) continue;
				JsonObject o = el.getAsJsonObject();
				OrderInfo info = new OrderInfo(str(o, "orderSide"), (int) num(o, "activeOrders"), num(o, "price"));
				if ("BUY".equalsIgnoreCase(info.orderSide())) buy = info;
				else if ("SELL".equalsIgnoreCase(info.orderSide())) sell = info;
			}
		}
		return new MarketItem(material, category, buy, sell);
	}

	public CompletableFuture<List<MarketCategory>> fetchCategories() {
		return get("/market/categories").thenApply(json -> {
			List<MarketCategory> out = new ArrayList<>();
			if (!json.isJsonArray()) return out;
			for (JsonElement el : json.getAsJsonArray()) {
				if (!el.isJsonObject()) continue;
				JsonObject o = el.getAsJsonObject();
				out.add(new MarketCategory(str(o, "name"), str(o, "material"), str(o, "icon")));
			}
			return out;
		});
	}

	public CompletableFuture<Map<String, List<HistoryPoint>>> fetchHistory(String material) {
		String encoded = URLEncoder.encode(material.toUpperCase(Locale.ROOT), StandardCharsets.UTF_8);
		return get("/market/history/" + encoded).thenApply(json -> {
			Map<String, List<HistoryPoint>> out = new LinkedHashMap<>();
			if (!json.isJsonObject()) return out;
			for (Map.Entry<String, JsonElement> interval : json.getAsJsonObject().entrySet()) {
				List<HistoryPoint> points = new ArrayList<>();
				if (interval.getValue().isJsonArray()) {
					JsonArray arr = interval.getValue().getAsJsonArray();
					for (JsonElement el : arr) {
						if (!el.isJsonObject()) continue;
						JsonObject o = el.getAsJsonObject();
						points.add(new HistoryPoint(num(o, "avgPrice"), num(o, "minPrice"), num(o, "maxPrice"),
								(long) num(o, "items"), (long) num(o, "transactions"), str(o, "timestamp")));
					}
				}
				out.put(interval.getKey(), points);
			}
			return out;
		});
	}

	public CompletableFuture<List<MerchantRate>> fetchMerchantRates() {
		return get("/merchant/rates").thenApply(json -> {
			List<MerchantRate> out = new ArrayList<>();
			if (!json.isJsonArray()) return out;
			for (JsonElement el : json.getAsJsonArray()) {
				if (!el.isJsonObject()) continue;
				JsonObject o = el.getAsJsonObject();
				out.add(MerchantRate.of(str(o, "source"), str(o, "target"), num(o, "base"), num(o, "exchangeRate")));
			}
			return out;
		});
	}

	private static String str(JsonObject o, String key) {
		JsonElement e = o.get(key);
		return e == null || e.isJsonNull() ? "" : e.getAsString();
	}

	private static double num(JsonObject o, String key) {
		JsonElement e = o.get(key);
		try {
			return e == null || e.isJsonNull() ? Double.NaN : e.getAsDouble();
		} catch (RuntimeException ex) {
			return Double.NaN;
		}
	}

	public static final class ApiException extends RuntimeException {
		public ApiException(String message) {
			super(message);
		}
	}
}
