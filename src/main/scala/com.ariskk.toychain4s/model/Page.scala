package com.ariskk.toychain4s.model

case class Cursor(value: String) extends AnyVal

case class Page(from: Option[Cursor], size: Int) {
  lazy val toUrlParams =
    s"${from.map(x => s"from=${x.value}&").getOrElse("")}size=$size"
}
