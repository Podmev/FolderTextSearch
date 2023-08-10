import circlet.pipelines.script.ScriptApi

job("Build and run tests") {
    startOn {
        gitPush { enabled = true }
        //schedule { "0 8 * * *" } every day at 8 am
    }

    failOn {
        nonZeroExitCode { enabled = true }
        testFailed { enabled = true }
        outOfMemory { enabled = true }
    }

    container("openjdk:11") {
        resources {
            memory = 2048
        }
        kotlinScript { api ->
            try {
                println("Running in branch: " + api.gitBranch())
                api.gradlew("build")
            } catch (ex: Exception) {
                writeFailMessageToChat("Build failed", api, ex)
            }
        }
    }

    container("openjdk:11") {
        resources {
            memory = 2048
        }
        kotlinScript { api ->
            try {
                println("Running tests in branch: " + api.gitBranch())
                api.gradlew("test")
            } catch (ex: Exception) {
                writeFailMessageToChat("Test failed", api, ex)
            }
        }
    }

}

/**
 * Writing fail message to chat CI-Channel with topic and details
 * */
suspend fun writeFailMessageToChat(topic: String, api: ScriptApi, ex: Exception) {
    val message = "$topic in ${api.gitRepositoryName()}:" +
            " branch ${api.gitBranch()}" +
            ", revision:${api.gitRevision()}" +
            ", ${ex.message}"
    writeMessageToChat(message, api, "CI-Channel")
}

/**
 * Writing message to chat
 * */
suspend fun writeMessageToChat(message: String, api: ScriptApi, channelName: String) {
    val channel = ChannelIdentifier.Channel(ChatChannel.FromName(channelName))
    val content = ChatMessage.Text(message)
    api.space().chats.messages.sendMessage(channel = channel, content = content)
}