package de.optools.storage;

import de.optools.storage.model.FinanceEntry;
import de.optools.storage.model.JobSessionRecord;

import java.io.IOException;
import java.util.List;

/**
 * Persistence backend abstraction. 0.1 Alpha ships {@link LocalDataProvider}; a self-hosted OP Tools server can be
 * added later as another implementation (see {@link RemoteDataProvider}) without touching the feature modules,
 * which only talk to {@link DataStore}.
 */
public interface DataProvider {
	String id();

	String displayName();

	/** Whether the provider can actually be used in this build. */
	boolean isAvailable();

	List<JobSessionRecord> loadJobSessions() throws IOException;

	List<FinanceEntry> loadFinanceEntries() throws IOException;

	void saveJobSessions(List<JobSessionRecord> sessions) throws IOException;

	void saveFinanceEntries(List<FinanceEntry> entries) throws IOException;

	/** Stable random id of this installation, stamped into every record. */
	String deviceId();

	default void close() {
	}
}
