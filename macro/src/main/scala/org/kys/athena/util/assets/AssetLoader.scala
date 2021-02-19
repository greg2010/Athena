package org.kys.athena.util.assets

import scala.annotation.compileTimeOnly
import scala.language.experimental.macros
import scala.reflect.macros.blackbox


@compileTimeOnly("enable macro paradise to expand macro annotations")
object AssetLoader {
  def require(path: String): String = macro requireImpl

  def requireImpl(c: blackbox.Context)(path: c.Expr[String]): c.Expr[String] = {
    import c.universe._
    path match {
      case Expr(Literal(_)) => {
        reify {
          JSImporter.require[String](s"../../src/main/resources" + path.splice)
        }
      }
      case _ => {
        throw new IllegalArgumentException("Cannot call require with non-literal values.\n" +
                                           "This will produce an invalid JS require that will likely break " +
                                           "things.")
      }
    }
    /*
    def eval[B](tree: Tree): B = c.eval[B](c.Expr[B](c.untypecheck(tree.duplicate)))
    path match {
      case Expr(Literal(Constant(s: String))) => {
        reify(JSImporter.require(s"../../src/main/resources" + path.splice))
      }
      case Expr(Select(This(TypeName(n)), TermName(f))) => {
        val pp = q"org.kys.athena.riot.api.dto.league.TierEnum.Challenger.toString"
        println(showRaw(pp))
        val expr = c.Expr[String](pp)
        println(c.eval(expr))
        reify(JSImporter.require(s"../../src/main/resourcestest123" + expr.splice))
      }
      case Expr(Apply(Select(x), y)) => {
        println("hi")
        x._1 match {
          case Select(z) => println(z._2)
        }
        println(showRaw(x))
        path
      }
      case _ => {
        println(showRaw(path))
        val pp = q"org.kys.athena.riot.api.dto.league.TierEnum.Challenger.toString"
        println(showRaw(pp))
        println(showRaw(c.eval(path)))
        path
      }
    }
    //val x = c.eval(c.Expr[String](c.untypecheck(path.tree.duplicate)))
    //println(showRaw(x))
    //JSImporter.require[String](s"../../src/main/resources${path.splice}")

    /*path match {
      case Literal(Constant(str: String)) => {
        reify {
          JSImporter.require[String](s"../../src/main/resources$str")
        }
      }
      case Ident(TermName(n)) => {
        val tree = q"$n"
        val expr = c.Expr[String](tree)
        requireImpl(c)(expr)
      }
    }*/*/
  }

}
