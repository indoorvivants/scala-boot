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
  import Db.codecs.*

  def search(query: String)(using Zone): Vector[SearchResult] =
    pool.withLease {

      sql"""
      select ${repository_summary.sql}, ts_rank(ts, websearch_to_tsquery('english', $varchar)) as rank 
      from repositories
      where 
        deleted = FALSE and 
        ts @@ websearch_to_tsquery('english', $varchar)
      order by rank DESC;
      """
        .all(query -> query, search_result)
    }

  def deleteRepo(id: Int)(using Zone) =
    pool.withLease {
      sql"update repositories set deleted = TRUE where repoId = $int4".exec(id)
    }

  def addRepo(
      data: RepositoryInfo
  )(using Zone): Option[Int] =
    pool.withLease {
      sql"""
      insert into 
        repositories(${repository_fields.sql}) 
      values(${repository_fields}) 
      returning 
        repoId"""
        .one(data, int4)
    }
  def getAllRepos(using Zone) =
    pool.withLease {
      sql"select repoId, ${repository_fields.sql} from repositories where deleted = FALSE"
        .all(
          savedRepository
        )
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

  private object codecs:
    lazy val search_result =
      (repository_summary.codec ~ float4).as[SearchResult]
    lazy val repository_fields =
      Fragment(
        "name,last_commit,readme_markdown, metadata, headline,summary,stars"
      )
        .applied(
          (text ~ varchar ~ text ~ roach_json[
            Metadata
          ] ~ text.opt ~ text.opt ~ int4)
            .as[scalaboot.protocol.RepositoryInfo]
        )

    lazy val repository_summary =
      Fragment(
        "name,headline,summary,stars"
      )
        .applied(
          (text ~ text.opt ~ text.opt ~ int4)
            .as[scalaboot.protocol.RepositorySummary]
        )

    lazy val savedRepository =
      (int4 ~ repository_fields.codec).as[SavedRepository]
  end codecs

end Db
