package de.optools.storage.model;

import java.util.UUID;

/**
 * Base for every persisted record. The fields are what a later self-hosted backend needs for a simple
 * last-writer-wins synchronisation: a globally unique id, timestamps, the originating device and a tombstone flag.
 * Locally they cost almost nothing, so they are part of the model from day one.
 */
public abstract class SyncRecord {
	public String id = UUID.randomUUID().toString();
	public long createdAt = System.currentTimeMillis();
	public long updatedAt = createdAt;
	/** Random id of the installation that created the record (see {@code meta.json}). */
	public String deviceId = "";
	/** Soft delete, so deletions can be synchronised later. */
	public boolean deleted = false;

	public void touch() {
		updatedAt = System.currentTimeMillis();
	}
}
