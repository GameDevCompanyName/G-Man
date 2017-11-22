import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import game.Intellect
import game.State
import gamedev.protocol.Protocol
import gamedev.protocol.data.*

object Arguments {
    @Option(name = "-u", usage = "Specify server url")
    var url: String = ""

    @Option(name = "-p", usage = "Specify server port")
    var port: Int = -1

    fun use(args: Array<String>): Arguments =
            CmdLineParser(this).parseArgument(*args).let{ this }
}

fun main(args: Array<String>) {
    Arguments.use(args)

    println("..............*nothing but silence*...............")

    // Протокол обмена с сервером
    val protocol = Protocol(Arguments.url, Arguments.port)

    // Состояние игрового поля
    val gameState = State()

    protocol.handShake("Gordon Freeman")
    val setupData = protocol.setup()
    gameState.init(setupData)

    val intellect = Intellect(gameState, protocol, setupData.punters)

    println("Received id = ${setupData.punter}")

    protocol.ready()

    gameloop@ while(true) {
        val message = protocol.serverMessage()
        when(message) {
            is GameResult -> {
                println(".........")
                val myScore = message.stop.scores[protocol.myId]
                println("Score: ${myScore.score}")
                break@gameloop
            }
            is Timeout -> {
                intellect.timeout()
                println("Forget about freeman...")
            }
            is GameTurnMessage -> {
                for(move in message.move.moves) {
                    when(move) {
                        is PassMove -> {}
                        is ClaimMove -> {
                            gameState.update(move.claim)
                            intellect.update(move.claim)
                        }
                    }
                }
            }
        }

        intellect.makeMove()
    }
}
