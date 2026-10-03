package de.optools.storage;

import de.optools.storage.model.FinanceEntry;
import de.optools.storage.model.JobSessionRecord;

import java.io.IOException;
import java.util.List;

/**
 * Placeholder for a self-hosted OP Tools backend ("Coming Soon").
 *
 * <p>Planned design: the local store stays the source of truth while playing (offline capable); this provider
 * would push/pull {@link de.optools.storage.model.SyncRecord}s by {@code id}/{@code updatedAt} against the
 * configured server. It is intentionally not functional in 0.1 Alpha and is never selected by
 * {@link DataStore}.</p>
 */
public final class RemoteDataProvider implements DataProvider {
	private final String address;

	public RemoteDataProvider(String address) {
		this.address = address;
	}

	public String address() {
		return address;
	}

	@Override
	public String id() {
		return "remote";
	}

	@Override
	public String displayName() {
		return "Eigener OP Tools Server (Coming Soon)";
	}

	@Override
	public boolean isAvailable() {
		return false;
	}

	private static IOException unsupported() {
		return new IOException("Der OP Tools Server ist in Version 0.1 Alpha noch nicht verfügbar.");
	}

	@Override
	public List<JobSessionRecord> loadJobSessions() throws IOException {
		throw unsupported();
	}

	@Override
	public List<FinanceEntry> loadFinanceEntries() throws IOException {
		throw unsupported();
	}

	@Override
	public void saveJobSessions(List<JobSessionRecord> sessions) throws IOException {
		throw unsupported();
	}

	@Override
	public void saveFinanceEntries(List<FinanceEntry> entries) throws IOException {
		throw unsupported();
	}

	@Override
	public String deviceId() {
		return "";
	}
}
