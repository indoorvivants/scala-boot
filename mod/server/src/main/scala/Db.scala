package scalaboot.server

import snunit.tapir.SNUnitIdServerInterpreter.*
import sttp.tapir.*
import roach.*
import roach.codecs.*
import roach.upickle.jsonOf as roach_json
import scalanative.unsafe.*
import roach.*
import scala.util.Using
import scala.scalanative.unsafe.Zone
import roach.codecs.*
import scalaboot.protocol.*

class Db(pool: Pool):
  val repository_fields =
    Fragment("name,last_commit,readme_markdown, metadata, headline,summary")
      .applied(
        (text ~ varchar ~ text ~ roach_json[Metadata] ~ text.opt ~ text.opt)
          .as[scalaboot.protocol.RepositoryInfo]
      )
  def getAllRepos(using Zone): Vector[RepositoryInfo] =
    pool.withLease {
      sql"select ${repository_fields.sql} from repositories".all(
        repository_fields.codec
      )
    }

  val search_result = (repository_fields.codec ~ float4).as[SearchResult]

  def search(query: String)(using Zone): Vector[SearchResult] =
    pool.withLease {

      sql"""
      select ${repository_fields.sql}, ts_rank(ts, to_tsquery('english', $varchar)) as rank 
      from repositories
      where ts @@ to_tsquery('english', $varchar)
      order by rank DESC;
      """
        .all(query -> query, search_result)
    }
  def addRepo(
      data: RepositoryInfo
  )(using Zone): Option[Int] =
    pool.withLease {
      sql"""insert into 
      repositories(${repository_fields.sql})) 
      values(${repository_fields}) returning repoId""".one(data, int4)
    }

end Db

object Db:
  private def migrate(pool: Pool)(using Zone) =
    Migrate.all(pool)(
      ResourceFile("/v001.sql"),
      ResourceFile("/v002.sql")
    )

  def use(connectionString: String)(f: Db => Unit)(using Zone) =
    Pool.single(connectionString) { pool =>
      migrate(pool)
      f(new Db(pool))
    }
end Db
