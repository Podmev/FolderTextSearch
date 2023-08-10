job("Build and run tests") {
    startOn {
        gitPush { enabled = true }
        //schedule { "0 8 * * *"} every day at 8 am
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
                val channel = ChannelIdentifier.Channel(ChatChannel.FromName("CI-Channel"))
                val content = ChatMessage.Text("Build failed")
                api.space().chats.messages.sendMessage(channel = channel, content = content)
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
                val channel = ChannelIdentifier.Channel(ChatChannel.FromName("CI-Channel"))
                val content = ChatMessage.Text("Tests failed")
                api.space().chats.messages.sendMessage(channel = channel, content = content)
            }
        }
    }

}