package example

import java.time.LocalDateTime

sealed trait State

case class Waiting() extends State

case class Initialized() extends State

case class StartElection() extends State

case class Working(coordinator: Int, lastProcessedMessageTime: LocalDateTime) extends State

case class Electing(id: String, startOfElection: LocalDateTime) extends State

case class Coordinating() extends State

case class Disabled() extends State