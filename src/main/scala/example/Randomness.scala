package example

import scala.util.Random

object Randomness {
  private val random = new Random()

  def withProbability[R](p: Int, over: Int)(f: => R): Option[R] = {
    val rolled = random.nextInt(over)
    Option.when(rolled <= p)(f)
  }

}
