package functional

import scalaz._
import Scalaz._
import scala.util.matching.Regex.Match

case class Json(private val data: Map[String, Any]) {
  def apply(key: String): Option[Any] =
    data.contains(key).option(data(key))

  override def toString: String =
    data.mkString("{\n", "\n", "\n}")
}

object Json {
  type Error = String
  type JsonString = String
  type Parser[A, B] = State[A] => Option[State[B]]

  def parse(jsonText: JsonString)(implicit converters: Converter = new Converter()): Option[Json] =
    for {
      json <- Option(jsonText)
      state <- obj(State(json))
    } yield Json(state.data)

  private def expr(state: State[Any])(implicit converters: Converter): Option[State[Any]] =
    text(state) orElse arr(state) orElse obj(state)

  private def text[B](state: State[Any])(implicit converters: Converter): Option[State[B]] =
    quoted(state).map { text => state.advance(text.group(0).length, converters.convert(text.group(1))) }

  def quoted(state: State[Any]): Option[Match] =
    """^"(.+?)"""".r.findFirstMatchIn(state.jsonLeft)

  private def arr(state: State[Any])(implicit converters: Converter): Option[State[List[Any]]] =
    for {
      s1 <- symbol("[", state)
      s2 <- repeat(expr, s1, ",")
      s3 <- symbol("]", s2)
    } yield s3

  private def obj(state: State[Any])(implicit converters: Converter): Option[State[Map[String, Any]]] =
    for {
      s1 <- symbol("{", state)
      s2 <- repeat(tuple, s1, ",")
      s3 <- symbol("}", s2)
    } yield s3.mapData { _.toMap }

  private def repeat[B <: Any](parser: Parser[Any, B], state: State[Any], separator: String)(implicit converters: Converter): Option[State[List[B]]] = {
    val first = parser(state)
    if (first.isEmpty)
      state.newData(List()).some
    else {
      for {
          s1 <- first
          s2 <- symbol(separator, s1)
          s3 <- repeat(parser, s2, separator)
        } yield s3.mapData { s1.data +: _ }
    } orElse first.map { _.mapData { List(_) } }
  }

  private def tuple(state: State[Any])(implicit converters: Converter): Option[State[(String, Any)]] =
    for {
      s1 <- text[String](state)
      s2 <- symbol(":", s1)
      converters2 = converters.withDefault(s1.data)
      s3 <- expr(s2)(converters2)
    } yield s3.mapData(s1.data -> _)

  def symbol[A](symbol: String, state: State[A]): Option[State[A]] =
    state.jsonLeft.startsWith(symbol).option(state.advance(1))

  case class State[+T](jsonLeft: JsonString, data: T = null) {
    def advance(chars: Int) =
      State(jsonLeft.substring(chars), data)

    def advance[U](chars: Int, newData: U) =
      State(jsonLeft.substring(chars), newData)

    def newData[U](newData: U) =
      State(jsonLeft, newData)

    def mapData[U](f: T => U) =
      State(jsonLeft, f(data))
  }

  class Converter(converters: Map[String, String => Any] = Map(), defaultConverter: String => Any = _.toString) {
    def convert[B](a: String): B = defaultConverter(a).asInstanceOf[B]
    def withDefault(f: String => Any) = new Converter(converters, f)
    def withDefault(key: String) = converters.contains(key) ? new Converter(converters, converters(key)) | this
  }

  def toJson(state: State[Map[String, Any]]): Json = new Json(state.data)
}


