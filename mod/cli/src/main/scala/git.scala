package scalaboot

import template.*

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import libgit.all.*
import os.RelPath

def clone(local: os.Path, remote: String, branch: Option[String] = None) =
  import libgit.all.*
  Zone:
    val repo = alloc[Ptr[git_repository]]()
    val opts = alloc[git_clone_options]()

    gitInit

    doOrError(
      git_clone_options_init(opts, 1.toUInt),
      "failed to init git clone options"
    )
    branch.foreach { br =>
      (!opts).checkout_branch = toCString(br)
    }

    doOrError(
      git_clone(repo, toCString(remote), toCString(local.toString), opts),
      "Failed to clone"
    )
end clone

def listFiles(local: os.Path): List[RelPath] =
  import libgit.all.*
  Zone:
    val index = alloc[Ptr[git_index]]()
    val repo = alloc[Ptr[git_repository]]()
    try
      gitInit

      doOrError(
        git_repository_open(repo, toCString(local.toString())),
        "failed to open repository"
      )

      doOrError(git_repository_index(index, !repo), "failed to open repository")

      val entries = List.newBuilder[os.RelPath]

      val entryCount = git_index_entrycount(!index)
      for i <- 0 until entryCount.toInt
      do
        val entry = git_index_get_byindex(!index, size_t(i.toCSize))
        entries += os.RelPath(fromCString((!entry).path))

      entries.result()
    finally
      git_index_free(!index)
      git_repository_free(!repo)
    end try
end listFiles

lazy val gitInit = git_libgit2_init()

private inline def doOrError(inline f: Int, inline prefix: String) =
  Err.assert(
    f == 0,
    prefix + ": " + fromCString((!git_error_last()).message)
  )
