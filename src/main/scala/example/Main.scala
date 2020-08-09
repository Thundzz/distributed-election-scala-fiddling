package example

import scala.util.chaining._

object WorkerManager {
  val identifiers: Seq[Int] = List.range(0, 10)
  val workers: Seq[Worker] = identifiers.map(id => new Worker(id, identifiers))

  def getWorker(id: Int): Worker = workers(id)
}

object Main extends App {
  val threads: Seq[Thread] = WorkerManager.workers.map(w => {
    new Thread(w).tap(_.start())
  })
  threads.foreach(_.join())
}

