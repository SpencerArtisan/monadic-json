package functional

import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import functional.Json._

class JsonTest extends FunSpec with Inside with Matchers with MockitoSugar {
  it("should not parse empty string") {
    Json.parse("").isEmpty should be (true)
  }

  it("should not parse null") {
    Json.parse(null).isEmpty should be (true)
  }

  it("should not parse non-json") {
    Json.parse("invalid").isEmpty should be (true)
  }

  it("should parse empty json") {
    val json = Json.parse("{}")
    json.get("key") should be (None)
  }

  it("should parse json value") {
    val json = Json.parse("""{"key":"value"}""")
    json.get("key") should be (Some("value"))
  }

  it("should parse empty json array") {
    val json = Json.parse("""{"key":[]}""")
    json.get("key") should be (Some(List()))
  }

  it("should parse json array with empty object") {
    val json = Json.parse("""{"key":[{}]}""")
    json.get("key").get should be (List(Map()))
  }

  it("should parse json array with non-empty values") {
    val json = Json.parse("""{"key":["1"]}""")
    json.get("key").get should be (List("1"))
  }

  it("should parse json array with non-empty objects") {
    val json = Json.parse("""{"key":[{"key2":"value"}]}""")
    json.get("key").get should be (List(Map("key2" -> "value")))
  }

  it("should parse json value with spaces") {
    val json = Json.parse("""{"key":"a value"}""")
    json.get("key") should be (Some("a value"))
  }

  it("should parse two json values") {
    val json = Json.parse("""{"key1":"value1","key2":"value2"}""")
    json.get("key1") should be (Some("value1"))
    json.get("key2") should be (Some("value2"))
  }

  it("should parse multiple json values") {
    val json = Json.parse("""{"key1":"value1","key2":"value2","key3":"value3"}""")
    json.get.apply("key1") should be (Some("value1"))
    json.get.apply("key2") should be (Some("value2"))
    json.get.apply("key3") should be (Some("value3"))
  }

  it("should use a custom value converter") {
    val json = Json.parse("""{"key":"42"}""")(new Converter(Map("key" -> ((s:String) => s.toInt))))
    json.get("key") should be (Some(42))
  }

  it("should use a custom value converter 2") {
    val json = Json.parse("""{"key":"hello"}""")(new Converter(Map("key" -> ((s:String) => s.toUpperCase))))
    json.get("key") should be (Some("HELLO"))
  }
}