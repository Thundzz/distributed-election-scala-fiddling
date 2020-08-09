package example

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import scala.collection.mutable

class Worker(identifier: Int, others: Seq[Int]) extends Runnable {

  private var stopped = false
  private var state: State = Initialized()

  private val maxElectionDuration = Duration.ofSeconds(1)
  private val maxIdleDuration = Duration.ofSeconds(5)
  private val mailbox = new mutable.Queue[Message]()

  override def run(): Unit = {
    while (!stopped) {
      val messages = mailbox.dequeueAll(_ => true)
      messages.foreach(message => {
        (state, message) match {
          case (Disabled(), _) => ()
          case (_, Coordinator(id)) => startWorking(id)
          case (_, Election(electionId, initiator, _)) =>
            sendMessage(initiator, Ok(electionId))
            state = StartElection()
          case _ => ()
        }
      })
      state match {
        case Waiting() => waiting()
        case StartElection() => initElection()
        case Initialized() => initialized()
        case Coordinating() => coordinate()
        case Disabled() => actDisabled()
        case working: Working => doWork(working, messages)
        case electionState: Electing => electing(electionState, messages)
      }
      Thread.sleep(10)
    }
  }


  private def sendMessage(other: Int, message: Message) {
    WorkerManager.getWorker(other).receiveMessage(message)
  }

  private def receiveMessage(message: Message): Unit = {
    mailbox.enqueue(message)
  }

  private def actDisabled(): Unit = {
    Randomness.withProbability(1, 10000) {
      println(s"$identifier woke up")
      initElection()
    }
  }

  private def coordinate(): Unit = {
    Randomness.withProbability(1, 1000) {
      println(s"$identifier shutting down")
      this.state = Disabled()
    }.orElse(
      Randomness.withProbability(1, 100) {
        others.foreach(sendMessage(_, Work(identifier, "hello")))
      }
    )
  }

  private def initialized(): Unit = {
    Randomness.withProbability(1, 1000) {
      initElection()
    }
  }

  private def initElection(): Unit = {
    println(s"$identifier initialized election")
    val uuid = UUID.randomUUID().toString
    val startOfElection = LocalDateTime.now()
    this.state = Electing(uuid, LocalDateTime.now())
    others.filter(o => o > identifier).foreach({
      other =>
        sendMessage(other, Election(uuid, identifier, startOfElection))
    })
  }

  private def startCoordinating(): Unit = {
    println(s"$identifier proclaimed itself the master.")
    this.state = Coordinating()
    others.filter(o => o < identifier).foreach({
      other =>
        sendMessage(other, Coordinator(identifier))
    })
  }

  private def isElectionFinished(electionState: Electing) = {
    val durationSinceBeginning = Duration.between(electionState.startOfElection, LocalDateTime.now())
    durationSinceBeginning.compareTo(maxElectionDuration) >= 0
  }

  private def startWorking(id: Int): Unit = {
    println(s"I am $identifier and my new master is $id")
    this.state = Working(coordinator = id, LocalDateTime.now())
  }

  private def electing(electionState: Electing, messages: Seq[Message]): Unit = {
    if (isElectionFinished(electionState)) {
      startCoordinating()
    } else {
      messages.foreach({
        case Ok(electionId) if electionId == electionState.id =>
          this.state = Waiting()
        case _ => ()
      })
    }
  }

  def doWork(state: Working, messages: Seq[Message]): Unit = {
    val sinceLastMsg = Duration.between(state.lastProcessedMessageTime, LocalDateTime.now())
    if (sinceLastMsg.compareTo(maxIdleDuration) > 0) {
      initElection()
    } else {
      messages.foreach({
        case Work(initiator, task) =>
          if (state.coordinator == initiator) {
            println(s"I am $identifier : $task")
            this.state = state.copy(lastProcessedMessageTime = LocalDateTime.now())
          }

        case _ =>
      })
    }
  }

  def waiting(): Unit = {}

  private def stop(): Unit = {
    stopped = true
  }

}
