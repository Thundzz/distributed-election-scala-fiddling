package example

import java.time.LocalDateTime

sealed trait Message

case object KillMessage extends Message

case object NoopMessage extends Message

case class Election(electionId: String, initiator: Int, startOfElection: LocalDateTime) extends Message

case class Ok(electionId: String) extends Message

case class Coordinator(id: Int) extends Message

case class Work(initiator: Int, task: String) extends Message