package com.livelike.engagementsdk.widget.services.messaging.pubnub

import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult

/**
 * This adapter class provides empty implementations of the methods from pubnub subscribe callbacks.
 * Any custom listener that cares only about a subset of the methods of this listener can simply
 * subclass this adapter class instead of implementing the class directly.
 **/
internal abstract class PubnubSubscribeCallbackAdapter : SubscribeCallback() {

    override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
    }

    override fun status(pubnub: PubNub, pnStatus: PNStatus) {
    }

    override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {
    }

    override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
    }

    override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {
    }

    override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
    }

    override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {
    }

    override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {
    }

    override fun channel(pubnub: PubNub, pnChannelMetadataResult: PNChannelMetadataResult) {
    }
}
