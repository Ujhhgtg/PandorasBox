package dev.ujhhgtg.pandorasbox.utils

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import dev.ujhhgtg.pandorasbox.BrowserHistory
import java.io.InputStream
import java.io.OutputStream

object BrowserHistorySerializer : Serializer<BrowserHistory> {
    override val defaultValue: BrowserHistory = BrowserHistory.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): BrowserHistory {
        try {
            return BrowserHistory.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: BrowserHistory, output: OutputStream) {
        t.writeTo(output)
    }
}
