import java.io.*

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
            cpu = 512
            memory = 2048
        }
        kotlinScript { api ->
            println("Running in branch: " + api.gitBranch())

            val path = "~/build"
            val sharedFile = File(path)
            api.fileShare().put(sharedFile, "artifact")

            api.gradlew("build")
//            val recipient = MessageRecipient.Channel(ChatChannel.FromName("CI-Channel"))
//            val content = ChatMessage.Text("Build has completed - build number: " + api.executionNumber())
//            api.space().chats.messages.sendMessage(recipient, content)
        }
    }

    container("openjdk:11") {
        resources {
            cpu = 512
            memory = 2048
        }
        kotlinScript { api ->
            println("Running in branch: " + api.gitBranch())
            api.gradlew("test")
//            val recipient = MessageRecipient.Channel(ChatChannel.FromName("CI-Channel"))
//            val content = ChatMessage.Text("Test has completed - build number: " + api.executionNumber())
//            api.space().chats.messages.sendMessage(recipient, content)
        }
    }

}