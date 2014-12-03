package macros
import scala.language.experimental.macros
import scala.reflect.macros._

object Provider {
  def dummyMacro: Int = macro dummymacroImpl
  def dummymacroImpl(c: Context): c.Tree = {
    import c.universe._
    val res = Relay.relay
    q"$res"
  }
}