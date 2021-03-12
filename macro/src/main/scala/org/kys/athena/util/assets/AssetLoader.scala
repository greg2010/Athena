package org.kys.athena.util.assets

import scala.annotation.compileTimeOnly
import scala.language.experimental.macros
import scala.reflect.macros.blackbox


@compileTimeOnly("enable macro paradise to expand macro annotations")
object AssetLoader {
  def test: Unit = {}
  def require(path: String): String = macro requireImpl

  def requireImpl(c: blackbox.Context)(path: c.Expr[String]): c.Expr[String] = {
    import c.universe._
    def eval[B](tree: Tree): B = c.eval[B](c.Expr[B](c.untypecheck(tree.duplicate)))
    try {
      val pathStr = eval[String](path.tree)
      val expr = c.Expr[String](Literal(Constant(pathStr)))
      reify {
        JSImporter.require[String](s"../../src/main/resources" + expr.splice)
      }
    } catch {
      case e: Throwable =>
        c.abort(c.enclosingPosition,
                s"""
                   |Exception during require macro expansion.
                   |This method cannot be called with values not known at compile-time.
                   |This will produce an invalid JS require that will likely break things.
                   |Caused by: $e
                   |""".stripMargin)
    }
  }

}
