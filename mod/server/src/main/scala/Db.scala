package scalaboot.server

import snunit.tapir.SNUnitIdServerInterpreter.*
import sttp.tapir.*
import roach.*
import roach.codecs.*
import scalanative.unsafe.*
import roach.*
import scala.util.Using
import scala.scalanative.unsafe.Zone
import roach.codecs.*

class Db(pool: Pool):
  def getAllRepos(using Zone): Vector[String] =
    pool.withLease {
      sql"select name from repositories".all(text)
    }
  def addRepo(
      name: String,
      readme_markdown: String,
      last_commit: String,
      headline: Option[String],
      summary: Option[String]
  )(using Zone) =
    pool.withLease {
      sql"""insert into 
      repositories(name, readme_markdown, 
        metadata, last_commit) 
      values 
      ($text, $text, '{}'::json, $text)"""
        .exec(
          ((name, readme_markdown), last_commit)
        )
    }
end Db

object Db:
  private def migrate(pool: Pool)(using Zone) =
    Migrate.all(pool)(
      ResourceFile("/v001.sql")
    )

  def use(connectionString: String)(f: Db => Unit)(using Zone) =
    Pool.single(connectionString) { pool =>
      migrate(pool)
      f(new Db(pool))
    }
end Db
