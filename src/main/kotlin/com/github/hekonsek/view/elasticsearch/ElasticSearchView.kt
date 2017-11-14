package com.github.hekonsek.view.elasticsearch

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.XContentType.JSON
import org.elasticsearch.transport.client.PreBuiltTransportClient
import java.net.InetAddress
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ElasticSearchView(clusterName: String, val dateFields: List<String>, val indexTypeResolver: (key : String?, Map<String, Any>) -> IndexType) {

    private val client: TransportClient

    private val mapper: ObjectMapper

    init {
        val settings = Settings.builder().put("cluster.name", clusterName).build()
        client = PreBuiltTransportClient(settings)
                .addTransportAddress(InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300))
        val grafanaTimestampFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        grafanaTimestampFormat.timeZone = TimeZone.getTimeZone("GMT")
        mapper = ObjectMapper().setDateFormat(grafanaTimestampFormat)
    }

    fun onEvent(key : String?, event : MutableMap<String, Any>) {
        val indexType = indexTypeResolver(key, event)
        dateFields.forEach {
            event[it] = Date(event[it] as Long)
        }
        val elasticJson = mapper.writeValueAsString(event)
        client.prepareIndex(indexType.index, indexType.type).setId(key).setSource(elasticJson, JSON).get()
    }

}

data class IndexType(val index : String, val type: String)