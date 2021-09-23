package com.livelike.engagementsdk.core.services.messaging.proxies

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.JsonObject
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logVerbose

internal class ImagePreloaderMessagingClient(
    upstream: MessagingClient,
    val context: Context
) :
    MessagingClientProxy(upstream) {
    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun start() {
        upstream.start()
    }

    override fun onClientMessageEvents(client: MessagingClient, events: List<ClientMessage>) {
    }

    private val processingList = mutableListOf<ImageMessage>()
    private val downloadedImages = mutableListOf<String>()

    class ImageMessage(
        val clientMessage: ClientMessage,
        val messagingClient: MessagingClient,
        val imageCount: Int,
        var imagePreloaded: Int = 0
    ) {
        override fun equals(other: Any?): Boolean {
            other as ImageMessage
            return other.clientMessage == this.clientMessage
        }

        override fun hashCode(): Int {
            var result = clientMessage.hashCode()
            result = 31 * result + messagingClient.hashCode()
            result = 31 * result + imageCount
            result = 31 * result + imagePreloaded
            return result
        }
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        logDebug { "Message received at ImagePreloaderMessagingClient" }
        val imageList = getImagesFromJson(event.message, mutableListOf())

        if (imageList.isEmpty()) {
            logVerbose { "No images in this widget." }
            listener?.onClientMessageEvent(client, event)
            return
        }

        val currentImageMessage = ImageMessage(event, client, imageList.size)

        processingList.add(currentImageMessage)
        imageList.forEach {
            Glide.with(context)
                .load(it)
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        updateProcessingList(currentImageMessage)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        updateProcessingList(currentImageMessage)
                        return false
                    }
                })
                .apply(
                    RequestOptions().override(
                        AndroidResource.dpToPx(74),
                        AndroidResource.dpToPx(74)
                    ).transform(
                        MultiTransformation(FitCenter(), RoundedCorners(12))
                    )
                ).preload()
        }
    }

    fun updateProcessingList(imageMessage: ImageMessage) {
        val msg = processingList.find { msg -> msg == imageMessage }
        processingList.remove(msg)
        msg?.let {
            msg.imagePreloaded++
            if (msg.imageCount == msg.imagePreloaded) {
                listener?.onClientMessageEvent(
                    imageMessage.messagingClient,
                    imageMessage.clientMessage
                )
            } else {
                processingList.add(msg)
            }
        }
    }

    private fun getImagesFromJson(
        jsonObject: JsonObject,
        imagesList: MutableList<String>
    ): MutableList<String> {
        val elements = jsonObject.entrySet()
        elements.forEach { element ->
            when {
                element.key == "image_url" -> {
                    if (!element.value.isJsonNull && !downloadedImages.contains(element.value.asString)) {
                        imagesList.add(element.value.asString)
                        downloadedImages.add(element.value.asString)
                    }
                }
                element.value.isJsonObject -> getImagesFromJson(
                    element.value.asJsonObject,
                    imagesList
                )
                element.value.isJsonArray -> element.value.asJsonArray.forEach {
                    if (it.isJsonObject) {
                        getImagesFromJson(it.asJsonObject, imagesList)
                    }
                }
            }
        }
        return imagesList
    }
}

internal fun MessagingClient.withPreloader(
    context: Context
): ImagePreloaderMessagingClient {
    return ImagePreloaderMessagingClient(this, context)
}
