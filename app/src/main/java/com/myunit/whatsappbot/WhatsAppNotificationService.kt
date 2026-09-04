package com.myunit.whatsappbot

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlin.concurrent.thread

class WhatsAppNotificationService : NotificationListenerService() {

    private val processedMessages = LinkedHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
            val extras = sbn.notification.extras
            val messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
            val senderName = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""

            val msgKey = "$senderName:$messageText"
            val currentTime = System.currentTimeMillis()
            if (processedMessages.containsKey(msgKey) && (currentTime - processedMessages[msgKey]!! < 5000)) {
                return
            }
            processedMessages[msgKey] = currentTime

            val prefs = getSharedPreferences("BotSettings", Context.MODE_PRIVATE)
            val allowPrivate = prefs.getBoolean("allow_private", true)
            val allowGroup = prefs.getBoolean("allow_group", false)

            val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) 
                    || extras.containsKey("android.hiddenConversationTitle")

            if (isGroup && !allowGroup) return
            if (!isGroup && !allowPrivate) return

            val actions = sbn.notification.actions ?: return
            for (action in actions) {
                val remoteInputs = action.remoteInputs ?: continue
                for (remoteInput in remoteInputs) {
                    if (remoteInput.resultKey.contains("reply", ignoreCase = true) || remoteInput.allowFreeFormInput) {
                        
                        thread {
                            val replyText = fetchReplyFromPython(senderName, messageText)
                            if (!replyText.isNullOrEmpty()) {
                                executeDirectReply(action, remoteInput, replyText)
                            }
                        }
                        break
                    }
                }
            }
        }
    }

    private fun fetchReplyFromPython(sender: String, message: String): String? {
        return try {
            val url = URL("http://127.0.0.1:5000/webhook")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 3000
            conn.readTimeout = 4000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("query", JSONObject().apply {
                    put("sender", sender)
                    put("message", message)
                })
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.buffering().readText()
                val jsonRes = JSONObject(responseStr)
                val replies = jsonRes.optJSONArray("replies")
                if (replies != null && replies.length() > 0) {
                    replies.getJSONObject(0).optString("text", null)
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun executeDirectReply(action: Notification.Action, remoteInput: RemoteInput, replyText: String) {
        try {
            val intent = Intent()
            val bundle = Bundle()
            bundle.putCharSequence(remoteInput.resultKey, replyText)
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
            action.actionIntent.send(this, 0, intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
