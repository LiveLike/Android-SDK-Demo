package com.livelike.engagementsdk.core.services.messaging.proxies

import com.google.gson.JsonObject
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.core.utils.LogLevel
import com.livelike.engagementsdk.core.utils.minimumLogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor

@RunWith(MockitoJUnitRunner::class)
class SynchronizedMessagingClientTest {

    @Mock
    private lateinit var messaingClient: MessagingClient

    private var timeSource: () -> EpochTime = {
        EpochTime(
            100L
        )
    }
    private lateinit var subject: SynchronizedMessagingClient
    private lateinit var listener: MessagingEventListener

    val dispatcher = TestCoroutineDispatcher()

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            minimumLogLevel = LogLevel.None
        }
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        Dispatchers.setMain(dispatcher)
        listener = mock(MessagingEventListener::class.java)
        subject = SynchronizedMessagingClient(messaingClient, timeSource, 86000L)
    }

    @After
    fun tearDown() {
//        dispatcher.cleanupTestCoroutines()
        Dispatchers.resetMain()
    }

    @Test
    fun `should publish event if timestamp zero`() {
        val clientMessage = ClientMessage(JsonObject(), "", EpochTime(0))
        subject.listener = listener
        subject.onClientMessageEvent(messaingClient, clientMessage)
        subject.processQueueForScheduledEvent()
        verify(listener).onClientMessageEvent(subject, clientMessage)
    }

    @Test
    fun `should not publish event if timestamp gt time`() {
        val clientMessage = ClientMessage(
            JsonObject(), "",
            EpochTime(timeSource.invoke().timeSinceEpochInMs + 50)
        )
        subject.listener = listener
        subject.onClientMessageEvent(messaingClient, clientMessage)
        subject.processQueueForScheduledEvent()
        verify(listener, never()).onClientMessageEvent(subject, clientMessage)
    }

    @Test
    fun `should publish event if timestamp lt time`() {
        val clientMessage = ClientMessage(
            JsonObject(), "",
            EpochTime(timeSource.invoke().timeSinceEpochInMs - 50)
        )
        subject.listener = listener
        subject.onClientMessageEvent(messaingClient, clientMessage)
        subject.processQueueForScheduledEvent()
        verify(listener).onClientMessageEvent(subject, clientMessage)
    }

    @Test
    fun `should publish past event even if event ahead of current video time is on top of sync queue`() {
        val clientMessage = ClientMessage(
            JsonObject(), "",
            EpochTime(timeSource.invoke().timeSinceEpochInMs + 50000)
        )
        val pastClientMessage = ClientMessage(
            JsonObject().apply { addProperty("id", 1) }, "",
            EpochTime(timeSource.invoke().timeSinceEpochInMs - 5000)
        )
        subject.listener = listener
        subject.onClientMessageEvent(messaingClient, clientMessage)
        subject.onClientMessageEvent(messaingClient, pastClientMessage)
        subject.processQueueForScheduledEvent()
        verify(listener).onClientMessageEvent(subject, pastClientMessage)
    }

    @Test
    fun `history load will publish past messages immediately and push future in queue `() {
        val clientMessage = ClientMessage(
            JsonObject(), "",
            EpochTime(timeSource.invoke().timeSinceEpochInMs + 50000)
        )
        val pastClientMessage = ClientMessage(
            JsonObject().apply { addProperty("id", 10) }, "",
            EpochTime(timeSource.invoke().timeSinceEpochInMs - 5000)
        )

        val eventList = mutableListOf<ClientMessage>()
        eventList.add(clientMessage)
        eventList.add(pastClientMessage)
        subject.listener = listener
        subject.onClientMessageEvents(messaingClient, eventList)

        val listCaptor =
            argumentCaptor<List<ClientMessage>>()
        val client =
            argumentCaptor<MessagingClient>()

        verify(listener).onClientMessageEvents(client.capture(), listCaptor.capture())

        assert(listCaptor.firstValue.size == 1)
        // matching id of message
        assert((listCaptor.firstValue[0] as ClientMessage).message.get("id").asInt == 10)
    }
}
