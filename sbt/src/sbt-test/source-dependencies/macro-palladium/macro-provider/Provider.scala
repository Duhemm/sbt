import scala.language.experimental.macros
import scala.reflect.core._

object Provider {
  macro hello: String = Lit.String("Hello world !")
}