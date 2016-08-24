package sbt

import sbt.internal.util.CacheImplicits._

import sbt.internal.util.{ HList, HCons, HNil, LinePosition, LineRange, NoPosition, RangePosition, SourcePosition }

import sbt.internal.util.Types.:+:

import sjsonnew.{ JsonFormat, LList, LNil, Builder, Unbuilder, deserializationError }
import sjsonnew.LList.:*:
import sjsonnew.BasicJsonProtocol.{ asSingleton, unionFormat3 }

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, File, InputStream, OutputStream }

trait OtherFormats {

  implicit def NoPositionFormat = asSingleton(NoPosition)

  implicit def LinePositionFormat = LList.iso(
    { l: LinePosition => ("path", l.path) :*: ("startLine", l.startLine) :*: LNil },
    { in: String :*: Int :*: LNil => new LinePosition(in.head, in.tail.head) }
  )

  implicit def LineRangeFormat = LList.iso(
    { l: LineRange => ("start", l.start) :*: ("end", l.end) :*: LNil },
    { in: Int :*: Int :*: LNil => new LineRange(in.head, in.tail.head) }
  )

  implicit def RangePositionFormat = LList.iso(
    { r: RangePosition => ("path", r.path) :*: ("range", r.range) :*: LNil },
    { in: String :*: LineRange :*: LNil => new RangePosition(in.head, in.tail.head) }
  )

  implicit def SourcePositionFormat = unionFormat3[SourcePosition, NoPosition.type, LinePosition, RangePosition]

  implicit def HConsFormat[H: JsonFormat, T <: HList: JsonFormat]: JsonFormat[H :+: T] =
    new JsonFormat[H :+: T] {
      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): H :+: T =
        jsOpt match {
          case Some(js) =>
            unbuilder.beginObject(js)
            val h = unbuilder.readField[H]("h")
            val t = unbuilder.readField[T]("t")
            unbuilder.endObject()

            HCons(h, t)

          case None =>
            deserializationError("Expect JValue but found None")
        }

      override def write[J](obj: H :+: T, builder: Builder[J]): Unit = {
        builder.beginObject()
        builder.addField("h", obj.head)
        builder.addField("t", obj.tail)
        builder.endObject()
      }
    }

  implicit val HNilFormat: JsonFormat[HNil] = asSingleton(HNil)

  implicit def streamFormat[T](write: (T, OutputStream) => Unit, read: InputStream => T): JsonFormat[T] = {
    lazy val byteArrayFormat = implicitly[JsonFormat[Array[Byte]]]
    val toBytes = (t: T) => { val bos = new ByteArrayOutputStream(); write(t, bos); bos.toByteArray }
    val fromBytes = (bs: Array[Byte]) => read(new ByteArrayInputStream(bs))

    new JsonFormat[T] {
      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): T =
        fromBytes(byteArrayFormat.read(jsOpt, unbuilder))

      override def write[J](obj: T, builder: Builder[J]): Unit =
        byteArrayFormat.write(toBytes(obj), builder)
    }
  }
}