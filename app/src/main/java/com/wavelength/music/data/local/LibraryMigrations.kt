package com.wavelength.music.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Schemas 1–6 were shipped with destructive fallback. Migrate their existing tables in
// place, including early builds that gained the source column without a version bump.
internal val libraryMigrations: Array<Migration> = (1..6).map { oldVersion ->
    object : Migration(oldVersion, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS local_songs (
                    mediaStoreId INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, artist TEXT NOT NULL,
                    album TEXT NOT NULL, durationMs INTEGER NOT NULL, contentUri TEXT NOT NULL,
                    albumArtUri TEXT)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS playlists (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL,
                    isFolder INTEGER NOT NULL DEFAULT 0, parentFolderId INTEGER)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS playlist_tracks (
                    entryId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, playlistId INTEGER NOT NULL,
                    trackId TEXT NOT NULL, name TEXT NOT NULL, artist TEXT NOT NULL, albumArtUrl TEXT NOT NULL,
                    audioUrl TEXT NOT NULL, source TEXT NOT NULL, addedAt INTEGER NOT NULL,
                    position INTEGER NOT NULL DEFAULT 0)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS downloaded_tracks (
                    id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, artist TEXT NOT NULL, albumArtUrl TEXT NOT NULL,
                    filePath TEXT NOT NULL, sizeBytes INTEGER NOT NULL DEFAULT 0, source TEXT NOT NULL,
                    downloadedAt INTEGER NOT NULL)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS search_history (
                    query TEXT NOT NULL PRIMARY KEY, searchedAt INTEGER NOT NULL)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS play_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, trackId TEXT NOT NULL, name TEXT NOT NULL,
                    artist TEXT NOT NULL, albumArtUrl TEXT NOT NULL, audioUrl TEXT NOT NULL,
                    source TEXT NOT NULL, playedAt INTEGER NOT NULL)"""
            )

            db.addColumnIfMissing("favorite_tracks", "source", "TEXT NOT NULL DEFAULT 'JIOSAAVN'")
            db.addColumnIfMissing("recently_played", "source", "TEXT NOT NULL DEFAULT 'JIOSAAVN'")
            db.addColumnIfMissing("playlist_tracks", "position", "INTEGER NOT NULL DEFAULT 0")
            db.addColumnIfMissing("downloaded_tracks", "sizeBytes", "INTEGER NOT NULL DEFAULT 0")
            db.addColumnIfMissing("playlists", "isFolder", "INTEGER NOT NULL DEFAULT 0")
            db.addColumnIfMissing("playlists", "parentFolderId", "INTEGER")

            db.execSQL(
                """INSERT INTO play_events (trackId, name, artist, albumArtUrl, audioUrl, source, playedAt)
                    SELECT trackId, name, artist, albumArtUrl, audioUrl, source, timestamp
                    FROM recently_played ORDER BY timestamp, entryId"""
            )
            db.execSQL(
                """DELETE FROM recently_played WHERE entryId != (
                    SELECT latest.entryId FROM recently_played AS latest
                    WHERE latest.trackId = recently_played.trackId
                    ORDER BY latest.timestamp DESC, latest.entryId DESC LIMIT 1)"""
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recently_played_trackId ON recently_played (trackId)")
        }
    }
}.toTypedArray()

private fun SupportSQLiteDatabase.addColumnIfMissing(table: String, column: String, definition: String) {
    val exists = query("PRAGMA table_info($table)").use { cursor ->
        val index = cursor.getColumnIndexOrThrow("name")
        generateSequence { if (cursor.moveToNext()) cursor.getString(index) else null }.any { it == column }
    }
    if (!exists) execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
}
