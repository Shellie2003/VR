package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DeletedRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletedRecordDao {
    @Query("SELECT * FROM deleted_records")
    fun getAllTombstones(): Flow<List<DeletedRecord>>

    // IGNORE: re-recording the same deletion (e.g. merging a tombstone that already exists
    // locally, or a double-tap delete) is a harmless no-op thanks to the unique index.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTombstone(record: DeletedRecord)
}
